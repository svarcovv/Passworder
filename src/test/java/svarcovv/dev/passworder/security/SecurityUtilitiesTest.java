package svarcovv.dev.passworder.security;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilitiesTest {
    @Test void generatorLengthAndRequiredCharacterGroups() {
        var samples = new HashSet<String>();
        for (int i = 0; i < 200; i++) {
            String value = new String(PasswordGenerator.generate(24, true, true)); samples.add(value);
            assertEquals(24, value.length()); assertTrue(value.matches(".*[23456789].*"));
            assertTrue(value.matches(".*[!@#$%&*+\\-=?_].*")); assertTrue(value.matches(".*[a-zA-Z].*"));
            assertFalse(value.contains("0")); assertFalse(value.contains("1"));
        }
        assertEquals(200, samples.size());
    }
    @Test void generatorOptionsAndLimits() {
        assertTrue(new String(PasswordGenerator.generate(12, false, false)).matches("[a-zA-Z]{12}"));
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(0, true, true));
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(129, true, true));
    }
    @Test void opensOnlyWebUrls() {
        assertEquals("https://example.test", WebLinks.parse("example.test").toString());
        assertEquals("http://localhost:8080/page", WebLinks.parse("http://localhost:8080/page").toString());
        for (String value : new String[]{"javascript:alert(1)", "file:///C:/Windows", "ftp://example.test", "https://user:secret@example.test", "https://", "bad url", ""})
            assertThrows(IllegalArgumentException.class, () -> WebLinks.parse(value));
    }
}
