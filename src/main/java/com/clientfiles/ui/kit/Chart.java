package com.clientfiles.ui.kit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.util.List;
import java.util.function.Supplier;

/** The two dashboard charts. The exact values also exist in the Documents table. */
public final class Chart {
    private Chart() {
    }

    public record Slice(String label, int value, String icon, Supplier<Color> color) {
    }

    public record Bar(String label, String detail, int value) {
    }

    /** Share of documents per status, with the total in the middle and a direct legend. */
    public static final class Donut extends Widget {
        private static final int STROKE = 16;
        private final List<Slice> slices;
        private final int total;
        private double progress = 1;

        public Donut(List<Slice> slices) {
            this.slices = slices;
            this.total = slices.stream().mapToInt(Slice::value).sum();
            setPreferredSize(new Dimension(320, 190));
            StringBuilder summary = new StringBuilder("Documents by status: ");
            slices.forEach(s -> summary.append(s.label()).append(' ').append(s.value()).append(", "));
            getAccessibleContext().setAccessibleDescription(summary.toString());
        }

        public void play(int delay) {
            if (Anim.motionEnabled()) {
                progress = 0;
                Anim.run(delay, 800, Anim.IN_OUT, t -> {
                    progress = t;
                    repaint();
                }, null);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            int size = Math.min(getHeight() - 10, 170);
            int x = 4;
            int y = (getHeight() - size) / 2;
            double inset = STROKE / 2.0;
            Arc2D.Double arc = new Arc2D.Double(x + inset, y + inset, size - STROKE, size - STROKE, 0, 0, Arc2D.OPEN);

            g2.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            if (total == 0) {
                g2.setColor(c.surfaceAlt());
                arc.setAngleExtent(360);
                g2.draw(arc);
            } else {
                double circumference = Math.PI * (size - STROKE);
                double gap = 2 / circumference * 360;
                double angle = 90;
                double sweep = 360 * progress;
                for (Slice slice : slices) {
                    if (slice.value() == 0) {
                        continue;
                    }
                    double extent = slice.value() * 360.0 / total;
                    double shown = Math.min(extent, sweep);
                    if (shown > 0) {
                        g2.setColor(slice.color().get());
                        arc.setAngleStart(angle - gap / 2);
                        arc.setAngleExtent(-Math.max(0, shown - gap));
                        g2.draw(arc);
                    }
                    angle -= extent;
                    sweep -= extent;
                }
            }

            g2.setFont(Theme.semibold(28));
            FontMetrics big = g2.getFontMetrics();
            String number = Integer.toString(total);
            g2.setColor(c.text());
            g2.drawString(number, x + (size - big.stringWidth(number)) / 2f, y + size / 2f + 4);
            g2.setFont(Theme.regular(12));
            FontMetrics small = g2.getFontMetrics();
            g2.setColor(c.textMuted());
            g2.drawString("documents", x + (size - small.stringWidth("documents")) / 2f, y + size / 2f + 22);

            int legendX = x + size + 28;
            int rowHeight = 44;
            int legendY = (getHeight() - slices.size() * rowHeight) / 2;
            for (Slice slice : slices) {
                Icons.paint(g2, slice.icon(), Icons.MEDIUM, slice.color().get(), legendX, legendY + 13);
                g2.setFont(Theme.regular(14));
                g2.setColor(c.text());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(slice.label(), legendX + 26, Gfx.baseline(fm, legendY, rowHeight));
                g2.setFont(Theme.semibold(14));
                FontMetrics strong = g2.getFontMetrics();
                String value = Integer.toString(slice.value());
                g2.drawString(value, getWidth() - strong.stringWidth(value) - 4, Gfx.baseline(strong, legendY, rowHeight));
                legendY += rowHeight;
            }
            g2.dispose();
        }
    }

    /** One value per period; hovering a column shows its exact figure. */
    public static final class Bars extends Widget {
        private static final int MAX_BAR_WIDTH = 40;
        private static final int AXIS_HEIGHT = 26;
        private static final int TOP = 26;
        private final List<Bar> bars;
        private final int max;
        private final Anim.Value hoverAlpha = new Anim.Value(this, 0);
        private int hovered = -1;
        private double progress = 1;

        public Bars(List<Bar> bars) {
            this.bars = bars;
            this.max = Math.max(1, bars.stream().mapToInt(Bar::value).max().orElse(1));
            setPreferredSize(new Dimension(420, 190));
            StringBuilder summary = new StringBuilder("Documents per month: ");
            bars.forEach(b -> summary.append(b.detail()).append(' ').append(b.value()).append(", "));
            getAccessibleContext().setAccessibleDescription(summary.toString());
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int index = e.getX() * bars.size() / Math.max(1, getWidth());
                    if (index != hovered) {
                        hovered = Math.min(index, bars.size() - 1);
                        hoverAlpha.to(1);
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = -1;
                    hoverAlpha.set(0);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        public void play(int delay) {
            if (Anim.motionEnabled()) {
                progress = 0;
                Anim.run(delay, 700, t -> t, t -> {
                    progress = t;
                    repaint();
                }, null);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            int plot = getHeight() - AXIS_HEIGHT - TOP;
            int base = TOP + plot;
            int slot = getWidth() / Math.max(1, bars.size());
            int width = Math.min(MAX_BAR_WIDTH, slot - 16);

            g2.setColor(c.border());
            g2.fillRect(0, base, getWidth(), 1);

            for (int i = 0; i < bars.size(); i++) {
                Bar bar = bars.get(i);
                int x = i * slot + (slot - width) / 2;
                double grow = Anim.OUT.at(Math.max(0, Math.min(1, progress * 1.5 - i * .1)));
                int height = (int) Math.round(bar.value() * (double) plot / max * grow);
                boolean active = i == hovered;
                if (active) {
                    g2.setColor(Gfx.alpha(c.surfaceAlt(), hoverAlpha.get()));
                    g2.fillRoundRect(i * slot + 4, 0, slot - 8, base, 10, 10);
                }
                if (bar.value() > 0) {
                    g2.setColor(active ? c.accentHover() : c.accent());
                    int radius = Math.min(8, height * 2);
                    g2.fillRoundRect(x, base - height, width, height + radius, radius, radius);
                    g2.setColor(c.surface());
                    g2.fillRect(x, base + 1, width, radius);
                    g2.setColor(c.border());
                    g2.fillRect(x, base, width, 1);
                }
                g2.setFont(active ? Theme.semibold(12) : Theme.regular(12));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(active ? c.text() : c.textMuted());
                g2.drawString(bar.label(), i * slot + (slot - fm.stringWidth(bar.label())) / 2f,
                        Gfx.baseline(fm, base, AXIS_HEIGHT));
                boolean last = i == bars.size() - 1;
                if ((active || last) && grow > .6) {
                    String value = Integer.toString(bar.value());
                    g2.setFont(Theme.semibold(13));
                    FontMetrics strong = g2.getFontMetrics();
                    g2.setColor(c.text());
                    g2.drawString(value, i * slot + (slot - strong.stringWidth(value)) / 2f, base - height - 7);
                }
            }
            g2.dispose();
        }
    }
}
