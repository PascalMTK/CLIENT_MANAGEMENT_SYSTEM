package com.clientfiles.ui;

import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;

/** The product mark: a folded sheet with a check, and the wordmark. */
public final class Brand {
    public static final Color MINT = new Color(0x3DBFA3);
    public static final Color INK = new Color(0x06231D);
    public static final Color PAPER = new Color(0xF2F6F4);

    private Brand() {
    }

    public static void mark(Graphics2D g, double x, double y, double size) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale(size / 32, size / 32);
        g2.setColor(MINT);
        g2.fillRoundRect(0, 0, 32, 32, 10, 10);
        g2.setColor(INK);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D sheet = new Path2D.Double();
        sheet.moveTo(19, 7);
        sheet.lineTo(11, 7);
        sheet.quadTo(9, 7, 9, 9);
        sheet.lineTo(9, 23);
        sheet.quadTo(9, 25, 11, 25);
        sheet.lineTo(21, 25);
        sheet.quadTo(23, 25, 23, 23);
        sheet.lineTo(23, 11);
        sheet.closePath();
        g2.draw(sheet);
        Path2D check = new Path2D.Double();
        check.moveTo(12.5, 17);
        check.lineTo(15, 19.5);
        check.lineTo(19.5, 14.5);
        g2.draw(check);
        g2.dispose();
    }

    public static void wordmark(Graphics2D g, int x, int y, int height, Color color) {
        mark(g, x, y, height);
        g.setFont(Theme.semibold(height * .56f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString("Client Files", x + height + 11, Gfx.baseline(fm, y, height));
    }

    public static BufferedImage icon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = Gfx.hq(image.createGraphics());
        mark(g, 0, 0, size);
        g.dispose();
        return image;
    }
}
