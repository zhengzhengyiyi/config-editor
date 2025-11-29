package io.github.zhengzhengyiyi.gui.widget;

import io.github.zhengzhengyiyi.api.FileType;
import io.github.zhengzhengyiyi.util.TextSearchEngine;
import io.github.zhengzhengyiyi.util.highlighter.HighLighter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class GeneralMultilineEditor extends AbstractEditor {
    private final TextRenderer textRenderer;
    private boolean isDraggingHorizontalScroll = false;
    private int dragStartX = 0;
    private int dragStartScrollOffset = 0;
    public String text = "";
    private int scrollOffset = 0;
    private int horizontalScrollOffset = 0;
    public static int maxVisibleLines = 10;
    private boolean editable = true;
    private Consumer<String> changedListener;
    private int cursorPosition = 0;
    private long lastCursorBlinkTime = 0;
    private boolean cursorVisible = true;
    private String filename = "";
    private int maxLineWidth = 0;
    public int lastCursorX = 0;
    
    private TextSearchEngine searchEngine = new TextSearchEngine();
    private boolean isSearching = false;
    public String searchQuery = "";
    private FileType fileType = FileType.TXT;

    public GeneralMultilineEditor(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.setFocused(false);
    }
    
    public FileType getFileTypeFromName(String fileName) {
        String lowerName = fileName.toLowerCase();
        
        for (FileType type : FileType.values()) {
            String extension = "." + type.getExtension();
            if (lowerName.endsWith(extension)) {
                return type;
            }
        }
        return FileType.UNKNOW;
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        context.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
        
        try {
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);

            String[] lines = this.text.split("\n", -1);
            int lineHeight = this.textRenderer.fontHeight + 2;
            int maxVisibleLines = this.height / lineHeight;

            calculateMaxLineWidth(lines);

            if (isSearching && !searchQuery.isEmpty()) {
                renderSearchHighlights(context, lines, lineHeight, maxVisibleLines);
            }

            HighLighter highlighter = fileType.getHighLighter();
//            HighLighter highlighter = new PropertiesSyntaxHighlighter();
            		
            
            for (int i = 0; i < lines.length; i++) {
                if (i >= this.scrollOffset && i < this.scrollOffset + maxVisibleLines) {
                    int yPos = this.getY() + 4 + (i - this.scrollOffset) * lineHeight;
                    String lineNum = String.valueOf(i + 1);
                    context.drawText(textRenderer, lineNum, this.getX() + 2 - horizontalScrollOffset, yPos, 0xFF888888, false);
                    
                    highlighter.drawHighlightedText(context, this.textRenderer, lines[i], this.getX() + 4 + 12 - horizontalScrollOffset, yPos, this.editable);
                }
            }

            if (this.isFocused() && this.editable) {
                long currentTime = Util.getMeasuringTimeNano();
                if (currentTime - lastCursorBlinkTime > 500000000) {
                    cursorVisible = !cursorVisible;
                    lastCursorBlinkTime = currentTime;
                }
                if (cursorVisible) {
                    int lineIndex = 0;
                    int xPos = this.getX() + 4 + 12;
                    int remaining = this.cursorPosition;
                    for (int i = 0; i < lines.length; i++) {
                        if (remaining <= lines[i].length()) {
                            xPos += highlighter.getTextWidthUpToChar(this.textRenderer, lines[i], remaining);
                            lineIndex = i;
                            break;
                        }
                        remaining -= (lines[i].length() + 1);
                    }
                    
                    if (lineIndex >= this.scrollOffset && lineIndex < this.scrollOffset + maxVisibleLines) {
                        int yPos = this.getY() + 4 + (lineIndex - this.scrollOffset) * lineHeight;
                        context.drawVerticalLine(xPos - horizontalScrollOffset, yPos - 1, yPos + this.textRenderer.fontHeight + 1, 0xFFFFFFFF);
                    }
                }
            }

            renderScrollBars(context, lines.length, maxVisibleLines);
        } finally {
            context.disableScissor();
        }
    }

    private void calculateMaxLineWidth(String[] lines) {
        maxLineWidth = 0;
        for (String line : lines) {
            int lineWidth = fileType.getHighLighter().getTextWidth(textRenderer, line);
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
        }
    }

    private void renderScrollBars(DrawContext context, int totalLines, int maxVisibleLines) {
        int scrollbarWidth = 5;
        
        if (totalLines > maxVisibleLines) {
            int scrollbarHeight = Math.max(20, (int)((float)this.height * (float)maxVisibleLines / (float)totalLines));
            int scrollbarY = this.getY() + (int)((float)(this.height - scrollbarHeight) * (float)this.scrollOffset / (float)(totalLines - maxVisibleLines));
            context.fill(this.getX() + this.width - scrollbarWidth, this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF555555);
            context.fill(this.getX() + this.width - scrollbarWidth + 1, scrollbarY, this.getX() + this.width - 1, scrollbarY + scrollbarHeight, 0xFFBBBBBB);
        }
        
        if (maxLineWidth > this.width - 20) {
            int visibleWidth = this.width - 20;
            int scrollbarHeight = 5;
            int scrollbarX = this.getX() + (int)((float)(this.width - scrollbarWidth) * (float)this.horizontalScrollOffset / (float)(maxLineWidth - visibleWidth));
            int scrollbarY = this.getY() + this.height - scrollbarHeight;
            
            context.fill(this.getX(), this.getY() + this.height - scrollbarHeight, this.getX() + this.width, this.getY() + this.height, 0xFF555555);
            context.fill(scrollbarX, scrollbarY, scrollbarX + Math.max(20, (int)((float)this.width * (float)visibleWidth / (float)maxLineWidth)), scrollbarY + scrollbarHeight, 0xFFBBBBBB);
        }
    }

    private void renderSearchHighlights(DrawContext context, String[] lines, int lineHeight, int maxVisibleLines) {
        searchEngine.setScrollOffset(this.scrollOffset);
        
        int currentLineStart = 0;
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            
            if (lineIndex >= scrollOffset && lineIndex < scrollOffset + maxVisibleLines) {
                int yPos = getY() + 4 + (lineIndex - scrollOffset) * lineHeight;
                int xBase = getX() + 4 + 12 - horizontalScrollOffset;
                
                for (int matchPos : searchEngine.matchPositions) {
                    if (matchPos >= currentLineStart && matchPos < currentLineStart + line.length()) {
                        int matchInLine = matchPos - currentLineStart;
                        int matchEndInLine = Math.min(matchInLine + searchQuery.length(), line.length());
                        
                        if (matchInLine < matchEndInLine) {
                            String beforeMatch = line.substring(0, matchInLine);
                            String matchText = line.substring(matchInLine, matchEndInLine);
                            
                            int xStart = xBase + fileType.getHighLighter().getTextWidth(textRenderer, beforeMatch);
                            int highlightWidth = fileType.getHighLighter().getTextWidth(textRenderer, matchText);
                            int yStart = yPos + textRenderer.fontHeight - 1;
                            
                            boolean isCurrentMatch = matchPos == searchEngine.getCurrentMatchPosition();
                            int color = isCurrentMatch ? 0x66FFD700 : 0x66FFFF00;
                            
                            context.fill(xStart, yStart, xStart + highlightWidth, yStart + 2, color);
                        }
                    }
                }
            }
            
            currentLineStart += line.length() + 1;
        }
    }
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        
        if (isMouseOverHorizontalScrollBar(mouseX, mouseY)) {
            isDraggingHorizontalScroll = true;
            dragStartX = (int) mouseX;
            dragStartScrollOffset = horizontalScrollOffset;
            return true;
        }
        
        if (this.isMouseOver(mouseX, mouseY) && this.editable) {
            this.setFocused(true);
            
            int lineHeight = this.textRenderer.fontHeight + 2;
            int clickedY = (int)mouseY - (this.getY() + 4);
            int lineIndex = MathHelper.clamp(clickedY / lineHeight + this.scrollOffset, 0, this.text.split("\n", -1).length - 1);
            
            String[] lines = this.text.split("\n", -1);
            String line = lines[lineIndex];
            
            int clickedX = (int)mouseX - (this.getX() + 4 + 12) + horizontalScrollOffset;
            
            int charIndex = fileType.getHighLighter().getCharIndexFromTokens(textRenderer, line, clickedX);
            
            int newPosition = 0;
            for (int i = 0; i < lineIndex; i++) {
                newPosition += lines[i].length() + 1;
            }
            newPosition += charIndex;
            
            this.cursorPosition = MathHelper.clamp(newPosition, 0, this.text.length());
            return true;
        } else {
            this.setFocused(false);
            return false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;
        
        int lineHeight = this.textRenderer.fontHeight + 2;
        int maxLines = this.text.split("\n", -1).length;
        int maxVisibleLines = this.height / lineHeight;
        
        int newScrollOffset = this.scrollOffset - (int)Math.signum(amount);
        this.scrollOffset = MathHelper.clamp(newScrollOffset, 0, Math.max(0, maxLines - maxVisibleLines));
        return true;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private boolean isMouseOverHorizontalScrollBar(double mouseX, double mouseY) {
        if (maxLineWidth <= this.width - 20) return false;
        
        int scrollbarHeight = 5;
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
               mouseY >= this.getY() + this.height - scrollbarHeight && mouseY <= this.getY() + this.height;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!this.editable) {
            return false;
        }
        
        int keyCode = input.getKeycode();
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.cursorPosition > 0) {
                this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
                this.cursorPosition--;
                this.onTextChanged();
                updateCursorX();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (this.cursorPosition < this.text.length()) {
                this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
                this.onTextChanged();
                updateCursorX();
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.cursorPosition = MathHelper.clamp(this.cursorPosition - 1, 0, this.text.length());
            updateCursorX();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.cursorPosition = MathHelper.clamp(this.cursorPosition + 1, 0, this.text.length());
            updateCursorX();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            this.cursorPosition = getLineStart(this.cursorPosition);
            updateCursorX();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            this.cursorPosition = getLineEnd(this.cursorPosition);
            updateCursorX();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            int lineStart = getLineStart(this.cursorPosition);
            int currentX = this.cursorPosition - lineStart;
            
            int prevLineEnd = lineStart > 0 ? getLineEnd(lineStart - 1) : -1;
            if (prevLineEnd != -1) {
                int prevLineStart = getLineStart(prevLineEnd);
                int prevLineLength = prevLineEnd - prevLineStart;
                int newX = Math.min(currentX, prevLineLength);
                this.cursorPosition = prevLineStart + newX;
            }
            updateCursorX();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            int lineEnd = getLineEnd(this.cursorPosition);
            int currentX = this.cursorPosition - getLineStart(this.cursorPosition);
            
            int nextLineStart = lineEnd < this.text.length() ? lineEnd + 1 : -1;
            if (nextLineStart != -1) {
                int nextLineEnd = getLineEnd(nextLineStart);
                int nextLineLength = nextLineEnd - nextLineStart;
                int newX = Math.min(currentX, nextLineLength);
                this.cursorPosition = nextLineStart + newX;
            }
            updateCursorX();
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.text = this.text.substring(0, this.cursorPosition) + "\n" + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.onTextChanged();
            updateCursorX();
            return true;
        }

        return false;
    }
    
    @Override
    public boolean charTyped(CharInput input) {
        if (!this.isFocused() || !this.editable) {
            return false;
        }
        
        if (input.isValidChar()) {
            String chr = input.asString();
            this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.onTextChanged();
            updateCursorX();
            return true;
        }
        
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    public String getText() {
        return this.text;
    }
    
    public void setText(String text) {
        this.text = text;
        this.cursorPosition = MathHelper.clamp(this.cursorPosition, 0, this.text.length());
        this.onTextChanged();
        updateCursorX();
        
//        this.setFileName(text);
    }
    
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setChangedListener(Consumer<String> changedListener) {
        this.changedListener = changedListener;
    }

    private void onTextChanged() {
        if (this.changedListener != null) {
            this.changedListener.accept(this.text);
        }
    }
    
    private void updateCursorX() {
        int lineStart = getLineStart(this.cursorPosition);
        String currentLine = this.text.substring(lineStart, this.cursorPosition);
        this.lastCursorX = fileType.getHighLighter().getTextWidth(textRenderer, currentLine);
        
        int visibleWidth = this.width - 20;
        if (lastCursorX > horizontalScrollOffset + visibleWidth) {
            horizontalScrollOffset = lastCursorX - visibleWidth + 10;
        } else if (lastCursorX < horizontalScrollOffset) {
            horizontalScrollOffset = Math.max(0, lastCursorX - 10);
        }
    }

    private int getLineStart(int pos) {
        int start = this.text.lastIndexOf('\n', pos - 1) + 1;
        return start;
    }

    private int getLineEnd(int pos) {
        int end = this.text.indexOf('\n', pos);
        if (end == -1) {
            end = this.text.length();
        }
        return end;
    }
    
    public void insertTextAtCursor(String text) {
        this.text = this.text.substring(0, this.cursorPosition) + text + this.text.substring(this.cursorPosition);
        this.cursorPosition += text.length();
        this.onTextChanged();
        updateCursorX();
    }
    
    public int getCursorPosition() {
        return this.cursorPosition;
    }

    public void setCursorPosition(int position) {
        this.cursorPosition = MathHelper.clamp(position, 0, this.text.length());
        updateCursorX();
    }
    
    public void setFileName(String v) {
        this.filename = v;
        this.fileType = getFileTypeFromName(v);
    }
    
    public String getFileName() {
        return this.filename;
    }

    public void startSearch(String query) {
        this.searchQuery = query;
        this.isSearching = true;
        searchEngine.search(query, text);
        if (searchEngine.hasMatches()) {
            scrollToCurrentMatch();
        }
    }
    
    public void findNext() {
        if (isSearching && searchEngine.hasMatches()) {
            searchEngine.nextMatch();
            scrollToCurrentMatch();
        }
    }
    
    public void findPrevious() {
        if (isSearching && searchEngine.hasMatches()) {
            searchEngine.previousMatch();
            scrollToCurrentMatch();
        }
    }
    
    private void scrollToCurrentMatch() {
        Integer matchPos = searchEngine.getCurrentMatchPosition();
        if (matchPos != null) {
            int lineIndex = getLineIndex(matchPos);
            if (lineIndex < scrollOffset || lineIndex >= scrollOffset + maxVisibleLines) {
                scrollOffset = Math.max(0, lineIndex - 2);
            }
        }
    }
    
    public void endSearch() {
        isSearching = false;
        searchQuery = "";
        searchEngine.clear();
    }
    
    private int getLineIndex(int position) {
        int line = 0;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
    
    public boolean isSearching() {
        return isSearching;
    }
    
    public int getSearchMatchCount() {
        return searchEngine.getMatchCount();
    }
    
    public int getCurrentSearchIndex() {
        return searchEngine.getCurrentMatchIndex() + 1;
    }
    
    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isDraggingHorizontalScroll) {
            int visibleWidth = this.width - 20;
            int dragDeltaX = (int) click.x() - dragStartX;
            int scrollRange = Math.max(0, maxLineWidth - visibleWidth);
            
            if (scrollRange > 0) {
                float scrollRatio = (float) dragDeltaX / (float) this.width;
                int newScrollOffset = dragStartScrollOffset + (int) (scrollRatio * scrollRange);
                horizontalScrollOffset = MathHelper.clamp(newScrollOffset, 0, scrollRange);
            }
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (isDraggingHorizontalScroll) {
            isDraggingHorizontalScroll = false;
            return true;
        }
        return super.mouseReleased(click);
    }
}
