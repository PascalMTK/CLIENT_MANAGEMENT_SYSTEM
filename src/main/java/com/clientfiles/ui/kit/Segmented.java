package com.clientfiles.ui.kit;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** A row of mutually exclusive filters with a sliding highlight. Arrow keys move the choice. */
public class Segmented extends Widget {
    private static final int HEIGHT = 40;
    private static final int PAD = 4;
    private static final int ITEM_PADDING = 14;

    private final List<String> labels = new ArrayList<>();
    private final List<Integer> counts = new ArrayList<>();
    private final Anim.Value left = new Anim.Value(this, PAD);
    private final Anim.Value right = new Anim.Value(this, PAD);
    private int selected;
    private boolean focused;
    private transient IntConsumer onChange = i -> { };

    public Segmented(String... items) {
        for (String item : items) {
            labels.add(item);
            counts.add(-1);
        }
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName("Filter");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int x = PAD;
                for (int i = 0; i < labels.size(); i++) {
                    int width = itemWidth(i);
                    if (e.getX() >= x && e.getX() < x + width) {
                        select(i);
                    }
                    x += width;
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    select(Math.min(labels.size() - 1, selected + 1));
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    select(Math.max(0, selected - 1));
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = Gfx.byKeyboard(e);
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });
    }

    public void onChange(IntConsumer listener) {
        this.onChange = listener;
    }

    public int selected() {
        return selected;
    }

    public void select(int index) {
        if (index != selected) {
            selected = index;
            placeHighlight(true);
            onChange.accept(index);
            getAccessibleContext().setAccessibleDescription(labels.get(index));
        }
    }

    public void setCount(int index, int count) {
        counts.set(index, count);
        placeHighlight(false);
        revalidate();
        repaint();
    }

    private void placeHighlight(boolean animate) {
        int x = PAD;
        for (int i = 0; i < selected; i++) {
            x += itemWidth(i);
        }
        int end = x + itemWidth(selected);
        if (animate) {
            left.to(x, Anim.MEDIUM, Anim.OUT);
            right.to(end, Anim.MEDIUM, Anim.OUT);
        } else {
            left.set(x);
            right.set(end);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        placeHighlight(false);
    }

    private String countText(int index) {
        return counts.get(index) < 0 ? "" : Integer.toString(counts.get(index));
    }

    private int itemWidth(int index) {
        FontMetrics fm = getFontMetrics(Theme.semibold(13));
        int count = countText(index).isEmpty() ? 0 : fm.stringWidth(countText(index)) + 7;
        return fm.stringWidth(labels.get(index)) + count + 2 * ITEM_PADDING;
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 2 * PAD;
        for (int i = 0; i < labels.size(); i++) {
            width += itemWidth(i);
        }
        return new Dimension(width, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        Theme.Palette c = Theme.c();
        g2.setColor(c.surfaceAlt());
        g2.fillRoundRect(0, 0, getWidth(), HEIGHT, 12, 12);
        g2.setColor(c.surface());
        g2.fillRoundRect((int) left.get(), PAD, (int) (right.get() - left.get()), HEIGHT - 2 * PAD, 9, 9);
        g2.setColor(c.border());
        g2.drawRoundRect((int) left.get(), PAD, (int) (right.get() - left.get()) - 1, HEIGHT - 2 * PAD - 1, 9, 9);

        g2.setFont(Theme.semibold(13));
        FontMetrics fm = g2.getFontMetrics();
        int x = PAD;
        int baseline = Gfx.baseline(fm, 0, HEIGHT);
        for (int i = 0; i < labels.size(); i++) {
            g2.setColor(i == selected ? c.text() : c.textMuted());
            g2.drawString(labels.get(i), x + ITEM_PADDING, baseline);
            if (!countText(i).isEmpty()) {
                g2.setColor(c.textMuted());
                g2.drawString(countText(i), x + ITEM_PADDING + fm.stringWidth(labels.get(i)) + 7, baseline);
            }
            x += itemWidth(i);
        }
        if (focused) {
            g2.setColor(c.accent());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, HEIGHT - 3, 12, 12);
        }
        g2.dispose();
    }
}
