package com.clientfiles.ui.kit;

import java.awt.Color;
import java.awt.Font;
import java.util.function.Supplier;
import javax.swing.JLabel;

/** A label whose colour is a design token, so it follows theme changes by itself. */
public class Text extends JLabel {
    private final transient Supplier<Color> color;

    public Text(String text, Font font, Supplier<Color> color) {
        super(text);
        this.color = color;
        setFont(font);
    }

    public static Text title(String text) {
        return new Text(text, Theme.semibold(16), () -> Theme.c().text());
    }

    public static Text body(String text) {
        return new Text(text, Theme.regular(14), () -> Theme.c().text());
    }

    public static Text muted(String text) {
        return new Text(text, Theme.regular(13), () -> Theme.c().textMuted());
    }

    @Override
    public Color getForeground() {
        return color == null ? super.getForeground() : color.get();
    }
}
