package io.github.zhengzhengyiyi.gui.theme;

import io.github.zhengzhengyiyi.ConfigEditorClient;
import io.github.zhengzhengyiyi.config.ModConfigData;

public class ThemeManager {
    private static ThemeManager instance;

    // ── Dark palette ──────────────────────────────────────────────────────────
    public static final int DARK_BACKGROUND      = 0xFF1E1E2E; // deep navy-charcoal
    public static final int DARK_SIDEBAR         = 0xFF181825; // slightly darker sidebar
    public static final int DARK_PANEL           = 0xFF252535; // card / panel surface
    public static final int DARK_BORDER          = 0xFF45475A; // subtle border
    public static final int DARK_BORDER_ACCENT   = 0xFF89B4FA; // blue accent border
    public static final int DARK_TEXT            = 0xFFCDD6F4; // primary text
    public static final int DARK_TEXT_MUTED      = 0xFF6C7086; // muted / secondary text
    public static final int DARK_ACCENT          = 0xFF89B4FA; // blue accent (Catppuccin Mocha Blue)
    public static final int DARK_ACCENT_GREEN    = 0xFFA6E3A1; // success green
    public static final int DARK_ACCENT_RED      = 0xFFF38BA8; // error red
    public static final int DARK_ACCENT_YELLOW   = 0xFFF9E2AF; // warning yellow
    public static final int DARK_ACCENT_MAUVE    = 0xFFCBA6F7; // purple/mauve
    public static final int DARK_SELECTED_ROW    = 0xFF313244; // selected list row
    public static final int DARK_HOVER_ROW       = 0xFF2A2A3C; // hovered list row

    // ── Light palette ─────────────────────────────────────────────────────────
    public static final int LIGHT_BACKGROUND     = 0xFFEFF1F5; // Catppuccin Latte base
    public static final int LIGHT_SIDEBAR        = 0xFFE6E9EF; // sidebar
    public static final int LIGHT_PANEL          = 0xFFDCE0E8; // card surface
    public static final int LIGHT_BORDER         = 0xFFBCC0CC; // border
    public static final int LIGHT_BORDER_ACCENT  = 0xFF1E66F5; // blue accent
    public static final int LIGHT_TEXT           = 0xFF4C4F69; // primary text
    public static final int LIGHT_TEXT_MUTED     = 0xFF9CA0B0; // muted text
    public static final int LIGHT_ACCENT         = 0xFF1E66F5; // blue accent
    public static final int LIGHT_ACCENT_GREEN   = 0xFF40A02B; // success
    public static final int LIGHT_ACCENT_RED     = 0xFFD20F39; // error
    public static final int LIGHT_ACCENT_YELLOW  = 0xFFDF8E1D; // warning
    public static final int LIGHT_ACCENT_MAUVE   = 0xFF8839EF; // purple
    public static final int LIGHT_SELECTED_ROW   = 0xFFCCD0DA; // selected row
    public static final int LIGHT_HOVER_ROW      = 0xFFD4D8E2; // hovered row

    // Cached values — invalidated when the theme setting changes
    private ModConfigData.ThemeMode cachedTheme = null;
    private int cachedBg = DARK_BACKGROUND;
    private boolean cachedIsDark = true;

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private void recompute() {
        ModConfigData.ThemeMode theme = ConfigEditorClient.configManager.getConfig().theme;
        if (theme != cachedTheme) {
            cachedTheme = theme;
            if (theme == ModConfigData.ThemeMode.AUTO) {
                cachedIsDark = isSystemDarkMode();
            } else {
                cachedIsDark = theme == ModConfigData.ThemeMode.DARK;
            }
            cachedBg = cachedIsDark ? DARK_BACKGROUND : LIGHT_BACKGROUND;
        }
    }

    public boolean isDark() {
        recompute();
        return cachedIsDark;
    }

    public int getBackgroundColor()   { recompute(); return cachedIsDark ? DARK_BACKGROUND    : LIGHT_BACKGROUND; }
    public int getSidebarColor()      { recompute(); return cachedIsDark ? DARK_SIDEBAR        : LIGHT_SIDEBAR; }
    public int getPanelColor()        { recompute(); return cachedIsDark ? DARK_PANEL          : LIGHT_PANEL; }
    public int getBorderColor()       { recompute(); return cachedIsDark ? DARK_BORDER         : LIGHT_BORDER; }
    public int getBorderAccentColor() { recompute(); return cachedIsDark ? DARK_BORDER_ACCENT  : LIGHT_BORDER_ACCENT; }
    public int getTextColor()         { recompute(); return cachedIsDark ? DARK_TEXT           : LIGHT_TEXT; }
    public int getMutedTextColor()    { recompute(); return cachedIsDark ? DARK_TEXT_MUTED     : LIGHT_TEXT_MUTED; }
    public int getAccentColor()       { recompute(); return cachedIsDark ? DARK_ACCENT         : LIGHT_ACCENT; }
    public int getAccentGreen()       { recompute(); return cachedIsDark ? DARK_ACCENT_GREEN   : LIGHT_ACCENT_GREEN; }
    public int getAccentRed()         { recompute(); return cachedIsDark ? DARK_ACCENT_RED     : LIGHT_ACCENT_RED; }
    public int getAccentYellow()      { recompute(); return cachedIsDark ? DARK_ACCENT_YELLOW  : LIGHT_ACCENT_YELLOW; }
    public int getAccentMauve()       { recompute(); return cachedIsDark ? DARK_ACCENT_MAUVE   : LIGHT_ACCENT_MAUVE; }
    public int getSelectedRowColor()  { recompute(); return cachedIsDark ? DARK_SELECTED_ROW   : LIGHT_SELECTED_ROW; }
    public int getHoverRowColor()     { recompute(); return cachedIsDark ? DARK_HOVER_ROW      : LIGHT_HOVER_ROW; }

    /** Call this after the user changes the theme so the cache is immediately refreshed. */
    public void invalidate() {
        cachedTheme = null;
    }

    private boolean isSystemDarkMode() {
        return true; // default to dark; no reliable cross-platform API in MC context
    }
}
