package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;

final class FeedbackLabel extends JLabel {
    private final Motion.Value opacity = new Motion.Value(1, value -> repaint());
    FeedbackLabel() { super(" "); putClientProperty("html.disable", true); }
    void showMessage(String message) {
        setText(message); setForeground(Theme.accent);
        opacity.to(.35, 0, false); opacity.to(1, 180, false);
    }
    void clearMessage() { opacity.to(0, 180, false); }
    void reset() { opacity.stop(); setText(" "); }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, opacity.get()))));
        super.paintComponent(g); g.dispose();
    }
    @Override public void removeNotify() { opacity.stop(); super.removeNotify(); }
}
