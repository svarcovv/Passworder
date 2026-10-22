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
    private final Path versionFile;
    private String lastSignal;
    public SingleInstance(Path directory) throws IOException {
        this(directory, "dev");
    }
    public SingleInstance(Path directory, String version) throws IOException {
        signal = directory.resolve("activate");
        versionFile = directory.resolve("running.version");
        Path lockPath = directory.resolve("instance.lock");
        channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        LocalFiles.restrict(lockPath);
        FileLock acquired;
        try { acquired = channel.tryLock(); } catch (OverlappingFileLockException e) { acquired = null; }
        lock = acquired;
        if (lock == null) Files.writeString(signal, UUID.randomUUID().toString());
        else {
            Files.writeString(versionFile, version); LocalFiles.restrict(versionFile);
            if (Files.exists(signal)) lastSignal = Files.readString(signal);
        }
    }
    public boolean primary() { return lock != null; }
    public String runningVersion() {
        try { return Files.exists(versionFile) && Files.size(versionFile) < 128 ? Files.readString(versionFile).strip() : "unknown"; }
        catch (IOException e) { return "unknown"; }
    }
    public boolean activationRequested() {
        try {
            if (!Files.exists(signal) || Files.size(signal) > 128) return false;
            String current = Files.readString(signal);
            if (!current.equals(lastSignal)) { lastSignal = current; return true; }
        } catch (IOException ignored) {}
        return false;
    }
    @Override public void close() throws IOException {
        try { if (lock != null && lock.isValid()) { Files.deleteIfExists(versionFile); lock.release(); } }
        finally { channel.close(); }
    }
}
