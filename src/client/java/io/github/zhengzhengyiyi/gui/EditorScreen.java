package io.github.zhengzhengyiyi.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.github.zhengzhengyiyi.util.BackupHelper;
import io.github.zhengzhengyiyi.*;
import io.github.zhengzhengyiyi.config.ConfigData;
import io.github.zhengzhengyiyi.config.ConfigManager;
import io.github.zhengzhengyiyi.gui.theme.ThemeManager;
import io.github.zhengzhengyiyi.gui.widget.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.IOException;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorScreen.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<Path> configFiles;
    private int selectedIndex = 0;
    private MultilineEditor multilineEditor;
    private boolean modified = false;
    private ButtonWidget saveButton;
    private ButtonWidget openFolderButton;
    private ButtonWidget backupButton;
    private String buffer = "";
    private TextFieldWidget searchField;
    private ButtonWidget searchNextButton;
    private ButtonWidget searchPrevButton;
    private boolean searchVisible = false;
    private ThemeManager themeManager;
    private ButtonWidget themeToggleButton;

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
                .dimensions(this.width - 350, 5, 45, 16)
                .build();
        this.addDrawableChild(themeToggleButton);
        
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            configFiles = Files.list(configDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());
            LOGGER.info("Found {} config files", configFiles.size());
        } catch (Exception e) {
            configFiles = new ArrayList<>();
            LOGGER.error("Failed to list config files", e);
        }

        int buttonY = 25;
        for (int i = 0; i < configFiles.size(); i++) {
            int index = i;
            Path file = configFiles.get(i);
            String fileName = file.getFileName().toString();
            
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(fileName.length() > 20 ? fileName.substring(0, 17) + "..." : fileName),
                    button -> switchFile(index))
                    .dimensions(10, buttonY, 130, 20)
                    .build());
            buttonY += 23;
        }
        
        multilineEditor = new MultilineEditor(
                170, 20, 
                this.width - 180, this.height - 60,
                Text.translatable("configeditor.editor"));
        multilineEditor.setChangedListener(text -> {
        	if (!buffer.equals(text)) {
	            modified = true;
	            updateButtonStates();
        	} else {
        		modified = false;
        	}
        });

        saveButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.save"),
                button -> saveFile())
                .dimensions(this.width - 170, this.height - 30, 80, 20)
                .build();
        this.addDrawableChild(saveButton);
        
        backupButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.backup"), 
                button -> BackupHelper.backupEntireConfigDirectory())
        		.dimensions(0, 0, 70, 20)
        		.build();
        this.addDrawableChild(backupButton);

        openFolderButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.openfolder"),
                button -> openConfigFolder())
                .dimensions(this.width - 80, this.height - 30, 70, 20)
                .build();
        this.addDrawableChild(openFolderButton);
        
        ButtonWidget exitButton = ButtonWidget.builder(
                Text.translatable("configeditor.button.close"),
                button -> this.close())
                .dimensions(0, this.height - 25, 80, 20)
                .build();
        this.addDrawableChild(exitButton);
        
        searchField = new TextFieldWidget(textRenderer, this.width - 250, 5, 120, 16, Text.translatable("configeditor.search.placeholder"));
        searchField.setVisible(false);
        addDrawableChild(searchField);
        
        searchNextButton = ButtonWidget.builder(Text.translatable("configeditor.search.next"), button -> {
        	if (multilineEditor != null) {
        		multilineEditor.findNext();
        	}
        }).dimensions(this.width - 125, 5, 50, 16).build();
        searchNextButton.visible = false;
        addDrawableChild(searchNextButton);
        
        searchPrevButton = ButtonWidget.builder(Text.translatable("configeditor.search.prev"), button -> {
        	if (multilineEditor != null) {
        		multilineEditor.findPrevious();
        	}
        }).dimensions(this.width - 70, 5, 50, 16).build();
        searchPrevButton.visible = false;
        addDrawableChild(searchPrevButton);
        
        addDrawableChild(ButtonWidget.builder(Text.translatable("configeditor.button.search"), button -> {
            toggleSearch();
        }).dimensions(this.width - 300, 5, 45, 16).build());
        
        this.addDrawableChild(multilineEditor);
        this.setInitialFocus(multilineEditor);

        if (!configFiles.isEmpty()) {
            loadFile(selectedIndex);
        } else {
            multilineEditor.setText("{}");
            multilineEditor.setEditable(false);
            LOGGER.warn("No config files found in config directory");
        }
        
        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
            entrypoint.onEditerOpen(this);
        }
        
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (saveButton != null) {
            saveButton.active = modified && !configFiles.isEmpty();
        }
    }

    private void switchFile(int index) {
        if (modified) {
            ConfirmScreen confirmScreen = new ConfirmScreen(
                result -> {
                    if (result) {
                        saveFileAsync(() -> loadFile(index));
                        this.client.setScreen(null);
                        this.client.setScreen(this);
                    } else {
                        loadFile(index);
                        this.client.setScreen(null);
                        this.client.setScreen(this);
                    }
                },
                Text.translatable("configeditor.confirm.title"),
                Text.translatable("configeditor.confirm.unsaved")
            );
            this.client.setScreen(confirmScreen);
        } else {
            loadFile(index);
        }
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
            String content = Files.readString(file);
            buffer = content;
            JsonElement json = JsonParser.parseString(content);
            String formattedContent = GSON.toJson(json);
            multilineEditor.setText(formattedContent);
            LOGGER.info("Successfully loaded config file: {}", file.getFileName());
        } catch (Exception e) {
        	String text = null;
        	try {
        		text = Files.readString(file);
        	} catch (IOException ioexception) {
        		LOGGER.error("tried to read file except IOException: ", ioexception.toString());
        	}
        	if (text == null) {
        		LOGGER.error("Failed to load config file: {}", file.getFileName(), e);
	            multilineEditor.setText("{}");
	            multilineEditor.setEditable(false);
	            showErrorPopup(Text.translatable("configeditor.error.loadfailed"));
        	}
        	multilineEditor.setText(text);
        }
        
        updateButtonStates();
    }

    private void saveFile() {
        saveFileAsync(null);
    }

    private void saveFileAsync(Runnable callback) {
        if (configFiles.isEmpty()) return;
        
        Path file = configFiles.get(selectedIndex);
        String content = multilineEditor.getText();
        
        try {
            JsonParser.parseString(content);
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Invalid JSON syntax in file: {}", file.getFileName());
            showErrorPopup(Text.translatable("configeditor.error.invalidjson"));
            return;
        }
        
        new Thread(() -> {
            try {
                Files.writeString(file, content);
                modified = false;
                LOGGER.info("Successfully saved config file: {}", file.getFileName());
                
                if (callback != null) {
                    client.execute(callback);
                } else {
                    client.execute(() -> {
                        updateButtonStates();
//                        showMessagePopup(Text.translatable("configeditor.message.saved"));
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Failed to save config file: {}", file.getFileName(), e);
                client.execute(() -> 
                    showErrorPopup(Text.translatable("configeditor.error.savefailed")));
            }
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
           		this.client.setScreen(this);
           	},
            Text.translatable("configeditor.confirm.title"),
            message
        ));
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
//        super.render(context, mouseX, mouseY, delta);
    	if (ConfigManager.getConfig().doRenderBackground) context.fill(0, 0, this.width, this.height, themeManager.getBackgroundColor());
        super.render(context, mouseX, mouseY, delta);
        
        if (!configFiles.isEmpty()) {
            String status = modified ? "* " + configFiles.get(selectedIndex).getFileName().toString() : 
                configFiles.get(selectedIndex).getFileName().toString();
            context.drawText(this.textRenderer, status, 170, 5, modified ? 0xFFFF00 : 0xFFFFFF, false);
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
    
    public MultilineEditor getTextWidget() {
        return this.multilineEditor;
    }
    
    private void toggleSearch() {
        if (multilineEditor == null) return;
        
        searchVisible = !searchVisible;
        searchField.setVisible(searchVisible);
        searchNextButton.visible = searchVisible;
        searchPrevButton.visible = searchVisible;
        
        if (searchVisible) {
            setFocused(searchField);
            String searchText = searchField.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
            	multilineEditor.startSearch(searchText);
            }
        } else {
        	multilineEditor.endSearch();
        }
    }

    @Override
    public void close() {
        super.close();
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (multilineEditor == null) return super.keyPressed(keyCode, scanCode, modifiers);
        
        if (searchVisible) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
            	multilineEditor.startSearch(searchField.getText());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                toggleSearch();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_F3) {
            	multilineEditor.findNext();
                return true;
            }
        }
        
        if (keyCode == GLFW.GLFW_KEY_F && hasControlDown()) {
            toggleSearch();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void toggleTheme() {
        ConfigData config = ConfigManager.getConfig();
        switch (config.theme) {
            case DARK -> config.theme = ConfigData.ThemeMode.LIGHT;
            case LIGHT -> config.theme = ConfigData.ThemeMode.AUTO;
            case AUTO -> config.theme = ConfigData.ThemeMode.DARK;
        }
        ConfigManager.save();
        themeToggleButton.setMessage(Text.translatable(getThemeButtonText()));
    }
    
    private String getThemeButtonText() {
        return switch (ConfigManager.getConfig().theme) {
            case DARK -> "configeditor.theme.dark";
            case LIGHT -> "configeditor.theme.light";
            case AUTO -> "configeditor.theme.auto";
        };
    }
}