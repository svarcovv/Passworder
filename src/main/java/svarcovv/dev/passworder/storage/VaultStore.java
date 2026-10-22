package svarcovv.dev.passworder.storage;

import java.io.IOException;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.*;

public final class VaultStore {
    private final Path path;
    public VaultStore(Path path) { this.path = path.toAbsolutePath().normalize(); }
    public Path path() { return path; }
    public boolean exists() { return Files.exists(path); }
    public VaultSession create(char[] password) throws IOException, GeneralSecurityException {
        if (password.length < 12 || password.length > 1024) throw new IllegalArgumentException("Мастер-пароль: от 12 до 1024 символов.");
        if (exists()) throw new IOException("Хранилище уже существует.");
        byte[] salt = VaultCodec.random(32), key = VaultCodec.derive(password, salt, VaultCodec.ITERATIONS);
        var session = new VaultSession(path, key, salt, VaultCodec.ITERATIONS, new ArrayList<>());
        try { session.save(); return session; }
        catch (IOException | GeneralSecurityException e) { session.close(); throw e; }
    }
    public VaultSession open(char[] password) throws IOException, GeneralSecurityException {
        if (password.length > 1024) throw new IllegalArgumentException("Слишком длинный мастер-пароль.");
        if (Files.size(path) > VaultCodec.MAX_FILE_SIZE) throw new IOException("Хранилище превысило 32 МБ.");
        byte[] file = Files.readAllBytes(path), key = null;
        try {
            var header = VaultCodec.header(file);
            key = VaultCodec.derive(password, header.salt(), header.iterations());
            var entries = VaultCodec.decrypt(file, header, key);
            return new VaultSession(path, key, header.salt(), header.iterations(), entries);
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            if (key != null) Arrays.fill(key, (byte) 0);
            throw e;
        } finally { Arrays.fill(file, (byte) 0); }
    }
    /** Validate first. Never replace a usable vault with an unverified backup. */
    public VaultSession restore(Path backup, char[] password) throws IOException, GeneralSecurityException {
        Path source = backup.toAbsolutePath().normalize();
        if (source.equals(path) || (Files.exists(path) && Files.isSameFile(source, path)))
            throw new IOException("Выберите копию, а не текущее хранилище.");
        try (var validated = new VaultStore(source).open(password)) {
            byte[] file = VaultCodec.encrypt(validated.entries(), validated.key(), validated.salt(), validated.iterations());
            try { LocalFiles.atomicWrite(path, file, true); }
            finally { Arrays.fill(file, (byte) 0); }
        }
        return open(password);
    }
}
