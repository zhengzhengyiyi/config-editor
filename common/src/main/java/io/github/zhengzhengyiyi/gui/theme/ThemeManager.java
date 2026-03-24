package io.github.zhengzhengyiyi.gui.theme;

import io.github.zhengzhengyiyi.CommonEntryPoint;
import io.github.zhengzhengyiyi.config.ModConfigData;

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
        ModConfigData.ThemeMode theme = CommonEntryPoint.configManager.getConfig().theme;
        if (theme == ModConfigData.ThemeMode.AUTO) {
            return isSystemDarkMode() ? DARK_BACKGROUND : LIGHT_BACKGROUND;
        }
        return theme == ModConfigData.ThemeMode.DARK ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }
    
    private boolean isSystemDarkMode() {
        return false;
    }
}
