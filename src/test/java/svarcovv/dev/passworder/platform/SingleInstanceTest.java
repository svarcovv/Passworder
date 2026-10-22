package svarcovv.dev.passworder.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SingleInstanceTest {
    @TempDir Path directory;
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
