package io.github.zhengzhengyiyi.gui.widget;

import java.util.function.Consumer;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Abstract base class for all editor implementations.
 * Provides common functionality and contract for text editors.
 */
public abstract class AbstractEditor extends ClickableWidget {
    
    public AbstractEditor(int x, int y, int width, int height, Text message) {
		super(x, y, width, height, message);
	}

	/**
     * Gets the current text content of the editor.
     * @return the text content
     */
    public abstract String getText();
    
    /**
     * Sets the text content of the editor.
     * @param text the new text content
     */
    public abstract void setText(String text);
    
    /**
     * Sets whether the editor is editable.
     * @param editable true if the editor should be editable
     */
    public abstract void setEditable(boolean editable);
    
    /**
     * Sets the listener for text change events.
     * @param changedListener the consumer to call when text changes
     */
    public abstract void setChangedListener(Consumer<String> changedListener);
    
    /**
     * Gets the current cursor position.
     * @return the cursor position
     */
    public abstract int getCursorPosition();
    
    /**
     * Sets the cursor position.
     * @param position the new cursor position
     */
    public abstract void setCursorPosition(int position);
    
    /**
     * Inserts text at the current cursor position.
     * @param text the text to insert
     */
    public abstract void insertTextAtCursor(String text);
    
    /**
     * Starts a text search with the given query.
     * @param query the search query
     */
    public abstract void startSearch(String query);
    
    /**
     * Finds the next search match.
     */
    public abstract void findNext();
    
    /**
     * Finds the previous search match.
     */
    public abstract void findPrevious();
    
    /**
     * Ends the current search operation.
     */
    public abstract void endSearch();
    
    /**
     * Checks if a search is currently active.
     * @return true if searching
     */
    public abstract boolean isSearching();
    
    /**
     * Gets the number of search matches found.
     * @return the match count
     */
    public abstract int getSearchMatchCount();
    
    /**
     * Gets the current search match index (1-based).
     * @return the current match index
     */
    public abstract int getCurrentSearchIndex();
}
