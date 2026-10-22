package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;

final class FeedbackLabel extends JLabel {
    private final Motion.Value highlight = new Motion.Value(0, value -> repaint());
    FeedbackLabel() { super(" "); putClientProperty("html.disable", true); }
    void showMessage(String message) {
        setText(message); setForeground(Theme.accent);
        highlight.to(1, 0, false); highlight.to(0, 600, false);
    }
    void clearMessage() { highlight.stop(); setText(" "); }
    void reset() { highlight.stop(); setText(" "); }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(Theme.mix(getBackground(), Theme.accent, .08 * highlight.get()));
        if (highlight.get() > 0) g.fillRoundRect(0, 0, Math.min(getWidth(), getPreferredSize().width + 10), getHeight(), 6, 6);
        super.paintComponent(g); g.dispose();
    }
    @Override public void removeNotify() { highlight.stop(); super.removeNotify(); }
}
