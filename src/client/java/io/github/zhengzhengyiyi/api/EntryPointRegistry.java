package io.github.zhengzhengyiyi.api;

import io.github.zhengzhengyiyi.ConfigEditorClient;

public class EntryPointRegistry {
	
	/**
	 * use this method to register your ApiEntrypoint implementation
	 * @param entrypoint the entrypoint class to register
	 */
	public static void register(ApiEntrypoint entrypoint) {
		ConfigEditorClient.TOTAL_ENTRYPOINTS.add(entrypoint);
	}
}
