package svarcovv.dev.passworder.storage;

import svarcovv.dev.passworder.model.VaultEntry;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

final class VaultCodec {
    static final int ITERATIONS = 600_000;
    static final int HEADER_SIZE = 8 + 4 + 32 + 12;
    static final int MAX_FILE_SIZE = 32 * 1024 * 1024;
    private static final byte[] MAGIC = "PWDER001".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom RANDOM = new SecureRandom();
    record Header(int iterations, byte[] salt, byte[] nonce) {}

    static byte[] random(int length) { byte[] b = new byte[length]; RANDOM.nextBytes(b); return b; }
    static byte[] derive(char[] password, byte[] salt, int iterations) throws GeneralSecurityException {
        var spec = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    static Header header(byte[] file) throws IOException {
        if (file.length < HEADER_SIZE + 16 || file.length > MAX_FILE_SIZE) throw new IOException("Неверный размер хранилища.");
        ByteBuffer b = ByteBuffer.wrap(file);
        byte[] magic = new byte[8]; b.get(magic);
        if (!Arrays.equals(magic, MAGIC)) throw new IOException("Неизвестный формат хранилища.");
        int iterations = b.getInt();
        if (iterations < ITERATIONS || iterations > 2_000_000) throw new IOException("Неподдерживаемые параметры хранилища.");
        byte[] salt = new byte[32], nonce = new byte[12]; b.get(salt); b.get(nonce);
        return new Header(iterations, salt, nonce);
    }
    static byte[] encrypt(List<VaultEntry> entries, byte[] key, byte[] salt, int iterations)
            throws IOException, GeneralSecurityException {
        byte[] plain = encode(entries);
        try {
            byte[] nonce = random(12);
            byte[] header = ByteBuffer.allocate(HEADER_SIZE).put(MAGIC).putInt(iterations).put(salt).put(nonce).array();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(header);
            byte[] encrypted = cipher.doFinal(plain);
            if (encrypted.length + HEADER_SIZE > MAX_FILE_SIZE) throw new IOException("Хранилище превысило 32 МБ.");
            return ByteBuffer.allocate(HEADER_SIZE + encrypted.length).put(header).put(encrypted).array();
        } finally { Arrays.fill(plain, (byte) 0); }
    }
    static List<VaultEntry> decrypt(byte[] file, Header header, byte[] key) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, header.nonce()));
        cipher.updateAAD(file, 0, HEADER_SIZE);
        byte[] plain = cipher.doFinal(file, HEADER_SIZE, file.length - HEADER_SIZE);
        try { return decode(plain); } finally { Arrays.fill(plain, (byte) 0); }
    }
    private static byte[] encode(List<VaultEntry> entries) throws IOException {
        if (entries.size() > 100_000) throw new IOException("Слишком много записей.");
        // ByteArrayOutputStream normally retains a second plaintext copy. Wipe its backing buffer too.
        try (var buffer = new WipeableBuffer(); var out = new DataOutputStream(buffer)) {
            out.writeInt(1); out.writeInt(entries.size());
            for (var e : entries) {
                out.writeLong(e.id().getMostSignificantBits()); out.writeLong(e.id().getLeastSignificantBits());
                out.writeUTF(e.title()); out.writeUTF(e.login()); out.writeUTF(e.url());
                char[] secret = e.password();
                try { out.writeInt(secret.length); for (char c : secret) out.writeChar(c); }
                finally { Arrays.fill(secret, '\0'); }
                out.writeBoolean(e.favorite()); out.writeLong(e.created().toEpochMilli()); out.writeLong(e.updated().toEpochMilli());
                if (buffer.size() > MAX_FILE_SIZE - HEADER_SIZE - 16) throw new IOException("Хранилище превысило 32 МБ.");
            }
            return buffer.toByteArray();
        }
    }
    private static List<VaultEntry> decode(byte[] plain) throws IOException {
        List<VaultEntry> entries = new ArrayList<>();
        try (var in = new DataInputStream(new ByteArrayInputStream(plain))) {
            if (in.readInt() != 1) throw new IOException("Неподдерживаемая версия записей.");
            int count = in.readInt();
            if (count < 0 || count > 100_000) throw new IOException("Поврежден список записей.");
            Set<UUID> ids = new HashSet<>();
            for (int i = 0; i < count; i++) {
                UUID id = new UUID(in.readLong(), in.readLong());
                if (!ids.add(id)) throw new IOException("Повторяющийся идентификатор записи.");
                String title = in.readUTF(), login = in.readUTF(), url = in.readUTF();
                int length = in.readInt();
                if (length < 1 || length > 4096) throw new IOException("Поврежден пароль записи.");
                char[] secret = new char[length];
                try {
                    for (int c = 0; c < length; c++) secret[c] = in.readChar();
                    entries.add(new VaultEntry(id, title, login, secret, url, in.readBoolean(),
                            Instant.ofEpochMilli(in.readLong()), Instant.ofEpochMilli(in.readLong())));
                } finally { Arrays.fill(secret, '\0'); }
            }
            if (in.read() != -1) throw new IOException("Лишние данные в хранилище.");
            return entries;
        } catch (IOException | RuntimeException e) {
            entries.forEach(VaultEntry::close);
            throw new IOException("Повреждены данные хранилища.", e);
        }
    }
    private static final class WipeableBuffer extends ByteArrayOutputStream {
        @Override public void close() { Arrays.fill(buf, (byte) 0); reset(); }
    }
}
