package io.github.zhengzhengyiyi.gui;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.zhengzhengyiyi.util.BackupHelper;
import io.github.zhengzhengyiyi.*;
import io.github.zhengzhengyiyi.config.ModConfigData;
import io.github.zhengzhengyiyi.gui.theme.ThemeManager;
import io.github.zhengzhengyiyi.gui.widget.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.ClickableWidget;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.IOException;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorScreen.class);
    private List<Path> configFiles;
    private int selectedIndex = 0;
    private MultilineEditor editor;
    private boolean modified = false;
    private ButtonWidget saveButton;
    private ButtonWidget openFolderButton;
    private ButtonWidget backupButton;
    private String buffer = "";
    private TextFieldWidget searchField;
    private ButtonWidget visualEditButton;
    private ButtonWidget aiChatButton;
    private ButtonWidget searchNextButton;
    private ButtonWidget searchPrevButton;
    private ButtonWidget managePluginsButton;
    private boolean searchVisible = false;
    private ThemeManager themeManager;
    private ButtonWidget themeToggleButton;
    private int fileListScrollOffset = 0;
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    private List<ButtonWidget> fileButtonList;

    public EditorScreen() {
        super(Text.translatable("configeditor.title"));
    }

    @Override
    protected void init() {
        super.init();
        
        themeManager = ThemeManager.getInstance();
        themeToggleButton = ButtonWidget.builder(
                Text.translatable(getThemeButtonText()),
                button -> toggleTheme())
                .dimensions(this.width - 55, 5, 50, 20)
                .build();
        this.addDrawableChild(themeToggleButton);
        
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            configFiles = new ArrayList<>();
            loadConfigFilesRecursively(configDir, configFiles);
        } catch (Exception e) {
            configFiles = new ArrayList<>();
            LOGGER.error("Failed to list config files", e);
        }

        fileButtonList = new ArrayList<>();
        
        scrollUpButton = ButtonWidget.builder(Text.literal("↑"), button -> scrollUp())
                .dimensions(145, 25, 20, 20)
                .build();
        scrollDownButton = ButtonWidget.builder(Text.literal("↓"), button -> scrollDown())
                .dimensions(145, this.height - 45, 20, 20)
                .build();
        
        managePluginsButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.plugins"),
                button -> client.setScreen(new PluginManagerScreen(this)))
                .dimensions(this.width - 40, this.height - 30, 40, 20)
                .build();
        
        this.addDrawableChild(scrollUpButton);
        this.addDrawableChild(scrollDownButton);
        this.addDrawableChild(managePluginsButton);
        
        renderFileList();
        
        editor = new MultilineEditor(
                170, 20,
                this.width - 180, this.height - 55,
                Text.translatable("configeditor.editor"));
        editor.setChangedListener(text -> {
            if (!buffer.equals(text)) {
                modified = true;
                updateButtonStates();
            } else {
                modified = false;
            }
        });

        int bottomY = this.height - 30;
        int centerX = this.width / 2;
        
        backupButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.backup"), 
                button -> BackupHelper.backupEntireConfigDirectory())
                .dimensions(centerX - 200, bottomY, 80, 20)
                .build();

        saveButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.save"),
                button -> saveFile())
                .dimensions(centerX - 110, bottomY, 80, 20)
                .build();
        
        visualEditButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.visual"),
                button -> openVisualEditor())
                .dimensions(centerX + 10, bottomY, 100, 20)
                .build();
//        visualEditButton.active = false;
        
        openFolderButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.openfolder"),
                button -> openConfigFolder())
                .dimensions(0, 0, 80, 20)
                .build();
        
        int searchX = this.width - 320;
        searchField = new TextFieldWidget(
            textRenderer, 
            searchX, 
            5, 
            150, 
            20, 
            Text.translatable("configeditor.search.placeholder")
        );
        searchField.setChangedListener(text -> {
            if (!text.trim().isEmpty()) {
                startSearch(text.trim());
            } else {
                endSearch();
            }
        });
        searchField.setVisible(true);

        searchPrevButton = ButtonWidget.builder(
            Text.translatable("configeditor.search.prev"), 
            button -> findPrevious())
            .dimensions(searchX + 155, 5, 40, 20)
            .build();
        searchPrevButton.active = false;
        searchPrevButton.visible = true;
        
        searchNextButton = ButtonWidget.builder(
            Text.translatable("configeditor.search.next"), 
            button -> findNext())
            .dimensions(searchX + 195, 5, 40, 20)
            .build();
        searchNextButton.active = false;
        searchNextButton.visible = true;
        
        ButtonWidget closeSearchButton = ButtonWidget.builder(
            Text.literal("×"), 
            button -> {
                searchField.setText("");
                endSearch();
            })
            .dimensions(searchX + 235, 5, 20, 20)
            .build();
        closeSearchButton.visible = true;
        
        int quickButtonX = this.width - 100;
        int quickButtonY = this.height - 20;
        int quickButtonWidth = 55;
        int quickButtonHeight = 20;
        
        aiChatButton = ButtonWidget.builder(
            Text.translatable("configeditor.button.aichat"),
            button -> openAiChat())
            .dimensions(quickButtonX, quickButtonY, quickButtonWidth, quickButtonHeight)
            .build();
        
        this.addDrawableChild(backupButton);
        this.addDrawableChild(saveButton);
        this.addDrawableChild(visualEditButton);
        this.addDrawableChild(openFolderButton);
        
        this.addDrawableChild(searchField);
        this.addDrawableChild(searchPrevButton);
        this.addDrawableChild(searchNextButton);
        this.addDrawableChild(closeSearchButton);
        
        this.addDrawableChild(aiChatButton);
        
        this.addDrawableChild(editor);
        
        this.setInitialFocus(editor);

        if (!configFiles.isEmpty()) {
            loadFile(selectedIndex);
        } else {
            editor.setText("{}");
            editor.setEditable(false);
            LOGGER.warn("No config files found in config directory");
        }
        
        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
            entrypoint.onEditerOpen(this);
        }
        
        updateButtonStates();
        updateScrollButtons();
    }
    
    public boolean isConfigFile(Path file) {
        String fileName = file.getFileName().toString();
        
        if (fileName.equals(".DS_Store") || 
            fileName.startsWith(".") || 
            fileName.equals("Thumbs.db")) {
            return false;
        }
        
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".json") || 
               lowerName.endsWith(".txt") || 
               lowerName.endsWith(".yml") || 
               lowerName.endsWith(".yaml") || 
               lowerName.endsWith(".properties") || 
               lowerName.endsWith(".toml") || 
               lowerName.endsWith(".conf") || 
               lowerName.endsWith(".cfg") || 
               lowerName.endsWith(".ini");
    }

    private String formatFileName(String filePath) {
        if (filePath.length() > 22) {
            int lastSeparator = filePath.lastIndexOf('/');
            if (lastSeparator != -1) {
                String folder = filePath.substring(0, lastSeparator);
                String fileName = filePath.substring(lastSeparator + 1);
                
                if (folder.length() > 8) {
                    folder = folder.substring(0, 7) + "..";
                }
                if (fileName.length() > 12) {
                    fileName = fileName.substring(0, 11) + "..";
                }
                return folder + "/" + fileName;
            } else {
                return filePath.substring(0, 17) + "...";
            }
        }
        return filePath;
    }

    private void renderFileList() {
        for (ButtonWidget button : fileButtonList) {
            this.remove(button);
        }
        fileButtonList.clear();
        
        int buttonY = 25;
        for (int i = fileListScrollOffset; i < configFiles.size() && i < fileListScrollOffset + 15; i++) {
            int index = i;
            Path file = configFiles.get(i);
            Path configDir = FabricLoader.getInstance().getConfigDir();
            String relativePath = configDir.relativize(file).toString();
            
            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(formatFileName(relativePath)),
                    _button -> switchFile(index))
                    .dimensions(10, buttonY, 130, 20)
                    .build();
            this.addDrawableChild(button);
            fileButtonList.add(button);
            buttonY += 23;
        }
    }

    private void scrollUp() {
        if (fileListScrollOffset > 0) {
            fileListScrollOffset--;
            renderFileList();
            updateScrollButtons();
        }
    }

    private void scrollDown() {
        if (fileListScrollOffset < configFiles.size() - 15) {
            fileListScrollOffset++;
            renderFileList();
            updateScrollButtons();
        }
    }

    private void updateScrollButtons() {
        scrollUpButton.active = fileListScrollOffset > 0;
        scrollDownButton.active = fileListScrollOffset < configFiles.size() - 15;
    }

    private void updateButtonStates() {
        if (saveButton != null) {
            saveButton.active = true;
        }
    }

    private void switchFile(int index) {
        loadFile(index);
    }
    
    private void loadFile(int index) {
        if (index < 0 || index >= configFiles.size()) {
            LOGGER.error("Invalid file index: {}", index);
            return;
        }
        
        selectedIndex = index;
        modified = false;
        Path file = configFiles.get(index);
        
        try {
            String content = readFileWithFallbackEncoding(file);
            buffer = content;
            
//            boolean isJson = checkIfJson(content);
            editor.setFileName(getFileName(file.getFileName()));
//            editor.setJsonMode(isJson);
            editor.setText(content);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load config file: {}", file.getFileName(), e);
            editor.setFileName(getFileName(file.getFileName()));
//            editor.setJsonMode(true);
            editor.setText("{}");
            editor.setEditable(false);
            showErrorPopup(Text.translatable("configeditor.error.loadfailed"));
        }
        
        updateButtonStates();
    }
    
    public static String getFileName(Path path) {
        if (path == null) {
            return "";
        }
        return path.getFileName().toString();
    }

    private String readFileWithFallbackEncoding(Path file) throws IOException {
        try {
            return Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            try {
                return Files.readString(file, java.nio.charset.Charset.defaultCharset());
            } catch (java.nio.charset.MalformedInputException e2) {
                return Files.readString(file, java.nio.charset.StandardCharsets.ISO_8859_1);
            }
        }
    }

    private void loadConfigFilesRecursively(Path directory, List<Path> fileList) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return;
        }
        
        try (var stream = Files.list(directory)) {
            List<Path> entries = stream.collect(Collectors.toList());
            
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    loadConfigFilesRecursively(entry, fileList);
                } else if (Files.isRegularFile(entry) && isConfigFile(entry)) {
                    fileList.add(entry);
                }
            }
        }
    }

    public boolean checkIfJson(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = content.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return false;
        }
        
        try {
            JsonParser.parseString(content);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    private void startSearch(String query) {
        editor.startSearch(query);
        searchNextButton.active = true;
        searchPrevButton.active = true;
    }

    private void findNext() {
        editor.findNext();
    }

    private void findPrevious() {
        editor.findPrevious();
    }

    private void endSearch() {
        editor.endSearch();
        searchNextButton.active = false;
        searchPrevButton.active = false;
    }

    private void saveFile() {
        saveFileAsync(null);
    }
    
    public void setEditorText(String text) {
        if (editor != null) {
            editor.setText(text);
            buffer = text;
            modified = true;
            updateButtonStates();
        }
    }
    
    private void saveFileAsync(Runnable callback) {
        if (configFiles.isEmpty()) return;
        
        Path file = configFiles.get(selectedIndex);
        String content = editor.getText();
        
//        if (editor.isJsonMode()) {
//            try {
//                JsonParser.parseString(content);
//            } catch (JsonSyntaxException e) {
//                LOGGER.warn("Invalid JSON syntax in file: {}", file.getFileName());
//                showErrorPopup(Text.translatable("configeditor.error.invalidjson"));
//                return;
//            }
//        }
        
        new Thread(() -> {
            int retryCount = 0;
            final int maxRetries = 3;
            
            while (retryCount <= maxRetries) {
                try {
                    try (FileChannel channel = FileChannel.open(file, 
                         StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                        FileLock lock = channel.tryLock(0, Long.MAX_VALUE, false);
                        if (lock != null) {
                            try {
                                Files.writeString(file, content, StandardOpenOption.TRUNCATE_EXISTING);
                                modified = false;
                                LOGGER.info("Successfully saved file: {}", file.getFileName());
                                
                                if (callback != null) {
                                    client.execute(callback);
                                } else {
                                    client.execute(this::updateButtonStates);
                                }
                                return;
                            } finally {
                                lock.release();
                            }
                        }
                    }
                    
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        Thread.sleep(300);
                    }
                    
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount > maxRetries) {
                        LOGGER.error("Failed to save file after {} attempts: {}", maxRetries, file.getFileName(), e);
                        client.execute(() -> {
                            if (this.client.currentScreen != null && this.client.currentScreen.equals(this)) {
                                showErrorPopup(Text.translatable("configeditor.error.savefailed"));
                            }
                        });
                    }
                }
            }
            
            client.execute(() -> {
                if (this.client.currentScreen != null && this.client.currentScreen.equals(this)) {
                    showErrorPopup(Text.translatable("configeditor.error.fileretryfailed"));
                }
            });
            
        }).start();
    }
    
    private void openConfigFolder() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            LOGGER.info("Config folder location: {}", configDir);
            Util.getOperatingSystem().open(configDir.toUri());
        } catch (Exception e) {
            LOGGER.error("Failed to get config folder", e);
        }
    }

    private void showErrorPopup(Text message) {
    	client.setScreen(new ConfirmScreen(
            result -> {
                this.close();
                this.client.setScreen(null);
            },
            Text.translatable("configeditor.confirm.title"),
            message
        ));
    }
    
    private void openVisualEditor() {
		this.client.setScreen(new JsonVisualEditorScreen(this.editor.getText(), this.editor.getFileName()));
	}

	private void openAiChat() {
		MinecraftClient.getInstance().setScreen(new AIChatScreen());
	}

	public void validateCurrentJson() {
		String content = editor.getText();
		try {
			JsonParser.parseString(content);
			showMessagePopup(Text.translatable("configeditor.message.jsonvalid"));
		} catch (JsonSyntaxException e) {
			showMessagePopup(Text.translatable("configeditor.message.jsoninvalid"));
		}
	}

    public void showMessagePopup(Text message) {
        client.setScreen(new ConfirmScreen(
            result -> {
                this.close();
                this.client.setScreen(null);
            },
            Text.translatable("configeditor.confirm.title"),
            message
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (ConfigEditorClient.configManager.getConfig().doRenderBackground) {
            context.fill(0, 0, this.width, this.height, themeManager.getBackgroundColor());
        }
        super.render(context, mouseX, mouseY, delta);
        
        if (!configFiles.isEmpty()) {
            String status = modified ? "* " + configFiles.get(selectedIndex).getFileName().toString() : 
                configFiles.get(selectedIndex).getFileName().toString();
            String editorType = "[File]";
            context.drawText(this.textRenderer, status + editorType, 170, 5, modified ? 0xFFFF00 : 0xFFFFFF, false);
        }
        
        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
            entrypoint.renderButton(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (modified) {
            ConfirmScreen confirmScreen = new ConfirmScreen(
                result -> {
                    if (result) {
                        saveFileAsync(() -> {this.close(); this.client.setScreen(null);});
                    } else {
                        this.close();
                        this.client.setScreen(null);
                    }
                },
                Text.translatable("configeditor.confirm.title"),
                Text.translatable("configeditor.confirm.unsavedclose")
            );
            this.client.setScreen(confirmScreen);
            return false;
        }
        return true;
    }
    
    public ClickableWidget getTextWidget() {
        return editor;
    }
    
    private void toggleSearch() {
        searchVisible = !searchVisible;
        searchField.setVisible(searchVisible);
        searchNextButton.visible = searchVisible;
        searchPrevButton.visible = searchVisible;
        
        if (searchVisible) {
            setFocused(searchField);
            String searchText = searchField.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
                startSearch(searchText);
            }
        } else {
            endSearch();
        }
    }

    @Override
    public void close() {
        super.close();
        
        configFiles = null;
        fileButtonList.clear();
        fileButtonList = null;
        
        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
            entrypoint.onEditerClose(this);
        }
        LOGGER.info("Config editor closed");
    }

    @Override
    public void removed() {
        super.removed();
        LOGGER.info("Config editor screen removed");
    }
    
    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == GLFW.GLFW_KEY_Q && input.hasCtrl() && input.hasAlt()) {
            this.client.setScreen(null);
            LOGGER.info("Config editor force closed by user shortcut");
            return true;
        }
        
        if (editor == null) return super.keyPressed(input);
        
        if (searchVisible) {
            if (input.getKeycode() == GLFW.GLFW_KEY_ENTER) {
                startSearch(searchField.getText());
                return true;
            }
            if (input.getKeycode() == GLFW.GLFW_KEY_ESCAPE) {
                toggleSearch();
                return true;
            }
            if (input.getKeycode() == GLFW.GLFW_KEY_F3) {
                findNext();
                return true;
            }
        }
        
        if (input.getKeycode() == GLFW.GLFW_KEY_F && input.hasCtrl()) {
            toggleSearch();
            return true;
        }
        
        return super.keyPressed(input);
    }
    
    private void toggleTheme() {
        ModConfigData config = ConfigEditorClient.configManager.getConfig();
        switch (config.theme) {
            case DARK -> config.theme = ModConfigData.ThemeMode.LIGHT;
            case LIGHT -> config.theme = ModConfigData.ThemeMode.AUTO;
            case AUTO -> config.theme = ModConfigData.ThemeMode.DARK;
        }
        ConfigEditorClient.configManager.save();
        themeToggleButton.setMessage(Text.translatable(getThemeButtonText()));
    }
    
    private String getThemeButtonText() {
        return switch (ConfigEditorClient.configManager.getConfig().theme) {
            case DARK -> "configeditor.theme.dark";
            case LIGHT -> "configeditor.theme.light";
            case AUTO -> "configeditor.theme.auto";
        };
    }
}
