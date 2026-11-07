package com.clientfiles.ui;

import java.util.prefs.Preferences;

/** Per-user interface preferences, kept between sessions. */
public final class Settings {
    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

    private Settings() {
    }

    public static boolean darkTheme() {
        return PREFS.getBoolean("darkTheme", false);
    }

    public static void darkTheme(boolean dark) {
        PREFS.putBoolean("darkTheme", dark);
    }

    public static boolean reducedMotion() {
        return PREFS.getBoolean("reducedMotion", false);
    }

    public static void reducedMotion(boolean reduced) {
        PREFS.putBoolean("reducedMotion", reduced);
    }
}
