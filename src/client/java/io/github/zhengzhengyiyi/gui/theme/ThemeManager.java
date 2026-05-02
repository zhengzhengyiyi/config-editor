package io.github.zhengzhengyiyi.gui.theme;

import io.github.zhengzhengyiyi.ConfigEditorClient;
import io.github.zhengzhengyiyi.config.ModConfigData;

public class ThemeManager {
    private static ThemeManager instance;

    public static final int DARK_BACKGROUND  = 0xFF2D2D2D;
    public static final int LIGHT_BACKGROUND = 0xFFFFFFFF;

    // Cached values — invalidated when the theme setting changes
    private ModConfigData.ThemeMode cachedTheme = null;
    private int cachedColor = DARK_BACKGROUND;

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public int getBackgroundColor() {
        ModConfigData.ThemeMode theme = ConfigEditorClient.configManager.getConfig().theme;
        // Only recompute when the theme setting has actually changed
        if (theme != cachedTheme) {
            cachedTheme = theme;
            if (theme == ModConfigData.ThemeMode.AUTO) {
                cachedColor = isSystemDarkMode() ? DARK_BACKGROUND : LIGHT_BACKGROUND;
            } else {
                cachedColor = theme == ModConfigData.ThemeMode.DARK ? DARK_BACKGROUND : LIGHT_BACKGROUND;
            }
        }
        return cachedColor;
    }

    /** Call this after the user changes the theme so the cache is immediately refreshed. */
    public void invalidate() {
        cachedTheme = null;
    }

    private boolean isSystemDarkMode() {
        return false;
    }
}
