package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;

final class SettingsPanel extends JPanel {
    SettingsPanel(MainFrame owner) {
        super(new BorderLayout()); setBorder(BorderFactory.createEmptyBorder(0, 28, 4, 28));
        JPanel body = Theme.vertical();
        var hotkey = new JComboBox<>(new String[]{"Ctrl+Alt+P", "Ctrl+Shift+P", "Ctrl+Alt+Space"}); hotkey.setName("settings.hotkey"); hotkey.setSelectedItem(owner.settings().hotkey());
        Theme.field(body, "Показать окно поверх остальных", hotkey);
        var autoLock = new JComboBox<>(new Integer[]{1, 5, 15, 30}); autoLock.setName("settings.autoLock"); autoLock.setSelectedItem(owner.settings().autoLockMinutes());
        Theme.field(body, "Блокировать после бездействия (минут)", autoLock);
        hotkey.setMaximumSize(new Dimension(560, 42)); autoLock.setMaximumSize(new Dimension(560, 42));
        var animations = new JCheckBox("Плавные переходы и анимации", owner.settings().animations());
        animations.setName("settings.animations"); Theme.align(animations); body.add(animations); Theme.gap(body, 20);
        var save = Theme.primary("Сохранить настройки", () -> owner.applySettings(owner.settings().dark(), (String) hotkey.getSelectedItem(), (Integer) autoLock.getSelectedItem(), animations.isSelected()));
        save.setIcon(new Glyph("check", 18, Theme.accentText));
        save.setName("settings.save"); Theme.align(save); body.add(save); Theme.gap(body, 30);
        JSeparator separator = new JSeparator(); separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); Theme.align(separator); body.add(separator); Theme.gap(body, 26);
        addLabel(body, "Резервная копия", 17, Theme.text, true); Theme.gap(body, 10);
        addLabel(body, "Копия зашифрована тем же мастер-паролем.", 13, Theme.muted, false); Theme.gap(body, 14);
        JButton backup = Theme.button("Сохранить копию", owner::backup); backup.setIcon(new Glyph("download", 18, Theme.muted)); Theme.align(backup); body.add(backup); Theme.gap(body, 25);
        addLabel(body, "Файл хранилища", 17, Theme.text, true); Theme.gap(body, 10);
        JTextField path = new JTextField(owner.vaultPath().toString()); path.setEditable(false); path.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        path.setPreferredSize(new Dimension(560, 44)); path.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); Theme.align(path); body.add(path); Theme.gap(body, 12);
        addLabel(body, "Мастер-пароль не сохраняется. Восстановить его нельзя.", 13, Theme.muted, false); Theme.gap(body, 22);
        addLabel(body, "Ctrl+N - запись   /   Ctrl+F - поиск   /   Ctrl+Shift+L - блокировка", 12, Theme.muted, false);
        JScrollPane scroll = new SmoothScrollPane(body); scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(18); add(scroll);
    }
    private void addLabel(JPanel p, String text, int size, Color color, boolean bold) { JLabel l = Theme.label(text, size, color, bold); Theme.align(l); p.add(l); }
}
