package svarcovv.dev.passworder.security;

import java.security.SecureRandom;
import java.util.*;

public final class PasswordGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private PasswordGenerator() {}
    public static char[] generate(int length, boolean digits, boolean symbols) {
        if (length < 12 || length > 128) throw new IllegalArgumentException("Длина: от 12 до 128 символов.");
        List<String> groups = new ArrayList<>(List.of(LETTERS));
        if (digits) groups.add("23456789");
        if (symbols) groups.add("!@#$%&*+-=?_");
        String alphabet = String.join("", groups);
        char[] result = new char[length];
        int i = 0;
        for (String group : groups) result[i++] = group.charAt(RANDOM.nextInt(group.length()));
        while (i < length) result[i++] = alphabet.charAt(RANDOM.nextInt(alphabet.length()));
        for (int j = length - 1; j > 0; j--) {
            int k = RANDOM.nextInt(j + 1); char temp = result[j]; result[j] = result[k]; result[k] = temp;
        }
        return result;
    }
}
