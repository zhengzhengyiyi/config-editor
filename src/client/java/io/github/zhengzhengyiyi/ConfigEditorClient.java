package io.github.zhengzhengyiyi;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import io.github.zhengzhengyiyi.gui.EditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.List;
import java.util.ArrayList;

public class ConfigEditorClient implements ClientModInitializer {
	public static String MOD_ID = "config_editor";
	
	/**
	 * The keybinding for open the configure
	 * @see KeyBinding
	 */
	public static KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"zhengzhengyiyi.key.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			"key.category.zhengzhengyiyi"
	));
	/**
	 * The Logger for the other mod's entry point, usually for printing errors.
	 */
	public static Logger API_ENTRYPOINT_LOGGER = LoggerFactory.getLogger("api_entry_point");
	
	/**
	 * The Logger for the whole mod.
	 */
	public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	/**
	 * 
	 * put every api entry point into a List
	 * 
	 * @see List
	 */
	public static final List<ApiEntrypoint> ENTRYPOINTS = new ArrayList<>();
	
	@Override
	public void onInitializeClient() {
		FabricLoader.getInstance()
        .getEntrypointContainers(MOD_ID, ApiEntrypoint.class)
        .forEach(entrypoint -> {
            String modId = entrypoint.getProvider().getMetadata().getId();
            try {
            	entrypoint.getEntrypoint().init();
            	
            	ENTRYPOINTS.add(entrypoint.getEntrypoint());
            } catch (Throwable e) {
            	API_ENTRYPOINT_LOGGER.error("Mod '{}' has a broken 'config_editor' entrypoint implementation.", modId, e);
            }
        });
		
		ENTRYPOINTS.add(new UndoRedoEntrypoint());
		
		ClientTickEvents.END_CLIENT_TICK.register((client) -> {
			if (key.isPressed()) {
				client.setScreen(new EditorScreen());
			}
		});
	}
}
