package de.diddiz.LogBlock;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;

public class Session
{
	public QueryParams lastQuery = null;
	public LookupCacheElement[] lookupCache = null;
	public int page = 1;
	public Map<Tool, ToolData> toolData;

	Session(LogBlock logblock, Player player) {
		toolData = new HashMap<Tool, ToolData>();
		if (player != null)
			for (final Tool tool : logblock.getLBConfig().toolsByType.values())
				toolData.put(tool, new ToolData(tool, logblock, player));
	}
}

class ToolData
{
	boolean enabled;
	QueryParams params;
	ToolMode mode;

	ToolData(Tool tool, LogBlock logblock, Player player) {
		enabled = tool.defaultEnabled && logblock.hasPermission(player, "logblock.tools." + tool.name);
		params = tool.params.clone();
		mode = tool.mode;
	}
}
