package io.github.zhengzhengyiyi.gui;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.zhengzhengyiyi.util.BackupHelper;
import io.github.zhengzhengyiyi.*;
import io.github.zhengzhengyiyi.config.ModConfigData;
import io.github.zhengzhengyiyi.gui.theme.ThemeManager;
import io.github.zhengzhengyiyi.gui.widget.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorScreen.class);
    private List<Path> configFiles;
    private int selectedIndex = 0;
    private MultilineEditor editor;
    private boolean modified = false;
    private Button saveButton;
    private Button openFolderButton;
    private Button backupButton;
    private String buffer = "";
    private EditBox searchField;
    private Button visualEditButton;
    private Button aiChatButton;
    private Button searchNextButton;
    private Button searchPrevButton;
    private Button managePluginsButton;
    private boolean searchVisible = false;
    private ThemeManager themeManager;
    private Button themeToggleButton;
    private int fileListScrollOffset = 0;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private List<Button> fileButtonList;
    private Minecraft client = Minecraft.getInstance();

    public EditorScreen() {
        super(Component.translatable("configeditor.title"));
    }

    @SuppressWarnings("null")
    @Override
    protected void init() {
        super.init();
        
        themeManager = ThemeManager.getInstance();
        themeToggleButton = Button.builder(
                Component.translatable(getThemeButtonText()),
                button -> toggleTheme())
                .bounds(this.width - 55, 5, 50, 20)
                .build();
        this.addRenderableWidget(themeToggleButton);
        
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            configFiles = new ArrayList<>();
            loadConfigFilesRecursively(configDir, configFiles);
        } catch (Exception e) {
            configFiles = new ArrayList<>();
            LOGGER.error("Failed to list config files", e);
        }

        fileButtonList = new ArrayList<>();
        
        scrollUpButton = Button.builder(Component.literal("↑"), button -> scrollUp())
                .bounds(145, 25, 20, 20)
                .build();
        scrollDownButton = Button.builder(Component.literal("↓"), button -> scrollDown())
                .bounds(145, this.height - 45, 20, 20)
                .build();
        
        managePluginsButton = Button.builder(
                Component.translatable("configeditor.button.plugins"),
                button -> client.setScreen(new PluginManagerScreen(this)))
                .bounds(this.width - 40, this.height - 30, 40, 20)
                .build();
        
        this.addRenderableWidget(scrollUpButton);
        this.addRenderableWidget(scrollDownButton);
        this.addRenderableWidget(managePluginsButton);
        
        renderFileList();
        
        editor = new MultilineEditor(
                170, 20,
                this.width - 180, this.height - 55,
                Component.translatable("configeditor.editor"));
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
        
        backupButton = Button.builder(
                Component.translatable("configeditor.button.backup"), 
                button -> BackupHelper.backupEntireConfigDirectory())
                .bounds(centerX - 200, bottomY, 80, 20)
                .build();

        saveButton = Button.builder(
                Component.translatable("configeditor.button.save"),
                button -> saveFile())
                .bounds(centerX - 110, bottomY, 80, 20)
                .build();
        
        visualEditButton = Button.builder(
                Component.translatable("configeditor.button.visual"),
                button -> openVisualEditor())
                .bounds(centerX + 10, bottomY, 100, 20)
                .build();
//        visualEditButton.active = false;
        
        openFolderButton = Button.builder(
                Component.translatable("configeditor.button.openfolder"),
                button -> openConfigFolder())
                .bounds(0, 0, 80, 20)
                .build();
        
        int searchX = this.width - 320;
        searchField = new EditBox(
            font, 
            searchX, 
            5, 
            150, 
            20, 
            Component.translatable("configeditor.search.placeholder")
        );
        searchField.setResponder(text -> {
            if (!text.trim().isEmpty()) {
                startSearch(text.trim());
            } else {
                endSearch();
            }
        });
        searchField.setVisible(true);

        searchPrevButton = Button.builder(
            Component.translatable("configeditor.search.prev"), 
            button -> findPrevious())
            .bounds(searchX + 155, 5, 40, 20)
            .build();
        searchPrevButton.active = false;
        searchPrevButton.visible = true;
        
        searchNextButton = Button.builder(
            Component.translatable("configeditor.search.next"), 
            button -> findNext())
            .bounds(searchX + 195, 5, 40, 20)
            .build();
        searchNextButton.active = false;
        searchNextButton.visible = true;
        
        Button closeSearchButton = Button.builder(
            Component.literal("×"), 
            button -> {
                searchField.setValue("");
                endSearch();
            })
            .bounds(searchX + 235, 5, 20, 20)
            .build();
        closeSearchButton.visible = true;
        
        int quickButtonX = this.width - 100;
        int quickButtonY = this.height - 20;
        int quickButtonWidth = 55;
        int quickButtonHeight = 20;
        
        aiChatButton = Button.builder(
            Component.translatable("configeditor.button.aichat"),
            button -> openAiChat())
            .bounds(quickButtonX, quickButtonY, quickButtonWidth, quickButtonHeight)
            .build();
        
        this.addRenderableWidget(backupButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(visualEditButton);
        this.addRenderableWidget(openFolderButton);
        
        this.addRenderableWidget(searchField);
        this.addRenderableWidget(searchPrevButton);
        this.addRenderableWidget(searchNextButton);
        this.addRenderableWidget(closeSearchButton);
        
        this.addRenderableWidget(aiChatButton);
        
        this.addRenderableWidget(editor);
        
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

    @SuppressWarnings("null")
    private void renderFileList() {
        for (Button button : fileButtonList) {
            this.removeWidget(button);
        }
        fileButtonList.clear();
        
        int buttonY = 25;
        for (int i = fileListScrollOffset; i < configFiles.size() && i < fileListScrollOffset + 15; i++) {
            int index = i;
            Path file = configFiles.get(i);
            Path configDir = FabricLoader.getInstance().getConfigDir();
            String relativePath = configDir.relativize(file).toString();
            
            Button button = Button.builder(
                    Component.literal(formatFileName(relativePath)),
                    _button -> switchFile(index))
                    .bounds(10, buttonY, 130, 20)
                    .build();
            this.addRenderableWidget(button);
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
            showErrorPopup(Component.translatable("configeditor.error.loadfailed"));
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
//                showErrorPopup(Component.translatable("configeditor.error.invalidjson"));
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
                            if (this.minecraft.screen != null && this.minecraft.screen.equals(this)) {
                                showErrorPopup(Component.translatable("configeditor.error.savefailed"));
                            }
                        });
                    }
                }
            }

            client.execute(() -> {
                if (this.minecraft.screen != null && this.minecraft.screen.equals(this)) {
                    showErrorPopup(Component.translatable("configeditor.error.fileretryfailed"));
                }
            });
            
        }).start();
    }
    
    @SuppressWarnings("null")
    private void openConfigFolder() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            LOGGER.info("Config folder location: {}", configDir);
            Util.getPlatform().openUri(configDir.toUri());
        } catch (Exception e) {
            LOGGER.error("Failed to get config folder", e);
        }
    }

    @SuppressWarnings("null")
    private void showErrorPopup(Component message) {
    	client.setScreen(new ConfirmScreen(
            result -> {
                this.onClose();
                this.minecraft.setScreen(null);
            },
            Component.translatable("configeditor.confirm.title"),
            message
        ));
    }
    
    private void openVisualEditor() {
		this.minecraft.setScreen(new JsonVisualEditorScreen(this.editor.getText(), this.editor.getFileName()));
	}

	private void openAiChat() {
		Minecraft.getInstance().setScreen(new AIChatScreen());
	}

	public void validateCurrentJson() {
		String content = editor.getText();
		try {
			JsonParser.parseString(content);
			showMessagePopup(Component.translatable("configeditor.message.jsonvalid"));
		} catch (JsonSyntaxException e) {
			showMessagePopup(Component.translatable("configeditor.message.jsoninvalid"));
		}
	}

    @SuppressWarnings("null")
    public void showMessagePopup(Component message) {
        client.setScreen(new ConfirmScreen(
            result -> {
                this.onClose();
                this.minecraft.setScreen(null);
            },
            Component.translatable("configeditor.confirm.title"),
            message
        ));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (ConfigEditorClient.configManager.getConfig().doRenderBackground) {
            context.fill(0, 0, this.width, this.height, themeManager.getBackgroundColor());
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        
        if (!configFiles.isEmpty()) {
            String status = modified ? "* " + configFiles.get(selectedIndex).getFileName().toString() : 
                configFiles.get(selectedIndex).getFileName().toString();
            String editorType = "[File]";
            context.text(this.font, status + editorType, 170, 5, modified ? 0xFFFF00 : 0xFFFFFF, false);
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
                        saveFileAsync(() -> {this.onClose(); this.minecraft.setScreen(null);});
                    } else {
                        this.onClose();
                        this.minecraft.setScreen(null);
                    }
                },
                Component.translatable("configeditor.confirm.title"),
                Component.translatable("configeditor.confirm.unsavedclose")
            );
            this.minecraft.setScreen(confirmScreen);
            return false;
        }
        return true;
    }
    
    public AbstractWidget getTextWidget() {
        return editor;
    }
    
    private void toggleSearch() {
        searchVisible = !searchVisible;
        searchField.setVisible(searchVisible);
        searchNextButton.visible = searchVisible;
        searchPrevButton.visible = searchVisible;
        
        if (searchVisible) {
            setFocused(searchField);
            String searchText = searchField.getValue();
            if (searchText != null && !searchText.trim().isEmpty()) {
                startSearch(searchText);
            }
        } else {
            endSearch();
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        
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
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_Q && input.hasControlDown() && input.hasAltDown()) {
            this.minecraft.setScreen(null);
            LOGGER.info("Config editor force closed by user shortcut");
            return true;
        }

        // Pass Ctrl+A/C/V through to the focused widget (editor or search field)
        // before any screen-level handling so they always work
        if (input.hasControlDown()) {
            int k = input.key();
            if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_C || k == GLFW.GLFW_KEY_V) {
                // Let the focused child handle it first
                var focused = getFocused();
                if (focused != null && focused.keyPressed(input)) {
                    return true;
                }
            }
        }
        
        if (editor == null) return super.keyPressed(input);
        
        if (searchVisible) {
            if (input.key() == GLFW.GLFW_KEY_ENTER) {
                startSearch(searchField.getValue());
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                toggleSearch();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_F3) {
                findNext();
                return true;
            }
        }
        
        if (input.key() == GLFW.GLFW_KEY_F && input.hasControlDown()) {
            toggleSearch();
            return true;
        }
        
        return super.keyPressed(input);
    }
    
    @SuppressWarnings("null")
    private void toggleTheme() {
        ModConfigData config = ConfigEditorClient.configManager.getConfig();
        switch (config.theme) {
            case DARK -> config.theme = ModConfigData.ThemeMode.LIGHT;
            case LIGHT -> config.theme = ModConfigData.ThemeMode.AUTO;
            case AUTO -> config.theme = ModConfigData.ThemeMode.DARK;
        }
        ConfigEditorClient.configManager.save();
        themeManager.invalidate();
        themeToggleButton.setMessage(Component.translatable(getThemeButtonText()));
    }
    
    private String getThemeButtonText() {
        return switch (ConfigEditorClient.configManager.getConfig().theme) {
            case DARK -> "configeditor.theme.dark";
            case LIGHT -> "configeditor.theme.light";
            case AUTO -> "configeditor.theme.auto";
        };
    }
}
