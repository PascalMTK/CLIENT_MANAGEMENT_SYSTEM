package com.clientfiles.ui.kit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;

/** The one button of the application: a real JButton, so keyboard and screen readers just work. */
public class Btn extends JButton {
    public enum Kind { PRIMARY, SECONDARY, GHOST, DANGER, DANGER_GHOST, SIDEBAR }

    /** Room around the visible button for the focus ring. */
    private static final int RING = 3;
    private static final int ARC = 10;
    private static final int HEIGHT = 40 + 2 * RING;

    private final Kind kind;
    private final String iconName;
    private final Anim.Value hover = new Anim.Value(this, 0);
    private final Anim.Value press = new Anim.Value(this, 0);
    private final Anim.Value focus = new Anim.Value(this, 0);
    private boolean loading;
    private double spin;
    private Anim.Handle spinner;

    public Btn(String text, String iconName, Kind kind, Runnable action) {
        super(text);
        this.kind = kind;
        this.iconName = iconName;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(Theme.semibold(14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (action != null) {
            addActionListener(e -> action.run());
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover.to(1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover.to(0);
                press.to(0);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                press.to(1, 80, Anim.OUT);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                press.to(0, Anim.MEDIUM, Anim.SPRING);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focus.to(Gfx.byKeyboard(e) ? 1 : 0);
            }

            @Override
            public void focusLost(FocusEvent e) {
                focus.to(0);
            }
        });
    }

    /** A square button with only an icon; the tooltip doubles as its accessible name. */
    public static Btn icon(String iconName, String label, Kind kind, Runnable action) {
        Btn button = new Btn("", iconName, kind, action);
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        return button;
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
        if (spinner != null) {
            spinner.cancel();
            spinner = null;
        }
        if (loading) {
            spinner = Anim.forever(this, ms -> {
                spin = ms;
                repaint();
            });
        }
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR));
        repaint();
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        if (!loading) {
            super.fireActionPerformed(event);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (getText().isEmpty()) {
            return new Dimension(HEIGHT, HEIGHT);
        }
        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.stringWidth(getText()) + 2 * 18 + 2 * RING;
        return new Dimension(width + (iconName == null ? 0 : Icons.MEDIUM + 8), HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        Theme.Palette c = Theme.c();
        int w = getWidth() - 2 * RING;
        int h = getHeight() - 2 * RING;
        double scale = 1 - .025 * press.get();
        g2.translate(getWidth() / 2.0, getHeight() / 2.0);
        g2.scale(scale, scale);
        g2.translate(-getWidth() / 2.0, -getHeight() / 2.0);
        if (!isEnabled()) {
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(.45f));
        }

        double hovered = hover.get();
        Color background = null;
        Color border = null;
        Color foreground;
        switch (kind) {
            case PRIMARY -> {
                background = Gfx.mix(c.accent(), c.accentHover(), hovered);
                foreground = c.onAccent();
            }
            case DANGER -> {
                background = Gfx.mix(c.danger(), c.dangerHover(), hovered);
                foreground = c.onDanger();
            }
            case SECONDARY -> {
                background = Gfx.mix(c.surface(), c.surfaceAlt(), hovered);
                border = Gfx.mix(c.border(), c.borderStrong(), hovered);
                foreground = c.text();
            }
            case GHOST -> {
                background = Gfx.alpha(c.surfaceAlt(), hovered);
                foreground = Gfx.mix(c.textMuted(), c.text(), hovered);
            }
            case DANGER_GHOST -> {
                background = Gfx.alpha(c.dangerSoft(), hovered);
                foreground = c.danger();
            }
            default -> {
                background = Gfx.alpha(c.sidebarHover(), hovered);
                foreground = Gfx.mix(c.sidebarText(), c.sidebarTextStrong(), hovered);
            }
        }
        g2.setColor(background);
        g2.fillRoundRect(RING, RING, w, h, ARC, ARC);
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(RING, RING, w - 1, h - 1, ARC, ARC);
        }

        FontMetrics fm = g2.getFontMetrics(getFont());
        String text = getText();
        int glyph = (iconName != null || loading) ? Icons.MEDIUM : 0;
        int gap = glyph > 0 && !text.isEmpty() ? 8 : 0;
        int x = (getWidth() - glyph - gap - fm.stringWidth(text)) / 2;
        int iconY = (getHeight() - Icons.MEDIUM) / 2;
        if (loading) {
            Spinner.paint(g2, x + 2, iconY + 2, Icons.MEDIUM - 4, spin, foreground);
        } else if (iconName != null) {
            Icons.paint(g2, iconName, Icons.MEDIUM, foreground, x, iconY);
        }
        g2.setFont(getFont());
        g2.setColor(foreground);
        g2.drawString(text, x + glyph + gap, Gfx.baseline(fm, 0, getHeight()));

        if (focus.get() > 0) {
            g2.setColor(Gfx.alpha(kind == Kind.SIDEBAR ? c.sidebarTextStrong() : c.accent(), focus.get()));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARC + 4, ARC + 4);
        }
        g2.dispose();
    }
}
