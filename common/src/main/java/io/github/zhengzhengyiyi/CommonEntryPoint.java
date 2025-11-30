package io.github.zhengzhengyiyi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.zhengzhengyiyi.addon.*;
import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import io.github.zhengzhengyiyi.api.config.ConfigManager;
import io.github.zhengzhengyiyi.config.ModConfigData;
import net.minecraft.text.Text;

import java.util.List;
import java.nio.file.Paths;
import java.util.ArrayList;

public class CommonEntryPoint {
	public final static String MOD_ID = "config_editor";
	public static ConfigManager<ModConfigData> configManager = new ConfigManager<>(Paths.get("config", "editor_config.json"), new ModConfigData(), ModConfigData.class);

	/**
	 * The Logger for the other mod's entry point, usually for printing errors.
	 */
	public static Logger API_ENTRYPOINT_LOGGER = LoggerFactory.getLogger("api_entry_point");
	
	/**
	 * The Logger for the whole mod.
	 */
	public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	/**
	 * 
	 * put every api entry point into a List
	 * 
	 * @see List
	 */
	public static final List<ApiEntrypoint> ENTRYPOINTS = new ArrayList<>();
	
	public static void init() {
		ENTRYPOINTS.add(new UndoRedoEntrypoint());
		ENTRYPOINTS.add(new TextStatsEntrypoint());
		ENTRYPOINTS.add(new DateTimeDisplayEntrypoint());
		ENTRYPOINTS.add(new AutoBracketCompletionEntrypoint());
		ENTRYPOINTS.add(new TextStatsEntrypoint());
		
//		testLanguageResources();
	}
	
	public static void fabric_init() {
		
	}
	
	public static void testLanguageResources() {
		Text test = Text.translatable("configEditor.test");
		
		LOGGER.info("----------------test------------------");
		LOGGER.info(test.toString());
		LOGGER.info(test.getLiteralString());
		LOGGER.info("--------------test-ended--------------");
	}
}
