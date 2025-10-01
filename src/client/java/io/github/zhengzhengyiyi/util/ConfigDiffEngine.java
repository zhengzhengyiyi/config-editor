package io.github.zhengzhengyiyi.util;

import java.util.ArrayList;
import java.util.List;

public class ConfigDiffEngine {
    public static class DiffLine {
        public enum ChangeType { UNCHANGED, ADDED, DELETED, MODIFIED }
        
        public final int oldLineNumber;
        public final int newLineNumber;
        public final String oldContent;
        public final String newContent;
        public final ChangeType type;
        
        public DiffLine(int oldLineNumber, int newLineNumber, String oldContent, String newContent, ChangeType type) {
            this.oldLineNumber = oldLineNumber;
            this.newLineNumber = newLineNumber;
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.type = type;
        }
        
        public String getDisplayContent() {
            return type == ChangeType.DELETED ? oldContent : newContent;
        }
    }

    public static List<DiffLine> computeDiff(String oldText, String newText) {
        String[] oldLines = oldText.split("\n", -1);
        String[] newLines = newText.split("\n", -1);
        
        List<DiffLine> diffLines = new ArrayList<>();
        
        int oldIndex = 0;
        int newIndex = 0;
        
        while (oldIndex < oldLines.length || newIndex < newLines.length) {
            if (oldIndex < oldLines.length && newIndex < newLines.length && 
                oldLines[oldIndex].equals(newLines[newIndex])) {
                diffLines.add(new DiffLine(oldIndex + 1, newIndex + 1, oldLines[oldIndex], newLines[newIndex], DiffLine.ChangeType.UNCHANGED));
                oldIndex++;
                newIndex++;
            } else if (newIndex < newLines.length && (oldIndex >= oldLines.length || !containsLine(oldLines, newLines[newIndex], oldIndex))) {
                diffLines.add(new DiffLine(0, newIndex + 1, "", newLines[newIndex], DiffLine.ChangeType.ADDED));
                newIndex++;
            } else if (oldIndex < oldLines.length && (newIndex >= newLines.length || !containsLine(newLines, oldLines[oldIndex], newIndex))) {
                diffLines.add(new DiffLine(oldIndex + 1, 0, oldLines[oldIndex], "", DiffLine.ChangeType.DELETED));
                oldIndex++;
            } else {
                if (oldIndex < oldLines.length) {
                    diffLines.add(new DiffLine(oldIndex + 1, 0, oldLines[oldIndex], "", DiffLine.ChangeType.DELETED));
                    oldIndex++;
                }
                if (newIndex < newLines.length) {
                    diffLines.add(new DiffLine(0, newIndex + 1, "", newLines[newIndex], DiffLine.ChangeType.ADDED));
                    newIndex++;
                }
            }
        }
        
        return diffLines;
    }
    
    private static boolean containsLine(String[] lines, String target, int startIndex) {
        for (int i = startIndex; i < lines.length; i++) {
            if (lines[i].equals(target)) {
                return true;
            }
        }
        return false;
    }
    
    public static String generateDiffSummary(List<DiffLine> diffLines) {
        int added = 0, deleted = 0, modified = 0, unchanged = 0;
        
        for (DiffLine line : diffLines) {
            switch (line.type) {
                case ADDED -> added++;
                case DELETED -> deleted++;
                case MODIFIED -> modified++;
                case UNCHANGED -> unchanged++;
            }
        }
        
        return String.format("changed: +%d -%d ~%d =%d", added, deleted, modified, unchanged);
    }
    
    public static String applyDiff(String originalText, List<DiffLine> diffLines, boolean acceptAll) {
        List<String> resultLines = new ArrayList<>();
        
        for (DiffLine line : diffLines) {
            if (acceptAll || line.type != DiffLine.ChangeType.DELETED) {
                resultLines.add(line.newContent.isEmpty() ? line.oldContent : line.newContent);
            }
        }
        
        return String.join("\n", resultLines);
    }
}
