package svarcovv.dev.passworder.platform;

import svarcovv.dev.passworder.storage.LocalFiles;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/** Profile-local coordination. A different build requests a clean handover. */
public final class SingleInstance implements AutoCloseable {
    private final FileChannel channel;
    private FileLock lock;
    private final Path directory, signal, versionFile, infoFile, replacement;
    private final String version, build;
    private String lastSignal;
    public SingleInstance(Path directory) throws IOException { this(directory, "dev", "dev"); }
    public SingleInstance(Path directory, String version) throws IOException { this(directory, version, version); }
    public SingleInstance(Path directory, String version, String build) throws IOException {
        this.directory = directory; this.version = version; this.build = build;
        signal = directory.resolve("activate"); versionFile = directory.resolve("running.version");
        infoFile = directory.resolve("running.properties"); replacement = directory.resolve("replace.request");
        Path lockPath = directory.resolve("instance.lock");
        channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE); LocalFiles.restrict(lockPath);
        tryAcquire();
    }
    public boolean tryAcquire() throws IOException {
        if (primary()) return true;
        try { lock = channel.tryLock(); } catch (OverlappingFileLockException e) { lock = null; }
        if (lock == null) return false;
        try {
            Files.deleteIfExists(replacement);
            Files.writeString(versionFile, version); LocalFiles.restrict(versionFile);
            Properties info = new Properties(); info.setProperty("protocol", "1"); info.setProperty("version", version);
            info.setProperty("build", build); info.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            write(infoFile, info);
            if (Files.exists(signal)) lastSignal = Files.readString(signal);
            return true;
        } catch (IOException error) { lock.release(); lock = null; throw error; }
    }
    public boolean primary() { return lock != null && lock.isValid(); }
    public String runningVersion() {
        try { return Files.exists(versionFile) && Files.size(versionFile) < 128 ? Files.readString(versionFile).strip() : "unknown"; }
        catch (IOException e) { return "unknown"; }
    }
    public boolean sameBuild() { Properties info = read(infoFile); return version.equals(info.getProperty("version")) && build.equals(info.getProperty("build")); }
    public boolean supportsReplacement() { return read(infoFile).getProperty("protocol", "").equals("1"); }
    public void requestActivation() throws IOException { LocalFiles.atomicWrite(signal, UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), false); }
    public void requestReplacement() throws IOException {
        Properties request = new Properties(); request.setProperty("build", build); request.setProperty("pid", read(infoFile).getProperty("pid", ""));
        write(replacement, request);
    }
    public boolean replacementRequested() {
        Properties request = read(replacement);
        return Long.toString(ProcessHandle.current().pid()).equals(request.getProperty("pid")) && !build.equals(request.getProperty("build", build));
    }
    public boolean activationRequested() {
        try {
            if (!Files.exists(signal) || Files.size(signal) > 128) return false;
            String current = Files.readString(signal);
            if (!current.equals(lastSignal)) { lastSignal = current; return true; }
        } catch (IOException ignored) {}
        return false;
    }
    private static Properties read(Path path) {
        Properties properties = new Properties();
        try { if (Files.exists(path) && Files.size(path) < 4096) try (var in = Files.newInputStream(path)) { properties.load(in); } }
        catch (IOException ignored) {}
        return properties;
    }
    private static void write(Path path, Properties properties) throws IOException {
        var out = new ByteArrayOutputStream(); properties.store(out, null); LocalFiles.atomicWrite(path, out.toByteArray(), false);
    }
    @Override public void close() throws IOException {
        try {
            if (primary()) {
                Files.deleteIfExists(versionFile); Files.deleteIfExists(infoFile); Files.deleteIfExists(replacement); lock.release();
            }
        } finally { channel.close(); }
    }
}
