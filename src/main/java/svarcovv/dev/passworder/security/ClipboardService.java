package svarcovv.dev.passworder.security;

import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.*;
import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.*;

/** Clear only our own clipboard contents; never overwrite something copied later. */
public final class ClipboardService implements ClipboardOwner {
    private interface ClipboardApi extends StdCallLibrary {
        ClipboardApi INSTANCE = Native.load("user32", ClipboardApi.class, W32APIOptions.DEFAULT_OPTIONS);
        WinDef.HWND GetClipboardOwner();
    }
    private final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private Transferable owned;
    private Timer timer;
    public void copy(String value) {
        clear();
        var selection = new StringSelection(value);
        clipboard.setContents(selection, this); owned = selection;
        timer = new Timer(20_000, e -> clear()); timer.setRepeats(false); timer.start();
    }
    public void clear() {
        if (timer != null) { timer.stop(); timer = null; }
        if (owned != null) {
            try {
                // Windows may return a Transferable proxy, so object identity is insufficient.
                // Check both native ownership and data to preserve later copies from other apps.
                boolean ours = true;
                if (Platform.isWindows()) {
                    var owner = ClipboardApi.INSTANCE.GetClipboardOwner();
                    var process = new IntByReference();
                    if (owner != null) User32.INSTANCE.GetWindowThreadProcessId(owner, process);
                    ours = owner != null && process.getValue() == Kernel32.INSTANCE.GetCurrentProcessId();
                }
                if (ours && owned.getTransferData(DataFlavor.stringFlavor).equals(clipboard.getData(DataFlavor.stringFlavor)))
                    clipboard.setContents(new StringSelection(""), null);
            } catch (IllegalStateException | UnsupportedFlavorException | java.io.IOException ignored) {}
            owned = null;
        }
    }
    @Override public void lostOwnership(Clipboard clipboard, Transferable contents) {
        javax.swing.SwingUtilities.invokeLater(() -> { if (owned == contents) { owned = null; if (timer != null) timer.stop(); } });
    }
}
