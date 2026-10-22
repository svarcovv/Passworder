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
                var settings = new AppSettings(directory); Theme.apply(settings.dark());
                SingleInstance instance = new SingleInstance(directory, AppVersion.CURRENT);
                if (!instance.primary()) {
                    if (!AppVersion.CURRENT.equals(instance.runningVersion())) {
                        JOptionPane.showMessageDialog(null,
                                "Уже работает другая версия Passworder.\nВыберите «Выход» в меню значка в трее, затем запустите новую версию еще раз.",
                                "Passworder " + AppVersion.CURRENT, JOptionPane.INFORMATION_MESSAGE);
                    }
                    instance.close(); System.exit(0); return;
                }
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
