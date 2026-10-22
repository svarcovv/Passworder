package svarcovv.dev.passworder.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

final class SmoothScrollPane extends JScrollPane {
    private final Timer timer;
    private double position, target;
    private boolean changing;
    SmoothScrollPane(Component view) {
        super(view); setBorder(BorderFactory.createEmptyBorder());
        timer = new Timer(16, e -> tick()); timer.setCoalesce(true);
        getVerticalScrollBar().setUnitIncrement(24);
        getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!changing && timerReady()) { timer.stop(); position = target = e.getValue(); }
        });
    }
    private boolean timerReady() { return timer != null; }
    @Override protected void processMouseWheelEvent(MouseWheelEvent event) {
        var bar = event.isShiftDown() ? getHorizontalScrollBar() : getVerticalScrollBar();
        if (!Motion.enabled() || event.isControlDown() || !bar.isVisible() || event.isShiftDown()) { super.processMouseWheelEvent(event); return; }
        double delta = event.getPreciseWheelRotation() * (event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                ? bar.getBlockIncrement() : event.getScrollAmount() * bar.getUnitIncrement());
        if (!timer.isRunning()) position = target = bar.getValue();
        else if (Math.signum(delta) != Math.signum(target - position)) target = position;
        target = Math.max(bar.getMinimum(), Math.min(bar.getMaximum() - bar.getVisibleAmount(), target + delta));
        timer.start(); event.consume();
    }
    private void tick() {
        var bar = getVerticalScrollBar();
        target = Math.max(bar.getMinimum(), Math.min(bar.getMaximum() - bar.getVisibleAmount(), target));
        position += (target - position) * .28;
        if (Math.abs(target - position) < .6) { position = target; timer.stop(); }
        changing = true;
        try { bar.setValue((int) Math.round(position)); } finally { changing = false; }
    }
    @Override public void removeNotify() { if (timer != null) timer.stop(); super.removeNotify(); }
}
