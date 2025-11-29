package io.github.zhengzhengyiyi.neoforge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;

import io.github.zhengzhengyiyi.gui.*;

import org.lwjgl.glfw.GLFW;

import io.github.zhengzhengyiyi.CommonEntryPoint;
import io.github.zhengzhengyiyi.NbtSaver;

@Mod(CommonEntryPoint.MOD_ID)
public final class NeoForgeEntryPoint {
    public static final Lazy<KeyBinding> openGuiKey = Lazy.of(() -> new KeyBinding(
            "key.config_editor.openGUI",
            KeyConflictContext.UNIVERSAL,
            net.minecraft.client.util.InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KeyBinding.Category.GAMEPLAY
        ));

    public static final Lazy<KeyBinding> openAiChatKey = Lazy.of(() -> new KeyBinding(
            "key.config_editor.openAiChat",
            KeyConflictContext.UNIVERSAL,
            net.minecraft.client.util.InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KeyBinding.Category.GAMEPLAY
        ));
    public static final Lazy<KeyBinding> displayNbtKey = Lazy.of(() -> new KeyBinding(
            "key.config_editor.displayNbtKey",
            KeyConflictContext.UNIVERSAL,
            net.minecraft.client.util.InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KeyBinding.Category.GAMEPLAY
        ));

    public NeoForgeEntryPoint() {
        CommonEntryPoint.init();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openGuiKey.get().wasPressed()) {
            MinecraftClient.getInstance().setScreen(new EditorScreen());
        }

        if (openAiChatKey.get().wasPressed()) {
            MinecraftClient.getInstance().setScreen(new EditorScreen());
        }
        
        if (displayNbtKey.get().wasPressed()) {
            new NbtSaver().saveAndOpenEditor();
        }
    }

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(openGuiKey.get());
        event.register(openAiChatKey.get());
        event.register(displayNbtKey.get());
    }
}