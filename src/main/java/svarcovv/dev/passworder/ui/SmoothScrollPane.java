package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

final class SmoothScrollPane extends JScrollPane {
    private double position, target, velocity;
    private long previousTick;
    private boolean changing;
    private final Motion.Frame animation = new Motion.Frame() {
        @Override public void tick(long now) {
            double seconds = (now - previousTick) / 1_000_000_000.0; previousTick = now;
            target = bounded(target);
            // Exact critically damped spring. Wheel bursts preserve position and velocity.
            double omega = 24, distance = position - target, c = velocity + omega * distance;
            double decay = Math.exp(-omega * seconds);
            position = bounded(target + (distance + c * seconds) * decay);
            velocity = (velocity - omega * c * seconds) * decay;
            if (Math.abs(target - position) < .25 && Math.abs(velocity) < 4) finish();
            else scrollTo(position);
        }
        @Override public void finish() { Motion.stop(this); velocity = 0; position = target = bounded(target); scrollTo(position); }
    };
    SmoothScrollPane(Component view) {
        super(view); setBorder(BorderFactory.createEmptyBorder());
        getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        getVerticalScrollBar().setUnitIncrement(24);
        getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!changing && e.getValue() != (int) Math.round(position)) {
                Motion.stop(animation); velocity = 0; position = target = e.getValue();
            }
        });
    }
    private double bounded(double value) {
        var bar = getVerticalScrollBar();
        return Math.max(bar.getMinimum(), Math.min(bar.getMaximum() - bar.getVisibleAmount(), value));
    }
    @Override protected void processMouseWheelEvent(MouseWheelEvent event) {
        var bar = getVerticalScrollBar();
        if (!Motion.enabled() || event.isControlDown() || !bar.isVisible() || event.isShiftDown()) { super.processMouseWheelEvent(event); return; }
        double delta = event.getPreciseWheelRotation() * (event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                ? bar.getBlockIncrement() : event.getScrollAmount() * bar.getUnitIncrement());
        if (!Motion.running(animation)) {
            if ((int) Math.round(position) != bar.getValue()) position = target = bar.getValue();
            velocity = 0; previousTick = System.nanoTime();
        }
        else if (Math.signum(delta) != Math.signum(target - position)) target = position;
        target = bounded(target + delta);
        boolean precise = event.getPreciseWheelRotation() != event.getWheelRotation();
        if (precise) animation.finish(); else Motion.start(animation);
        event.consume();
    }
    private void scrollTo(double value) {
        changing = true;
        try { getVerticalScrollBar().setValue((int) Math.round(value)); } finally { changing = false; }
    }
    @Override public void removeNotify() { Motion.stop(animation); super.removeNotify(); }
}
