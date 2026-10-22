package svarcovv.dev.passworder.ui;

import com.formdev.flatlaf.*;
import javax.swing.*;
import java.awt.*;

public final class Theme {
    public static Color background, surface, sidebar, border, text, muted, accent, primary, accentText, selected, danger;
    public static boolean dark;
    private Theme() {}
    public static void apply(boolean isDark) {
        dark = isDark;
        background = color(isDark ? "151C26" : "F5F7FA");
        surface = color(isDark ? "1B2430" : "FFFFFF");
        sidebar = color(isDark ? "111822" : "EDF1F6");
        border = color(isDark ? "2D3949" : "DFE5ED");
        text = color(isDark ? "E9EFF7" : "1C293B");
        muted = color(isDark ? "9DAEC2" : "63748B");
        accent = color(isDark ? "68C3D5" : "16758D");
        primary = color(isDark ? "285F84" : "245D87");
        accentText = color("F5FAFF");
        selected = color(isDark ? "1D2C3E" : "DFE8F3");
        danger = color(isDark ? "F0A69B" : "A33129");
        if (isDark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Component.arc", 8); UIManager.put("Button.arc", 8); UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1); UIManager.put("Component.innerFocusWidth", 0); UIManager.put("Component.focusColor", accent);
        UIManager.put("Component.innerOutlineWidth", 0);
        UIManager.put("TextField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("PasswordField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("ComboBox.background", surface); UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.buttonBackground", surface); UIManager.put("ComboBox.buttonEditableBackground", surface);
        UIManager.put("ComboBox.buttonArrowColor", muted);
        UIManager.put("Panel.background", background); UIManager.put("Label.foreground", text);
        UIManager.put("Button.background", surface); UIManager.put("Button.foreground", text);
        UIManager.put("TextField.background", surface); UIManager.put("PasswordField.background", surface);
        UIManager.put("TextField.foreground", text); UIManager.put("PasswordField.foreground", text);
        UIManager.put("Table.background", surface); UIManager.put("Table.foreground", text);
        UIManager.put("Table.selectionBackground", selected); UIManager.put("Table.selectionForeground", text);
        UIManager.put("TableHeader.background", surface); UIManager.put("TableHeader.foreground", muted);
        UIManager.put("TableHeader.height", 44); UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("TableHeader.separatorColor", surface); UIManager.put("TableHeader.bottomSeparatorColor", border);
        UIManager.put("TableHeader.cellBorder", BorderFactory.createEmptyBorder(0, 16, 0, 16));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(0, 16, 0, 16));
        UIManager.put("Table.focusSelectedCellHighlightBorder", BorderFactory.createEmptyBorder(0, 16, 0, 16));
        UIManager.put("Table.gridColor", border); UIManager.put("Table.cellMargins", new Insets(0, 16, 0, 16));
        UIManager.put("ScrollPane.background", surface); UIManager.put("ScrollBar.track", surface);
        UIManager.put("ToolTip.background", surface); UIManager.put("ToolTip.foreground", text);
        UIManager.put("TitlePane.background", background); UIManager.put("TitlePane.foreground", text);
        UIManager.put("TitlePane.inactiveBackground", background); UIManager.put("TitlePane.unifiedBackground", false);
        UIManager.put("ScrollBar.width", 10); UIManager.put("ScrollBar.thumbArc", 10);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.thumb", border); UIManager.put("ScrollBar.hoverThumbColor", muted);
        UIManager.put("ScrollPane.smoothScrolling", true);
        UIManager.put("Slider.thumbColor", accent); UIManager.put("Slider.trackValueColor", primary);
        UIManager.put("CheckBox.icon.selectedBackground", primary); UIManager.put("ComboBox.selectionBackground", selected);
        UIManager.put("OptionPane.background", background);
        UIManager.put("Component.borderColor", border);
    }
    public static Color color(String hex) { return Color.decode("#" + hex); }
    public static Color mix(Color from, Color to, double amount) {
        double t = Math.max(0, Math.min(1, amount));
        return new Color((int) Math.round(from.getRed() * (1 - t) + to.getRed() * t),
                (int) Math.round(from.getGreen() * (1 - t) + to.getGreen() * t),
                (int) Math.round(from.getBlue() * (1 - t) + to.getBlue() * t));
    }
    public static JLabel label(String value, int size, Color color, boolean bold) {
        JLabel label = new JLabel(value); label.putClientProperty("html.disable", true);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size)); label.setForeground(color); return label;
    }
    public static JButton button(String text, Runnable action) {
        return new MotionButton(text, MotionButton.Style.SECONDARY, action);
    }
    public static JButton primary(String text, Runnable action) {
        return new MotionButton(text, MotionButton.Style.PRIMARY, action);
    }
    static JButton icon(String name, String tooltip, Runnable action) {
        var button = new MotionButton("", MotionButton.Style.ICON, action);
        button.setIcon(new Glyph(name, 20, muted)); button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setPreferredSize(new Dimension(42, 42)); button.setMinimumSize(new Dimension(42, 42)); return button;
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
