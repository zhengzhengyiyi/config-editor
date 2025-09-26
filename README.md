# Config Editor - Minecraft Mod

A powerful in-game JSON configuration file editor for Minecraft, designed to make mod configuration management easier and more intuitive.

![Minecraft](https://img.shields.io/badge/Minecraft-Fabric-green.svg)
![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)

## ✨ Features

### 📝 Advanced Text Editing
- **Syntax Highlighting**: Color-coded JSON syntax for better readability
- **Real-time Error Checking**: Instant validation with visual error indicators
- **Multi-line Editing**: Full support for large configuration files
- **Search & Replace**: Find and navigate through text quickly

### 🎯 User-Friendly Interface
- **File Browser**: Easy navigation through config directory
- **One-Click Operations**: Save, backup, and open folder with single clicks
- **Modification Indicators**: Clear visual cues for unsaved changes
- **Confirmation Dialogs**: Prevent accidental data loss

### 🔧 Technical Features
- **Auto-Formatting**: Automatically formats JSON for consistency
- **Backup System**: Creates automatic backups before saving
- **Plugin System**: Extensible API for additional functionality

## 🛠️ Developer API

Config Editor provides a comprehensive API for mod developers to extend functionality:

### ApiEntrypoint Interface

```java
public interface ApiEntrypoint {
    // Required method - initialize your plugin
    void init();

    // Editor lifecycle events
    default void onEditerOpen(EditorScreen editor) {}
    default void onEditerClose(EditorScreen editor) {}

    // Input handling
    default ActionResult onMouseDown(int x, int y) {
        return ActionResult.SUCCESS;
    }
    default void onMouseScroll() {}
    default ActionResult onType(int keyCode, int scanCode, int modifiers) {
        return ActionResult.SUCCESS;
    }
    default ActionResult onCharTyped(char character, int modifiers) {
        return ActionResult.SUCCESS;
    }

    // Custom rendering
    default void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {}
}
```

### Basic Plugin Example

```java
public class ExamplePlugin implements ApiEntrypoint {
    @Override
    public void init() {
        ApiEntrypoint.LOGGER.info("Example plugin initialized");
    }

    @Override
    public void onEditerOpen(EditorScreen editor) {
        // Setup when editor opens
    }

    @Override
    public ActionResult onType(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_T) {
            // Handle custom key binding
            return ActionResult.FAIL; // Prevent default handling
        }
        return ActionResult.PASS; // Allow default handling
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render custom UI elements
        context.drawText(...);
    }
}
```

### Action Results
- `SUCCESS`: Event handled successfully
- `PASS`: Allow other handlers to process the event
- `FAIL`: Event handled and should stop propagation

### Fabric Mod JSON Registration

```json
{
  "entrypoints": {
    "config_editor": [
      "com.yourmod.YourPluginClass"
    ]
  }
}
```

## 📁 Supported Files

- **JSON Configuration Files** (`.json`)

**Happy configuring!** 🎮

*If you find this mod useful, please consider giving it a star on Modrinth!*
