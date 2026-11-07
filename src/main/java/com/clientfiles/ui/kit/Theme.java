package com.clientfiles.ui.kit;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import com.formdev.flatlaf.util.FontUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/** Design tokens. Components read them while painting, so a theme change needs no listeners. */
public final class Theme {

    public record Palette(boolean dark, Color bg, Color surface, Color surfaceAlt, Color border, Color borderStrong,
                          Color text, Color textMuted,
                          Color accent, Color accentHover, Color onAccent, Color accentSoft,
                          Color success, Color successSoft, Color warning, Color warningSoft,
                          Color danger, Color dangerHover, Color onDanger, Color dangerSoft,
                          Color info, Color infoSoft, Color scrim,
                          Color sidebar, Color sidebarHover, Color sidebarActive, Color sidebarBorder,
                          Color sidebarText, Color sidebarTextStrong) {
    }

    private static final Palette LIGHT = new Palette(false,
            rgb(0xF6F5F1), rgb(0xFFFFFF), rgb(0xF1EFEA), rgb(0xE5E2DA), rgb(0xC9C5BA),
            rgb(0x1B1F1E), rgb(0x5C6360),
            rgb(0x0E6B5C), rgb(0x0B5A4D), rgb(0xFFFFFF), rgb(0xDDEFEA),
            rgb(0x1B7A4B), rgb(0xDFF3E7), rgb(0x8A5A07), rgb(0xFBF0D9),
            rgb(0xB3261E), rgb(0x96201A), rgb(0xFFFFFF), rgb(0xFBE6E4),
            rgb(0x2B5FA8), rgb(0xE3ECF8), new Color(0x10, 0x18, 0x16, 110),
            rgb(0x10201C), rgb(0x1A2E29), rgb(0x20392F), rgb(0x24372F),
            rgb(0xA9BBB5), rgb(0xF2F6F4));

    private static final Palette DARK = new Palette(true,
            rgb(0x0F1312), rgb(0x171C1B), rgb(0x1F2524), rgb(0x2A3230), rgb(0x3D4745),
            rgb(0xE9EDEB), rgb(0x9CA7A3),
            rgb(0x3DBFA3), rgb(0x5BD0B6), rgb(0x06231D), rgb(0x15332C),
            rgb(0x5CC98F), rgb(0x15301F), rgb(0xE3B158), rgb(0x33280F),
            rgb(0xF0857C), rgb(0xF4A098), rgb(0x2B0C0A), rgb(0x3A1A18),
            rgb(0x86B2EE), rgb(0x1A2A40), new Color(0, 0, 0, 150),
            rgb(0x0B0F0E), rgb(0x161D1B), rgb(0x1C2A26), rgb(0x1E2624),
            rgb(0x9CA9A5), rgb(0xEEF3F1));

    private static Palette current = LIGHT;

    private Theme() {
    }

    public static Palette c() {
        return current;
    }

    public static boolean isDark() {
        return current.dark();
    }

    /** Installs the look and feel. Call {@link FlatLaf#updateUI()} afterwards if windows exist. */
    public static void install(boolean dark) {
        current = dark ? DARK : LIGHT;
        // All sizes here are design pixels; only the operating system's scaling should apply.
        System.setProperty("flatlaf.uiScale.enabled", "false");
        FlatInterFont.install();
        FlatInterFont.installSemiBold();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
        FlatLaf.setup(dark ? new FlatDarkLaf() : new FlatLightLaf());

        Palette c = current;
        UIManager.put("defaultFont", new FontUIResource(regular(14)));
        put("Panel.background", c.bg());
        put("TitlePane.background", c.bg());
        put("TitlePane.inactiveBackground", c.bg());
        put("TitlePane.foreground", c.text());
        put("TitlePane.inactiveForeground", c.textMuted());
        put("Component.focusColor", c.accent());
        put("Component.focusedBorderColor", c.accent());
        put("Component.borderColor", c.border());
        put("Component.accentColor", c.accent());
        put("TextField.background", c.surface());
        put("TextField.foreground", c.text());
        put("PasswordField.background", c.surface());
        put("PasswordField.foreground", c.text());
        put("TextArea.background", c.surface());
        put("TextArea.foreground", c.text());
        put("TextField.placeholderForeground", c.textMuted());
        put("TextComponent.selectionBackground", c.accentSoft());
        put("TextComponent.selectionForeground", c.text());
        put("ComboBox.background", c.surface());
        put("ComboBox.foreground", c.text());
        put("ComboBox.buttonBackground", c.surface());
        put("ComboBox.popupBackground", c.surface());
        put("ComboBox.selectionBackground", c.accentSoft());
        put("ComboBox.selectionForeground", c.text());
        put("List.selectionBackground", c.accentSoft());
        put("List.selectionForeground", c.text());
        put("PopupMenu.background", c.surface());
        put("PopupMenu.borderColor", c.border());
        put("ToolTip.background", c.dark() ? c.surfaceAlt() : c.text());
        put("ToolTip.foreground", c.dark() ? c.text() : c.surface());
        UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));
        put("ScrollBar.thumb", c.borderStrong());
        put("ScrollBar.hoverThumbColor", c.textMuted());
        put("ScrollBar.pressedThumbColor", c.textMuted());
        put("ScrollPane.background", c.bg());
        put("TableHeader.bottomSeparatorColor", c.border());
        put("TableHeader.separatorColor", c.surface());
        put("CheckBox.icon.selectedBackground", c.accent());
        put("CheckBox.icon.selectedBorderColor", c.accent());
        put("CheckBox.icon.checkmarkColor", c.onAccent());
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.trackInsets", new Insets(0, 0, 0, 0));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.allowsAbsolutePositioning", true);
        UIManager.put("ScrollPane.smoothScrolling", true);
        UIManager.put("PopupMenu.borderCornerRadius", 10);
        UIManager.put("Popup.borderCornerRadius", 10);
        UIManager.put("TableHeader.showTrailingVerticalLine", false);
        UIManager.put("TitlePane.unifiedBackground", false);
        UIManager.put("TextComponent.selectAllOnFocusPolicy", "never");
    }

    public static Font regular(float size) {
        return FontUtils.getCompositeFont(FlatInterFont.FAMILY, Font.PLAIN, 1).deriveFont(size);
    }

    public static Font semibold(float size) {
        return FontUtils.getCompositeFont(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 1).deriveFont(size);
    }

    public static Font bold(float size) {
        return FontUtils.getCompositeFont(FlatInterFont.FAMILY, Font.BOLD, 1).deriveFont(size);
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    private static Color rgb(int value) {
        return new Color(value);
    }
}
