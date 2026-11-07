package com.clientfiles.ui.kit;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;

/** A raised surface with a hairline edge: the basic container of every page. */
public class Card extends Reveal {
    public static final int ARC = 16;

    public Card() {
        this(new BorderLayout(), 20);
    }

    public Card(LayoutManager layout, int padding) {
        super(layout);
        setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        g2.setColor(Theme.c().surface());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
        g2.setColor(Theme.c().border());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
    }
}
