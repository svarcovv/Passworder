package svarcovv.dev.passworder.platform;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.*;
import java.io.IOException;
import java.nio.file.*;

/** Migration for pre-handover releases, scoped to the owner of this profile's lock. */
final class LegacyInstance {
    private LegacyInstance() {}
    interface RestartManager extends StdCallLibrary {
        RestartManager API = Native.load("Rstrtmgr", RestartManager.class, W32APIOptions.UNICODE_OPTIONS);
        int RmStartSession(IntByReference session, int flags, char[] key);
        int RmRegisterResources(int session, int fileCount, StringArray files, int processCount, UniqueProcess[] processes, int serviceCount, StringArray services);
        int RmGetList(int session, IntByReference needed, IntByReference count, ProcessInfo[] processes, IntByReference reasons);
        int RmShutdown(int session, int flags, Pointer callback);
        int RmEndSession(int session);
    }
    @Structure.FieldOrder({"pid", "started"})
    public static class UniqueProcess extends Structure {
        public int pid; public WinBase.FILETIME started = new WinBase.FILETIME();
    }
    @Structure.FieldOrder({"process", "name", "service", "type", "status", "session", "restartable"})
    public static class ProcessInfo extends Structure {
        public UniqueProcess process = new UniqueProcess();
        public char[] name = new char[256], service = new char[64];
        public int type, status, session, restartable;
    }
    static void close(Path lockFile) throws IOException {
        if (!Platform.isWindows()) throw new IOException("Закройте предыдущую версию Passworder.");
        ProcessInfo[] owners = owners(lockFile);
        if (owners.length == 0) return;
        var processes = (UniqueProcess[]) new UniqueProcess().toArray(owners.length);
        for (int i = 0; i < owners.length; i++) {
            var owner = owners[i].process;
            ProcessHandle process = ProcessHandle.of(Integer.toUnsignedLong(owner.pid)).orElse(null);
            if (process == null) continue;
            Path executable = Path.of(process.info().command().orElse(""));
            Path config = executable.resolveSibling("app").resolve("Passworder.cfg");
            if (!executable.getFileName().toString().equalsIgnoreCase("Passworder.exe") || !Files.isRegularFile(config)
                    || !Files.readString(config).contains("app.mainclass=svarcovv.dev.passworder.Passworder"))
                throw new IOException("Закройте предыдущую версию Passworder перед обновлением.");
            if (hasDialog(owner.pid)) throw new IOException("Сохраните или закройте окно редактирования в предыдущей версии Passworder.");
            processes[i].pid = owner.pid; processes[i].started = owner.started;
        }
        int session = start();
        try {
            check(RestartManager.API.RmRegisterResources(session, 0, null, processes.length, processes, 0, null));
            check(RestartManager.API.RmShutdown(session, 0, null));
        } finally { RestartManager.API.RmEndSession(session); }
    }
    private static ProcessInfo[] owners(Path file) throws IOException {
        int session = start();
        try {
            check(RestartManager.API.RmRegisterResources(session, 1, new StringArray(new String[]{file.toAbsolutePath().toString()}, true), 0, null, 0, null));
            var needed = new IntByReference(); var count = new IntByReference(); var reasons = new IntByReference();
            int status = RestartManager.API.RmGetList(session, needed, count, null, reasons);
            if (status == 0 && needed.getValue() == 0) return new ProcessInfo[0];
            if (status != 234 || needed.getValue() < 1 || needed.getValue() > 16) throw new IOException("Не удалось определить работающий экземпляр Passworder.");
            var result = (ProcessInfo[]) new ProcessInfo().toArray(needed.getValue()); count.setValue(result.length);
            check(RestartManager.API.RmGetList(session, needed, count, result, reasons));
            return java.util.Arrays.copyOf(result, count.getValue());
        } finally { RestartManager.API.RmEndSession(session); }
    }
    private static int start() throws IOException {
        var session = new IntByReference(); check(RestartManager.API.RmStartSession(session, 0, new char[33])); return session.getValue();
    }
    private static void check(int status) throws IOException { if (status != 0) throw new IOException("Не удалось завершить предыдущую версию Passworder (Windows " + status + ")."); }
    private static boolean hasDialog(int pid) {
        boolean[] found = {false};
        User32.INSTANCE.EnumWindows((window, data) -> {
            var owner = new IntByReference(); User32.INSTANCE.GetWindowThreadProcessId(window, owner);
            if (owner.getValue() == pid && User32.INSTANCE.IsWindowVisible(window)) {
                char[] name = new char[128]; User32.INSTANCE.GetClassName(window, name, name.length);
                if (Native.toString(name).equals("SunAwtDialog")) found[0] = true;
            }
            return true;
        }, null);
        return found[0];
    }
}
