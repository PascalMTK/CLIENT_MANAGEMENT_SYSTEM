package com.clientfiles.ui.kit;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Short confirmations at the bottom centre, where they cover neither the window controls nor the
 * actions of a drawer. They never take the keyboard focus.
 */
public final class Toasts {
    public enum Kind { SUCCESS, ERROR, INFO }

    private static final int TOAST_WIDTH = 400;
    private static final int MARGIN = 24;
    private static final int GAP = 10;
    private static final int VISIBLE_MS = 4500;
    private static final String STACK = "clientfiles.toasts";

    private Toasts() {
    }

    public static void success(Component anchor, String message) {
        show(anchor, Kind.SUCCESS, message);
    }

    public static void error(Component anchor, String message) {
        show(anchor, Kind.ERROR, message);
    }

    public static void show(Component anchor, Kind kind, String message) {
        JRootPane root = SwingUtilities.getRootPane(anchor);
        if (root == null) {
            return;
        }
        JLayeredPane layer = root.getLayeredPane();
        new Toast(layer, stack(layer), kind, message).open();
    }

    @SuppressWarnings("unchecked")
    private static List<Toast> stack(JLayeredPane layer) {
        Object existing = layer.getClientProperty(STACK);
        if (existing != null) {
            return (List<Toast>) existing;
        }
        List<Toast> toasts = new ArrayList<>();
        layer.putClientProperty(STACK, toasts);
        layer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                arrange(layer, toasts);
            }
        });
        return toasts;
    }

    private static void arrange(JLayeredPane layer, List<Toast> toasts) {
        int y = layer.getHeight() - MARGIN;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast toast = toasts.get(i);
            y -= toast.height;
            toast.setBounds((layer.getWidth() - TOAST_WIDTH) / 2, y, TOAST_WIDTH, toast.height);
            y -= GAP;
        }
    }

    private static final class Toast extends Widget {
        private final JLayeredPane layer;
        private final List<Toast> stack;
        private final Kind kind;
        private final List<String> lines;
        private final int height;
        private double t;
        private boolean closing;

        Toast(JLayeredPane layer, List<Toast> stack, Kind kind, String message) {
            this.layer = layer;
            this.stack = stack;
            this.kind = kind;
            FontMetrics fm = getFontMetrics(Theme.regular(14));
            lines = Gfx.wrap(message, fm, TOAST_WIDTH - 72);
            height = Math.max(56, 32 + lines.size() * fm.getHeight());
            getAccessibleContext().setAccessibleName(message);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    close();
                }
            });
        }

        void open() {
            stack.add(this);
            layer.add(this, JLayeredPane.POPUP_LAYER);
            arrange(layer, stack);
            Anim.run(Anim.MEDIUM, Anim.OUT, v -> {
                t = v;
                repaint();
            });
            Anim.run(VISIBLE_MS, 1, Anim.OUT, v -> { }, this::close);
        }

        void close() {
            if (closing) {
                return;
            }
            closing = true;
            Anim.run(0, Anim.FAST, Anim.IN, v -> {
                t = 1 - v;
                repaint();
            }, () -> {
                layer.remove(this);
                layer.repaint();
                stack.remove(this);
                arrange(layer, stack);
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            g2.setComposite(AlphaComposite.SrcOver.derive((float) t));
            g2.translate(0, (1 - t) * 14);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(c.surface());
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            Color tone = switch (kind) {
                case SUCCESS -> c.success();
                case ERROR -> c.danger();
                case INFO -> c.info();
            };
            g2.setColor(c.borderStrong());
            g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            String icon = switch (kind) {
                case SUCCESS -> "circle-check";
                case ERROR -> "circle-alert";
                case INFO -> "info";
            };
            Icons.paint(g2, icon, Icons.LARGE - 2, tone, 16, (h - Icons.LARGE + 2) / 2);
            g2.setFont(Theme.regular(14));
            g2.setColor(c.text());
            FontMetrics fm = g2.getFontMetrics();
            int y = (h - lines.size() * fm.getHeight()) / 2 + fm.getAscent();
            for (String line : lines) {
                g2.drawString(line, 48, y);
                y += fm.getHeight();
            }
            g2.dispose();
        }
    }
}
