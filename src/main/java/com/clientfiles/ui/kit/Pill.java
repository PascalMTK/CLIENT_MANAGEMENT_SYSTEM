package com.clientfiles.ui.kit;

import com.clientfiles.core.DocStatus;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** A status tag: always icon and word together, never colour alone. */
public class Pill extends Widget {
    private static final int HEIGHT = 26;

    public record Style(String label, String icon, Color foreground, Color background) {
    }

    private final Style style;

    public Pill(Style style) {
        this.style = style;
        getAccessibleContext().setAccessibleName(style.label());
    }

    public static Style of(DocStatus status) {
        Theme.Palette c = Theme.c();
        return switch (status) {
            case PENDING -> new Style(status.label(), "clock", c.warning(), c.warningSoft());
            case APPROVED -> new Style(status.label(), "circle-check", c.success(), c.successSoft());
            case REJECTED -> new Style(status.label(), "circle-x", c.danger(), c.dangerSoft());
        };
    }

    public static Style active(boolean active) {
        Theme.Palette c = Theme.c();
        return active ? new Style("Active", "circle-check", c.success(), c.successSoft())
                : new Style("Inactive", "circle-x", c.textMuted(), c.surfaceAlt());
    }

    public static int width(FontMetrics fm, Style style) {
        return 8 + Icons.SMALL - 2 + 5 + fm.stringWidth(style.label()) + 10;
    }

    public static void paint(Graphics2D g, Style style, int x, int y) {
        g.setFont(Theme.semibold(12));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(style.background());
        g.fillRoundRect(x, y, width(fm, style), HEIGHT, HEIGHT, HEIGHT);
        Icons.paint(g, style.icon(), Icons.SMALL - 2, style.foreground(), x + 8, y + (HEIGHT - Icons.SMALL + 2) / 2);
        g.setColor(style.foreground());
        g.drawString(style.label(), x + 8 + Icons.SMALL - 2 + 5, Gfx.baseline(fm, y, HEIGHT));
    }

    public static int height() {
        return HEIGHT;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width(getFontMetrics(Theme.semibold(12)), style), HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        paint(g2, style, 0, 0);
        g2.dispose();
    }
}
