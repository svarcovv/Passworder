package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;

final class SettingsPanel extends JPanel {
    SettingsPanel(MainFrame owner) {
        super(new BorderLayout()); setBorder(BorderFactory.createEmptyBorder(36, 36, 24, 36));
        JPanel body = Theme.vertical();
        addLabel(body, "Настройки", 32, Theme.text, true); Theme.gap(body, 9);
        addLabel(body, "Как удобно вам.", 14, Theme.muted, false); Theme.gap(body, 32);
        var theme = new JComboBox<>(new String[]{"Темная", "Светлая"}); theme.setName("settings.theme"); theme.setSelectedIndex(owner.settings().dark() ? 0 : 1);
        Theme.field(body, "Оформление", theme);
        var hotkey = new JComboBox<>(new String[]{"Ctrl+Alt+P", "Ctrl+Shift+P", "Ctrl+Alt+Space"}); hotkey.setSelectedItem(owner.settings().hotkey());
        Theme.field(body, "Показать окно поверх остальных", hotkey);
        var autoLock = new JComboBox<>(new Integer[]{1, 5, 15, 30}); autoLock.setSelectedItem(owner.settings().autoLockMinutes());
        Theme.field(body, "Блокировать после бездействия (минут)", autoLock);
        var save = Theme.primary("Применить", () -> owner.applySettings(theme.getSelectedIndex() == 0, (String) hotkey.getSelectedItem(), (Integer) autoLock.getSelectedItem()));
        save.setName("settings.save"); Theme.align(save); body.add(save); Theme.gap(body, 30);
        JSeparator separator = new JSeparator(); separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); Theme.align(separator); body.add(separator); Theme.gap(body, 26);
        addLabel(body, "Резервная копия", 20, Theme.text, true); Theme.gap(body, 10);
        addLabel(body, "Копия зашифрована тем же мастер-паролем.", 13, Theme.muted, false); Theme.gap(body, 14);
        JButton backup = Theme.button("Сохранить копию", owner::backup); Theme.align(backup); body.add(backup); Theme.gap(body, 25);
        addLabel(body, "Данные на этом компьютере", 20, Theme.text, true); Theme.gap(body, 10);
        JTextField path = new JTextField(owner.vaultPath().toString()); path.setEditable(false); path.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        path.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); Theme.align(path); body.add(path); Theme.gap(body, 12);
        addLabel(body, "Мастер-пароль не сохраняется. Восстановить его нельзя.", 13, Theme.muted, false); Theme.gap(body, 22);
        addLabel(body, "Ctrl+N - запись   /   Ctrl+F - поиск   /   Ctrl+Shift+L - блокировка", 12, Theme.muted, false);
        JScrollPane scroll = new JScrollPane(body); scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(18); add(scroll);
    }
    private void addLabel(JPanel p, String text, int size, Color color, boolean bold) { JLabel l = Theme.label(text, size, color, bold); Theme.align(l); p.add(l); }
}
