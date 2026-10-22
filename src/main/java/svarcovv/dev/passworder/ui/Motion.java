package svarcovv.dev.passworder.ui;

import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import javax.swing.*;
import java.util.function.DoubleConsumer;

/** Short, interruptible animations. Security transitions (locking) never retain a snapshot. */
final class Motion {
    private static boolean enabled = true;
    private Motion() {}
    static boolean enabled() { return enabled; }
    static void setEnabled(boolean value) { enabled = value; if (!value) stopTransitions(); }
    static void stopTransitions() { FlatAnimatedLafChange.stop(); }
    static void transition(JComponent root, int duration, Runnable update) {
        stopTransitions();
        boolean animate = enabled && root.isShowing();
        if (animate) { FlatAnimatedLafChange.duration = duration; FlatAnimatedLafChange.showSnapshot(); }
        try { update.run(); root.revalidate(); root.repaint(); if (root.getTopLevelAncestor() != null) root.getTopLevelAncestor().validate(); }
        finally { if (animate) FlatAnimatedLafChange.hideSnapshotWithAnimation(); }
    }
    static final class Value {
        private final Timer timer;
        private final DoubleConsumer changed;
        private double value, from, target;
        private long started;
        private int duration;
        private boolean bounce;
        Value(double initial, DoubleConsumer changed) {
            value = target = initial; this.changed = changed;
            timer = new Timer(16, e -> tick()); timer.setCoalesce(true);
        }
        double get() { return value; }
        void to(double next, int milliseconds, boolean spring) {
            if (next == target && timer.isRunning()) return;
            from = value; target = next; duration = milliseconds; bounce = spring; started = System.nanoTime();
            if (!enabled || milliseconds == 0) { stop(); value = next; changed.accept(value); }
            else if (Math.abs(value - target) > .0001) timer.restart();
        }
        private void tick() {
            double t = Math.min(1, (System.nanoTime() - started) / (duration * 1_000_000.0));
            double eased = t * t * (3 - 2 * t);
            if (bounce) { double u = t - 1; eased = 1 + 2.2 * u * u * u + 1.2 * u * u; }
            value = from + (target - from) * eased;
            if (t == 1) { value = target; timer.stop(); }
            changed.accept(value);
        }
        void stop() { timer.stop(); }
        boolean running() { return timer.isRunning(); }
    }
}
