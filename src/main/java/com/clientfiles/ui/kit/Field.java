package com.clientfiles.ui.kit;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/** A labelled input with focus glow, inline validation and a reserved line for its error. */
public class Field extends JPanel {
    private static final int RING = 3;
    private static final int ARC = 10;
    private static final int BOX_HEIGHT = 42 + 2 * RING;

    private final JTextComponent input;
    private final Text error = new Text(" ", Theme.regular(12), () -> Theme.c().danger());
    private final Box box;
    private final Anim.Value focus = new Anim.Value(this, 0);
    private final Anim.Value invalid = new Anim.Value(this, 0);
    private transient Function<String, String> validator;
    private double shake;
    private Anim.Handle shakeHandle;

    public Field(String label, String placeholder) {
        this(label, placeholder, new JTextField(), null);
    }

    public static Field password(String label) {
        JPasswordField input = new JPasswordField();
        input.putClientProperty("FlatLaf.styleClass", "");
        Field field = new Field(label, null, input, null);
        char echo = input.getEchoChar();
        boolean[] visible = {false};
        Btn[] toggle = new Btn[1];
        toggle[0] = new RevealButton(() -> {
            visible[0] = !visible[0];
            input.setEchoChar(visible[0] ? (char) 0 : echo);
            ((RevealButton) toggle[0]).showing(visible[0]);
            input.requestFocusInWindow();
        });
        field.setTrailing(toggle[0]);
        return field;
    }

    public static Field multiline(String label, String placeholder) {
        JTextArea area = new JTextArea(4, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return new Field(label, placeholder, area, null);
    }

    protected Field(String label, String placeholder, JTextComponent input, String leadingIcon) {
        super(new BorderLayout(0, 2));
        this.input = input;
        setOpaque(false);

        Text caption = new Text(label, Theme.semibold(13), () -> Theme.c().text());
        caption.setBorder(BorderFactory.createEmptyBorder(0, RING, 0, 0));
        caption.setLabelFor(input);
        add(caption, BorderLayout.NORTH);

        input.setOpaque(false);
        input.setBorder(BorderFactory.createEmptyBorder(input instanceof JTextArea ? 10 : 0, 0, 0, 0));
        input.setFont(Theme.regular(14));
        input.setCaretColor(null);
        if (placeholder != null) {
            input.putClientProperty("JTextField.placeholderText", placeholder);
        }
        input.getAccessibleContext().setAccessibleName(label);

        box = new Box(input);
        if (leadingIcon != null) {
            JLabel icon = new JLabel(Icons.of(leadingIcon, Icons.SMALL, () -> Theme.c().textMuted()));
            icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
            box.add(icon, BorderLayout.WEST);
        }
        add(box, BorderLayout.CENTER);

        error.setBorder(BorderFactory.createEmptyBorder(0, RING, 0, 0));
        add(error, BorderLayout.SOUTH);

        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focus.to(1);
            }

            @Override
            public void focusLost(FocusEvent e) {
                focus.to(0);
                if (validator != null && !text().isEmpty() && validator.apply(text()) != null) {
                    validateField();
                }
            }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearErrorAfterEdit();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearErrorAfterEdit();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearErrorAfterEdit();
            }
        });
    }

    private void clearErrorAfterEdit() {
        if (invalid.get() > 0) {
            setError(null);
        }
    }

    public Field validator(Function<String, String> rule) {
        this.validator = rule;
        return this;
    }

    public void setTrailing(JComponent component) {
        box.add(component, BorderLayout.EAST);
    }

    public String text() {
        return input.getText();
    }

    public char[] password() {
        return ((JPasswordField) input).getPassword();
    }

    public void setText(String text) {
        input.setText(text == null ? "" : text);
        setError(null);
    }

    public void setEditable(boolean editable) {
        input.setEditable(editable);
    }

    public JTextComponent input() {
        return input;
    }

    /** Runs the rule and shows its message. Returns true when the value is acceptable. */
    public boolean validateField() {
        String message = validator == null ? null : validator.apply(text());
        setError(message);
        return message == null;
    }

    public void setError(String message) {
        boolean hasError = message != null;
        error.setText(hasError ? message : " ");
        invalid.to(hasError ? 1 : 0);
        input.getAccessibleContext().setAccessibleDescription(message);
    }

    public void shake() {
        if (shakeHandle != null) {
            shakeHandle.cancel();
        }
        shakeHandle = Anim.run(360, t -> t, t -> {
            shake = t >= 1 ? 0 : Math.sin(t * Math.PI * 5) * 5 * (1 - t);
            repaint();
        });
    }

    public void focus() {
        input.requestFocusInWindow();
    }

    @Override
    public void paint(Graphics g) {
        if (shake == 0) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(shake, 0);
        super.paint(g2);
        g2.dispose();
    }

    /** Places a component under a caption, spaced exactly like a text field. */
    public static JPanel labelled(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        Text caption = new Text(label, Theme.semibold(13), () -> Theme.c().text());
        caption.setBorder(BorderFactory.createEmptyBorder(0, RING, 0, 0));
        caption.setLabelFor(component);
        component.getAccessibleContext().setAccessibleName(label);
        panel.add(caption, BorderLayout.NORTH);
        JPanel inset = new JPanel(new BorderLayout());
        inset.setOpaque(false);
        int controlFocusWidth = 2;
        int margin = RING - controlFocusWidth;
        inset.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        inset.add(component);
        panel.add(inset, BorderLayout.CENTER);
        panel.add(new Text(" ", Theme.regular(12), () -> Theme.c().danger()), BorderLayout.SOUTH);
        return panel;
    }

    private final class Box extends JPanel {
        Box(JTextComponent input) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(RING, 12 + RING, RING, 4 + RING));
            if (input instanceof JTextArea) {
                JScrollPane scroll = new JScrollPane(input);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                scroll.setOpaque(false);
                scroll.getViewport().setOpaque(false);
                add(scroll);
            } else {
                add(input);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width, input instanceof JTextArea ? 104 : BOX_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            int w = getWidth() - 2 * RING;
            int h = getHeight() - 2 * RING;
            Color glow = Gfx.mix(c.accent(), c.danger(), invalid.get());
            if (focus.get() > 0) {
                g2.setColor(Gfx.alpha(glow, .22 * focus.get()));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(RING - 1, RING - 1, w + 1, h + 1, ARC + 3, ARC + 3);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.setColor(input.isEditable() ? c.surface() : c.surfaceAlt());
            g2.fillRoundRect(RING, RING, w, h, ARC, ARC);
            Color edge = Gfx.mix(c.borderStrong(), c.accent(), focus.get());
            g2.setColor(Gfx.mix(edge, c.danger(), invalid.get()));
            g2.drawRoundRect(RING, RING, w - 1, h - 1, ARC, ARC);
            g2.dispose();
        }
    }

    /** The eye button of a password field. */
    private static final class RevealButton extends Btn {
        private boolean showing;

        RevealButton(Runnable toggle) {
            super("", null, Kind.GHOST, toggle);
            setToolTipText("Show password");
            getAccessibleContext().setAccessibleName("Show password");
            setPreferredSize(new Dimension(34, 34));
        }

        void showing(boolean showing) {
            this.showing = showing;
            String label = showing ? "Hide password" : "Show password";
            setToolTipText(label);
            getAccessibleContext().setAccessibleName(label);
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(34, 34);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Gfx.hq(g);
            Icons.paint(g2, showing ? "eye-off" : "eye", Icons.MEDIUM - 2, Theme.c().textMuted(),
                    (getWidth() - Icons.MEDIUM + 2) / 2, (getHeight() - Icons.MEDIUM + 2) / 2);
            g2.dispose();
        }
    }

    /** Focuses the first field that failed, as the summary of a rejected form. */
    public static boolean validateAll(Field... fields) {
        Field firstInvalid = null;
        for (Field field : fields) {
            if (!field.validateField() && firstInvalid == null) {
                firstInvalid = field;
            }
        }
        if (firstInvalid == null) {
            return true;
        }
        firstInvalid.focus();
        firstInvalid.shake();
        return false;
    }
}
