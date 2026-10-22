package svarcovv.dev.passworder.ui;

import svarcovv.dev.passworder.model.VaultEntry;
import svarcovv.dev.passworder.security.WebLinks;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

final class VaultPanel extends JPanel {
    private final MainFrame owner;
    private final JTextField search = new JTextField();
    private final JTable table;
    private final JLabel count;
    private final JPanel tableArea = new JPanel(new CardLayout());
    private final JLabel emptyHeading = Theme.label("Пока здесь пусто", 22, Theme.text, true);
    private final JLabel emptyHint = Theme.label("Добавьте первую запись - она останется на этом компьютере.", 14, Theme.muted, false);
    private List<VaultEntry> visible = List.of();
    private final boolean favorites;
    private final AbstractTableModel model = new AbstractTableModel() {
        private final String[] columns = {"Сервис", "Логин", "Пароль", "Сайт", "Действия"};
        @Override public int getRowCount() { return visible.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            var entry = visible.get(row);
            return switch (column) { case 0 -> entry.title(); case 1 -> entry.login(); case 2 -> "••••••••••••"; case 3 -> entry.url().isEmpty() ? "Не указан" : WebLinks.host(entry.url()); default -> ""; };
        }
    };
    VaultPanel(MainFrame owner, boolean favorites) {
        super(new BorderLayout(0, 24)); this.owner = owner; this.favorites = favorites;
        setBorder(BorderFactory.createEmptyBorder(34, 36, 24, 36));
        JPanel top = Theme.vertical();
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.add(Theme.label(favorites ? "Избранное" : "Ваши пароли", 32, Theme.text, true), BorderLayout.WEST);
        JLabel local = Theme.label("Только на этом ПК", 12, Theme.accent, true); local.setIcon(new Glyph("lock", 15, Theme.accent));
        titleRow.add(local, BorderLayout.EAST); Theme.align(titleRow); top.add(titleRow); Theme.gap(top, 9);
        var description = Theme.label("Логины, пароли и ссылки. Все в одном месте.", 14, Theme.muted, false);
        Theme.align(description); top.add(description); Theme.gap(top, 30);
        JPanel tools = new JPanel(new BorderLayout(18, 0));
        search.setName("vault.search"); search.putClientProperty("JTextField.placeholderText", "Найти по названию, логину или сайту");
        search.putClientProperty("JTextField.leadingIcon", new Glyph("search", 19, Theme.muted));
        search.putClientProperty("JTextField.showClearButton", true); search.setPreferredSize(new Dimension(360, 44));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); } public void removeUpdate(DocumentEvent e) { refresh(); } public void changedUpdate(DocumentEvent e) { refresh(); }
        }); tools.add(search, BorderLayout.CENTER);
        JButton add = Theme.primary("Добавить запись", () -> owner.edit(null)); add.setName("vault.add"); add.setIcon(new Glyph("plus", 19, Theme.accentText)); tools.add(add, BorderLayout.EAST);
        Theme.align(tools); top.add(tools); Theme.gap(top, 22);
        JPanel summary = new JPanel(new BorderLayout()); count = Theme.label("", 13, Theme.muted, false); summary.add(count, BorderLayout.WEST);
        summary.add(Theme.label("По алфавиту", 12, Theme.muted, false), BorderLayout.EAST); Theme.align(summary); top.add(summary);
        add(top, BorderLayout.NORTH);
        table = new JTable(model); table.setName("vault.table"); table.setRowHeight(76); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false); table.setIntercellSpacing(new Dimension(0, 1)); table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false); table.getTableHeader().setResizingAllowed(true);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        int[] widths = {220, 220, 150, 170, 112};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumnModel().getColumn(4).setMinWidth(108); table.getColumnModel().getColumn(4).setMaxWidth(120);
        var scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(Theme.border));
        scroll.getViewport().setBackground(Theme.surface); tableArea.add(scroll, "table");
        JPanel empty = new JPanel(new GridBagLayout()); empty.setBackground(Theme.surface); empty.setBorder(BorderFactory.createLineBorder(Theme.border));
        JPanel emptyContent = Theme.vertical(); emptyContent.setBackground(Theme.surface);
        JLabel mark = new JLabel(new Glyph("key", 42, Theme.accent)); mark.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(mark); Theme.gap(emptyContent, 20);
        emptyHeading.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(emptyHeading); Theme.gap(emptyContent, 10);
        emptyHint.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(emptyHint); Theme.gap(emptyContent, 24);
        JButton first = Theme.primary("Добавить запись", () -> owner.edit(null)); first.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(first); empty.add(emptyContent); tableArea.add(empty, "empty"); add(tableArea, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(Theme.label("Клик по логину или паролю - скопировать", 12, Theme.muted, false), BorderLayout.WEST);
        bottom.add(Theme.label("Буфер очистится через 20 с", 12, Theme.muted, false), BorderLayout.EAST); add(bottom, BorderLayout.SOUTH);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) popup(e); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) popup(e); }
            @Override public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int row = table.rowAtPoint(e.getPoint()), col = table.columnAtPoint(e.getPoint()); if (row < 0) return;
                VaultEntry entry = visible.get(row);
                if (col == 1) owner.copyLogin(entry);
                else if (col == 2) owner.copyPassword(entry);
                else if (col == 3 && !entry.url().isBlank()) owner.openLink(entry.url());
                else if (col == 4) {
                    int offset = e.getX() - table.getCellRect(row, col, true).x;
                    if (offset < 39) owner.copyPassword(entry); else if (offset < 75) owner.edit(entry); else menu(entry).show(table, e.getX(), e.getY());
                } else if (e.getClickCount() == 2) owner.edit(entry);
            }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy", () -> { if (selected() != null) owner.copyPassword(selected()); });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "login", () -> { if (selected() != null) owner.copyLogin(selected()); });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "edit", () -> { if (selected() != null) owner.edit(selected()); });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete", () -> { if (selected() != null) owner.delete(selected()); });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK), "menu", () -> { if (selected() != null) { Rectangle r = table.getCellRect(table.getSelectedRow(), 0, true); menu(selected()).show(table, r.x + 20, r.y + 20); } });
        refresh();
    }
    private void bind(KeyStroke key, String name, Runnable action) {
        table.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
        table.getActionMap().put(name, new AbstractAction() { public void actionPerformed(ActionEvent e) { action.run(); } });
    }
    private VaultEntry selected() { int row = table.getSelectedRow(); return row < 0 || row >= visible.size() ? null : visible.get(row); }
    private void popup(MouseEvent e) { int row = table.rowAtPoint(e.getPoint()); if (row >= 0) { table.setRowSelectionInterval(row, row); menu(visible.get(row)).show(table, e.getX(), e.getY()); } }
    private JPopupMenu menu(VaultEntry entry) {
        JPopupMenu menu = new JPopupMenu();
        item(menu, "Копировать логин", () -> owner.copyLogin(entry)); item(menu, "Копировать пароль", () -> owner.copyPassword(entry));
        if (!entry.url().isBlank()) item(menu, "Открыть сайт", () -> owner.openLink(entry.url()));
        menu.addSeparator(); item(menu, "Редактировать", () -> owner.edit(entry)); item(menu, "Удалить", () -> owner.delete(entry)); return menu;
    }
    private void item(JPopupMenu menu, String label, Runnable action) { JMenuItem item = new JMenuItem(label); item.addActionListener(e -> action.run()); menu.add(item); }
    void focusSearch() { search.requestFocusInWindow(); search.selectAll(); }
    void refresh() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        visible = owner.entries().stream().filter(e -> !favorites || e.favorite())
                .filter(e -> query.isEmpty() || (e.title() + "\n" + e.login() + "\n" + e.url()).toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(VaultEntry::title, String.CASE_INSENSITIVE_ORDER)).toList();
        model.fireTableDataChanged(); if (count == null) return;
        count.setText("Записей: " + visible.size());
        boolean searching = !query.isEmpty(); emptyHeading.setText(searching ? "Ничего не нашлось" : favorites ? "Избранное пока пусто" : "Пока здесь пусто");
        emptyHint.setText(searching ? "Попробуйте другой логин, сайт или название." : favorites ? "Отметьте запись при добавлении или редактировании." : "Добавьте первую запись - она останется на этом компьютере.");
        ((CardLayout) tableArea.getLayout()).show(tableArea, visible.isEmpty() ? "empty" : "table");
    }
    private final class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
            Color bg = selected ? Theme.selected : Theme.surface;
            if (col == 0) {
                var entry = visible.get(row);
                JPanel p = new JPanel(new BorderLayout(12, 0)); p.setBackground(bg); p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 10));
                JLabel monogram = Theme.label(entry.title().substring(0, 1).toUpperCase(Locale.ROOT), 18, Theme.accent, true);
                monogram.setHorizontalAlignment(SwingConstants.CENTER); monogram.setPreferredSize(new Dimension(30, 30)); p.add(monogram, BorderLayout.WEST);
                JPanel texts = new JPanel(new GridLayout(2, 1, 0, 1)); texts.setBackground(bg);
                JLabel name = Theme.label(entry.title(), 14, Theme.text, true); if (entry.favorite()) name.setIcon(new Glyph("star", 12, Theme.accent)); texts.add(name);
                texts.add(Theme.label(entry.url().isEmpty() ? "Без ссылки" : WebLinks.host(entry.url()), 12, Theme.muted, false)); p.add(texts, BorderLayout.CENTER); return p;
            }
            if (col == 4) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 26)); p.setBackground(bg);
                for (String icon : new String[]{"copy", "edit", "more"}) p.add(new JLabel(new Glyph(icon, 18, Theme.muted))); return p;
            }
            super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            putClientProperty("html.disable", true); setBackground(bg); setForeground(col == 3 && !visible.get(row).url().isEmpty() ? Theme.accent : Theme.text);
            setFont(new Font(col == 2 ? "Consolas" : "Segoe UI", Font.PLAIN, 14)); setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
            setToolTipText(col == 1 || col == 2 ? "Нажмите, чтобы скопировать" : col == 3 ? "Открыть в браузере" : null); return this;
        }
    }
}
