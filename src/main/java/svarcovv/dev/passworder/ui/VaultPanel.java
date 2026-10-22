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
    private final SmoothScrollPane scroll;
    private int hoveredRow = -1, hoveredAction = -1;
    private final Motion.Value actionHover = new Motion.Value(0, value -> repaintActions());
    private void repaintActions() { if (table != null && hoveredRow >= 0) table.repaint(table.getCellRect(hoveredRow, 4, true)); }
    private void repaintRow(int row) { if (row >= 0) table.repaint(0, row * table.getRowHeight(), table.getWidth(), table.getRowHeight()); }
    record ViewState(boolean favorites, String query, UUID selection, int scroll) {}
    ViewState capture() { return new ViewState(favorites, search.getText(), selected() == null ? null : selected().id(), scroll.getVerticalScrollBar().getValue()); }
    void restore(ViewState state) {
        if (favorites != state.favorites()) return;
        search.setText(state.query()); select(state.selection());
    }
    void restoreScroll(ViewState state) { if (favorites == state.favorites()) scroll.getVerticalScrollBar().setValue(state.scroll()); }
    private void select(UUID id) {
        if (id == null) return;
        for (int i = 0; i < visible.size(); i++) if (visible.get(i).id().equals(id)) { table.setRowSelectionInterval(i, i); return; }
    }
    private int actionAt(Point point) {
        int row = table.rowAtPoint(point); if (row < 0 || table.columnAtPoint(point) != 4) return -1;
        Rectangle cell = table.getCellRect(row, 4, true);
        int offset = point.x - cell.x - (cell.width - 108) / 2;
        return offset < 0 || offset >= 108 ? -1 : offset / 36;
    }
    private void updateHover(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint()), action = actionAt(e.getPoint());
        if (row != hoveredRow || action != hoveredAction) {
            int previous = hoveredRow; hoveredRow = row; hoveredAction = action;
            actionHover.to(0, 0, false); actionHover.to(action < 0 ? 0 : 1, 130, false); repaintRow(previous); repaintRow(row);
        }
        table.setCursor(Cursor.getPredefinedCursor(row >= 0 && table.columnAtPoint(e.getPoint()) > 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private final JLabel count;
    private final JPanel tableArea = new JPanel(new CardLayout());
    private final JLabel emptyHeading = Theme.label("Пока здесь пусто", 22, Theme.text, true);
    private final JLabel emptyHint = Theme.label("Добавьте логин, пароль и ссылку на сервис.", 14, Theme.muted, false);
    private List<VaultEntry> visible = List.of();
    private final boolean favorites;
    private final AbstractTableModel model = new AbstractTableModel() {
        private final String[] columns = {"Сервис", "Логин", "Пароль", "Сайт", "Действия"};
        @Override public int getRowCount() { return visible.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            var entry = visible.get(row);
            return switch (column) { case 0 -> entry.title(); case 1 -> entry.login(); case 2 -> "••••••••"; case 3 -> entry.url().isEmpty() ? "-" : WebLinks.host(entry.url()); default -> ""; };
        }
    };
    VaultPanel(MainFrame owner, boolean favorites) {
        super(new BorderLayout(0, 16)); this.owner = owner; this.favorites = favorites;
        setBorder(BorderFactory.createEmptyBorder(0, 28, 4, 28));
        JPanel top = Theme.vertical();
        JPanel tools = new JPanel(new BorderLayout(12, 0));
        search.setName("vault.search"); search.putClientProperty("JTextField.placeholderText", "Поиск по названию, логину или сайту");
        search.putClientProperty("JTextField.leadingIcon", new Glyph("search", 19, Theme.muted));
        search.putClientProperty("JTextField.showClearButton", true); search.setPreferredSize(new Dimension(360, 44));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); } public void removeUpdate(DocumentEvent e) { refresh(); } public void changedUpdate(DocumentEvent e) { refresh(); }
        }); tools.add(search, BorderLayout.CENTER);
        JButton add = Theme.primary("Добавить запись", () -> owner.edit(null)); add.setName("vault.add"); add.setIcon(new Glyph("plus", 19, Theme.accentText)); tools.add(add, BorderLayout.EAST);
        Theme.align(tools); top.add(tools); Theme.gap(top, 18);
        JPanel summary = new JPanel(new BorderLayout()); count = Theme.label("", 13, Theme.muted, false); summary.add(count, BorderLayout.WEST);
        summary.add(Theme.label("По алфавиту", 12, Theme.muted, false), BorderLayout.EAST); Theme.align(summary); top.add(summary);
        add(top, BorderLayout.NORTH);
        table = new JTable(model) {
            @Override public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint()), col = columnAtPoint(e.getPoint()); if (row < 0) return null;
                var entry = visible.get(row);
                return switch (col) {
                    case 0 -> "Сервис: " + entry.title(); case 1 -> "Копировать логин: " + entry.login();
                    case 2 -> "Копировать пароль"; case 3 -> entry.url().isEmpty() ? "Добавить ссылку" : entry.url();
                    default -> switch (actionAt(e.getPoint())) { case 0 -> "Копировать пароль"; case 1 -> "Редактировать"; default -> "Другие действия"; };
                };
            }
        }; table.setName("vault.table"); table.setRowHeight(68); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false); table.setIntercellSpacing(new Dimension(0, 1)); table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false); table.getTableHeader().setResizingAllowed(false);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        int[] widths = {235, 220, 155, 190, 108}, minimums = {144, 136, 112, 124, 108};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(minimums[i]);
        }
        table.getColumnModel().getColumn(4).setMaxWidth(108);
        JLabel header = Theme.label("", 12, Theme.muted, false); header.setOpaque(true); header.setBackground(Theme.surface);
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, col) -> {
            header.setText(value.toString()); header.setHorizontalAlignment(col == 4 ? SwingConstants.CENTER : SwingConstants.LEFT);
            header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border),
                    BorderFactory.createEmptyBorder(0, col == 4 ? 0 : 16, 0, 8))); return header;
        });
        scroll = new SmoothScrollPane(table); scroll.setName("vault.scroll");
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.border));
        scroll.getViewport().setBackground(Theme.surface); tableArea.add(scroll, "table");
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateHover(e); }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { int previous = hoveredRow; hoveredRow = hoveredAction = -1; actionHover.stop(); repaintRow(previous); }
        });
        JPanel empty = new JPanel(new GridBagLayout()); empty.setBackground(Theme.surface); empty.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.border));
        JPanel emptyContent = Theme.vertical(); emptyContent.setBackground(Theme.surface);
        JLabel mark = new JLabel(new Glyph("key", 42, Theme.accent)); mark.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(mark); Theme.gap(emptyContent, 20);
        emptyHeading.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(emptyHeading); Theme.gap(emptyContent, 10);
        emptyHint.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(emptyHint); Theme.gap(emptyContent, 24);
        JButton first = Theme.primary("Добавить запись", () -> owner.edit(null)); first.setAlignmentX(CENTER_ALIGNMENT); emptyContent.add(first); empty.add(emptyContent); tableArea.add(empty, "empty"); add(tableArea, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(Theme.label("Клик по логину или паролю - копировать", 12, Theme.muted, false), BorderLayout.WEST);
        bottom.add(Theme.label("Enter - редактировать", 12, Theme.muted, false), BorderLayout.EAST); add(bottom, BorderLayout.SOUTH);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) popup(e); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) popup(e); }
            @Override public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int row = table.rowAtPoint(e.getPoint()), col = table.columnAtPoint(e.getPoint()); if (row < 0) return;
                VaultEntry entry = visible.get(row);
                if (col == 1) owner.copyLogin(entry);
                else if (col == 2) owner.copyPassword(entry);
                else if (col == 3) { if (!entry.url().isBlank()) owner.openLink(entry.url()); else owner.edit(entry); }
                else if (col == 4) {
                    int action = actionAt(e.getPoint());
                    if (action == 0) owner.copyPassword(entry); else if (action == 1) owner.edit(entry); else menu(entry).show(table, e.getX(), e.getY());
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
        UUID selection = selected() == null ? null : selected().id();
        visible = owner.entries().stream().filter(e -> !favorites || e.favorite())
                .filter(e -> query.isEmpty() || (e.title() + "\n" + e.login() + "\n" + e.url()).toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(VaultEntry::title, String.CASE_INSENSITIVE_ORDER)).toList();
        model.fireTableDataChanged(); select(selection); if (count == null) return;
        count.setText("Записей: " + visible.size());
        boolean searching = !query.isEmpty(); emptyHeading.setText(searching ? "Ничего не нашлось" : favorites ? "Избранное пока пусто" : "Пока здесь пусто");
        emptyHint.setText(searching ? "Попробуйте другой логин, сайт или название." : favorites ? "Отметьте запись при добавлении или редактировании." : "Добавьте логин, пароль и ссылку на сервис.");
        ((CardLayout) tableArea.getLayout()).show(tableArea, visible.isEmpty() ? "empty" : "table");
    }
    @Override public void removeNotify() { actionHover.stop(); super.removeNotify(); }
    private Color rowColor(boolean selected, int row) {
        return selected ? Theme.selected : row == hoveredRow ? Theme.mix(Theme.surface, Theme.text, .018) : Theme.surface;
    }
    private final class CellRenderer extends DefaultTableCellRenderer {
        private final ActionsCell actions = new ActionsCell();
        private final Font normal = new Font("Segoe UI", Font.PLAIN, 14), password = new Font("Consolas", Font.PLAIN, 14);
        private final Icon favorite = new Glyph("star", 12, Theme.muted);
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
            Color bg = rowColor(selected, row);
            if (col == 4) { actions.background = bg; actions.row = row; return actions; }
            super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            setIcon(col == 0 && visible.get(row).favorite() ? favorite : null); setIconTextGap(8); setHorizontalTextPosition(SwingConstants.LEFT);
            putClientProperty("html.disable", true); setBackground(bg);
            setForeground(col == 3 ? visible.get(row).url().isEmpty() ? Theme.muted : Theme.accent : Theme.text);
            setFont(col == 2 ? password : normal); setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
            return this;
        }
    }
    private final class ActionsCell extends JComponent {
        private Color background;
        private int row;
        private final Glyph[] normal = {new Glyph("copy", 17, Theme.muted), new Glyph("edit", 17, Theme.muted), new Glyph("more", 17, Theme.muted)};
        private final Glyph[] active = {new Glyph("copy", 17, Theme.text), new Glyph("edit", 17, Theme.text), new Glyph("more", 17, Theme.text)};
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(background); g.fillRect(0, 0, getWidth(), getHeight());
            int start = (getWidth() - 108) / 2;
            for (int i = 0; i < 3; i++) {
                boolean hovered = row == hoveredRow && i == hoveredAction;
                if (hovered) { g.setColor(Theme.mix(background, Theme.text, .07 * actionHover.get())); g.fillRoundRect(start + i * 36 + 3, getHeight() / 2 - 15, 30, 30, 8, 8); }
                (hovered ? active[i] : normal[i]).paintIcon(this, g, start + i * 36 + 9, getHeight() / 2 - 8);
            }
            g.dispose();
        }
    }
}
