package svarcovv.dev.passworder.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/** A password is kept in a wipeable buffer. Never include entries in logs. */
public final class VaultEntry implements AutoCloseable {
    private final UUID id;
    private final String title, login, url;
    private final char[] password;
    private final boolean favorite;
    private final Instant created, updated;

    public VaultEntry(UUID id, String title, String login, char[] password, String url,
                      boolean favorite, Instant created, Instant updated) {
        if (title == null || title.isBlank() || title.length() > 256)
            throw new IllegalArgumentException("Название: от 1 до 256 символов.");
        if (login == null || login.length() > 4096 || url == null || url.length() > 4096)
            throw new IllegalArgumentException("Логин или ссылка слишком длинные.");
        if (password == null || password.length == 0 || password.length > 4096)
            throw new IllegalArgumentException("Пароль: от 1 до 4096 символов.");
        this.id = java.util.Objects.requireNonNull(id);
        this.title = title.strip(); this.login = login; this.url = url.strip();
        this.password = password.clone(); this.favorite = favorite;
        this.created = java.util.Objects.requireNonNull(created);
        this.updated = java.util.Objects.requireNonNull(updated);
    }
    public UUID id() { return id; }
    public String title() { return title; }
    public String login() { return login; }
    public String url() { return url; }
    public char[] password() { return password.clone(); }
    public boolean favorite() { return favorite; }
    public Instant created() { return created; }
    public Instant updated() { return updated; }
    @Override public void close() { Arrays.fill(password, '\0'); }
}
