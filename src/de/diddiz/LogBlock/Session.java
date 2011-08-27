package de.diddiz.LogBlock;

import java.util.HashMap;
import java.util.Map;

public class Session
{
	public QueryParams lastQuery = null;
	public LookupCacheElement[] lookupCache = null;
	public int page = 1;
	public Map<Tool, ToolData> toolData;

	Session(LogBlock logblock) {
		toolData = new HashMap<Tool, ToolData>();
		for (final Tool tool : logblock.getConfig().toolsByType.values())
			toolData.put(tool, new ToolData(tool));
	}
}

class ToolData
{
	boolean enabled;
	QueryParams params;
	ToolMode mode;

	ToolData(Tool tool) {
		enabled = tool.defaultEnabled;
		params = tool.params.clone();
		mode = tool.mode;
	}
}
