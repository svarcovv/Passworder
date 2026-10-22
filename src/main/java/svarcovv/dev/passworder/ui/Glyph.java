package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

public record Glyph(String kind, int size, Color color) implements Icon {
    private static final ConcurrentHashMap<String, FlatSVGIcon> CACHE = new ConcurrentHashMap<>();
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
        String name = switch (kind) { case "edit" -> "edit-2"; case "more" -> "more-horizontal"; case "link" -> "external-link"; default -> kind; };
        String cacheKey = name + ":" + size + ":" + color.getRGB();
        var icon = CACHE.computeIfAbsent(cacheKey, key -> new FlatSVGIcon("icons/feather/" + name + ".svg", size, size)
                .setColorFilter(new FlatSVGIcon.ColorFilter(original -> color)));
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y); g.scale(size / (double) icon.getIconWidth(), size / (double) icon.getIconHeight());
        icon.paintIcon(c, g, 0, 0); g.dispose();
    }
    public static BufferedImage appImage(int size) {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.color("245D87")); g.fillRoundRect(0, 0, size, size, size / 3, size / 3);
        int inner = size * 5 / 8, inset = (size - inner) / 2;
        new Glyph("shield", inner, Theme.color("ECF8FF")).paintIcon(null, g, inset, inset); g.dispose(); return image;
    }
}
