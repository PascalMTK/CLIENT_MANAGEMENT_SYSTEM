package com.clientfiles.ui.kit;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.Icon;

/** Lucide icons from resources, tinted at paint time so they follow the theme. */
public final class Icons {
    public static final int SMALL = 16;
    public static final int MEDIUM = 18;
    public static final int LARGE = 22;

    private static final Map<String, FlatSVGIcon> CACHE = new HashMap<>();
    /** The tint of the icon being painted; painting only happens on the event thread. */
    private static Color tint = Color.BLACK;
    private static final FlatSVGIcon.ColorFilter FILTER = new FlatSVGIcon.ColorFilter(c -> tint);

    private Icons() {
    }

    public static void paint(Graphics2D g, String name, int size, Color color, int x, int y) {
        FlatSVGIcon icon = CACHE.computeIfAbsent(name + "|" + size, k -> {
            FlatSVGIcon svg = new FlatSVGIcon("icons/" + name + ".svg", size, size);
            svg.setColorFilter(FILTER);
            return svg;
        });
        tint = color;
        icon.paintIcon(null, g, x, y);
    }

    public static Icon of(String name, int size, Supplier<Color> color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Icons.paint((Graphics2D) g, name, size, color.get(), x, y);
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }
}
