package svarcovv.dev.passworder.platform;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.*;
import java.util.concurrent.*;

/** RegisterHotKey observes only the chosen shortcut, never typed passwords. */
public final class GlobalHotkey implements AutoCloseable {
    private volatile int threadId;
    private Thread thread;
    public synchronized boolean register(String shortcut, Runnable action) {
        close();
        if (!Platform.isWindows()) return false;
        var ready = new CompletableFuture<Boolean>();
        thread = new Thread(() -> {
            boolean registered = false;
            try {
                threadId = Kernel32.INSTANCE.GetCurrentThreadId();
                int modifiers = 0x4000 | 0x0002 | (shortcut.contains("Alt") ? 0x0001 : 0x0004);
                int key = shortcut.endsWith("Space") ? 0x20 : 0x50;
                registered = User32.INSTANCE.RegisterHotKey(null, 0x5044, modifiers, key);
                ready.complete(registered);
                if (!registered) return;
                var message = new WinUser.MSG();
                int result;
                while ((result = User32.INSTANCE.GetMessage(message, null, 0, 0)) > 0) {
                    if (message.message == 0x0312) javax.swing.SwingUtilities.invokeLater(action);
                }
            } catch (Throwable e) { ready.complete(false); }
            finally {
                if (registered) User32.INSTANCE.UnregisterHotKey(null, 0x5044);
                threadId = 0;
            }
        }, "passworder-hotkey");
        thread.setDaemon(true); thread.start();
        try { return ready.get(3, TimeUnit.SECONDS); }
        catch (Exception e) { close(); return false; }
    }
    @Override public synchronized void close() {
        if (threadId != 0) User32.INSTANCE.PostThreadMessage(threadId, 0x0012, null, null);
        if (thread != null && thread != Thread.currentThread()) {
            try { thread.join(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        thread = null;
    }
}
