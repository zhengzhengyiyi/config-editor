package io.github.zhengzhengyiyi.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextSearchEngine {
    private String searchText = "";
    private boolean caseSensitive = false;
    private boolean useRegex = false;
    public List<Integer> matchPositions = new ArrayList<>();
    private int currentMatchIndex = -1;
    private int scrollOffset = 0;
    
    public void search(String text, String content) {
        this.searchText = text;
        this.matchPositions.clear();
        this.currentMatchIndex = -1;
        
        if (text == null || text.isEmpty() || content == null || content.isEmpty()) {
            return;
        }
        
        String searchPattern = caseSensitive ? text : text.toLowerCase();
        String searchContent = caseSensitive ? content : content.toLowerCase();
        
        if (useRegex) {
            try {
                Pattern pattern = Pattern.compile(searchPattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    matchPositions.add(matcher.start());
                }
            } catch (PatternSyntaxException e) {
                matchPositions.clear();
            }
        } else {
            int index = 0;
            while (index < searchContent.length()) {
                int foundIndex = searchContent.indexOf(searchPattern, index);
                if (foundIndex == -1) break;
                matchPositions.add(foundIndex);
                index = foundIndex + 1;
            }
        }
        
        if (!matchPositions.isEmpty()) {
            currentMatchIndex = 0;
        }
    }
    
    public boolean hasMatches() {
        return !matchPositions.isEmpty();
    }
    
    public int getMatchCount() {
        return matchPositions.size();
    }
    
    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }
    
    public Integer getCurrentMatchPosition() {
        if (currentMatchIndex >= 0 && currentMatchIndex < matchPositions.size()) {
            return matchPositions.get(currentMatchIndex);
        }
        return null;
    }
    
    public boolean nextMatch() {
        if (matchPositions.isEmpty()) return false;
        currentMatchIndex = (currentMatchIndex + 1) % matchPositions.size();
        return true;
    }
    
    public boolean previousMatch() {
        if (matchPositions.isEmpty()) return false;
        currentMatchIndex = (currentMatchIndex - 1 + matchPositions.size()) % matchPositions.size();
        return true;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
    }
    
    public void clear() {
        searchText = "";
        matchPositions.clear();
        currentMatchIndex = -1;
    }
    
    public void renderHighlights(DrawContext context, TextRenderer textRenderer, String content, int x, int y, int lineHeight, int visibleLines) {
        if (matchPositions.isEmpty()) return;
        
        String[] lines = content.split("\n", -1);
        int currentLineStart = 0;
        
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            if (lineIndex >= scrollOffset && lineIndex < scrollOffset + visibleLines) {
                String line = lines[lineIndex];
                int yPos = y + (lineIndex - scrollOffset) * lineHeight;
                
                for (int matchPos : matchPositions) {
                    if (matchPos >= currentLineStart && matchPos < currentLineStart + line.length()) {
                        int matchInLine = matchPos - currentLineStart;
                        String beforeMatch = line.substring(0, matchInLine);
                        String matchText = line.substring(matchInLine, Math.min(matchInLine + searchText.length(), line.length()));
                        
                        int xStart = x + textRenderer.getWidth(beforeMatch);
                        int highlightWidth = textRenderer.getWidth(matchText);
                        
                        if (matchPos == getCurrentMatchPosition()) {
                            context.fill(xStart, yPos, xStart + highlightWidth, yPos + lineHeight, 0x66FFD700);
                        } else {
                            context.fill(xStart, yPos, xStart + highlightWidth, yPos + lineHeight, 0x66FFFF00);
                        }
                    }
                }
            }
            String line = lines[lineIndex];
            
            currentLineStart += line.length() + 1;
        }
    }
    
    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
    }
}
