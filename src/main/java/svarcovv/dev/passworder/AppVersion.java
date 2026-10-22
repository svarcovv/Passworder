package svarcovv.dev.passworder;

import java.io.IOException;
import java.util.Properties;

public final class AppVersion {
    public static final String CURRENT = load();
    private AppVersion() {}
    private static String load() {
        try (var stream = AppVersion.class.getResourceAsStream("/passworder.properties")) {
            var properties = new Properties();
            if (stream != null) properties.load(stream);
            String version = properties.getProperty("version", "dev");
            return version.matches("[0-9]+\\.[0-9]+\\.[0-9]+") ? version : "dev";
        } catch (IOException e) { return "dev"; }
    }
}
