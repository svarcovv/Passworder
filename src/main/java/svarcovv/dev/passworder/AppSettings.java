package svarcovv.dev.passworder;

import svarcovv.dev.passworder.storage.LocalFiles;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class AppSettings {
    private final Path file;
    private final Properties values = new Properties();
    public AppSettings(Path directory) {
        file = directory.resolve("settings.properties");
        try { if (Files.exists(file) && Files.size(file) < 8192) try (var in = Files.newInputStream(file)) { values.load(in); } }
        catch (IOException ignored) { /* Non-secret preferences can fall back to defaults. */ }
    }
    public boolean dark() { return !values.getProperty("theme", "dark").equals("light"); }
    public String hotkey() { return values.getProperty("hotkey", "Ctrl+Alt+P"); }
    public boolean animations() { return Boolean.parseBoolean(values.getProperty("animations", "true")); }
    public int autoLockMinutes() {
        try { int v = Integer.parseInt(values.getProperty("autoLockMinutes", "5")); if (v == 1 || v == 5 || v == 15 || v == 30) return v; }
        catch (NumberFormatException ignored) {}
        return 5;
    }
    public void set(boolean dark, String hotkey, int minutes) throws IOException {
        set(dark, hotkey, minutes, animations());
    }
    public void set(boolean dark, String hotkey, int minutes, boolean animations) throws IOException {
        Properties candidate = new Properties(); candidate.setProperty("theme", dark ? "dark" : "light");
        candidate.setProperty("hotkey", hotkey); candidate.setProperty("autoLockMinutes", Integer.toString(minutes));
        candidate.setProperty("animations", Boolean.toString(animations));
        var bytes = new ByteArrayOutputStream(); candidate.store(bytes, "Passworder preferences. No vault data.");
        LocalFiles.atomicWrite(file, bytes.toByteArray(), false);
        values.clear(); values.putAll(candidate);
    }
}
