package svarcovv.dev.passworder;

import com.formdev.flatlaf.FlatLaf;
import svarcovv.dev.passworder.platform.SingleInstance;
import svarcovv.dev.passworder.storage.*;
import svarcovv.dev.passworder.ui.*;
import javax.swing.*;
import java.nio.file.*;

public final class Passworder {
    private Passworder() {}
    public static void main(String[] args) {
        FlatLaf.setUseNativeWindowDecorations(true);
        SwingUtilities.invokeLater(() -> {
            try {
                String local = System.getenv("LOCALAPPDATA");
                Path directory = Path.of(local == null ? System.getProperty("user.home") : local, local == null ? ".passworder" : "Passworder");
                LocalFiles.privateDirectory(directory);
                SingleInstance instance = new SingleInstance(directory);
                if (!instance.primary()) { instance.close(); System.exit(0); return; }
                var settings = new AppSettings(directory); Theme.apply(settings.dark());
                var frame = new MainFrame(new VaultStore(directory.resolve("vault.pwr")), settings, instance);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { instance.close(); } catch (Exception ignored) {} }, "passworder-cleanup"));
                frame.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Не удалось открыть папку Passworder в профиле пользователя. Проверьте права доступа.", "Passworder", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
