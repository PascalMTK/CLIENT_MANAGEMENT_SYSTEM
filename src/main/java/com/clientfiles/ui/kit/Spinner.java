package com.clientfiles.ui.kit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import javax.swing.JComponent;

public class Spinner extends JComponent {
    private double elapsed;
    private Anim.Handle handle;

    public Spinner() {
        setPreferredSize(new Dimension(28, 28));
    }

    /** Draws an arc for the given clock time; the static form is shared with the buttons. */
    public static void paint(Graphics2D g, double x, double y, double size, double elapsedMs, Color color) {
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Gfx.alpha(color, .22));
        g.draw(new Arc2D.Double(x, y, size, size, 0, 360, Arc2D.OPEN));
        g.setColor(color);
        double sweep = 90 + 50 * Math.sin(elapsedMs / 380);
        g.draw(new Arc2D.Double(x, y, size, size, -elapsedMs * .36, sweep, Arc2D.OPEN));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        handle = Anim.forever(this, ms -> {
            elapsed = ms;
            repaint();
        });
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        handle.cancel();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        int size = Math.min(getWidth(), getHeight()) - 4;
        paint(g2, (getWidth() - size) / 2.0, (getHeight() - size) / 2.0, size, elapsed, Theme.c().accent());
        g2.dispose();
    }
}
