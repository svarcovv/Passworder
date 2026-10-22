package svarcovv.dev.passworder.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public final class LocalFiles {
    private LocalFiles() {}
    public static void privateDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        restrict(directory);
    }
    public static void restrict(Path path) throws IOException {
        var acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            var entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(path)).setPermissions(EnumSet.allOf(AclEntryPermission.class));
            if (Files.isDirectory(path)) entry.setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT);
            acl.setAcl(List.of(entry.build()));
        } else if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(Files.isDirectory(path) ? "rwx------" : "rw-------"));
        }
    }
    public static void atomicWrite(Path target, byte[] bytes, boolean backup) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (!Files.exists(parent)) privateDirectory(parent);
        Path temp = Files.createTempFile(parent, ".passworder-", ".tmp");
        try {
            restrict(temp);
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            if (backup && Files.exists(target)) {
                byte[] previous = Files.readAllBytes(target);
                try { atomicWrite(target.resolveSibling(target.getFileName() + ".bak"), previous, false); }
                finally { Arrays.fill(previous, (byte) 0); }
            }
            // Fail without touching the original if this filesystem cannot replace atomically.
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
