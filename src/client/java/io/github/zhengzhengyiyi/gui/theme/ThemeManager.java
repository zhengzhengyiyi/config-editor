package io.github.zhengzhengyiyi.gui.theme;

import io.github.zhengzhengyiyi.config.ConfigData;
import io.github.zhengzhengyiyi.config.ConfigManager;

public class ThemeManager {
    private static ThemeManager instance;
    
    public static final int DARK_BACKGROUND = 0xFF2D2D2D;
    public static final int LIGHT_BACKGROUND = 0xFFFFFFFF;
    
    private ThemeManager() {}
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public int getBackgroundColor() {
        ConfigData.ThemeMode theme = ConfigManager.getConfig().theme;
        if (theme == ConfigData.ThemeMode.AUTO) {
            return isSystemDarkMode() ? DARK_BACKGROUND : LIGHT_BACKGROUND;
        }
        return theme == ConfigData.ThemeMode.DARK ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }
    
    private boolean isSystemDarkMode() {
        return false;
    }
}
