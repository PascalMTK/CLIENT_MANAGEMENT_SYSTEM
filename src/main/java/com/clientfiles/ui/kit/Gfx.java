package com.clientfiles.ui.kit;

import com.formdev.flatlaf.ui.FlatUIUtils;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

public final class Gfx {
    private Gfx() {
    }

    /** A copy of the graphics context with high-quality rendering. Dispose it after use. */
    public static Graphics2D hq(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        FlatUIUtils.setRenderingHints(g2);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    public static Color mix(Color a, Color b, double t) {
        double k = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k));
    }

    public static Color alpha(Color color, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    /** Soft shadow around a rounded box, built from translucent layers. */
    public static void shadow(Graphics2D g, int x, int y, int w, int h, int arc, int spread, double strength) {
        Color base = Theme.isDark() ? Color.BLACK : new Color(0x1B2A26);
        for (int i = spread; i > 0; i--) {
            double k = 1 - (double) i / spread;
            g.setColor(alpha(base, strength * k * k * 0.05));
            g.fillRoundRect(x - i, y - i + spread / 3, w + 2 * i, h + 2 * i, arc + 2 * i, arc + 2 * i);
        }
    }

    /** True when focus arrived by Tab: rings are for keyboard users, not after every click. */
    public static boolean byKeyboard(FocusEvent event) {
        return switch (event.getCause()) {
            case TRAVERSAL, TRAVERSAL_FORWARD, TRAVERSAL_BACKWARD, TRAVERSAL_UP, TRAVERSAL_DOWN -> true;
            default -> false;
        };
    }

    public static int baseline(FontMetrics fm, int y, int height) {
        return y + (height - fm.getHeight()) / 2 + fm.getAscent();
    }

    public static String ellipsize(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + "…") > maxWidth) {
            end--;
        }
        return text.substring(0, end).stripTrailing() + "…";
    }

    public static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        lines.add(line.toString());
        return lines;
    }
}
