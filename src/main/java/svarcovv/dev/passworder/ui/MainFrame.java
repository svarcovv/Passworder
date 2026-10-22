package svarcovv.dev.passworder.ui;

import svarcovv.dev.passworder.AppSettings;
import svarcovv.dev.passworder.AppVersion;
import svarcovv.dev.passworder.model.VaultEntry;
import svarcovv.dev.passworder.platform.*;
import svarcovv.dev.passworder.security.*;
import svarcovv.dev.passworder.storage.*;
import javax.crypto.AEADBadTagException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class MainFrame extends JFrame {
    private final VaultStore store;
    private final AppSettings settings;
    private final SingleInstance instance;
    private final GlobalHotkey hotkey = new GlobalHotkey();
    private final ClipboardService clipboard = new ClipboardService();
    private final JPanel root = new JPanel(new BorderLayout());
    private final FeedbackLabel status = new FeedbackLabel();
    private VaultSession session;
    private VaultPanel vaultPanel;
    private GeneratorPanel generatorPanel;
    private String page = "vault";
    private TrayIcon tray;
    private final java.util.ArrayList<JPasswordField> lockedFields = new java.util.ArrayList<>();
    private boolean hotkeyAvailable;
    private long lastActivity = System.nanoTime();
    private boolean busy, lockWhenReady;
    private final javax.swing.Timer heartbeat;
    private javax.swing.Timer statusTimer;
    private final AWTEventListener activityListener = event -> {
        if (event.getSource() instanceof Component c && (SwingUtilities.getWindowAncestor(c) == this || c == this
                || (SwingUtilities.getWindowAncestor(c) instanceof Dialog d && d.getOwner() == this))) lastActivity = System.nanoTime();
    };

    public MainFrame(VaultStore store, AppSettings settings, SingleInstance instance) {
        super("Passworder " + AppVersion.CURRENT); this.store = store; this.settings = settings; this.instance = instance;
        setIconImage(Glyph.appImage(64)); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        Motion.setEnabled(settings.animations());
        setMinimumSize(new Dimension(960, 660)); setSize(1280, 820); setLocationRelativeTo(null);
        setContentPane(root); status.putClientProperty("html.disable", true);
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { hideOrExit(); }
            @Override public void windowIconified(WindowEvent e) { lock(); }
        });
        bind("new", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), () -> { if (session != null && !busy) edit(null); });
        bind("search", KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), () -> { if (session != null && !busy) { if (!page.equals("vault") && !page.equals("favorites")) navigate("vault"); vaultPanel.focusSearch(); } });
        bind("lock", KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), this::lock);
        setupTray(); registerHotkey(); showLocked();
        heartbeat = new javax.swing.Timer(1000, e -> {
            if (instance != null && instance.activationRequested()) summon();
            if (session != null && System.nanoTime() - lastActivity > settings.autoLockMinutes() * 60_000_000_000L) lock();
        }); heartbeat.start();
    }
    private void bind(String name, KeyStroke stroke, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
        getRootPane().getActionMap().put(name, new AbstractAction() { public void actionPerformed(ActionEvent e) { action.run(); } });
    }
    public AppSettings settings() { return settings; }
    Path vaultPath() { return store.path(); }
    List<VaultEntry> entries() { return session == null ? List.of() : session.entries(); }
    private void showLocked() {
        clearLockedFields();
        vaultPanel = null; root.removeAll(); root.setBackground(Theme.background);
        JPanel center = new JPanel(new GridBagLayout()); center.setBackground(Theme.background);
        JPanel card = Theme.vertical(); card.setPreferredSize(new Dimension(450, 610)); card.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        JLabel icon = new JLabel(new ImageIcon(Glyph.appImage(62))); Theme.align(icon); card.add(icon); Theme.gap(card, 24);
        JLabel brand = Theme.label("Passworder", 34, Theme.text, true); Theme.align(brand); card.add(brand); Theme.gap(card, 10);
        boolean creating = !store.exists();
        JLabel subtitle = Theme.label(creating ? "Ваши пароли остаются у вас." : "Хранилище заблокировано.", 16, Theme.muted, false); Theme.align(subtitle); card.add(subtitle); Theme.gap(card, 34);
        var master = new JPasswordField(); master.setName("unlock.master");
        master.putClientProperty("JTextField.placeholderText", creating ? "Не меньше 12 символов" : "Введите мастер-пароль");
        Theme.field(card, creating ? "Придумайте мастер-пароль" : "Мастер-пароль", master);
        var confirm = new JPasswordField(); confirm.setName("unlock.confirm");
        lockedFields.add(master); lockedFields.add(confirm);
        if (creating) Theme.field(card, "Повторите мастер-пароль", confirm);
        JLabel error = Theme.label(" ", 12, Theme.danger, false); error.setName("unlock.error"); Theme.align(error); card.add(error); Theme.gap(card, 12);
        JButton submit = Theme.primary(creating ? "Создать хранилище" : "Разблокировать", () -> {}); submit.setName("unlock.submit");
        submit.addActionListener(e -> {
            if (busy) return;
            char[] pass = master.getPassword(), repeated = confirm.getPassword();
            if (creating && (pass.length < 12 || pass.length > 1024 || !Arrays.equals(pass, repeated))) {
                error.setText(pass.length < 12 ? "Нужно хотя бы 12 символов." : pass.length > 1024 ? "Не больше 1024 символов." : "Пароли не совпадают.");
                Arrays.fill(pass, '\0'); Arrays.fill(repeated, '\0'); return;
            }
            Arrays.fill(repeated, '\0'); master.setText(""); confirm.setText("");
            busy = true; submit.setEnabled(false); error.setText("Открываем хранилище..."); error.setForeground(Theme.muted);
            new SwingWorker<VaultSession, Void>() {
                @Override protected VaultSession doInBackground() throws Exception {
                    try { return creating ? store.create(pass) : store.open(pass); } finally { Arrays.fill(pass, '\0'); }
                }
                @Override protected void done() {
                    busy = false; submit.setEnabled(true);
                    try {
                        session = get();
                        if (lockWhenReady) { lockWhenReady = false; lock(); return; }
                        lastActivity = System.nanoTime(); page = "vault"; buildWorkspace();
                    } catch (Exception ex) {
                        lockWhenReady = false;
                        error.setForeground(Theme.danger); Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
                        error.setText(cause instanceof AEADBadTagException ? "Неверный пароль или файл поврежден." : "Не удалось открыть хранилище. Проверьте файл и доступ.");
                        master.requestFocusInWindow();
                    }
                }
            }.execute();
        });
        submit.setAlignmentX(Component.LEFT_ALIGNMENT); submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); card.add(submit); Theme.gap(card, 24);
        JLabel note = Theme.label(creating ? "Запомните его. Сбросить мастер-пароль нельзя." : "Без аккаунта и облачной синхронизации.", 13, Theme.muted, false);
        Theme.align(note); card.add(note); Theme.gap(card, 16);
        JButton restore = Theme.button("Восстановить из копии", () -> {
            char[] pass = master.getPassword(); master.setText(""); confirm.setText(""); restore(pass);
        }); Theme.align(restore); card.add(restore);
        center.add(card); root.add(center, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.setBorder(BorderFactory.createEmptyBorder(12, 24, 16, 24));
        bottom.add(Theme.label(hotkeyAvailable ? settings.hotkey() + " - показать окно" : "Горячая клавиша недоступна. Измените ее в настройках.", 12, Theme.muted, false), BorderLayout.WEST);
        bottom.add(themeButton(), BorderLayout.EAST); root.add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(submit); root.revalidate(); root.repaint(); SwingUtilities.invokeLater(master::requestFocusInWindow);
    }
    private void buildWorkspace() {
        clearLockedFields();
        VaultPanel.ViewState tableState = vaultPanel == null ? null : vaultPanel.capture();
        GeneratorPanel.State generatorState = generatorPanel != null && page.equals("generator") ? generatorPanel.capture() : null;
        if (generatorPanel != null) generatorPanel.clear();
        generatorPanel = null; vaultPanel = null;
        getRootPane().setDefaultButton(null); root.removeAll(); root.setBackground(Theme.background);
        JPanel sidebar = new JPanel(new BorderLayout()); sidebar.setPreferredSize(new Dimension(212, 0)); sidebar.setBackground(Theme.sidebar);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.border));
        JPanel nav = Theme.vertical(); nav.setBackground(Theme.sidebar); nav.setBorder(BorderFactory.createEmptyBorder(28, 16, 0, 16));
        JPanel logo = new JPanel(new BorderLayout(10, 0)); logo.setBackground(Theme.sidebar);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        logo.add(new JLabel(new ImageIcon(Glyph.appImage(32))), BorderLayout.WEST);
        logo.add(Theme.label("Passworder", 19, Theme.text, true), BorderLayout.CENTER);
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34)); Theme.align(logo); nav.add(logo); Theme.gap(nav, 34);
        String[][] items = {{"Все пароли", "grid", "vault"}, {"Избранное", "star", "favorites"}, {"Генератор", "key", "generator"}, {"Настройки", "settings", "settings"}};
        for (int i = 0; i < items.length; i++) {
            if (i > 0) Theme.gap(nav, 8);
            nav.add(navButton(items[i][0], items[i][1], items[i][2]));
        }
        sidebar.add(nav, BorderLayout.NORTH);
        JPanel foot = Theme.vertical(); foot.setBackground(Theme.sidebar); foot.setBorder(BorderFactory.createEmptyBorder(20, 16, 22, 16));
        JButton lock = new MotionButton("Заблокировать", MotionButton.Style.GHOST, this::lock);
        lock.setIcon(new Glyph("lock", 18, Theme.muted)); lock.setHorizontalAlignment(SwingConstants.LEFT);
        lock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); Theme.align(lock); foot.add(lock); Theme.gap(foot, 10);
        JLabel shortcut = Theme.label(settings.hotkey(), 12, Theme.muted, false); shortcut.setToolTipText("Показать окно");
        shortcut.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0)); Theme.align(shortcut); foot.add(shortcut); sidebar.add(foot, BorderLayout.SOUTH);
        root.add(sidebar, BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        JPanel heading = new JPanel(new BorderLayout(16, 0)); heading.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        String title = switch (page) { case "favorites" -> "Избранное"; case "generator" -> "Генератор паролей"; case "settings" -> "Настройки"; default -> "Все пароли"; };
        heading.add(Theme.label(title, 28, Theme.text, true), BorderLayout.WEST); heading.add(themeButton(), BorderLayout.EAST);
        main.add(heading, BorderLayout.NORTH);
        try {
            switch (page) {
                case "generator" -> { generatorPanel = new GeneratorPanel(this, generatorState); main.add(generatorPanel); }
                case "settings" -> main.add(new SettingsPanel(this));
                default -> { vaultPanel = new VaultPanel(this, page.equals("favorites")); main.add(vaultPanel); if (tableState != null) vaultPanel.restore(tableState); }
            }
        } finally { if (generatorState != null) generatorState.close(); }
        JPanel statusBar = new JPanel(new BorderLayout()); statusBar.setBorder(BorderFactory.createEmptyBorder(12, 28, 16, 28));
        status.reset(); status.setFont(new Font("Segoe UI", Font.PLAIN, 12)); statusBar.add(status, BorderLayout.CENTER);
        statusBar.add(Theme.label("Автоблокировка: " + settings.autoLockMinutes() + " мин", 12, Theme.muted, false), BorderLayout.EAST); main.add(statusBar, BorderLayout.SOUTH);
        root.add(main, BorderLayout.CENTER); root.revalidate(); root.repaint();
    }
    private JButton navButton(String name, String icon, String destination) {
        boolean active = page.equals(destination);
        var b = new MotionButton(name, MotionButton.Style.NAV, () -> navigate(destination)); b.setCurrent(active);
        b.setIcon(new Glyph(icon, 18, active ? Theme.accent : Theme.muted)); b.setIconTextGap(12); b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); b.setMinimumSize(new Dimension(160, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setName("nav." + destination); return b;
    }
    private JButton themeButton() {
        var button = Theme.icon(settings.dark() ? "sun" : "moon", settings.dark() ? "Включить светлую тему" : "Включить темную тему",
                () -> applySettings(!settings.dark(), settings.hotkey(), settings.autoLockMinutes()));
        button.setName("theme.toggle"); return button;
    }
    void navigate(String destination) {
        if (session == null || busy || page.equals(destination)) return;
        Motion.transition(root, 130, () -> { page = destination; buildWorkspace(); });
    }
    void edit(VaultEntry entry) {
        if (session == null || busy) return;
        new EntryDialog(this, entry, candidate -> {
            if (session == null) return false;
            try { session.put(candidate); if (vaultPanel != null) vaultPanel.refresh(); notifyStatus("Запись сохранена"); return true; }
            catch (Exception e) { error("Не удалось сохранить запись. Проверьте свободное место и права доступа."); return false; }
        }).setVisible(true);
    }
    void delete(VaultEntry entry) {
        if (session == null || busy) return;
        int answer = JOptionPane.showConfirmDialog(this, "Удалить выбранную запись?", "Удаление записи", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION || session == null) return;
        try { session.delete(entry.id()); if (vaultPanel != null) vaultPanel.refresh(); notifyStatus("Запись удалена"); }
        catch (Exception e) { error("Не удалось удалить запись. Файл хранилища не обновлен."); }
    }
    void copyPassword(VaultEntry entry) {
        if (session == null) return; char[] password = entry.password();
        try { copy(new String(password), "Пароль скопирован. Буфер очистится через 20 с."); } finally { Arrays.fill(password, '\0'); }
    }
    void copyLogin(VaultEntry entry) { if (session != null) copy(entry.login(), "Логин скопирован. Буфер очистится через 20 с."); }
    void copy(String value, String message) {
        if (session == null) return;
        try { clipboard.copy(value); notifyStatus(message); }
        catch (IllegalStateException e) { error("Буфер обмена занят. Попробуйте еще раз."); }
    }
    void openLink(String url) {
        if (session == null) return;
        try { Desktop.getDesktop().browse(WebLinks.parse(url)); } catch (Exception e) { error("Не удалось открыть ссылку в браузере."); }
    }
    public void lock() {
        Motion.stopTransitions();
        if (busy) { lockWhenReady = true; return; }
        clipboard.clear();
        for (Window window : getOwnedWindows()) window.dispose();
        if (generatorPanel != null) generatorPanel.clear(); generatorPanel = null;
        if (session != null) { session.close(); session = null; }
        showLocked();
    }
    void applySettings(boolean dark, String shortcut, int minutes) {
        applySettings(dark, shortcut, minutes, settings.animations());
    }
    void applySettings(boolean dark, String shortcut, int minutes, boolean animations) {
        if (busy) return;
        try {
            boolean themeChanged = settings.dark() != dark, shortcutChanged = !settings.hotkey().equals(shortcut);
            settings.set(dark, shortcut, minutes, animations); Motion.setEnabled(animations);
            boolean registered = !shortcutChanged || registerHotkey();
            if (tray != null) tray.setToolTip("Passworder - " + settings.hotkey());
            Motion.transition(root, themeChanged ? 220 : 120, () -> {
                if (themeChanged) { Theme.apply(dark); SwingUtilities.updateComponentTreeUI(this); }
                if (session == null) showLocked(); else buildWorkspace();
            });
            if (!registered) error("Это сочетание клавиш занято. Выберите другое в настройках.");
            else if (session != null) notifyStatus(themeChanged ? "Тема изменена" : "Настройки сохранены");
        } catch (IOException e) { error("Не удалось сохранить настройки."); }
    }
    private boolean registerHotkey() { hotkeyAvailable = hotkey.register(settings.hotkey(), this::summon); return hotkeyAvailable; }
    private void clearLockedFields() { lockedFields.forEach(field -> field.setText("")); lockedFields.clear(); }
    public void summon() {
        setVisible(true); setExtendedState(JFrame.NORMAL); setAlwaysOnTop(true); toFront(); requestFocus();
        javax.swing.Timer releaseTop = new javax.swing.Timer(450, e -> setAlwaysOnTop(false)); releaseTop.setRepeats(false); releaseTop.start();
    }
    private void setupTray() {
        if (!SystemTray.isSupported()) return;
        try {
            PopupMenu menu = new PopupMenu(); MenuItem open = new MenuItem("Открыть Passworder"), lock = new MenuItem("Заблокировать"), quit = new MenuItem("Выход");
            open.addActionListener(e -> SwingUtilities.invokeLater(this::summon)); lock.addActionListener(e -> SwingUtilities.invokeLater(this::lock)); quit.addActionListener(e -> SwingUtilities.invokeLater(this::exit));
            menu.add(open); menu.add(lock); menu.addSeparator(); menu.add(quit);
            tray = new TrayIcon(Glyph.appImage(32), "Passworder - " + settings.hotkey(), menu); tray.setImageAutoSize(true);
            tray.addActionListener(e -> SwingUtilities.invokeLater(this::summon)); SystemTray.getSystemTray().add(tray);
        } catch (AWTException | SecurityException e) { tray = null; }
    }
    private void hideOrExit() { if (tray != null) { lock(); setVisible(false); } else exit(); }
    private void exit() {
        if (busy) { error("Дождитесь завершения операции с хранилищем."); return; }
        lock(); heartbeat.stop(); if (statusTimer != null) statusTimer.stop(); hotkey.close(); clipboard.clear();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener); if (tray != null) SystemTray.getSystemTray().remove(tray);
        try { if (instance != null) instance.close(); } catch (IOException ignored) {}
        dispose(); System.exit(0);
    }
    void backup() {
        if (session == null) return;
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Сохранить зашифрованную копию");
        chooser.setFileFilter(new FileNameExtensionFilter("Хранилище Passworder (*.pwr)", "pwr"));
        chooser.setSelectedFile(new java.io.File("Passworder-" + LocalDate.now() + ".pwr"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION || session == null) return;
        Path dest = chooser.getSelectedFile().toPath(); if (!dest.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".pwr")) dest = Path.of(dest + ".pwr");
        if (Files.exists(dest) && JOptionPane.showConfirmDialog(this, "Заменить существующий файл копии?", "Резервная копия", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (session == null) return;
        try { session.export(dest); notifyStatus("Зашифрованная копия сохранена"); }
        catch (Exception e) { error("Не удалось сохранить копию. Выберите другой файл и проверьте доступ."); }
    }
    private void restore(char[] password) {
        if (busy) { Arrays.fill(password, '\0'); return; }
        if (password.length == 0) { Arrays.fill(password, '\0'); error("Сначала введите мастер-пароль резервной копии."); return; }
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Открыть зашифрованную копию");
        chooser.setFileFilter(new FileNameExtensionFilter("Хранилище Passworder (*.pwr, *.bak)", "pwr", "bak"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) { Arrays.fill(password, '\0'); return; }
        if (store.exists() && JOptionPane.showConfirmDialog(this, "Заменить текущее хранилище содержимым копии?", "Восстановление", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) { Arrays.fill(password, '\0'); return; }
        Path source = chooser.getSelectedFile().toPath(); busy = true;
        new SwingWorker<VaultSession, Void>() {
            @Override protected VaultSession doInBackground() throws Exception { try { return store.restore(source, password); } finally { Arrays.fill(password, '\0'); } }
            @Override protected void done() {
                busy = false;
                try { session = get(); if (lockWhenReady) { lockWhenReady = false; lock(); return; } lastActivity = System.nanoTime(); page = "vault"; buildWorkspace(); notifyStatus("Хранилище восстановлено из копии"); }
                catch (Exception e) { lockWhenReady = false; error("Копия не открылась. Проверьте мастер-пароль, файл и доступ к папке."); }
            }
        }.execute();
    }
    private void notifyStatus(String message) {
        status.showMessage(message); if (statusTimer != null) statusTimer.stop();
        statusTimer = new javax.swing.Timer(4500, e -> status.clearMessage()); statusTimer.setRepeats(false); statusTimer.start();
    }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, "Passworder", JOptionPane.WARNING_MESSAGE); }
}
