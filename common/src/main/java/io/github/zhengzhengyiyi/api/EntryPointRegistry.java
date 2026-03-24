package io.github.zhengzhengyiyi.api;

import io.github.zhengzhengyiyi.CommonEntryPoint;

public class EntryPointRegistry {
	
	/**
	 * use this method to register your ApiEntrypoint implementation
	 * @param entrypoint the entrypoint class to register
	 */
	public static void register(ApiEntrypoint entrypoint) {
		CommonEntryPoint.TOTAL_ENTRYPOINTS.add(entrypoint);
	}
}
