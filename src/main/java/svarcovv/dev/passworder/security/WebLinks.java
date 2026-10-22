package svarcovv.dev.passworder.security;

import java.net.URI;

public final class WebLinks {
    private WebLinks() {}
    public static URI parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Ссылка не указана.");
        String candidate = value.strip();
        if (!candidate.contains(":")) candidate = "https://" + candidate;
        try {
            URI uri = URI.create(candidate);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException();
            return uri;
        } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Укажите адрес сайта с http:// или https://."); }
    }
    public static String host(String value) {
        try { return parse(value).getHost().replaceFirst("^www\\.", ""); }
        catch (IllegalArgumentException e) { return "Ссылка не указана"; }
    }
}
