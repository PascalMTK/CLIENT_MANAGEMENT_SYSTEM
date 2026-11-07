package com.clientfiles.ui.kit;

import com.clientfiles.core.Format;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;
import javax.swing.JComponent;

public class Avatar extends JComponent {
    private final int size;
    private final transient Supplier<String> name;
    private final boolean onSidebar;

    public Avatar(int size, Supplier<String> name, boolean onSidebar) {
        this.size = size;
        this.name = name;
        this.onSidebar = onSidebar;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }

    public static void paint(Graphics2D g, String name, int x, int y, int size, Color background, Color foreground) {
        g.setColor(background);
        g.fillOval(x, y, size, size);
        g.setFont(Theme.semibold(size * .36f));
        FontMetrics fm = g.getFontMetrics();
        String initials = Format.initials(name);
        g.setColor(foreground);
        g.drawString(initials, x + (size - fm.stringWidth(initials)) / 2f, Gfx.baseline(fm, y, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        Theme.Palette c = Theme.c();
        if (onSidebar) {
            paint(g2, name.get(), 0, 0, size, c.sidebarActive(), c.sidebarTextStrong());
        } else {
            paint(g2, name.get(), 0, 0, size, c.accentSoft(), c.accent());
        }
        g2.dispose();
    }
}
