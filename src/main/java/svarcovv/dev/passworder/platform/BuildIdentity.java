package svarcovv.dev.passworder.platform;

import svarcovv.dev.passworder.Passworder;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;

public final class BuildIdentity {
    private BuildIdentity() {}
    public static String current() throws IOException {
        try {
            Path source = Path.of(Passworder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (Files.isDirectory(source)) {
                try (var files = Files.walk(source)) {
                    for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                        digest.update(source.relativize(path).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); hashFile(digest, path);
                    }
                }
            } else hashFile(digest, source);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.net.URISyntaxException | NoSuchAlgorithmException e) { throw new IOException("Cannot identify application build", e); }
    }
    private static void hashFile(MessageDigest digest, Path path) throws IOException {
        try (var in = Files.newInputStream(path)) { byte[] buffer = new byte[65536]; int read; while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read); }
    }
}
