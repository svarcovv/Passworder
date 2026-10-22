package svarcovv.dev.passworder.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;

/** Motion is confined to the backdrop; text, icons and hit bounds stay fixed. */
final class MotionButton extends JButton {
    enum Style { PRIMARY, SECONDARY, GHOST, NAV, ICON }
    private final Style style;
    private boolean keyboardFocus;
    private final Motion.Value hover = new Motion.Value(0, value -> repaint());
    private final Motion.Value press = new Motion.Value(0, value -> repaint());
    private final Motion.Value selection = new Motion.Value(0, value -> repaint());

    MotionButton(String text, Style style, Runnable action) {
        super(text); this.style = style;
        putClientProperty("html.disable", true); setOpaque(false); setContentAreaFilled(false);
        setBorderPainted(false); setFocusPainted(false); setRolloverEnabled(true);
        setMargin(new Insets(10, 16, 10, 16)); setIconTextGap(10);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setFont(new Font("Segoe UI", Font.PLAIN, 14));
        addActionListener(e -> action.run());
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                keyboardFocus = switch (e.getCause()) {
                    case TRAVERSAL, TRAVERSAL_FORWARD, TRAVERSAL_BACKWARD, TRAVERSAL_UP, TRAVERSAL_DOWN -> true;
                    default -> false;
                }; repaint();
            }
        });
        addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { keyboardFocus = false; repaint(); } });
        getModel().addChangeListener(e -> {
            hover.to(getModel().isRollover() && isEnabled() ? 1 : 0, 150, false);
            boolean down = getModel().isPressed() && getModel().isArmed();
            press.to(down ? 1 : 0, down ? 95 : 180, false);
        });
        setForeground(style == Style.PRIMARY ? Theme.accentText : Theme.text);
    }
    void setCurrent(boolean value) { selection.to(value ? 1 : 0, isShowing() ? 180 : 0, false); }
    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize(); return new Dimension(Math.max(42, size.width), 44);
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double h = hover.get(), p = press.get();
        Color base = switch (style) {
            case PRIMARY -> Theme.primary;
            case SECONDARY -> Theme.surface;
            case NAV -> Theme.mix(Theme.sidebar, Theme.selected, selection.get());
            default -> getParent() == null ? Theme.background : getParent().getBackground();
        };
        Color fill = Theme.mix(base, style == Style.PRIMARY ? Color.WHITE : Theme.text,
                (style == Style.PRIMARY ? .055 : .045) * h + Math.max(0, p) * .025);
        if (!isEnabled()) g.setComposite(AlphaComposite.SrcOver.derive(.48f));
        double inset = 2 + .6 * p;
        var shape = new RoundRectangle2D.Double(inset, inset, getWidth() - inset * 2, getHeight() - inset * 2, 12, 12);
        g.setColor(fill); g.fill(shape);
        if (style == Style.SECONDARY) { g.setColor(Theme.border); g.setStroke(new BasicStroke(1)); g.draw(shape); }
        if (style == Style.NAV && selection.get() > 0) {
            g.setColor(Theme.mix(fill, Theme.accent, selection.get())); g.fillRoundRect(3, getHeight() / 2 - 8, 2, 16, 2, 2);
        }
        if (isFocusOwner() && keyboardFocus) {
            g.setColor(Theme.accent); g.setStroke(new BasicStroke(1));
            g.draw(new RoundRectangle2D.Double(3.5, 3.5, getWidth() - 7, getHeight() - 7, 10, 10));
        }
        g.dispose(); paintContent(graphics);
    }
    private void paintContent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create(); g.setFont(getFont());
        FontMetrics metrics = g.getFontMetrics(); Insets insets = getInsets();
        Rectangle view = new Rectangle(insets.left, insets.top,
                getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
        Rectangle iconRect = new Rectangle(), textRect = new Rectangle();
        Icon icon = isEnabled() ? getIcon() : getDisabledIcon();
        String text = SwingUtilities.layoutCompoundLabel(this, metrics, getText(), icon,
                getVerticalAlignment(), getHorizontalAlignment(), getVerticalTextPosition(), getHorizontalTextPosition(),
                view, iconRect, textRect, getIconTextGap());
        if (icon != null) icon.paintIcon(this, g, iconRect.x, iconRect.y);
        g.setColor(isEnabled() ? getForeground() : Theme.muted);
        BasicGraphicsUtils.drawStringUnderlineCharAt(this, g, text, getDisplayedMnemonicIndex(), textRect.x, textRect.y + metrics.getAscent());
        g.dispose();
    }
    @Override public void removeNotify() { hover.stop(); press.stop(); selection.stop(); super.removeNotify(); }
}
