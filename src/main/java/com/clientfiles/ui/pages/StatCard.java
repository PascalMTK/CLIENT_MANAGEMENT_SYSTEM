package com.clientfiles.ui.pages;

import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Card;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/** A headline number. When it leads somewhere, the whole card is a button. */
final class StatCard extends Card {
    private final String label;
    private final String caption;
    private final String icon;
    private final int value;
    private final transient Supplier<Color> tone;
    private final transient Supplier<Color> toneSoft;
    private final Anim.Value hover = new Anim.Value(this, 0);
    private final boolean clickable;
    private double shown;
    private boolean focused;

    StatCard(String label, int value, String caption, String icon, Supplier<Color> tone, Supplier<Color> toneSoft,
             Runnable onClick) {
        this.label = label;
        this.value = value;
        this.caption = caption;
        this.icon = icon;
        this.tone = tone;
        this.toneSoft = toneSoft;
        this.clickable = onClick != null;
        this.shown = value;
        setPreferredSize(new Dimension(220, 128));
        getAccessibleContext().setAccessibleName(label + ": " + value + ". " + caption);
        if (clickable) {
            makeInteractive(onClick);
        }
    }

    private void makeInteractive(Runnable onClick) {
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover.to(1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover.to(0);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
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
        for (int key : new int[] {KeyEvent.VK_ENTER, KeyEvent.VK_SPACE}) {
            getInputMap().put(KeyStroke.getKeyStroke(key, 0), "activate");
        }
        getActionMap().put("activate", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onClick.run();
            }
        });
    }

    void countUp(int delay) {
        if (Anim.motionEnabled()) {
            shown = 0;
            Anim.run(delay, 800, Anim.OUT, t -> {
                shown = value * t;
                repaint();
            }, null);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = Gfx.hq(g);
        Theme.Palette c = Theme.c();
        int w = getWidth();
        if (hover.get() > 0) {
            g2.setColor(Gfx.alpha(c.borderStrong(), hover.get()));
            g2.drawRoundRect(0, 0, w - 1, getHeight() - 1, ARC, ARC);
        }
        if (focused) {
            g2.setColor(c.accent());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 3, getHeight() - 3, ARC, ARC);
        }

        g2.setFont(Theme.semibold(13));
        g2.setColor(c.textMuted());
        g2.drawString(label, 20, 34);

        g2.setColor(toneSoft.get());
        g2.fillRoundRect(w - 54, 16, 36, 36, 11, 11);
        Icons.paint(g2, icon, Icons.MEDIUM, tone.get(), w - 45, 25);

        g2.setFont(Theme.semibold(34));
        g2.setColor(c.text());
        g2.drawString(Integer.toString((int) Math.round(shown)), 20, 78);

        g2.setFont(Theme.regular(12));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(c.textMuted());
        g2.drawString(Gfx.ellipsize(caption, fm, w - 40), 20, 106);
        if (clickable) {
            Icons.paint(g2, "arrow-right", Icons.SMALL - 2, Gfx.alpha(c.textMuted(), hover.get()), w - 32, 95);
        }
        g2.dispose();
    }
}
