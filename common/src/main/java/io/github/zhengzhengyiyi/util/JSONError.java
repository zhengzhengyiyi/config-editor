package io.github.zhengzhengyiyi.util;

public class JSONError {
    public final int lineNumber;
    public final int columnNumber;
    public final String message;
    public final int startPosition;
    public final int endPosition;
    
    public JSONError(int lineNumber, int columnNumber, String message, int startPosition, int endPosition) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.message = message;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
}
