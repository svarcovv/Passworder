package svarcovv.dev.passworder.storage;

import svarcovv.dev.passworder.model.VaultEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.crypto.AEADBadTagException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VaultStoreTest {
    @TempDir Path directory;
    private final char[] master = "тестовый мастер-пароль 123!".toCharArray();
    private VaultStore store() { return new VaultStore(directory.resolve("vault.pwr")); }
    private VaultEntry entry(String title) {
        Instant now = Instant.ofEpochMilli(1_700_000_000_000L);
        return new VaultEntry(UUID.randomUUID(), title, "тест@example.test", "Secret-测试-123!".toCharArray(),
                "https://example.test/private", true, now, now);
    }
    @Test void persistsEveryFieldAndNeverWritesPlaintext() throws Exception {
        UUID id;
        try (var session = store().create(master)) { var entry = entry("Пример 😺"); id = entry.id(); session.put(entry); }
        try (var session = store().open(master)) {
            var entry = session.entries().getFirst();
            assertEquals(id, entry.id()); assertEquals("Пример 😺", entry.title());
            assertEquals("тест@example.test", entry.login()); assertEquals("https://example.test/private", entry.url());
            assertArrayEquals("Secret-测试-123!".toCharArray(), entry.password()); assertTrue(entry.favorite());
            assertEquals(Instant.ofEpochMilli(1_700_000_000_000L), entry.created()); assertEquals(entry.created(), entry.updated());
        }
        byte[] bytes = Files.readAllBytes(store().path());
        String utf8 = new String(bytes, StandardCharsets.UTF_8), utf16 = new String(bytes, StandardCharsets.UTF_16BE);
        for (String plain : List.of("Пример", "example.test", "Secret-测试-123!", "тестовый мастер")) {
            assertFalse(utf8.contains(plain)); assertFalse(utf16.contains(plain));
        }
    }
    @Test void wrongMasterDoesNotChangeVault() throws Exception {
        try (var session = store().create(master)) { session.put(entry("Original")); }
        byte[] original = Files.readAllBytes(store().path());
        assertThrows(AEADBadTagException.class, () -> store().open("incorrect password".toCharArray()));
        assertArrayEquals(original, Files.readAllBytes(store().path()));
    }
    @Test void rejectsTamperedCiphertextAndAuthenticatedHeader() throws Exception {
        try (var ignored = store().create(master)) {}
        byte[] original = Files.readAllBytes(store().path());
        for (int index : new int[]{12, 45, 56, original.length - 1}) {
            byte[] damaged = original.clone(); damaged[index] ^= 1; Files.write(store().path(), damaged);
            assertThrows(AEADBadTagException.class, () -> store().open(master), "index=" + index);
        }
    }
    @Test void rejectsTruncationUnknownVersionAndExpensiveKdf() throws Exception {
        try (var ignored = store().create(master)) {}
        byte[] original = Files.readAllBytes(store().path());
        Files.write(store().path(), Arrays.copyOf(original, 10)); assertThrows(IOException.class, () -> store().open(master));
        byte[] unknown = original.clone(); unknown[7] = '9'; Files.write(store().path(), unknown); assertThrows(IOException.class, () -> store().open(master));
        ByteBuffer.wrap(original).putInt(8, Integer.MAX_VALUE); Files.write(store().path(), original); assertThrows(IOException.class, () -> store().open(master));
    }
    @Test void usesFreshNonceForEachSaveAndKeepsPreviousEncryptedState() throws Exception {
        try (var session = store().create(master)) {
            byte[] initial = Files.readAllBytes(store().path()); session.put(entry("New")); byte[] next = Files.readAllBytes(store().path());
            assertFalse(Arrays.equals(Arrays.copyOfRange(initial, 44, 56), Arrays.copyOfRange(next, 44, 56)));
            assertArrayEquals(initial, Files.readAllBytes(directory.resolve("vault.pwr.bak")));
        }
        try (var backup = new VaultStore(directory.resolve("vault.pwr.bak")).open(master)) { assertTrue(backup.entries().isEmpty()); }
    }
    @Test void editingAndDeletingRoundTrip() throws Exception {
        try (var session = store().create(master)) {
            var original = entry("Before"); session.put(original);
            session.put(new VaultEntry(original.id(), "After", "new", "updated!".toCharArray(), "", false, original.created(), Instant.now()));
            assertEquals(1, session.entries().size()); assertEquals("After", session.entries().getFirst().title());
            try (var reopened = store().open(master)) { assertArrayEquals("updated!".toCharArray(), reopened.entries().getFirst().password()); }
            session.delete(original.id()); assertTrue(session.entries().isEmpty());
        }
        try (var reopened = store().open(master)) { assertTrue(reopened.entries().isEmpty()); }
    }
    @Test void failedWriteKeepsOldEntryInMemory() throws Exception {
        try (var session = store().create(master)) {
            var entry = entry("Keep me"); session.put(entry);
            // A directory at the backup destination forces an IO failure before the vault replacement.
            Files.delete(directory.resolve("vault.pwr.bak")); Files.createDirectory(directory.resolve("vault.pwr.bak"));
            byte[] before = Files.readAllBytes(store().path());
            assertThrows(IOException.class, () -> session.delete(entry.id()));
            assertEquals("Keep me", session.entries().getFirst().title()); assertArrayEquals(before, Files.readAllBytes(store().path()));
        }
        try (var reopened = store().open(master)) { assertEquals(1, reopened.entries().size()); }
    }
    @Test void exportAndRestoreVerifyMasterBeforeReplacing() throws Exception {
        Path export = directory.resolve("export.pwr");
        try (var session = store().create(master)) { session.put(entry("Backup")); session.export(export); session.put(entry("Later")); }
        byte[] before = Files.readAllBytes(store().path());
        assertThrows(AEADBadTagException.class, () -> store().restore(export, "wrong".toCharArray()));
        assertArrayEquals(before, Files.readAllBytes(store().path()));
        try (var restored = store().restore(export, master)) { assertEquals(1, restored.entries().size()); assertEquals("Backup", restored.entries().getFirst().title()); }
        assertArrayEquals(before, Files.readAllBytes(directory.resolve("vault.pwr.bak")));
    }
    @Test void cannotOverwriteVaultWithExportOrCreation() throws Exception {
        try (var session = store().create(master)) {
            assertThrows(IOException.class, () -> session.export(store().path()));
            assertThrows(IOException.class, () -> store().create(master));
        }
    }
    @Test void closingSessionWipesEntryBuffersAndRejectsAccess() throws Exception {
        var session = store().create(master); var entry = entry("Close"); session.put(entry); session.close();
        assertThrows(IllegalStateException.class, session::entries); assertArrayEquals(new char[entry.password().length], entry.password()); session.close();
    }
    @Test void fileAndTemporaryBackupsAreNeverLeftAsPlaintext() throws Exception {
        try (var session = store().create(master)) { session.put(entry("Private title")); }
        try (var files = Files.list(directory)) { assertTrue(files.noneMatch(p -> p.toString().endsWith(".tmp"))); }
    }
    @Test void exportDoesNotChangeParentDirectoryPermissions() throws Exception {
        Path exportDirectory = Files.createDirectory(directory.resolve("existing"));
        var acl = Files.getFileAttributeView(exportDirectory, java.nio.file.attribute.AclFileAttributeView.class);
        var before = acl == null ? null : acl.getAcl();
        try (var session = store().create(master)) { session.export(exportDirectory.resolve("copy.pwr")); }
        if (acl != null) assertEquals(before, acl.getAcl());
    }
}
