package io.github.zhengzhengyiyi.config;

import com.google.gson.annotations.SerializedName;

/**
 * Configuration data class that holds all configurable variables for the editor.
 * This class is used for JSON serialization and deserialization.
 */
public class ModConfigData extends io.github.zhengzhengyiyi.api.config.ConfigData {
	/**
	 * If it is true, you can not edit the files
	 */
    @SerializedName("readonly_mode")
    public boolean readonly_mode = false;
    
    /**
     * If enable, it will enable the auto hint like if you pressed "(" it will make a ")"
     */
    @SerializedName("hint")
    public boolean hint = true;
    
    /**
     * The theme of the editor {@link ThemeMode}
     */
    public ThemeMode theme = ThemeMode.DARK;
    
    /**
     * If true, it will use theme to render background
     */
    public boolean doRenderBackground = false;
    
    /**
     * to enable suggestions when you type
     */
    public boolean doSuggestions = true;
    
    /**
     * The theme can be either dark, light and auto
     */
    public enum ThemeMode {
        DARK, LIGHT, AUTO
    }
}
