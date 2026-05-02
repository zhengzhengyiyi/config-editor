package io.github.zhengzhengyiyi.api;

import org.slf4j.Logger;

import io.github.zhengzhengyiyi.ConfigEditorClient;
import io.github.zhengzhengyiyi.gui.EditorScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.Identifier;

/**
 * The entry point for other mods to interact with the config editor.
 * This interface allows for extending the functionality of the editor
 * screen by listening to events and rendering custom content.
 */
public interface ApiEntrypoint {
	
	/**
	 * The Logger that can be use for ApiEntrypoint
	 * 
	 * @see Logger
	 */
	final Logger LOGGER = ConfigEditorClient.API_ENTRYPOINT_LOGGER;
    /**
     * Initializes the API entry point. This method is called by the config editor mod
     * during its initialization phase. It is the only required method to be implemented.
     */
    void init();
    
    default Identifier getIdentifier() {
    	return Identifier.fromNamespaceAndPath("config_editor", "plugin");
    }

    /**
     * Called when the editor screen is opened.
     * @param editor The instance of the EditorScreen.
     */
    default void onEditerOpen(EditorScreen editor) {
    }

    /**
     * Called when the editor screen is closed.
     * @param editor The instance of the EditorScreen.
     */
    default void onEditerClose(EditorScreen editor) {
    }

    /**
     * Called when a mouse button is pressed on the screen.
     * @param x The x-coordinate of the mouse cursor.
     * @param y The y-coordinate of the mouse cursor.
     */
    default InteractionResult onMouseDown(int x, int y) {
    	return InteractionResult.SUCCESS;
    }

    /**
     * Called when the mouse scroll wheel is used.
     */
    default void onMouseScroll() {
    }

    /**
     * Called when a key is typed on the keyboard.
     */
    default InteractionResult onType(int keyCode, int scanCode, int modifiers) {
    	return InteractionResult.SUCCESS;
    }
    
    default InteractionResult onCharTyped(CharacterEvent input) {
    	return InteractionResult.SUCCESS;
    }

    /**
     * Called to render custom buttons or elements on the screen.
     * @param context The draw context used for rendering.
     * @param mouseX The x-coordinate of the mouse cursor.
     * @param mouseY The y-coordinate of the mouse cursor.
     * @param delta The partial ticks for smooth rendering.
     */
    default void renderButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }
}
