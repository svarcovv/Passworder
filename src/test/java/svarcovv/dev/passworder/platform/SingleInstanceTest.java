package svarcovv.dev.passworder.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SingleInstanceTest {
    @TempDir Path directory;
    @Test void secondaryLaunchReportsTheRunningVersionWithoutReplacingIt() throws Exception {
        try (var first = new SingleInstance(directory, "0.2.1")) {
            try (var second = new SingleInstance(directory, "0.1.0")) {
                assertFalse(second.primary()); assertEquals("0.2.1", second.runningVersion());
            }
            assertEquals("0.2.1", first.runningVersion());
        }
        assertFalse(java.nio.file.Files.exists(directory.resolve("running.version")));
    }
    @Test void secondLaunchActivatesFirstAndReleasesLockOnExit() throws Exception {
        try (var first = new SingleInstance(directory)) {
            assertTrue(first.primary()); assertFalse(first.activationRequested());
            try (var second = new SingleInstance(directory)) { assertFalse(second.primary()); }
            assertTrue(first.activationRequested()); assertFalse(first.activationRequested());
            try (var third = new SingleInstance(directory)) { assertFalse(third.primary()); }
            assertTrue(first.activationRequested());
        }
        try (var next = new SingleInstance(directory)) { assertTrue(next.primary()); }
    }
}
