package svarcovv.dev.passworder.ui;

import svarcovv.dev.passworder.AppSettings;
import svarcovv.dev.passworder.storage.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Manual desktop integration check. Uses an isolated, disposable vault with invented data. */
public final class DesktopSmoke {
    private static MainFrame frame;
    private static Path output;
    public static void main(String[] args) {
        int exit = 1;
        try {
            output = Path.of(args[0]); Files.createDirectories(output);
            Path data = Files.createTempDirectory("passworder-ui-test-"); LocalFiles.privateDirectory(data);
            edt(() -> { Theme.apply(true); frame = new MainFrame(new VaultStore(data.resolve("vault.pwr")), new AppSettings(data), null); frame.setVisible(true); });
            edt(() -> {
                field("unlock.master").setText("Only for testing 123!"); field("unlock.confirm").setText("Only for testing 123!"); button("unlock.submit").doClick();
            });
            await(() -> find("vault.table") != null, "creation");
            add("GitHub", "alex@example.test", "https://github.com", true);
            add("Почта", "mail@example.test", "https://example.test", false);
            add("Steam", "alex_player", "https://store.steampowered.com", false);
            add("Figma", "design@example.test", "https://figma.com", false);
            add("YouTube", "video@example.test", "https://youtube.com", false);
            edt(() -> {
                JTable table = (JTable) find("vault.table"); check(table.getRowCount() == 5, "five records");
                table.setRowSelectionInterval(0, 0); table.getActionMap().get("copy").actionPerformed(new ActionEvent(table, 0, "copy"));
            });
            check("Synthetic-Password-123!".equals(Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)), "copy password");
            edt(() -> { field("vault.search").setText("mail@example"); check(((JTable) find("vault.table")).getRowCount() == 1, "search login"); field("vault.search").setText("no such service"); check(((JTable) find("vault.table")).getRowCount() == 0, "search empty"); field("vault.search").setText(""); });
            screenshot("passworder-dark.png");
            edt(() -> button("nav.favorites").doClick());
            edt(() -> check(((JTable) find("vault.table")).getRowCount() == 1, "favorite filter"));
            edt(() -> { button("nav.settings").doClick(); ((JComboBox<?>) find("settings.theme")).setSelectedIndex(1); button("settings.save").doClick(); button("nav.vault").doClick(); });
            screenshot("passworder-light.png");
            // Windows uses delayed native clipboard formats. Check the real timeout too.
            Thread.sleep(20_500);
            check("".equals(Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)), "clipboard timeout");
            edt(() -> {
                JTable table = (JTable) find("vault.table"); table.setRowSelectionInterval(0, 0);
                table.getActionMap().get("copy").actionPerformed(new ActionEvent(table, 0, "copy"));
            });
            edt(frame::lock); check("".equals(Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)), "clipboard cleared on lock");
            edt(() -> { field("unlock.master").setText("wrong password"); button("unlock.submit").doClick(); });
            await(() -> find("unlock.error") instanceof JLabel label && label.getText().startsWith("Неверный"), "wrong password");
            edt(() -> { field("unlock.master").setText("Only for testing 123!"); button("unlock.submit").doClick(); });
            await(() -> find("vault.table") != null, "reopen");
            edt(() -> check(((JTable) find("vault.table")).getRowCount() == 5, "persistent records"));
            edt(() -> { JTable table = (JTable) find("vault.table"); table.setRowSelectionInterval(0, 0); SwingUtilities.invokeLater(() -> table.getActionMap().get("edit").actionPerformed(new ActionEvent(table, 0, "edit"))); });
            await(() -> find("entry.title") != null, "open edit");
            edt(() -> { field("entry.title").setText("Figma updated"); button("entry.save").doClick(); });
            await(() -> find("entry.save") == null, "save edit");
            edt(() -> { field("vault.search").setText("Figma updated"); check(((JTable) find("vault.table")).getRowCount() == 1, "edit persisted"); field("vault.search").setText(""); });
            edt(() -> { JTable table = (JTable) find("vault.table"); table.setRowSelectionInterval(0, 0); SwingUtilities.invokeLater(() -> table.getActionMap().get("delete").actionPerformed(new ActionEvent(table, 0, "delete"))); });
            await(() -> findOptionPane() != null, "delete confirmation");
            edt(() -> findOptionPane().setValue(JOptionPane.OK_OPTION));
            await(() -> findOptionPane() == null, "delete completed");
            edt(() -> check(((JTable) find("vault.table")).getRowCount() == 4, "delete row"));
            edt(() -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); check(!frame.isVisible(), "hide in tray"); });
            Robot robot = new Robot(); robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_ALT); robot.keyPress(KeyEvent.VK_P); robot.keyRelease(KeyEvent.VK_P); robot.keyRelease(KeyEvent.VK_ALT); robot.keyRelease(KeyEvent.VK_CONTROL);
            await(() -> frame.isVisible() && find("unlock.master") != null, "global hotkey and lock on close");
            System.out.println("Desktop smoke passed: create, add, search, favorites, copy, both themes, lock, wrong master, reopen, edit, delete, tray, global shortcut.");
            edt(() -> { frame.lock(); frame.dispose(); });
            try (var files = Files.walk(data)) { for (Path p : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
            exit = 0;
        } catch (Throwable e) { e.printStackTrace(); }
        finally { System.exit(exit); }
    }
    private static void add(String name, String login, String link, boolean favorite) throws Exception {
        edt(() -> SwingUtilities.invokeLater(() -> button("vault.add").doClick()));
        await(() -> find("entry.title") != null, "open create");
        edt(() -> { field("entry.title").setText(name); field("entry.login").setText(login); field("entry.password").setText("Synthetic-Password-123!"); field("entry.url").setText(link);
            if (favorite) for (Window w : Window.getWindows()) if (w.isShowing() && w instanceof JDialog) clickFavorite(w);
            button("entry.save").doClick(); });
        await(() -> find("entry.save") == null, "save record");
    }
    private static void clickFavorite(Container c) { for (Component child : c.getComponents()) { if (child instanceof JCheckBox box) box.setSelected(true); if (child instanceof Container nested) clickFavorite(nested); } }
    private static void screenshot(String name) throws Exception {
        edt(() -> { try { var image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB); var g = image.createGraphics(); frame.printAll(g); g.dispose(); ImageIO.write(image, "png", output.resolve(name).toFile()); } catch (Exception e) { throw new RuntimeException(e); } });
    }
    private static JTextField field(String name) { return (JTextField) find(name); }
    private static JButton button(String name) { return (JButton) find(name); }
    private static Component find(String name) { for (Window w : Window.getWindows()) if (w.isShowing()) { Component found = findIn(w, name); if (found != null) return found; } return null; }
    private static Component findIn(Component c, String name) { if (name.equals(c.getName())) return c; if (c instanceof Container p) for (Component child : p.getComponents()) { Component found = findIn(child, name); if (found != null) return found; } return null; }
    private static JOptionPane findOptionPane() { for (Window w : Window.getWindows()) if (w.isShowing() && w instanceof JDialog) { JOptionPane p = findPane(w); if (p != null) return p; } return null; }
    private static JOptionPane findPane(Component c) { if (c instanceof JOptionPane p) return p; if (c instanceof Container p) for (Component child : p.getComponents()) { JOptionPane result = findPane(child); if (result != null) return result; } return null; }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    private static void await(BooleanSupplier condition, String description) throws Exception {
        long end = System.nanoTime() + 15_000_000_000L;
        while (System.nanoTime() < end) { AtomicReference<Boolean> result = new AtomicReference<>(false); edt(() -> result.set(condition.getAsBoolean())); if (result.get()) return; Thread.sleep(80); }
        throw new AssertionError("Timed out: " + description);
    }
}
