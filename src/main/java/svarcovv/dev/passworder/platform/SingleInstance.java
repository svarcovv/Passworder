package svarcovv.dev.passworder.platform;

import svarcovv.dev.passworder.storage.LocalFiles;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.UUID;

/** File locking and an activation marker keep this completely offline. */
public final class SingleInstance implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    private final Path signal;
    private String lastSignal;
    public SingleInstance(Path directory) throws IOException {
        signal = directory.resolve("activate");
        Path lockPath = directory.resolve("instance.lock");
        channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        LocalFiles.restrict(lockPath);
        FileLock acquired;
        try { acquired = channel.tryLock(); } catch (OverlappingFileLockException e) { acquired = null; }
        lock = acquired;
        if (lock == null) Files.writeString(signal, UUID.randomUUID().toString());
        else if (Files.exists(signal)) lastSignal = Files.readString(signal);
    }
    public boolean primary() { return lock != null; }
    public boolean activationRequested() {
        try {
            if (!Files.exists(signal) || Files.size(signal) > 128) return false;
            String current = Files.readString(signal);
            if (!current.equals(lastSignal)) { lastSignal = current; return true; }
        } catch (IOException ignored) {}
        return false;
    }
    @Override public void close() throws IOException { if (lock != null && lock.isValid()) lock.release(); channel.close(); }
}
