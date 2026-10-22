package svarcovv.dev.passworder.ui;

import svarcovv.dev.passworder.model.VaultEntry;
import svarcovv.dev.passworder.security.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

final class EntryDialog extends JDialog {
    EntryDialog(MainFrame owner, VaultEntry original, Predicate<VaultEntry> save) {
        super(owner, original == null ? "Новая запись" : "Редактировать запись", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); setResizable(false);
        JPanel content = Theme.vertical(); content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        var heading = Theme.label(original == null ? "Добавить пароль" : "Изменить запись", 25, Theme.text, true);
        Theme.align(heading); content.add(heading); Theme.gap(content, 8);
        var sub = Theme.label("Сохранится только в вашем хранилище.", 13, Theme.muted, false);
        Theme.align(sub); content.add(sub); Theme.gap(content, 25);
        var title = new JTextField(original == null ? "" : original.title()); title.setName("entry.title");
        title.putClientProperty("JTextField.placeholderText", "Например, GitHub");
        var login = new JTextField(original == null ? "" : original.login()); login.setName("entry.login");
        login.putClientProperty("JTextField.placeholderText", "Никнейм или почта");
        var password = new JPasswordField(); password.setName("entry.password");
        if (original != null) { char[] p = original.password(); try { password.setText(new String(p)); } finally { Arrays.fill(p, '\0'); } }
        var url = new JTextField(original == null ? "" : original.url()); url.setName("entry.url");
        url.putClientProperty("JTextField.placeholderText", "https://example.com");
        Theme.field(content, "Название", title); Theme.field(content, "Логин", login);
        Theme.field(content, "Пароль", password);
        JPanel passwordActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JToggleButton show = new JToggleButton("Показать");
        char echo = password.getEchoChar();
        javax.swing.Timer conceal = new javax.swing.Timer(10_000, e -> { show.setSelected(false); password.setEchoChar(echo); }); conceal.setRepeats(false);
        show.addActionListener(e -> { password.setEchoChar(show.isSelected() ? (char) 0 : echo); if (show.isSelected()) conceal.restart(); else conceal.stop(); });
        passwordActions.add(show); passwordActions.add(Box.createHorizontalStrut(10));
        passwordActions.add(Theme.button("Сгенерировать", () -> {
            char[] generated = PasswordGenerator.generate(24, true, true);
            try { password.setText(new String(generated)); } finally { Arrays.fill(generated, '\0'); }
        }));
        Theme.align(passwordActions); content.add(passwordActions); Theme.gap(content, 20);
        Theme.field(content, "Ссылка на сайт (необязательно)", url);
        var favorite = new JCheckBox("В избранное", original != null && original.favorite());
        Theme.align(favorite); content.add(favorite); Theme.gap(content, 16);
        JLabel error = Theme.label(" ", 12, Theme.danger, false); error.setName("entry.error"); Theme.align(error); content.add(error); Theme.gap(content, 12);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(Theme.button("Отмена", this::dispose));
        JButton submit = Theme.primary("Сохранить", () -> {
            char[] secret = password.getPassword(); VaultEntry entry = null;
            try {
                String link = url.getText().strip(); if (!link.isEmpty()) link = WebLinks.parse(link).toString();
                Instant now = Instant.now();
                entry = new VaultEntry(original == null ? UUID.randomUUID() : original.id(), title.getText(), login.getText(),
                        secret, link, favorite.isSelected(), original == null ? now : original.created(), now);
                if (save.test(entry)) { entry = null; dispose(); }
            } catch (IllegalArgumentException e) { error.setText(e.getMessage()); }
            finally { Arrays.fill(secret, '\0'); if (entry != null) entry.close(); }
        });
        submit.setName("entry.save"); buttons.add(submit); Theme.align(buttons); content.add(buttons);
        setContentPane(content); getRootPane().setDefaultButton(submit);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { password.setText(""); conceal.stop(); } });
        pack(); setSize(540, getHeight()); setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(title::requestFocusInWindow);
    }
}
