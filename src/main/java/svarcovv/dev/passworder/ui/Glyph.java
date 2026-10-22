package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public record Glyph(String kind, int size, Color color) implements Icon {
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create(); g.translate(x, y); g.scale(size / 24.0, size / 24.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color); g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case "lock" -> { g.draw(new RoundRectangle2D.Double(5, 10, 14, 11, 3, 3)); g.draw(new Arc2D.Double(8, 2, 8, 13, 0, 180, Arc2D.OPEN)); g.drawLine(12, 14, 12, 17); }
            case "grid" -> { for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) g.draw(new RoundRectangle2D.Double(4 + a * 10, 4 + b * 10, 6, 6, 1, 1)); }
            case "key" -> { g.drawOval(3, 3, 10, 10); g.drawLine(12, 12, 21, 21); g.drawLine(16, 16, 19, 13); g.drawLine(19, 19, 22, 16); }
            case "settings" -> { g.drawOval(4, 4, 16, 16); g.drawOval(9, 9, 6, 6); for (int i = 0; i < 8; i++) { double t = i * Math.PI / 4; g.draw(new Line2D.Double(12 + 8 * Math.cos(t), 12 + 8 * Math.sin(t), 12 + 10 * Math.cos(t), 12 + 10 * Math.sin(t))); } }
            case "search" -> { g.drawOval(4, 3, 12, 12); g.drawLine(15, 14, 21, 20); }
            case "plus" -> { g.drawLine(12, 5, 12, 19); g.drawLine(5, 12, 19, 12); }
            case "copy" -> { g.draw(new RoundRectangle2D.Double(8, 8, 12, 13, 2, 2)); g.drawLine(4, 15, 4, 3); g.drawLine(4, 3, 15, 3); }
            case "edit" -> { Path2D p = new Path2D.Double(); p.moveTo(4, 15); p.lineTo(16, 3); p.lineTo(21, 8); p.lineTo(9, 20); p.lineTo(3, 21); p.closePath(); g.draw(p); g.drawLine(13, 6, 18, 11); }
            case "eye" -> { g.draw(new Ellipse2D.Double(2, 6, 20, 12)); g.drawOval(9, 9, 6, 6); }
            case "more" -> { g.fillOval(4, 10, 3, 3); g.fillOval(11, 10, 3, 3); g.fillOval(18, 10, 3, 3); }
            case "link" -> { g.drawLine(7, 17, 20, 4); g.drawLine(12, 4, 20, 4); g.drawLine(20, 4, 20, 12); g.drawLine(4, 7, 4, 21); g.drawLine(4, 21, 17, 21); }
            case "star" -> { Path2D p = new Path2D.Double(); for (int i = 0; i < 10; i++) { double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? 10 : 4.5; double px = 12 + r * Math.cos(a), py = 12 + r * Math.sin(a); if (i == 0) p.moveTo(px, py); else p.lineTo(px, py); } p.closePath(); g.draw(p); }
            default -> g.drawOval(3, 3, 18, 18);
        }
        g.dispose();
    }
    public static BufferedImage appImage(int size) {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.color("34644D")); g.fillRoundRect(0, 0, size, size, size / 3, size / 3);
        new Glyph("lock", size * 2 / 3, Theme.color("E1EEE5")).paintIcon(null, g, size / 6, size / 6); g.dispose(); return image;
    }
}
