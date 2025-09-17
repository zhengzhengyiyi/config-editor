package io.github.zhengzhengyiyi.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.github.zhengzhengyiyi.util.BackupHelper;
import io.github.zhengzhengyiyi.ConfigEditorClient;
import io.github.zhengzhengyiyi.gui.widget.UndoRedoEntrypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorScreen.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<Path> configFiles;
    private int selectedIndex = 0;
    private UndoRedoEntrypoint multilineEditor;
    private boolean modified = false;
    private ButtonWidget saveButton;
    private ButtonWidget openFolderButton;
    private ButtonWidget backupButton;
    private String buffer = "";

    public EditorScreen() {
        super(Text.translatable("zhengzhengyiyi.configeditor.title"));
    }

    @Override
    protected void init() {
        super.init();
        
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

        int buttonY = 20;
        for (int i = 0; i < configFiles.size(); i++) {
            int index = i;
            Path file = configFiles.get(i);
            String fileName = file.getFileName().toString();
            
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(fileName.length() > 20 ? fileName.substring(0, 17) + "..." : fileName),
                    button -> switchFile(index))
                    .dimensions(10, buttonY, 150, 20)
                    .build());
            buttonY += 25;
        }

        saveButton = ButtonWidget.builder(
                Text.translatable("zhengzhengyiyi.configeditor.save"),
                button -> saveFile())
                .dimensions(this.width - 170, this.height - 30, 80, 20)
                .build();
        this.addDrawableChild(saveButton);
        
        backupButton = ButtonWidget.builder(Text.translatable("zhengzhengyiyi.configeditor.backup"), button -> BackupHelper.backupEntireConfigDirectory())
        		.dimensions(0, 0, 70, 20)
        		.build();
        this.addDrawableChild(backupButton);

        openFolderButton = ButtonWidget.builder(
                Text.translatable("zhengzhengyiyi.configeditor.openfolder"),
                button -> openConfigFolder())
                .dimensions(this.width - 80, this.height - 30, 70, 20)
                .build();
        this.addDrawableChild(openFolderButton);

        multilineEditor = new UndoRedoEntrypoint(
                170, 20, 
                this.width - 180, this.height - 60,
                Text.translatable("zhengzhengyiyi.configeditor.editor"));
        multilineEditor.setChangedListener(text -> {
        	if (!buffer.equals(text)) {
	            modified = true;
	            updateButtonStates();
        	} else {
        		modified = false;
        	}
        });
        
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
        saveButton.active = modified && !configFiles.isEmpty();
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
                Text.translatable("zhengzhengyiyi.confirm.title"),
                Text.translatable("zhengzhengyiyi.confirm.unsaved")
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
            LOGGER.error("Failed to load config file: {}", file.getFileName(), e);
            multilineEditor.setText("{}");
            multilineEditor.setEditable(false);
            showErrorPopup(Text.translatable("zhengzhengyiyi.error.loadfailed"));
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
            showErrorPopup(Text.translatable("zhengzhengyiyi.error.invalidjson"));
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
                        showMessagePopup(Text.translatable("zhengzhengyiyi.message.saved"));
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Failed to save config file: {}", file.getFileName(), e);
                client.execute(() -> 
                    showErrorPopup(Text.translatable("zhengzhengyiyi.error.savefailed")));
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
            Text.translatable("zhengzhengyiyi.confirm.title"),
            Text.translatable("zhengzhengyiyi.error.title")
        ));
    }

    private void showMessagePopup(Text message) {
        client.setScreen(new ConfirmScreen(
        	result -> {
        		this.close();
        		this.client.setScreen(null);
        	},
            Text.translatable("zhengzhengyiyi.confirm.title"),
            Text.translatable("zhengzhengyiyi.message.title")
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        context.drawText(this.textRenderer, 
            Text.translatable("zhengzhengyiyi.configeditor.title"), 
            this.width / 2 - 50, 5, 0xFFFFFF, true);
        
        if (!configFiles.isEmpty()) {
            String status = modified ? "* " + configFiles.get(selectedIndex).getFileName().toString() : 
                configFiles.get(selectedIndex).getFileName().toString();
            context.drawText(this.textRenderer, status, 170, 5, modified ? 0xFF5555 : 0xFFFFFF, false);
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
                Text.translatable("zhengzhengyiyi.confirm.title"),
                Text.translatable("zhengzhengyiyi.confirm.unsavedclose")
            );
            this.client.setScreen(confirmScreen);
            return false;
        }
        return true;
    }
    
    public UndoRedoEntrypoint getTextWidget() {
        return this.multilineEditor;
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
}
