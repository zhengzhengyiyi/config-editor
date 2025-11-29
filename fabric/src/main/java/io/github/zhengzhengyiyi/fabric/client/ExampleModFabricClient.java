package io.github.zhengzhengyiyi.fabric.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import io.github.zhengzhengyiyi.CommonEntryPoint;
import io.github.zhengzhengyiyi.NbtSaver;
import io.github.zhengzhengyiyi.addon.AutoBracketCompletionEntrypoint;
import io.github.zhengzhengyiyi.addon.DateTimeDisplayEntrypoint;
import io.github.zhengzhengyiyi.addon.TextStatsEntrypoint;
import io.github.zhengzhengyiyi.addon.UndoRedoEntrypoint;
import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import io.github.zhengzhengyiyi.gui.AIChatScreen;
import io.github.zhengzhengyiyi.gui.EditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class ExampleModFabricClient implements ClientModInitializer {
	/**
	 * The keybinding for open the configure
	 * @see KeyBinding
	 */
	public static KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"zhengzhengyiyi.key.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyBinding.Category.GAMEPLAY
	));

	public static KeyBinding chatkey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"zhengzhengyiyi.key.open_chat_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyBinding.Category.GAMEPLAY
	));
	
	public static KeyBinding nbtDisplayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"zhengzhengyiyi.key.nbtDisplay",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyBinding.Category.GAMEPLAY
	));
	
	public static List<ApiEntrypoint> ENTRYPOINTS = new java.util.ArrayList<ApiEntrypoint>();
	
    @Override
    public void onInitializeClient() {
    	System.out.println("Initializing Config Editor Fabric Client...");
    	ENTRYPOINTS.add(new UndoRedoEntrypoint());
		ENTRYPOINTS.add(new TextStatsEntrypoint());
		ENTRYPOINTS.add(new DateTimeDisplayEntrypoint());
		ENTRYPOINTS.add(new AutoBracketCompletionEntrypoint());
		ENTRYPOINTS.add(new TextStatsEntrypoint());
		
    	FabricLoader.getInstance()
        .getEntrypointContainers(CommonEntryPoint.MOD_ID, ApiEntrypoint.class)
        .forEach(entrypoint -> {
//            String modId = entrypoint.getProvider().getMetadata().getId();
            try {
            	entrypoint.getEntrypoint().init();
            	
            	ENTRYPOINTS.add(entrypoint.getEntrypoint());
            } catch (Throwable e) {
            	e.printStackTrace();
            }
        });
    	
    	ClientTickEvents.END_CLIENT_TICK.register((client) -> {
			if (key.isPressed()) {
				client.setScreen(new EditorScreen());
			}
			if (chatkey.isPressed()) {
				client.setScreen(new AIChatScreen());
			}
			if (nbtDisplayKey.isPressed()) {
				new NbtSaver().saveAndOpenEditor();
			}
		});
		
        CommonEntryPoint.fabric_init();
        
        System.out.println("config editor Fabric Client Initialized");
    }
}
