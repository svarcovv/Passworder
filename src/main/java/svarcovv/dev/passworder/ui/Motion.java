package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.DoubleConsumer;

/** One EDT clock. Locking disposes all captured pixels immediately. */
final class Motion {
    private static boolean enabled = true;
    interface Frame { void tick(long now); void finish(); }
    private static final Set<Frame> active = new LinkedHashSet<>();
    private static final Timer clock = new Timer(8, e -> tick());
    private static Reveal reveal;
    private Motion() {}
    static boolean enabled() { return enabled; }
    static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            stopTransitions();
            for (Frame animation : Set.copyOf(active)) animation.finish();
        }
    }
    private static void tick() {
        long now = System.nanoTime();
        for (Frame animation : Set.copyOf(active)) if (active.contains(animation)) animation.tick(now);
        if (active.isEmpty()) clock.stop();
    }
    static void start(Frame frame) { active.add(frame); if (!clock.isRunning()) clock.start(); }
    static void stop(Frame frame) { active.remove(frame); if (active.isEmpty()) clock.stop(); }
    static boolean running(Frame frame) { return active.contains(frame); }
    static void stopTransitions() { if (reveal != null) reveal.close(); }
    static void reveal(JComponent root, Point origin, Runnable update) {
        stopTransitions();
        if (!enabled || !root.isShowing()) { update.run(); return; }
        BufferedImage before = snapshot(root);
        try {
            update.run(); root.revalidate(); root.getTopLevelAncestor().validate();
            BufferedImage after = snapshot(root);
            reveal = new Reveal(root, before, after, origin); reveal.start();
        } catch (RuntimeException | Error error) { before.flush(); throw error; }
    }
    private static BufferedImage snapshot(JComponent root) {
        var transform = root.getGraphicsConfiguration().getDefaultTransform();
        var image = new BufferedImage((int) Math.ceil(root.getWidth() * transform.getScaleX()),
                (int) Math.ceil(root.getHeight() * transform.getScaleY()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.scale(transform.getScaleX(), transform.getScaleY());
        root.paint(g); g.dispose(); return image;
    }
    private static final class Reveal extends JComponent {
        private final JComponent root;
        private final JLayeredPane layer;
        private final BufferedImage before, after;
        private final Point origin;
        private final double radius;
        private final Value progress = new Value(0, value -> repaint());
        private final ComponentListener resize = new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { close(); }
        };
        Reveal(JComponent root, BufferedImage before, BufferedImage after, Point origin) {
            this.root = root; this.layer = root.getRootPane().getLayeredPane();
            this.before = before; this.after = after; this.origin = origin;
            radius = Math.hypot(Math.max(origin.x, root.getWidth() - origin.x), Math.max(origin.y, root.getHeight() - origin.y));
            setOpaque(true); setBounds(SwingUtilities.convertRectangle(root, new Rectangle(root.getSize()), layer));
            addMouseListener(new MouseAdapter() {}); addMouseMotionListener(new MouseMotionAdapter() {});
            addMouseWheelListener(MouseWheelEvent::consume);
        }
        void start() {
            layer.add(this, JLayeredPane.DRAG_LAYER); root.addComponentListener(resize); progress.to(1, 360, false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(before, 0, 0, getWidth(), getHeight(), null);
            double r = radius * progress.get();
            g.clip(new Ellipse2D.Double(origin.x - r, origin.y - r, r * 2, r * 2));
            g.drawImage(after, 0, 0, getWidth(), getHeight(), null); g.dispose();
            if (!progress.running()) close();
        }
        void close() {
            progress.stop(); root.removeComponentListener(resize);
            Rectangle dirty = getBounds(); layer.remove(this); layer.repaint(dirty);
            before.flush(); after.flush(); if (reveal == this) reveal = null;
        }
    }
    static final class Value implements Frame {
        private final DoubleConsumer changed;
        private double value, from, target;
        private long started;
        private int duration;
        Value(double initial, DoubleConsumer changed) { value = target = initial; this.changed = changed; }
        double get() { return value; }
        void to(double next, int milliseconds, boolean spring) {
            if (next == target && running()) return;
            from = value; target = next; duration = milliseconds; started = System.nanoTime();
            if (!enabled || milliseconds <= 0) finish();
            else if (Math.abs(value - target) > .0001) Motion.start(this);
        }
        @Override public void tick(long now) {
            double t = Math.min(1, (now - started) / (duration * 1_000_000.0));
            double eased = t * t * (3 - 2 * t);
            value = from + (target - from) * eased;
            if (t == 1) { value = target; stop(); }
            changed.accept(value);
        }
        @Override public void finish() { stop(); value = target; changed.accept(value); }
        void stop() { Motion.stop(this); }
        boolean running() { return Motion.running(this); }
    }
}
