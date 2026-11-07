package com.clientfiles.ui;

import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Theme;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Switches between the light and dark themes with a crossfade of the whole window. */
final class ThemeToggle extends Btn {
    ThemeToggle(AppContext context) {
        super("", null, Kind.GHOST, () -> context.frame().setDarkTheme(!Theme.isDark()));
        setToolTipText("Switch between light and dark theme");
        getAccessibleContext().setAccessibleName("Switch between light and dark theme");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = Gfx.hq(g);
        Icons.paint(g2, Theme.isDark() ? "sun" : "moon", Icons.MEDIUM, Theme.c().textMuted(),
                (getWidth() - Icons.MEDIUM) / 2, (getHeight() - Icons.MEDIUM) / 2);
        g2.dispose();
    }
}
