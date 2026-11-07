package com.clientfiles.ui.pages;

import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** What a page shows instead of a blank area: what happened and what to do next. */
final class EmptyState extends JPanel {
    EmptyState(String icon, String title, String message, JComponent action) {
        super(new GridBagLayout());
        setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(0, 0, 6, 0);

        JComponent tile = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Gfx.hq(g);
                g2.setColor(Theme.c().surfaceAlt());
                g2.fillOval(0, 0, 64, 64);
                Icons.paint(g2, icon, 28, Theme.c().textMuted(), 18, 18);
                g2.dispose();
            }
        };
        tile.setPreferredSize(new Dimension(64, 64));
        add(tile, gbc);
        gbc.insets = new Insets(10, 0, 4, 0);
        add(Text.title(title), gbc);
        gbc.insets = new Insets(0, 0, 14, 0);
        add(new Text(message, Theme.regular(14), () -> Theme.c().textMuted()), gbc);
        if (action != null) {
            add(action, gbc);
        }
    }
}
