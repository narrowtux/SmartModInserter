package com.narrowtux.fmm.gui;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by tux on 02/08/15.
 */
public class GuiFiles {
    public static final String MAIN_WINDOW = "/mainwindow.fxml";
    public static final String SETTINGS_WINDOW = "/settingswindow.fxml";
    public static final String MODS_TAB = "/modstab.fxml";
    public static final String MODPACKS_TAB = "/modpackstab.fxml";
    public static final String SAVES_TAB = "/savestab.fxml";

    private static final Set<String> values = new LinkedHashSet<>();

    static {
        values.add(MAIN_WINDOW);
        values.add(SETTINGS_WINDOW);
        values.add(MODS_TAB);
        values.add(MODPACKS_TAB);
        values.add(SAVES_TAB);
    }

    public static Set<String> getValues() {
        return Collections.unmodifiableSet(values);
    }
}
