# Config Editor - Minecraft Mod

A professional in-game configuration file editor for Minecraft, providing advanced editing capabilities with syntax highlighting, real-time validation, and extensible plugin system.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.2-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.3-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.5-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.2-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.3-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.5-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.7-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.9-green.svg)
![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)
![License](https://img.shields.io/badge/License-Apache%20License%202.0-red.svg)

> ### **Thank you for 400+ downloads!**
> Next version will coming soon. Your support means everything!
> If you have any ideas, please post on [github discussions](https://github.com/zhengzhengyiyi/config-editor/discussions)

## Known Issues
- #### Please download version 1.1.3, there is some issue in version under 1.1.3
- #### May be have some unknown file lock issue.

## ✨ Features

### 📝 Professional Text Editing
- **Advanced Syntax Highlighting**: Full support for multiple file formats with color-coded syntax highlighting
- **Multi-Format Support**: JSON, Properties, TOML, YAML, CFG, INI, and plain text files
- **Real-time Validation**: Instant error checking with visual indicators and tooltips
- **Multi-line Editor**: Support for large configuration files with scrollable interface
- **Intelligent Search**: Text search engine with highlight and navigation

### 🎯 User Experience
- **File Management**: Browse and switch between configuration files easily
- **Visual Feedback**: Clear modification indicators and confirmation dialogs
- **Theme Support**: Dark, Light, and Auto themes with customizable backgrounds
- **Accessibility**: Full keyboard navigation and screen reader support

![a show case of the gui](https://cdn.modrinth.com/data/SHXjjvQ7/images/48a2664240b2ca15a8d4b6944145943320d49060_350.webp)

### 🔧 Advanced Capabilities
- **Auto-completion**: Code suggestions based on file structure and common patterns
- **Backup System**: Automatic backup creation with configurable retention
- **Performance Monitoring**: Built-in performance tracking for large files
- **File Navigation**: Easy browsing through config directory with scrollable file list

<details>
<summary>🛠️ Developer API</summary>

Config Editor provides a comprehensive API for developers to extend functionality through plugins:

### Core API Interface

build.gradle
```gradle
dependencies {
    implementation("io.github.zhengzhengyiyi:config_editor:project.config_editor_version")
}
```
gradle.properties
```properties
config_editor_version=1.1.4+1.21.5
```

```java
public interface ApiEntrypoint {
    // Plugin initialization
    void init();
    
    // Editor lifecycle events
    void onEditerOpen(EditorScreen editor);
    void onEditerClose(EditorScreen editor);
    
    // Input handling with precise control
    ActionResult onMouseDown(int x, int y);
    void onMouseScroll();
    ActionResult onType(int keyCode, int scanCode, int modifiers);
    ActionResult onCharTyped(char character, int modifiers);
    
    // Custom rendering capabilities
    void renderButton(DrawContext context, int mouseX, int mouseY, float delta);
}
```

### Built-in Plugins Examples

The mod includes several example plugins demonstrating API capabilities:

```java
// Auto bracket completion
public class AutoBracketCompletionEntrypoint implements ApiEntrypoint

// Date-time display in editor
public class DateTimeDisplayEntrypoint implements ApiEntrypoint

// Text statistics and analytics
public class TextStatsEntrypoint implements ApiEntrypoint

// Undo/redo functionality
public class UndoRedoEntrypoint implements ApiEntrypoint
```

### Advanced Plugin Development

```java
public class AdvancedPlugin implements ApiEntrypoint {
    private static final Logger LOGGER = ApiEntrypoint.LOGGER;
    
    @Override
    public void init() {
        LOGGER.info("Advanced plugin initialized with custom features");
    }
    
    @Override
    public ActionResult onType(int keyCode, int scanCode, int modifiers) {
        // Custom keyboard shortcuts
        if (keyCode == GLFW.GLFW_KEY_F1 && hasControlDown()) {
            showCustomHelp();
            return ActionResult.FAIL; // Prevent default handling
        }
        return ActionResult.PASS; // Allow normal processing
    }
    
    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // Add custom UI elements to editor
        context.drawText(context.getTextRenderer(), "Custom Plugin", 10, 10, 0xFFFFFF, false);
    }
}
```

### Action Result System
- `SUCCESS`: Event handled successfully, continue processing
- `PASS`: Allow other plugins to handle the event
- `FAIL`: Event handled completely, stop propagation

### Registration in fabric.mod.json

```json
{
  "entrypoints": {
    "config_editor": [
      "com.yourmod.YourCustomPlugin",
      "com.yourmod.AnotherPlugin"
    ]
  }
}
```
</details>

## 🔍 Technical Features

### Performance Optimization
- **Efficient Rendering**: Optimized text rendering for large files
- **Memory Management**: Smart caching and resource cleanup
- **Async Operations**: Non-blocking file operations

### File Handling
- **Safe File Operations**: File locking and conflict detection
- **Error Recovery**: Automatic recovery from corrupted files
- **Encoding Support**: Full UTF-8 support with proper encoding detection

## 📁 Supported File Types

- **JSON Configuration Files** (`.json`)
- **Properties Files** (`.properties`)
- **TOML Files** (`.toml`)
- **YAML Files** (`.yml`, `.yaml`)
- **Configuration Files** (`.cfg`, `.conf`, `.ini`)
- **Text Files** (`.txt`)

**Happy configuring!** 🎮

*If you find this mod useful, please consider giving it a star on Modrinth!*
