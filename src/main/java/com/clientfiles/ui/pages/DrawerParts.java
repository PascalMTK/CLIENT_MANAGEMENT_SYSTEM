package com.clientfiles.ui.pages;

import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Building blocks shared by the document and client drawers. */
final class DrawerParts {
    static final int WIDTH = 460;

    private DrawerParts() {
    }

    static JPanel closeBar(Runnable onClose) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(Btn.icon("x", "Close", Btn.Kind.GHOST, onClose), BorderLayout.EAST);
        return bar;
    }

    /** Long values wrap at word boundaries inside the drawer. */
    static JComponent wrappedTitle(String text) {
        JPanel lines = new JPanel();
        lines.setOpaque(false);
        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        Text sample = Text.title("");
        for (String line : com.clientfiles.ui.kit.Gfx.wrap(text,
                sample.getFontMetrics(Theme.semibold(20)), WIDTH - 60)) {
            lines.add(new Text(line, Theme.semibold(20), () -> Theme.c().text()));
        }
        return lines;
    }

    static JComponent fact(String icon, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(9, 0, 9, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        JLabel glyph = new JLabel(Icons.of(icon, Icons.MEDIUM, () -> Theme.c().textMuted()));
        row.add(glyph, BorderLayout.WEST);
        row.add(Text.muted(label));
        Text shown = Text.body(com.clientfiles.ui.kit.Gfx.ellipsize(value,
                glyph.getFontMetrics(Theme.regular(14)), WIDTH - 210));
        shown.setToolTipText(value);
        row.add(shown, BorderLayout.EAST);
        return row;
    }

    /** Keeps a column at the top when the drawer is taller than its content. */
    static JPanel pinnedToTop(JComponent content) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(content, BorderLayout.NORTH);
        return holder;
    }

    static JPanel vertical() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    static JComponent gap(int height) {
        return (JComponent) Box.createVerticalStrut(height);
    }
}
