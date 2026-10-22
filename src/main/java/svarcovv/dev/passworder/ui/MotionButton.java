package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;

/** The painted surface can squash, but the hit rectangle and layout never move. */
final class MotionButton extends JButton {
    enum Style { PRIMARY, SECONDARY, GHOST, NAV, ICON }
    private Style style;
    private boolean current;
    private boolean keyboardFocus;
    private final Motion.Value hover = new Motion.Value(0, value -> repaint());
    private final Motion.Value press = new Motion.Value(0, value -> repaint());

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
            press.to(down ? 1 : 0, down ? 85 : 210, !down);
        });
        setForeground(style == Style.PRIMARY ? Theme.accentText : Theme.text);
    }
    void setCurrent(boolean value) { current = value; repaint(); }
    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize(); return new Dimension(Math.max(42, size.width), 44);
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double h = hover.get(), p = press.get();
        // Leave a fixed inset, including the tiny release overshoot.
        g.translate(getWidth() / 2.0, getHeight() / 2.0);
        g.scale(1 + .012 * p, 1 - .038 * p); g.translate(-getWidth() / 2.0, -getHeight() / 2.0);
        Color base = switch (style) {
            case PRIMARY -> Theme.primary;
            case SECONDARY -> Theme.surface;
            case NAV -> current ? Theme.selected : Theme.sidebar;
            default -> getParent() == null ? Theme.background : getParent().getBackground();
        };
        Color fill = Theme.mix(base, style == Style.PRIMARY ? Color.WHITE : Theme.text,
                (style == Style.PRIMARY ? .055 : .045) * h + Math.max(0, p) * .025);
        if (!isEnabled()) g.setComposite(AlphaComposite.SrcOver.derive(.48f));
        var shape = new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
        g.setColor(fill); g.fill(shape);
        if (style == Style.SECONDARY) { g.setColor(Theme.border); g.setStroke(new BasicStroke(1)); g.draw(shape); }
        if (style == Style.NAV && current) {
            g.setColor(Theme.accent); g.fillRoundRect(3, getHeight() / 2 - 8, 2, 16, 2, 2);
        }
        if (isFocusOwner() && keyboardFocus) {
            g.setColor(Theme.accent); g.setStroke(new BasicStroke(1));
            g.draw(new RoundRectangle2D.Double(3.5, 3.5, getWidth() - 7, getHeight() - 7, 10, 10));
        }
        super.paintComponent(g); g.dispose();
    }
    @Override public void removeNotify() { hover.stop(); press.stop(); super.removeNotify(); }
}
