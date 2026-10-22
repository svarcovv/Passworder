package svarcovv.dev.passworder.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class MotionTest {
    @Test void hoverAndPressKeepTheSameHitAreaAndLayout() throws Exception {
        var reference = new AtomicReference<MotionButton>();
        edt(() -> {
            Theme.apply(true); Motion.setEnabled(true);
            var button = new MotionButton("Сохранить", MotionButton.Style.PRIMARY, () -> {});
            button.setBounds(20, 30, 180, 44); reference.set(button);
        });
        Dimension preferred = reference.get().getPreferredSize();
        for (int i = 0; i < 8; i++) {
            int step = i;
            edt(() -> { var b = reference.get(); b.getModel().setRollover(step % 2 == 0); b.getModel().setArmed(step % 3 == 0); b.getModel().setPressed(step % 3 == 0); });
            Thread.sleep(35);
            edt(() -> {
                var b = reference.get(); assertEquals(new Rectangle(20, 30, 180, 44), b.getBounds()); assertEquals(preferred, b.getPreferredSize());
                assertTrue(b.contains(0, 0)); assertTrue(b.contains(179, 43)); assertFalse(b.contains(180, 44));
            });
        }
        edt(() -> { reference.get().getModel().setPressed(false); reference.get().removeNotify(); });
    }
    @Test void retargetingDoesNotJumpAndReducedMotionIsImmediate() throws Exception {
        var reference = new AtomicReference<Motion.Value>();
        edt(() -> { Motion.setEnabled(true); var value = new Motion.Value(0, v -> {}); value.to(1, 180, false); reference.set(value); });
        Thread.sleep(75);
        edt(() -> { var value = reference.get(); double before = value.get(); assertTrue(before > 0 && before < 1); value.to(0, 120, false); assertEquals(before, value.get()); });
        Thread.sleep(200);
        edt(() -> {
            assertEquals(0, reference.get().get(), .0001); assertFalse(reference.get().running());
            Motion.setEnabled(false); reference.get().to(1, 200, false); assertEquals(1, reference.get().get()); assertFalse(reference.get().running()); Motion.setEnabled(true);
        });
    }
    @Test void smoothScrollRespectsManualChangesAndDoesNotOvershoot() throws Exception {
        var reference = new AtomicReference<SmoothScrollPane>();
        edt(() -> {
            Theme.apply(true); Motion.setEnabled(true); JPanel view = new JPanel(); view.setPreferredSize(new Dimension(400, 2400));
            var scroll = new SmoothScrollPane(view); scroll.setSize(500, 300); scroll.doLayout(); reference.set(scroll);
            scroll.getVerticalScrollBar().setValues(0, 300, 0, 2400);
            wheel(scroll, 3);
        });
        Thread.sleep(450);
        edt(() -> { var scroll = reference.get(); assertTrue(scroll.getVerticalScrollBar().getValue() > 0); scroll.getVerticalScrollBar().setValue(500); wheel(scroll, -1); });
        Thread.sleep(450);
        edt(() -> { int value = reference.get().getVerticalScrollBar().getValue(); assertTrue(value < 500 && value > 300); reference.get().removeNotify(); });
    }
    @Test void svgIconsStayInsideTheirDeclaredSizeAtDesktopScaling() throws Exception {
        edt(() -> {
            Theme.apply(true);
            for (String name : new String[]{"grid", "lock", "settings", "copy", "edit", "sun", "moon", "shield"}) {
                var icon = new Glyph(name, 18, Color.CYAN); var pixels = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                var g = pixels.createGraphics(); icon.paintIcon(null, g, 4, 4); g.dispose(); int count = 0;
                for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) if ((pixels.getRGB(x, y) >>> 24) != 0) {
                    count++; assertTrue(x >= 4 && x < 22 && y >= 4 && y < 22, name + " painted outside its bounds");
                }
                assertTrue(count > 10, name + " is missing");
            }
        });
    }
    private static void wheel(SmoothScrollPane scroll, int rotation) {
        scroll.dispatchEvent(new MouseWheelEvent(scroll, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, 30, 30, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation));
    }
    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
}
