package svarcovv.dev.passworder;

import com.formdev.flatlaf.FlatLaf;
import svarcovv.dev.passworder.platform.*;
import svarcovv.dev.passworder.storage.*;
import svarcovv.dev.passworder.ui.*;
import javax.swing.*;
import java.nio.file.*;
import java.util.Arrays;

public final class Passworder {
    private Passworder() {}
    public static void main(String[] args) {
        FlatLaf.setUseNativeWindowDecorations(true);
        boolean prepare = Arrays.asList(args).contains("--prepare-update");
        try {
            String local = System.getenv("LOCALAPPDATA");
            Path directory = Path.of(local == null ? System.getProperty("user.home") : local, local == null ? ".passworder" : "Passworder");
            LocalFiles.privateDirectory(directory);
            String identity = BuildIdentity.current() + (prepare ? ":installer" : "");
            SingleInstance instance = LaunchCoordinator.acquire(directory, AppVersion.CURRENT, identity, prepare);
            if (instance == null) { System.exit(0); return; }
            if (prepare) { instance.close(); System.exit(0); return; }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { instance.close(); } catch (Exception ignored) {} }, "passworder-cleanup"));
            SwingUtilities.invokeAndWait(() -> {
                var settings = new AppSettings(directory); Theme.apply(settings.dark());
                var frame = new MainFrame(new VaultStore(directory.resolve("vault.pwr")), settings, instance); frame.setVisible(true);
            });
        } catch (Exception e) {
            if (prepare) System.err.println(e.getMessage());
            if (!prepare) JOptionPane.showMessageDialog(null, e instanceof java.io.IOException ? e.getMessage()
                    : "Не удалось запустить Passworder. Проверьте доступ к папке пользователя.", "Passworder " + AppVersion.CURRENT, JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
