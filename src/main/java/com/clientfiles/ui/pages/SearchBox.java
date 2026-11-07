package com.clientfiles.ui.pages;

import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** The filter box of a table. Filtering happens in memory, on every keystroke. */
final class SearchBox extends JPanel {
    private final JTextField input = new JTextField();
    private final Anim.Value focus = new Anim.Value(this, 0);

    SearchBox(String placeholder, Runnable onChange) {
        super(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(300, 40));
        setBorder(BorderFactory.createEmptyBorder(0, 38, 0, 10));
        input.setOpaque(false);
        input.setBorder(null);
        input.setFont(Theme.regular(14));
        input.putClientProperty("JTextField.placeholderText", placeholder);
        input.getAccessibleContext().setAccessibleName(placeholder);
        add(input);
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange.run();
            }
        });
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focus.to(1);
            }

            @Override
            public void focusLost(FocusEvent e) {
                focus.to(0);
            }
        });
    }

    String text() {
        return input.getText().trim().toLowerCase(java.util.Locale.ROOT);
    }

    void setText(String text) {
        input.setText(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        Theme.Palette c = Theme.c();
        g2.setColor(c.surface());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(Gfx.mix(c.border(), c.accent(), focus.get()));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        Icons.paint(g2, "search", Icons.SMALL, c.textMuted(), 12, (getHeight() - Icons.SMALL) / 2);
        g2.dispose();
    }
}
