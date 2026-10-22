package svarcovv.dev.passworder.platform;

import java.io.IOException;
import java.nio.file.Path;

public final class LaunchCoordinator {
    private LaunchCoordinator() {}
    public static SingleInstance acquire(Path directory, String version, String build, boolean preparingInstall) throws IOException, InterruptedException {
        var instance = new SingleInstance(directory, version, build);
        try {
            if (instance.primary()) return instance;
            if (!preparingInstall && instance.sameBuild()) { instance.requestActivation(); instance.close(); return null; }
            if (!instance.supportsReplacement()) {
                instance.close();
                LegacyInstance.close(directory.resolve("instance.lock"));
                instance = new SingleInstance(directory, version, build);
            }
            long deadline = System.nanoTime() + 15_000_000_000L;
            while (!instance.primary() && System.nanoTime() < deadline) {
                instance.requestReplacement(); Thread.sleep(150); instance.tryAcquire();
            }
            if (!instance.primary()) throw new IOException("Завершите редактирование записи в работающем Passworder и повторите запуск.");
            return instance;
        } catch (IOException | InterruptedException | RuntimeException e) { instance.close(); throw e; }
    }
}
