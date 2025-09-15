package io.github.zhengzhengyiyi;

import org.lwjgl.glfw.GLFW;

import io.github.zhengzhengyiyi.gui.EditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class ConfigEditorClient implements ClientModInitializer {
	public static KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"zhengzhengyiyi.key.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			"key.category.zhengzhengyiyi"
	));
	
	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register((client) -> {
			if (key.isPressed()) {
				client.setScreen(new EditorScreen());
			}
		});
	}
}
