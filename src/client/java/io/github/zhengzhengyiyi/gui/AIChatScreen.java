package io.github.zhengzhengyiyi.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.zhengzhengyiyi.api.*;

public class AIChatScreen extends Screen {
    private TextFieldWidget inputField;
    private ButtonWidget sendButton;
    private ButtonWidget clearButton;
    private ButtonWidget settingsButton;
    private List<String> chatHistory;
    private List<String> userInputs;
    private boolean waitingForAIResponse = false;
    private boolean serverAvailable = false;
    private boolean checkingServer = true;
    private int chatScrollOffset = 0;
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    
    private static Logger LOGGER = LoggerFactory.getLogger(AIChatScreen.class);
    
    private AiClient aiClient = new AiClient();

    public AIChatScreen() {
        super(Text.literal("AI Chat"));
        this.chatHistory = new ArrayList<>();
        this.userInputs = new ArrayList<>();
        
        chatHistory.add("AI: Hello! How can I help you today?");
        chatHistory.add("AI: I'm ready to assist you with anything!");
    }

    @Override
    protected void init() {
        super.init();
        
        checkServerAvailability();
        
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;
        
        int inputY = panelY + panelHeight + 10;
        int inputWidth = panelWidth - 100;
        
        this.inputField = new TextFieldWidget(this.textRenderer, panelX, inputY, inputWidth, 20, Text.literal("Type your message..."));
        this.inputField.setMaxLength(256);
        this.inputField.setEditable(false);
        this.addDrawableChild(this.inputField);
        
        this.sendButton = ButtonWidget.builder(Text.literal("Send"), button -> sendMessage())
            .dimensions(panelX + inputWidth + 5, inputY, 45, 20)
            .build();
        this.sendButton.active = false;
        this.addDrawableChild(this.sendButton);
        
        this.clearButton = ButtonWidget.builder(Text.literal("Clear"), button -> clearChat())
            .dimensions(panelX + panelWidth - 90, panelY - 25, 45, 20)
            .build();
        this.addDrawableChild(this.clearButton);
        
        this.settingsButton = ButtonWidget.builder(Text.literal("Settings"), button -> openSettings())
            .dimensions(panelX + panelWidth - 45, panelY - 25, 45, 20)
            .build();
        this.addDrawableChild(this.settingsButton);
        
        this.scrollUpButton = ButtonWidget.builder(Text.literal("↑"), button -> scrollUp())
            .dimensions(panelX + panelWidth - 20, panelY + 25, 16, 16)
            .build();
        this.addDrawableChild(this.scrollUpButton);
        
        this.scrollDownButton = ButtonWidget.builder(Text.literal("↓"), button -> scrollDown())
            .dimensions(panelX + panelWidth - 20, panelY + panelHeight - 20, 16, 16)
            .build();
        this.addDrawableChild(this.scrollDownButton);
        
        this.setInitialFocus(this.inputField);
        updateScrollButtons();
    }

    private void checkServerAvailability() {
        checkingServer = true;
        aiClient.checkServerStatus().thenAccept(available -> {
            serverAvailable = available;
            checkingServer = false;
            if (!serverAvailable) {
                chatHistory.add("System: Ollama server is not available. Please make sure Ollama is running on localhost:11434");
            }
            updateUIState();
        }).exceptionally(throwable -> {
            serverAvailable = false;
            checkingServer = false;
            chatHistory.add("System: Failed to check server status: " + throwable.getMessage());
            updateUIState();
            return null;
        });
    }

    private void updateUIState() {
        if (this.inputField != null) {
            this.inputField.setEditable(serverAvailable && !waitingForAIResponse);
        }
        if (this.sendButton != null) {
            this.sendButton.active = serverAvailable && !waitingForAIResponse && 
                (this.inputField != null && !this.inputField.getText().trim().isEmpty());
        }
        updateScrollButtons();
    }

    private void updateScrollButtons() {
        if (scrollUpButton != null && scrollDownButton != null) {
            scrollUpButton.active = chatScrollOffset > 0;
            scrollDownButton.active = chatScrollOffset < getMaxScrollOffset();
        }
    }

    private int getMaxScrollOffset() {
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int maxVisibleLines = (panelHeight - 50) / 10;
        int totalLines = getTotalChatLines(panelWidth - 30);
        return Math.max(0, totalLines - maxVisibleLines);
    }

    private int getTotalChatLines(int maxWidth) {
        int totalLines = 0;
        for (String message : chatHistory) {
            List<String> wrappedLines = wrapText(message, maxWidth);
            totalLines += wrappedLines.size();
        }
        if (checkingServer || waitingForAIResponse) {
            totalLines += 1;
        }
        return totalLines;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine.append(currentLine.length() > 0 ? " " + word : word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                    currentLine = new StringBuilder();
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }

    private void scrollUp() {
        if (chatScrollOffset > 0) {
            chatScrollOffset--;
            updateScrollButtons();
        }
    }

    private void scrollDown() {
        if (chatScrollOffset < getMaxScrollOffset()) {
            chatScrollOffset++;
            updateScrollButtons();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;
        
        if (mouseX >= panelX && mouseX <= panelX + panelWidth && 
            mouseY >= panelY && mouseY <= panelY + panelHeight) {
            if (verticalAmount < 0) {
                scrollDown();
            } else if (verticalAmount > 0) {
                scrollUp();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//    	super.renderBackground(context, mouseY, mouseY, delta);
    	
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;
        
        context.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, 0xFF333333);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1a1a1a);
        
        String title = "AI Chat Assistant";
        if (checkingServer) {
            title += " (Checking server...)";
        } else if (!serverAvailable) {
            title += " (Server Offline)";
        }
        context.drawText(this.textRenderer, Text.literal(title), this.width / 2 - 70, panelY - 15, 0xFFFFFF, false);
        
        context.drawHorizontalLine(panelX + 5, panelX + panelWidth - 5, panelY + 20, 0xFF666666);
        
        int chatY = panelY + 35;
        int maxVisibleLines = (panelHeight - 50) / 12;
        int textAreaWidth = panelWidth - 30;
        
//        context.enableScissor(panelX + 5, panelY + 25, panelX + panelWidth - 5, panelY + panelHeight - 5);
//        
//        int currentLine = 0;
//        int visibleLineCount = 0;
//        
//        for (String message : chatHistory) {
//            List<String> wrappedLines = wrapText(message, textAreaWidth);
//            for (String line : wrappedLines) {
//                if (currentLine >= chatScrollOffset && visibleLineCount < maxVisibleLines) {
//                    int renderY = chatY + visibleLineCount * 12;
//                    int color = getMessageColor(message);
//                    context.drawText(this.textRenderer, Text.literal(line), panelX + 10, renderY, color, false);
//                    visibleLineCount++;
//                }
//                currentLine++;
//            }
//        }
//        
//        if ((checkingServer || waitingForAIResponse) && visibleLineCount < maxVisibleLines) {
//            int renderY = chatY + visibleLineCount * 12;
//            String loadingText = checkingServer ? 
//                "Checking server status" + ".".repeat((int)(System.currentTimeMillis() / 500 % 4)) :
//                "AI is thinking" + ".".repeat((int)(System.currentTimeMillis() / 500 % 4));
//            context.drawText(this.textRenderer, Text.literal(loadingText), panelX + 10, renderY, 0xFFFF00, false);
//        }
//        
//        context.disableScissor();
        
        drawScrollBar(context, panelX, panelY, panelWidth, panelHeight);
        
//        int inputY = panelY + panelHeight + 10;
        
//        context.fill(panelX - 1, inputY - 1, panelX + panelWidth - 99, inputY + 21, 0xFF555555);
//        context.fill(panelX, inputY, panelX + panelWidth - 100, inputY + 20, 0xFF2d2d2d);
        
//        if (!serverAvailable && this.inputField.getText().isEmpty()) {
//            context.drawText(this.textRenderer, Text.literal("Server unavailable - Read only mode"), panelX + 8, inputY + 6, 0xFF5555, false);
//        } else if (this.inputField.getText().isEmpty()) {
//            context.drawText(this.textRenderer, Text.literal("Type your message here..."), panelX + 8, inputY + 6, 0x888888, false);
//        }
        
        updateUIState();
        
        super.render(context, mouseX, mouseY, delta);
        
        context.enableScissor(panelX + 5, panelY + 25, panelX + panelWidth - 5, panelY + panelHeight - 5);
        
        int currentLine = 0;
        int visibleLineCount = 0;
        
        for (String message : chatHistory) {
            List<String> wrappedLines = wrapText(message, textAreaWidth);
            for (String line : wrappedLines) {
                if (currentLine >= chatScrollOffset && visibleLineCount < maxVisibleLines) {
                    int renderY = chatY + visibleLineCount * 12;
                    int color = getMessageColor(message);
                    context.drawText(this.textRenderer, line, panelX + 10, renderY, color, false);
//                    context.drawText(this.textRenderer, "test", 40, 40 - this.textRenderer.fontHeight - 10, color, true);
                    visibleLineCount++;
                }
                currentLine++;
            }
        }
        
        if ((checkingServer || waitingForAIResponse) && visibleLineCount < maxVisibleLines) {
            int renderY = chatY + visibleLineCount * 12;
            String loadingText = checkingServer ? 
                "Checking server status" + ".".repeat((int)(System.currentTimeMillis() / 500 % 4)) :
                "AI is thinking" + ".".repeat((int)(System.currentTimeMillis() / 500 % 4));
            context.drawText(this.textRenderer, loadingText, panelX + 10, renderY, 0xFFFFFFFF, false);
        }
        
        context.disableScissor();
    }

    private int getMessageColor(String message) {
//        if (message.startsWith("AI:")) {
//            return 0x55FF55;
//        } else if (message.startsWith("You:")) {
//            return 0xAAAAFF;
//        } else if (message.startsWith("System:")) {
//            return 0xFFFF55;
//        } else {
//            return 0xFFFFFF;
//        }
    	return 0xFFFFFFFF;
    }

    private void drawScrollBar(DrawContext context, int panelX, int panelY, int panelWidth, int panelHeight) {
        int scrollBarX = panelX + panelWidth - 18;
        int scrollBarY = panelY + 25;
        int scrollBarHeight = panelHeight - 30;
        
        context.fill(scrollBarX, scrollBarY, scrollBarX + 12, scrollBarY + scrollBarHeight, 0xFF444444);
        
        int maxScrollOffset = getMaxScrollOffset();
        if (maxScrollOffset > 0) {
            int thumbHeight = Math.max(20, (int)(scrollBarHeight * (1.0 / (maxScrollOffset + 1))));
            int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * (chatScrollOffset / (double)maxScrollOffset));
            context.fill(scrollBarX, thumbY, scrollBarX + 12, thumbY + thumbHeight, 0xFF888888);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
    	int keyCode = input.getKeycode();
    	
        if (keyCode == 256) {
            this.close();
            return true;
        }
        
        if (keyCode == 257 && this.inputField.isFocused() && !waitingForAIResponse && serverAvailable) {
            this.sendMessage();
            return true;
        }
        
        return super.keyPressed(input);
    }

    private void sendMessage() {
        String message = this.inputField.getText().trim();
        if (!message.isEmpty() && !waitingForAIResponse && serverAvailable) {
            userInputs.add(message);
            chatHistory.add("You: " + message);
            inputField.setText("");
            waitingForAIResponse = true;
            chatScrollOffset = getMaxScrollOffset();
            updateUIState();
            
            input(message);
        }
    }

    private void clearChat() {
        chatHistory.clear();
        userInputs.clear();
        chatScrollOffset = 0;
        if (serverAvailable) {
            chatHistory.add("AI: Chat cleared. How can I help you?");
        } else {
            chatHistory.add("System: Chat cleared. Server is still unavailable.");
        }
        updateUIState();
    }

    private void openSettings() {
        MinecraftClient.getInstance().setScreen(new EditorScreen());
    }

    private void input(String message) {
        aiClient.sendChatRequest("tinyllama:latest", message)
            .thenAccept(aiResponse -> {
                if (!aiResponse.isEmpty()) {
                    chatHistory.add("AI: " + aiResponse);
                    chatScrollOffset = getMaxScrollOffset();
                    LOGGER.info(aiResponse);
                }
                waitingForAIResponse = false;
                updateUIState();
            })
            .exceptionally(throwable -> {
                chatHistory.add("System: Error getting AI response: " + throwable.getMessage());
                chatScrollOffset = getMaxScrollOffset();
                waitingForAIResponse = false;
                updateUIState();
                return null;
            });
    }

    @Override
    public void close() {
        super.close();
    }
}
