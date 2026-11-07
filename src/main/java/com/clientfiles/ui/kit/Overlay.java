package com.clientfiles.ui.kit;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingUtilities;

/** A dialog or side drawer drawn inside the window, above a dimmed background. */
public final class Overlay extends JComponent {
    public enum Kind { DIALOG, DRAWER }

    private static final int MARGIN = 40;
    private static final int ARC = 18;

    private final JLayeredPane layer;
    private final Kind kind;
    private final int sheetWidth;
    private final Sheet sheet;
    private final Component previousFocus;
    private final ComponentAdapter resize = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            setBounds(contentArea());
            placeSheet();
            revalidate();
        }
    };
    private double t;
    private boolean closing;
    private Anim.Handle handle;
    private transient Runnable onClose;

    private Overlay(JLayeredPane layer, Kind kind, int width, JComponent content, Component focus) {
        this.layer = layer;
        this.kind = kind;
        this.sheetWidth = width;
        this.previousFocus = focus;
        this.sheet = new Sheet(content);
        setLayout(null);
        add(sheet);
        setFocusCycleRoot(true);
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        setFocusable(true);

        // Swallow every pointer event so that nothing behind the overlay can be reached.
        addMouseMotionListener(new MouseMotionAdapter() { });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!sheet.getBounds().contains(e.getPoint())) {
                    close();
                }
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                close();
            }
        });
    }

    public static Overlay open(Component anchor, Kind kind, int width, JComponent content) {
        JRootPane root = SwingUtilities.getRootPane(anchor);
        JLayeredPane layer = root.getLayeredPane();
        Overlay overlay = new Overlay(layer, kind, width, content,
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        overlay.setBounds(overlay.contentArea());
        layer.add(overlay, JLayeredPane.MODAL_LAYER, 0);
        layer.addComponentListener(overlay.resize);
        overlay.placeSheet();
        overlay.validate();
        overlay.animateIn();
        SwingUtilities.invokeLater(overlay::focusFirst);
        return overlay;
    }

    /** Everything but the title bar, so the window can still be moved and closed. */
    private Rectangle contentArea() {
        return ((JRootPane) SwingUtilities.getAncestorOfClass(JRootPane.class, layer)).getContentPane().getBounds();
    }

    public void onClose(Runnable action) {
        this.onClose = action;
    }

    private void focusFirst() {
        Component first = getFocusTraversalPolicy().getComponentAfter(this, this);
        if (first != null) {
            first.requestFocusInWindow();
        } else {
            requestFocusInWindow();
        }
    }

    private void placeSheet() {
        if (kind == Kind.DRAWER) {
            sheet.setBounds(getWidth() - sheetWidth, 0, sheetWidth, getHeight());
        } else {
            sheet.setSize(sheetWidth, 10);
            sheet.doLayout();
            int height = Math.min(sheet.getPreferredSize().height, getHeight() - 2 * MARGIN);
            sheet.setBounds((getWidth() - sheetWidth) / 2, (getHeight() - height) / 2, sheetWidth, height);
        }
    }

    private void animateIn() {
        handle = Anim.run(kind == Kind.DRAWER ? 300 : Anim.MEDIUM, Anim.OUT, v -> {
            t = v;
            repaint();
        });
    }

    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        if (handle != null) {
            handle.cancel();
        }
        double from = t;
        Anim.run(0, Anim.FAST + 40, Anim.IN, v -> {
            t = from * (1 - v);
            repaint();
        }, () -> {
            if (previousFocus != null && previousFocus.isShowing()) {
                previousFocus.requestFocusInWindow();
            }
            layer.removeComponentListener(resize);
            layer.remove(this);
            layer.repaint();
            if (onClose != null) {
                onClose.run();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        g2.setColor(Gfx.alpha(Theme.c().scrim(), Theme.c().scrim().getAlpha() / 255.0 * t));
        g2.fillRect(0, 0, getWidth(), getHeight());
        Rectangle r = sheet.getBounds();
        if (kind == Kind.DIALOG) {
            Gfx.shadow(g2, r.x, r.y, r.width, r.height, ARC, 28, t);
        } else {
            int x = r.x + (int) ((1 - t) * r.width);
            Gfx.shadow(g2, x, r.y - 40, r.width + 40, r.height + 80, 0, 28, t);
        }
        g2.dispose();
    }

    /** The content of a dialog: title, optional message, body and a row of buttons. */
    public static JPanel dialogBody(String title, String message, JComponent body, JComponent... buttons) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 26, 20, 26));

        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setOpaque(false);
        heading.add(new Text(title, Theme.semibold(18), () -> Theme.c().text()), BorderLayout.NORTH);
        if (message != null) {
            heading.add(new Wrapped(message), BorderLayout.CENTER);
        }
        panel.add(heading, BorderLayout.NORTH);
        if (body != null) {
            panel.add(body, BorderLayout.CENTER);
        }
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        row.setOpaque(false);
        for (JComponent button : buttons) {
            row.add(button);
        }
        panel.add(row, BorderLayout.SOUTH);
        return panel;
    }

    /** Asks before something that cannot be undone. Cancel gets the initial focus. */
    public static void confirm(Component anchor, String title, String message, String action, boolean danger,
                               Runnable onConfirm) {
        Overlay[] overlay = new Overlay[1];
        Btn cancel = new Btn("Cancel", null, Btn.Kind.SECONDARY, () -> overlay[0].close());
        Btn confirm = new Btn(action, null, danger ? Btn.Kind.DANGER : Btn.Kind.PRIMARY, () -> {
            overlay[0].close();
            onConfirm.run();
        });
        overlay[0] = open(anchor, Kind.DIALOG, 440, dialogBody(title, message, null, cancel, confirm));
    }

    private final class Sheet extends JPanel {
        Sheet(JComponent content) {
            super(new BorderLayout());
            setOpaque(false);
            add(content);
        }

        @Override
        public Dimension getPreferredSize() {
            getComponent(0).setSize(sheetWidth, 10);
            return new Dimension(sheetWidth, getComponent(0).getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            g2.setColor(Theme.c().surface());
            if (kind == Kind.DRAWER) {
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.c().border());
                g2.drawLine(0, 0, 0, getHeight());
            } else {
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            }
            g2.dispose();
        }

        @Override
        public void paint(Graphics g) {
            if (t >= 1) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            if (kind == Kind.DRAWER) {
                g2.translate((1 - t) * getWidth(), 0);
            } else {
                double scale = .96 + .04 * t;
                g2.setComposite(AlphaComposite.SrcOver.derive((float) t));
                g2.translate(getWidth() / 2.0, getHeight() / 2.0);
                g2.scale(scale, scale);
                g2.translate(-getWidth() / 2.0, -getHeight() / 2.0);
            }
            super.paint(g2);
            g2.dispose();
        }
    }

    /** Muted text that wraps to the dialog width. */
    private static final class Wrapped extends JComponent {
        private final String text;

        Wrapped(String text) {
            this.text = text;
        }

        @Override
        public Dimension getPreferredSize() {
            int width = getWidth() > 0 ? getWidth() : 388;
            int lines = Gfx.wrap(text, getFontMetrics(Theme.regular(14)), width).size();
            return new Dimension(width, lines * 21);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            g2.setFont(Theme.regular(14));
            g2.setColor(Theme.c().textMuted());
            int y = g2.getFontMetrics().getAscent();
            for (String line : Gfx.wrap(text, g2.getFontMetrics(), getWidth())) {
                g2.drawString(line, 0, y);
                y += 21;
            }
            g2.dispose();
        }
    }
}
