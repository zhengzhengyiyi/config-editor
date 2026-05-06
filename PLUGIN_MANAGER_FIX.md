# Plugin Manager Fix

## Problem
The plugin manager toggle buttons appeared to work (changing the button label), but the changes weren't actually persisted. When you closed and reopened the screen or restarted the game, all plugins were enabled again.

## Root Cause
1. **No persistence** — Plugin states were only stored in memory (`ConfigEditorClient.ENTRYPOINTS` list)
2. **Reset on startup** — The `ENTRYPOINTS` list was repopulated from scratch every time the game started
3. **No config save** — The toggle method didn't save changes to the config file

## Solution

### 1. Added `disabledPlugins` to Config (`ModConfigData.java`)
```java
@SerializedName("disabled_plugins")
public List<String> disabledPlugins = new ArrayList<>();
```
This stores plugin identifiers (e.g., `"config_editor:undo_redo"`) that should remain disabled.

### 2. Updated Startup Logic (`ConfigEditorClient.java`)
On initialization, the mod now:
- Loads the `disabledPlugins` list from config
- Only adds plugins to `ENTRYPOINTS` if they're NOT in the disabled list
- Keeps all plugins in `TOTAL_ENTRYPOINTS` for the plugin manager UI

### 3. Updated Toggle Logic (`PluginManagerScreen.java`)
When toggling a plugin:
- **Disable**: Removes from `ENTRYPOINTS` + adds ID to `disabledPlugins` config
- **Enable**: Adds to `ENTRYPOINTS` + removes ID from `disabledPlugins` config
- **Saves config immediately** using `ConfigEditorClient.configManager.save()`

## How It Works Now

### First Time (No Config)
1. All plugins load normally
2. `disabledPlugins` list is empty `[]`

### User Disables "Undo/Redo" Plugin
1. Plugin removed from `ENTRYPOINTS` (takes effect immediately)
2. `"config_editor:undo_redo"` added to `disabledPlugins` list
3. Config saved to `config/editor_config.json`:
```json
{
  "disabled_plugins": ["config_editor:undo_redo"],
  ...
}
```

### Next Game Launch
1. Mod reads config and sees `"config_editor:undo_redo"` in disabled list
2. Skips adding that plugin to `ENTRYPOINTS`
3. Plugin stays disabled ✅

### User Re-enables Plugin
1. Plugin added back to `ENTRYPOINTS`
2. `"config_editor:undo_redo"` removed from `disabledPlugins` list
3. Config saved with empty disabled list: `"disabled_plugins": []`

## Testing Checklist

- [x] Toggle plugin off → button shows "Disabled"
- [x] Close and reopen plugin manager → plugin still shows "Disabled"
- [x] Restart game → plugin still disabled
- [x] Toggle plugin back on → button shows "Enabled"
- [x] Restart game → plugin still enabled
- [x] Check `config/editor_config.json` → `disabled_plugins` list updates correctly

## Config File Example

After disabling "Text Stats" and "Date/Time Display":
```json
{
  "readonly_mode": false,
  "hint": true,
  "theme": "DARK",
  "doRenderBackground": false,
  "doSuggestions": true,
  "show_block_overlay": true,
  "show_entity_overlay": true,
  "disabled_plugins": [
    "config_editor:text_stats",
    "config_editor:datetime_display"
  ]
}
```

## Benefits

1. **Persistent** — Changes survive game restarts
2. **Immediate** — Changes take effect right away (no restart needed)
3. **Clean** — Uses existing config system, no new files
4. **Backward compatible** — Old configs without `disabled_plugins` work fine (defaults to empty list)
