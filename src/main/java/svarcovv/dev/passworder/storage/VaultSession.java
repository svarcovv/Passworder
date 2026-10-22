package svarcovv.dev.passworder.storage;

import svarcovv.dev.passworder.model.VaultEntry;
import java.io.IOException;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.*;

public final class VaultSession implements AutoCloseable {
    private final Path path;
    private final byte[] key, salt;
    private final int iterations;
    private List<VaultEntry> entries;
    private boolean closed;
    VaultSession(Path path, byte[] key, byte[] salt, int iterations, List<VaultEntry> entries) {
        this.path = path; this.key = key; this.salt = salt; this.iterations = iterations; this.entries = entries;
    }
    byte[] key() { requireOpen(); return key; }
    byte[] salt() { return salt; }
    int iterations() { return iterations; }
    public List<VaultEntry> entries() { requireOpen(); return Collections.unmodifiableList(entries); }
    void save() throws IOException, GeneralSecurityException { persist(entries); }
    private void persist(List<VaultEntry> candidate) throws IOException, GeneralSecurityException {
        requireOpen();
        byte[] file = VaultCodec.encrypt(candidate, key, salt, iterations);
        try { LocalFiles.atomicWrite(path, file, true); }
        finally { Arrays.fill(file, (byte) 0); }
    }
    public void put(VaultEntry entry) throws IOException, GeneralSecurityException {
        requireOpen();
        var candidate = new ArrayList<>(entries);
        var old = candidate.stream().filter(e -> e.id().equals(entry.id())).findFirst();
        candidate.removeIf(e -> e.id().equals(entry.id())); candidate.add(entry);
        persist(candidate); // Only publish changes in memory after the disk commit succeeds.
        entries = candidate; old.ifPresent(e -> { if (e != entry) e.close(); });
    }
    public void delete(UUID id) throws IOException, GeneralSecurityException {
        requireOpen();
        var candidate = new ArrayList<>(entries);
        var removed = candidate.stream().filter(e -> e.id().equals(id)).toList();
        candidate.removeIf(e -> e.id().equals(id));
        persist(candidate); entries = candidate; removed.forEach(VaultEntry::close);
    }
    public void export(Path destination) throws IOException, GeneralSecurityException {
        requireOpen();
        Path dest = destination.toAbsolutePath().normalize();
        if (dest.equals(path) || (Files.exists(dest) && Files.isSameFile(dest, path)))
            throw new IOException("Выберите другой файл для копии.");
        byte[] file = VaultCodec.encrypt(entries, key, salt, iterations);
        try { LocalFiles.atomicWrite(dest, file, false); }
        finally { Arrays.fill(file, (byte) 0); }
    }
    private void requireOpen() { if (closed) throw new IllegalStateException("Хранилище заблокировано."); }
    @Override public void close() {
        if (closed) return;
        closed = true; Arrays.fill(key, (byte) 0); entries.forEach(VaultEntry::close); entries.clear();
    }
}
