package svarcovv.dev.passworder.ui;

import svarcovv.dev.passworder.*;
import svarcovv.dev.passworder.platform.SingleInstance;
import svarcovv.dev.passworder.storage.*;
import javax.swing.*;
import java.nio.file.*;

/** External-process fixture for verifying that a handover preserves an open editor. */
public final class UpgradeFixture {
    public static void main(String[] args) throws Exception {
        Path data = Path.of(args[0]), ready = Path.of(args[1]), release = Path.of(args[2]);
        var instance = new SingleInstance(data, AppVersion.CURRENT, "editor-fixture");
        if (!instance.primary()) throw new IllegalStateException("Fixture profile is already in use");
        SwingUtilities.invokeAndWait(() -> {
            Theme.apply(true); var frame = new MainFrame(new VaultStore(data.resolve("vault.pwr")), new AppSettings(data), instance); frame.setVisible(true);
            var editor = new JDialog(frame, "Редактирование тестовой записи", false);
            editor.add(new JTextField("Synthetic unsaved entry")); editor.setSize(360, 160); editor.setLocationRelativeTo(frame); editor.setVisible(true);
            var timer = new Timer(100, e -> { if (Files.exists(release)) { editor.dispose(); ((Timer) e.getSource()).stop(); } }); timer.start();
            try { Files.writeString(ready, "ready"); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
