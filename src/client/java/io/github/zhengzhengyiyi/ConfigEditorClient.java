package io.github.zhengzhengyiyi;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.zhengzhengyiyi.addon.*;
import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import io.github.zhengzhengyiyi.api.config.ConfigManager;
import io.github.zhengzhengyiyi.config.ModConfigData;
import io.github.zhengzhengyiyi.gui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ConfigEditorClient implements ClientModInitializer {
	public static String MOD_ID = "config_editor";
	public static ConfigManager<ModConfigData> configManager = new ConfigManager<>(Paths.get("config", "editor_config.json"), new ModConfigData(), ModConfigData.class);

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
	
	/**
	 * The keybinding for open the configure
	 * @see KeyBinding
	 */
	public static KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"zhengzhengyiyi.key.open_gui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyMapping.Category.GAMEPLAY
	));

	public static KeyMapping chatkey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"zhengzhengyiyi.key.open_chat_gui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyMapping.Category.GAMEPLAY
	));
	
	public static KeyMapping nbtDisplayKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"zhengzhengyiyi.key.nbtDisplay",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyMapping.Category.GAMEPLAY
	));
	
	public static final List<ApiEntrypoint> DISABLED_ENTRYPOINTS = new ArrayList<>();
	
	public static final List<ApiEntrypoint> TOTAL_ENTRYPOINTS = new ArrayList<>();
	
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
		
		TOTAL_ENTRYPOINTS.add(new UndoRedoEntrypoint());
		TOTAL_ENTRYPOINTS.add(new TextStatsEntrypoint());
		TOTAL_ENTRYPOINTS.add(new DateTimeDisplayEntrypoint());
		TOTAL_ENTRYPOINTS.add(new AutoBracketCompletionEntrypoint());
		TOTAL_ENTRYPOINTS.add(new TextStatsEntrypoint());
		
		ENTRYPOINTS.add(new UndoRedoEntrypoint());
		ENTRYPOINTS.add(new TextStatsEntrypoint());
		ENTRYPOINTS.add(new DateTimeDisplayEntrypoint());
		ENTRYPOINTS.add(new AutoBracketCompletionEntrypoint());
		ENTRYPOINTS.add(new TextStatsEntrypoint());
		
		ClientTickEvents.END_CLIENT_TICK.register((client) -> {
			if (key.consumeClick()) {
				client.setScreen(new EditorScreen());
			}
			if (chatkey.consumeClick()) {
				client.setScreen(new AIChatScreen());
			}
			if (nbtDisplayKey.consumeClick()) {
				new NbtSaver().saveAndOpenEditor();
			}
		});
		
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            configManager.shutdown();
        });
		
//		testLanguageResources();
	}
	
	public static void testLanguageResources() {
		Component test = Component.translatable("configEditor.test");
		
		LOGGER.info("----------------test------------------");
		LOGGER.info(test.toString());
		LOGGER.info(test.getString());
		LOGGER.info("--------------test-ended--------------");
	}
}
