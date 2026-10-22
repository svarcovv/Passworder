package svarcovv.dev.passworder.ui;

import svarcovv.dev.passworder.security.PasswordGenerator;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

final class GeneratorPanel extends JPanel {
    private final JPasswordField output = new JPasswordField();
    GeneratorPanel(MainFrame owner) {
        super(new BorderLayout()); setBorder(BorderFactory.createEmptyBorder(36, 36, 30, 36));
        JPanel body = Theme.vertical();
        var heading = Theme.label("Генератор паролей", 32, Theme.text, true); Theme.align(heading); body.add(heading); Theme.gap(body, 9);
        var hint = Theme.label("Новый пароль для каждого аккаунта.", 14, Theme.muted, false); Theme.align(hint); body.add(hint); Theme.gap(body, 36);
        output.setName("generator.output"); output.setEchoChar((char) 0); output.setEditable(false); output.setFont(new Font("Consolas", Font.PLAIN, 23));
        Theme.field(body, "Ваш новый пароль", output);
        var length = new JSlider(12, 64, 24); length.setMajorTickSpacing(13); length.setPaintTicks(true);
        JLabel lengthLabel = Theme.label("Длина: 24 символа", 14, Theme.text, true); Theme.align(lengthLabel); body.add(lengthLabel); Theme.gap(body, 14);
        Theme.align(length); length.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48)); body.add(length); Theme.gap(body, 18);
        var digits = new JCheckBox("Цифры", true); var symbols = new JCheckBox("Специальные символы", true);
        Theme.align(digits); Theme.align(symbols); body.add(digits); Theme.gap(body, 8); body.add(symbols); Theme.gap(body, 24);
        Runnable generate = () -> {
            lengthLabel.setText("Длина: " + length.getValue() + " символов");
            char[] chars = PasswordGenerator.generate(length.getValue(), digits.isSelected(), symbols.isSelected());
            try { output.setText(new String(chars)); } finally { Arrays.fill(chars, '\0'); }
        };
        length.addChangeListener(e -> generate.run()); digits.addActionListener(e -> generate.run()); symbols.addActionListener(e -> generate.run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.add(Theme.primary("Копировать пароль", () -> { char[] chars = output.getPassword(); try { owner.copy(new String(chars), "Пароль скопирован. Буфер очистится через 20 с."); } finally { Arrays.fill(chars, '\0'); } }));
        buttons.add(Box.createHorizontalStrut(12)); buttons.add(Theme.button("Еще вариант", generate)); Theme.align(buttons); body.add(buttons); Theme.gap(body, 28);
        var info = Theme.label("Без похожих символов. Длина от 12 до 64.", 13, Theme.muted, false); Theme.align(info); body.add(info);
        body.setMaximumSize(new Dimension(720, Integer.MAX_VALUE)); add(body, BorderLayout.NORTH); generate.run();
    }
    void clear() { output.setText(""); }
}
