package svarcovv.dev.passworder.ui;

import svarcovv.dev.passworder.security.PasswordGenerator;
import javax.swing.*;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import java.awt.*;
import java.util.Arrays;

final class GeneratorPanel extends JPanel {
    private final JPasswordField output = new JPasswordField();
    private final JSlider length = new JSlider(12, 64, 24);
    private final JCheckBox digits = new JCheckBox("Цифры", true), symbols = new JCheckBox("Специальные символы", true);
    record State(int length, boolean digits, boolean symbols, char[] password) implements AutoCloseable {
        @Override public void close() { Arrays.fill(password, '\0'); }
    }
    State capture() { return new State(length.getValue(), digits.isSelected(), symbols.isSelected(), output.getPassword()); }
    GeneratorPanel(MainFrame owner, State initial) {
        super(new BorderLayout()); setBorder(BorderFactory.createEmptyBorder(0, 28, 4, 28));
        JPanel body = Theme.vertical();
        output.setName("generator.output"); output.setEchoChar((char) 0); output.setEditable(false); output.setFont(new Font("Consolas", Font.PLAIN, 20));
        output.setUI(new BasicPasswordFieldUI());
        output.setBorder(BorderFactory.createEmptyBorder()); output.setMargin(new Insets(0, 0, 0, 0));
        output.setBackground(Theme.surface); output.setForeground(Theme.text); output.setCaretColor(Theme.surface);
        JPanel passwordBox = new JPanel(new GridBagLayout()); passwordBox.setBackground(Theme.surface);
        passwordBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.border),
                BorderFactory.createEmptyBorder(0, 14, 0, 14)));
        GridBagConstraints fieldLayout = new GridBagConstraints(); fieldLayout.weightx = 1; fieldLayout.fill = GridBagConstraints.HORIZONTAL;
        passwordBox.add(output, fieldLayout);
        Theme.field(body, "Ваш новый пароль", passwordBox);
        passwordBox.setPreferredSize(new Dimension(600, 60)); passwordBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        length.setName("generator.length"); length.setMajorTickSpacing(13); length.setPaintTicks(true);
        if (initial != null) { length.setValue(initial.length()); digits.setSelected(initial.digits()); symbols.setSelected(initial.symbols()); }
        JLabel lengthLabel = Theme.label("Длина: 24 символа", 14, Theme.text, true); Theme.align(lengthLabel); body.add(lengthLabel); Theme.gap(body, 14);
        Theme.align(length); length.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48)); body.add(length); Theme.gap(body, 18);
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
        body.setMaximumSize(new Dimension(760, body.getPreferredSize().height)); body.setAlignmentY(TOP_ALIGNMENT);
        Box width = Box.createHorizontalBox(); width.add(body); width.add(Box.createHorizontalGlue()); add(width, BorderLayout.NORTH);
        if (initial == null) generate.run();
        else { output.setText(new String(initial.password())); lengthLabel.setText("Длина: " + length.getValue() + " символов"); }
    }
    void clear() { output.setText(""); }
}
