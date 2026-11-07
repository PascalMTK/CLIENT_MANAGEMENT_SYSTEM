package com.clientfiles.ui.kit;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/** A panel that can fade and slide into place without disturbing the layout around it. */
public class Reveal extends JPanel {
    private double alpha = 1;
    private double dx;
    private double dy;
    private Anim.Handle handle;

    public Reveal(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public void enter(int delay, int fromDx, int fromDy) {
        if (!Anim.motionEnabled()) {
            return;
        }
        if (handle != null) {
            handle.cancel();
        }
        alpha = 0;
        dx = fromDx;
        dy = fromDy;
        handle = Anim.run(delay, Anim.SLOW, Anim.OUT, t -> {
            alpha = t;
            dx = fromDx * (1 - t);
            dy = fromDy * (1 - t);
            repaint();
        }, null);
    }

    @Override
    public void paint(Graphics g) {
        if (alpha >= 1 && dx == 0 && dy == 0) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
        g2.translate(dx, dy);
        super.paint(g2);
        g2.dispose();
    }
}
