package svarcovv.dev.passworder.ui;

import com.formdev.flatlaf.*;
import javax.swing.*;
import java.awt.*;

public final class Theme {
    public static Color background, surface, sidebar, border, text, muted, accent, accentText, selected, danger;
    public static boolean dark;
    private Theme() {}
    public static void apply(boolean isDark) {
        dark = isDark;
        background = color(isDark ? "202527" : "F4F6F5");
        surface = color(isDark ? "272D30" : "FFFFFF");
        sidebar = color(isDark ? "1A1F21" : "E9EEEB");
        border = color(isDark ? "384144" : "DCE3DF");
        text = color(isDark ? "EDF1EE" : "202D27");
        muted = color(isDark ? "A5B1AD" : "5D6F65");
        accent = color(isDark ? "A7CEB7" : "34644D");
        accentText = color(isDark ? "1B3225" : "FFFFFF");
        selected = color(isDark ? "35473D" : "DCE9E0");
        danger = color(isDark ? "F0A69B" : "A33129");
        if (isDark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Component.arc", 10); UIManager.put("Button.arc", 10); UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 2); UIManager.put("Component.focusColor", accent);
        UIManager.put("Panel.background", background); UIManager.put("Label.foreground", text);
        UIManager.put("Button.background", surface); UIManager.put("Button.foreground", text);
        UIManager.put("TextField.background", surface); UIManager.put("PasswordField.background", surface);
        UIManager.put("TextField.foreground", text); UIManager.put("PasswordField.foreground", text);
        UIManager.put("Table.background", surface); UIManager.put("Table.foreground", text);
        UIManager.put("Table.selectionBackground", selected); UIManager.put("Table.selectionForeground", text);
        UIManager.put("TableHeader.background", surface); UIManager.put("TableHeader.foreground", muted);
        UIManager.put("TableHeader.height", 46); UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.gridColor", border); UIManager.put("Table.cellMargins", new Insets(0, 16, 0, 16));
        UIManager.put("ScrollPane.background", surface); UIManager.put("ScrollBar.track", surface);
        UIManager.put("ToolTip.background", surface); UIManager.put("ToolTip.foreground", text);
        UIManager.put("TitlePane.background", sidebar); UIManager.put("TitlePane.foreground", text);
        UIManager.put("OptionPane.background", background);
        UIManager.put("Component.borderColor", border);
    }
    public static Color color(String hex) { return Color.decode("#" + hex); }
    public static JLabel label(String value, int size, Color color, boolean bold) {
        JLabel label = new JLabel(value); label.putClientProperty("html.disable", true);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size)); label.setForeground(color); return label;
    }
    public static JButton button(String text, Runnable action) {
        JButton b = new JButton(text); b.addActionListener(e -> action.run()); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect"); b.setMargin(new Insets(11, 17, 11, 17)); return b;
    }
    public static JButton primary(String text, Runnable action) {
        JButton b = button(text, action); b.setBackground(accent); b.setForeground(accentText); b.setFont(b.getFont().deriveFont(Font.BOLD)); return b;
    }
    public static JPanel vertical() { JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p; }
    public static void gap(JPanel p, int height) { p.add(Box.createVerticalStrut(height)); }
    public static void align(JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); }
    public static void field(JPanel p, String name, JComponent input) {
        JLabel l = label(name, 13, muted, false); align(l); p.add(l); gap(p, 7); align(input);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); input.setPreferredSize(new Dimension(320, 42));
        p.add(input); gap(p, 18);
    }
}
