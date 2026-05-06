package io.github.zhengzhengyiyi.config;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

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
     * If true, shows a Jade/WTHIT-style overlay at the top of the screen
     * displaying the name of the block the crosshair is pointing at.
     */
    @SerializedName("show_block_overlay")
    public boolean showBlockOverlay = true;

    /**
     * If true, shows a Jade/WTHIT-style overlay just below the block overlay
     * displaying the name, registry id, and health bar of the entity the
     * crosshair is pointing at.
     */
    @SerializedName("show_entity_overlay")
    public boolean showEntityOverlay = true;

    /**
     * List of disabled plugin identifiers (namespace:path format).
     * Plugins in this list will not be loaded into ENTRYPOINTS on startup.
     */
    @SerializedName("disabled_plugins")
    public List<String> disabledPlugins = new ArrayList<>();

    /**
     * The theme can be either dark, light and auto
     */
    public enum ThemeMode {
        DARK, LIGHT, AUTO
    }
}
