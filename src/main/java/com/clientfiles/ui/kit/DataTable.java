package com.clientfiles.ui.kit;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * A sortable, keyboard-operable table of records. A row opens with a click or Enter; there are no
 * buttons inside cells, so every action lives in one place: the drawer that opens.
 */
public class DataTable<T> extends JTable {
    private static final int ROW_HEIGHT = 58;
    private static final int HEADER_HEIGHT = 40;
    private static final int PAD = 16;

    @FunctionalInterface
    public interface CellPainter<T> {
        void paint(Graphics2D g, T row, int width, int height);
    }

    public record Column<T>(String title, int width, Function<T, Comparable<?>> sortKey,
                            Function<T, String> text, CellPainter<T> painter) {
        public static <T> Column<T> text(String title, int width, Function<T, Comparable<?>> sortKey,
                                         Function<T, String> text) {
            return new Column<>(title, width, sortKey, text, null);
        }
    }

    private final List<Column<T>> columns;
    private final Model model;
    private final TableRowSorter<Model> sorter;
    private final Anim.Value hoverIn = new Anim.Value(this, 0);
    private final Anim.Value hoverOut = new Anim.Value(this, 0);
    private int hoverRow = -1;
    private int leavingRow = -1;
    private double entrance = 1;
    private transient Consumer<T> onOpen = row -> { };

    public DataTable(List<Column<T>> columns) {
        this.columns = columns;
        this.model = new Model();
        setModel(model);
        sorter = new TableRowSorter<>(model);
        for (int i = 0; i < columns.size(); i++) {
            sorter.setComparator(i, comparator(columns.get(i).sortKey()));
            getColumnModel().getColumn(i).setPreferredWidth(columns.get(i).width());
        }
        setRowSorter(sorter);
        setDefaultRenderer(Object.class, new Cell());
        getTableHeader().setDefaultRenderer(new Header());
        style();

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(rowAtPoint(e.getPoint()));
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                setHover(-1);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                if (row >= 0) {
                    onOpen.accept(rowAt(row));
                }
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
        getActionMap().put("open", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (getSelectedRow() >= 0) {
                    onOpen.accept(rowAt(getSelectedRow()));
                }
            }
        });
    }

    private void style() {
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setRowHeight(ROW_HEIGHT);
        setOpaque(false);
        setFillsViewportHeight(true);
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JTableHeader header = getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(false);
            header.setResizingAllowed(false);
            header.setOpaque(false);
            header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (model != null) {
            style();
        }
    }

    private static <T> Comparator<T> comparator(Function<T, Comparable<?>> key) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Comparator<Comparable> natural = Comparator.nullsLast(Comparator.naturalOrder());
        @SuppressWarnings({"unchecked", "rawtypes"})
        Comparator<T> byKey = (a, b) -> natural.compare((Comparable) key.apply(a), (Comparable) key.apply(b));
        return byKey;
    }

    public JScrollPane inScrollPane() {
        JScrollPane scroll = new JScrollPane(this) {
            /** The table and the look and feel both try to put a frame back around the content. */
            @Override
            public void setBorder(Border border) {
                super.setBorder(BorderFactory.createEmptyBorder());
            }
        };
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setColumnHeader(new javax.swing.JViewport());
        scroll.getColumnHeader().setView(getTableHeader());
        scroll.getColumnHeader().setOpaque(false);
        return scroll;
    }

    public void onOpen(Consumer<T> listener) {
        this.onOpen = listener;
    }

    public void setRows(List<T> rows) {
        model.rows = new ArrayList<>(rows);
        model.fireTableDataChanged();
        hoverRow = -1;
        leavingRow = -1;
    }

    public void playEntrance() {
        if (!Anim.motionEnabled()) {
            return;
        }
        entrance = 0;
        Anim.run(520, t -> t, v -> {
            entrance = v;
            repaint();
        });
    }

    public int visibleRows() {
        return getRowCount();
    }

    public void sortBy(int column, SortOrder order) {
        sorter.setSortKeys(List.of(new RowSorter.SortKey(column, order)));
    }

    public void setFilter(Predicate<T> filter) {
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                return filter.test(model.rows.get(entry.getIdentifier()));
            }
        });
        hoverRow = -1;
    }

    private T rowAt(int viewRow) {
        return model.rows.get(convertRowIndexToModel(viewRow));
    }

    private void setHover(int row) {
        if (row == hoverRow) {
            return;
        }
        leavingRow = hoverRow;
        hoverOut.set(hoverIn.get());
        hoverOut.to(0, Anim.MEDIUM, Anim.OUT);
        hoverRow = row;
        hoverIn.set(0);
        hoverIn.to(1, 90, Anim.OUT);
    }

    private double rowHover(int row) {
        if (row == hoverRow) {
            return hoverIn.get();
        }
        return row == leavingRow ? hoverOut.get() : 0;
    }

    private double rowEntrance(int row) {
        if (entrance >= 1 || row > 12) {
            return 1;
        }
        double start = row * .05;
        return Anim.OUT.at(Math.max(0, Math.min(1, (entrance - start) / .55)));
    }

    /** Text cell helper for custom painters. */
    public static void paintText(Graphics2D g, String text, Color color, int x, int y, int width, int height) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(Gfx.ellipsize(text, fm, width), x, Gfx.baseline(fm, y, height));
    }

    private final class Model extends AbstractTableModel {
        private List<T> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column).title();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return rows.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    private final class Cell extends Widget implements TableCellRenderer {
        private T value;
        private int row;
        private int column;
        private boolean selected;
        private boolean focused;

        @Override
        @SuppressWarnings("unchecked")
        public Component getTableCellRendererComponent(JTable table, Object cell, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            this.value = (T) cell;
            this.row = row;
            this.column = column;
            this.selected = isSelected;
            this.focused = table.isFocusOwner() && isSelected;
            getAccessibleContext().setAccessibleName(columns.get(column).text().apply(value));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            int w = getWidth();
            int h = getHeight();
            boolean first = column == 0;
            boolean last = column == columns.size() - 1;

            double appear = rowEntrance(row);
            if (appear < 1) {
                g2.setComposite(AlphaComposite.SrcOver.derive((float) appear));
                g2.translate(0, (1 - appear) * 8);
            }

            g2.setColor(c.border());
            g2.fillRect(first ? PAD : 0, h - 1, w - (first ? PAD : 0) - (last ? PAD : 0), 1);
            double hover = selected ? 1 : rowHover(row);
            if (hover > 0) {
                g2.setColor(Gfx.alpha(selected ? c.accentSoft() : c.surfaceAlt(), selected ? .75 : hover));
                g2.fillRoundRect(first ? 4 : -24, 3, w + (first ? 20 : 48) - (last ? 28 : 0), h - 6, 12, 12);
            }
            if (focused) {
                g2.setColor(c.accent());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(first ? 4 : -24, 3, w + (first ? 20 : 48) - (last ? 28 : 0) - 1, h - 7, 12, 12);
            }

            Column<T> col = columns.get(column);
            int left = first ? PAD + 4 : 8;
            g2.translate(left, 0);
            int inner = w - left - (last ? PAD : 8);
            if (col.painter() != null) {
                col.painter().paint(g2, value, inner, h);
            } else {
                g2.setFont(Theme.regular(14));
                paintText(g2, col.text().apply(value), c.text(), 0, 0, inner, h);
            }
            g2.dispose();
        }
    }

    private final class Header extends Widget implements TableCellRenderer {
        private String title;
        private boolean firstColumn;
        private SortOrder order = SortOrder.UNSORTED;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object cell, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            title = String.valueOf(cell);
            firstColumn = column == 0;
            order = SortOrder.UNSORTED;
            for (RowSorter.SortKey key : sorter.getSortKeys()) {
                if (key.getColumn() == column) {
                    order = key.getSortOrder();
                }
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            g2.setFont(Theme.semibold(12));
            FontMetrics fm = g2.getFontMetrics();
            int x = firstColumn ? PAD + 4 : 8;
            int baseline = Gfx.baseline(fm, 0, getHeight());
            boolean sorted = order != SortOrder.UNSORTED;
            g2.setColor(sorted ? c.text() : c.textMuted());
            g2.drawString(title, x, baseline);
            if (sorted) {
                int ax = x + fm.stringWidth(title) + 6;
                int ay = getHeight() / 2;
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int dir = order == SortOrder.ASCENDING ? -1 : 1;
                g2.drawLine(ax, ay - dir * 2, ax + 3, ay + dir * 2);
                g2.drawLine(ax + 3, ay + dir * 2, ax + 6, ay - dir * 2);
            }
            g2.dispose();
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Point point = event.getPoint();
        int row = rowAtPoint(point);
        int column = columnAtPoint(point);
        return row < 0 || column < 0 ? null : columns.get(convertColumnIndexToModel(column)).text().apply(rowAt(row));
    }
}
