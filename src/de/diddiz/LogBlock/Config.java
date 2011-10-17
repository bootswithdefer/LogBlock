package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.parseTimeSpec;
import static org.bukkit.Bukkit.getLogger;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.DataFormatException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;

public class Config
{
	public final Map<Integer, WorldConfig> worlds;
	public final String url, user, password;
	public final int delayBetweenRuns, forceToProcessAtLeast, timePerRun;
	public final boolean useBukkitScheduler;
	public final int keepLogDays;
	public final boolean dumpDeletedLog;
	public boolean logBlockPlacings, logBlockBreaks, logSignTexts, logExplosions, logFire, logLeavesDecay, logLavaFlow, logWaterFlow, logChestAccess, logButtonsAndLevers, logKills, logChat, logSnowForm, logSnowFade, logDoors, logCakes, logEndermen;
	public final boolean logCreeperExplosionsAsPlayerWhoTriggeredThese, logPlayerInfo;
	public final LogKillsLevel logKillsLevel;
	public final Set<Integer> dontRollback, replaceAnyway;
	public final int rollbackMaxTime, rollbackMaxArea;
	public final Map<String, Tool> toolsByName;
	public final Map<Integer, Tool> toolsByType;
	public final int defaultDist, defaultTime;
	public final int linesPerPage, linesLimit;
	public final boolean askRollbacks, askRedos, askClearLogs, askClearLogAfterRollback, askRollbackAfterBan;
	public final String banPermission;
	public final boolean installSpout, checkVersion;
	public final Set<Integer> hiddenPlayers, hiddenBlocks;

	public static enum LogKillsLevel {
		PLAYERS, MONSTERS, ANIMALS;
	}

	Config(LogBlock logblock) throws DataFormatException, IOException {
		final ConfigurationSection config = logblock.getConfig();
		final Map<String, Object> def = new HashMap<String, Object>();
		def.put("version", logblock.getDescription().getVersion());
		def.put("loggedWorlds", Arrays.asList("world", "world_nether"));
		def.put("mysql.host", "localhost");
		def.put("mysql.port", 3306);
		def.put("mysql.database", "minecraft");
		def.put("mysql.user", "username");
		def.put("mysql.password", "pass");
		def.put("consumer.delayBetweenRuns", 6);
		def.put("consumer.forceToProcessAtLeast", 20);
		def.put("consumer.timePerRun", 200);
		def.put("consumer.useBukkitScheduler", true);
		def.put("clearlog.dumpDeletedLog", false);
		def.put("clearlog.keepLogDays", -1);
		def.put("logging.logCreeperExplosionsAsPlayerWhoTriggeredThese", false);
		def.put("logging.logKillsLevel", "PLAYERS");
		def.put("logging.logPlayerInfo", true);
		def.put("logging.hiddenPlayers", new ArrayList<String>());
		def.put("logging.hiddenBlocks", Arrays.asList(0));
		def.put("rollback.dontRollback", Arrays.asList(10, 11, 46, 51));
		def.put("rollback.replaceAnyway", Arrays.asList(8, 9, 10, 11, 51));
		def.put("rollback.maxTime", "2 days");
		def.put("rollback.maxArea", 50);
		def.put("lookup.defaultDist", 20);
		def.put("lookup.defaultTime", "30 minutes");
		def.put("lookup.linesPerPage", 15);
		def.put("lookup.linesLimit", 1500);
		def.put("questioner.askRollbacks", true);
		def.put("questioner.askRedos", true);
		def.put("questioner.askClearLogs", true);
		def.put("questioner.askClearLogAfterRollback", true);
		def.put("questioner.askRollbackAfterBan", false);
		def.put("questioner.banPermission", "mcbans.ban.local");
		def.put("updater.installSpout", true);
		def.put("updater.checkVersion", true);
		def.put("tools.tool.aliases", Arrays.asList("t"));
		def.put("tools.tool.leftClickBehavior", "NONE");
		def.put("tools.tool.rightClickBehavior", "TOOL");
		def.put("tools.tool.defaultEnabled", true);
		def.put("tools.tool.item", 270);
		def.put("tools.tool.params", "area 0 all sum none limit 15 desc silent");
		def.put("tools.tool.mode", "LOOKUP");
		def.put("tools.tool.permissionDefault", "TRUE");
		def.put("tools.toolblock.aliases", Arrays.asList("tb"));
		def.put("tools.toolblock.leftClickBehavior", "TOOL");
		def.put("tools.toolblock.rightClickBehavior", "BLOCK");
		def.put("tools.toolblock.defaultEnabled", true);
		def.put("tools.toolblock.item", 7);
		def.put("tools.toolblock.params", "area 0 all sum none limit 15 desc silent");
		def.put("tools.toolblock.mode", "LOOKUP");
		def.put("tools.toolblock.permissionDefault", "TRUE");
		for (final Entry<String, Object> e : def.entrySet())
			if (!config.contains(e.getKey()))
				config.set(e.getKey(), e.getValue());
		logblock.saveConfig();
		url = "jdbc:mysql://" + config.getString("mysql.host") + ":" + config.getInt("mysql.port") + "/" + config.getString("mysql.database");
		user = config.getString("mysql.user");
		password = config.getString("mysql.password");
		delayBetweenRuns = config.getInt("consumer.delayBetweenRuns", 6);
		forceToProcessAtLeast = config.getInt("consumer.forceToProcessAtLeast", 0);
		timePerRun = config.getInt("consumer.timePerRun", 100);
		useBukkitScheduler = config.getBoolean("consumer.useBukkitScheduler", true);
		keepLogDays = config.getInt("clearlog.keepLogDays", -1);
		if (keepLogDays * 86400000L > System.currentTimeMillis())
			throw new DataFormatException("Too large timespan for keepLogDays. Must be shorter than " + (int)(System.currentTimeMillis() / 86400000L) + " days.");
		dumpDeletedLog = config.getBoolean("clearlog.dumpDeletedLog", false);
		logCreeperExplosionsAsPlayerWhoTriggeredThese = config.getBoolean("logging.logCreeperExplosionsAsPlayerWhoTriggeredThese", false);
		logPlayerInfo = config.getBoolean("logging.logPlayerInfo", true);
		try {
			logKillsLevel = LogKillsLevel.valueOf(config.getString("logging.logKillsLevel").toUpperCase());
		} catch (final IllegalArgumentException ex) {
			throw new DataFormatException("lookup.toolblockID doesn't appear to be a valid log level. Allowed are 'PLAYERS', 'MONSTERS' and 'ANIMALS'");
		}
		hiddenPlayers = new HashSet<Integer>();
		for (final Object playerName : config.getList("logging.hiddenPlayers"))
			hiddenPlayers.add(playerName.hashCode());
		hiddenBlocks = new HashSet<Integer>();
		for (final Object blocktype : config.getList("logging.hiddenBlocks")) {
			final Material mat = Material.matchMaterial(String.valueOf(blocktype));
			if (mat != null)
				hiddenBlocks.add(mat.getId());
			else
				throw new DataFormatException("Not a valid material: '" + blocktype + "'");
		}
		dontRollback = new HashSet<Integer>(toIntList(config.getList("rollback.dontRollback")));
		replaceAnyway = new HashSet<Integer>(toIntList(config.getList("rollback.replaceAnyway")));
		rollbackMaxTime = parseTimeSpec(config.getString("rollback.maxTime").split(" "));
		rollbackMaxArea = config.getInt("rollback.maxArea", 50);
		defaultDist = config.getInt("lookup.defaultDist", 20);
		defaultTime = parseTimeSpec(config.getString("lookup.defaultTime").split(" "));
		linesPerPage = config.getInt("lookup.linesPerPage", 15);
		linesLimit = config.getInt("lookup.linesLimit", 1500);
		askRollbacks = config.getBoolean("questioner.askRollbacks", true);
		askRedos = config.getBoolean("questioner.askRedos", true);
		askClearLogs = config.getBoolean("questioner.askClearLogs", true);
		askClearLogAfterRollback = config.getBoolean("questioner.askClearLogAfterRollback", true);
		askRollbackAfterBan = config.getBoolean("questioner.askRollbackAfterBan", false);
		banPermission = config.getString("questioner.banPermission");
		installSpout = config.getBoolean("updater.installSpout", true);
		checkVersion = config.getBoolean("updater.checkVersion", true);
		final List<Tool> tools = new ArrayList<Tool>();
		for (final String toolName : config.getConfigurationSection("tools").getKeys(false))
			try {
				final String path = "tools." + toolName;
				final List<String> aliases = toStringList(config.getList(path + ".aliases"));
				final ToolBehavior leftClickBehavior = ToolBehavior.valueOf(config.getString(path + ".leftClickBehavior").toUpperCase());
				final ToolBehavior rightClickBehavior = ToolBehavior.valueOf(config.getString(path + ".rightClickBehavior").toUpperCase());
				final boolean defaultEnabled = config.getBoolean(path + ".defaultEnabled", false);
				final int item = config.getInt(path + ".item", 0);
				final QueryParams params = new QueryParams(logblock);
				params.prepareToolQuery = true;
				params.parseArgs(Bukkit.getConsoleSender(), Arrays.asList(config.getString(path + ".params").split(" ")));
				final ToolMode mode = ToolMode.valueOf(config.getString(path + ".mode").toUpperCase());
				final PermissionDefault pdef = PermissionDefault.valueOf(config.getString(path + ".permissionDefault").toUpperCase());
				tools.add(new Tool(toolName, aliases, leftClickBehavior, rightClickBehavior, defaultEnabled, item, params, mode, pdef));
			} catch (final Exception ex) {
				getLogger().log(Level.WARNING, "Error at parsing tool '" + toolName + "': ", ex);
			}
		toolsByName = new HashMap<String, Tool>();
		toolsByType = new HashMap<Integer, Tool>();
		for (final Tool tool : tools) {
			toolsByType.put(tool.item, tool);
			toolsByName.put(tool.name.toLowerCase(), tool);
			for (final String alias : tool.aliases)
				toolsByName.put(alias, tool);
		}
		final List<String> worldNames = toStringList(config.getList("loggedWorlds"));
		worlds = new HashMap<Integer, WorldConfig>();
		if (worldNames.size() == 0)
			throw new DataFormatException("No worlds configured");
		for (final String world : worldNames)
			worlds.put(world.hashCode(), new WorldConfig(new File("plugins/LogBlock/" + friendlyWorldname(world) + ".yml")));
		for (final WorldConfig wcfg : worlds.values()) {
			if (wcfg.logBlockPlacings)
				logBlockPlacings = true;
			if (wcfg.logBlockBreaks)
				logBlockBreaks = true;
			if (wcfg.logSignTexts)
				logSignTexts = true;
			if (wcfg.logExplosions)
				logExplosions = true;
			if (wcfg.logFire)
				logFire = true;
			if (wcfg.logLeavesDecay)
				logLeavesDecay = true;
			if (wcfg.logLavaFlow)
				logLavaFlow = true;
			if (wcfg.logWaterFlow)
				logWaterFlow = true;
			if (wcfg.logChestAccess)
				logChestAccess = true;
			if (wcfg.logButtonsAndLevers)
				logButtonsAndLevers = true;
			if (wcfg.logKills)
				logKills = true;
			if (wcfg.logChat)
				logChat = true;
			if (wcfg.logSnowForm)
				logSnowForm = true;
			if (wcfg.logSnowFade)
				logSnowFade = true;
			if (wcfg.logDoors)
				logDoors = true;
			if (wcfg.logCakes)
				logCakes = true;
			if (wcfg.logEndermen)
				logEndermen = true;
		}
	}

	public static List<String> toStringList(List<?> list) {
		if (list == null)
			return new ArrayList<String>();
		final List<String> strs = new ArrayList<String>(list.size());
		for (final Object obj : list)
			if (obj instanceof String)
				strs.add((String)obj);
			else
				strs.add(String.valueOf(obj));
		return strs;
	}

	public static List<Integer> toIntList(List<?> list) {
		if (list == null)
			return new ArrayList<Integer>();
		final List<Integer> ints = new ArrayList<Integer>(list.size());
		for (final Object obj : list)
			if (obj instanceof Integer)
				ints.add((Integer)obj);
			else
				try {
					ints.add(Integer.valueOf(String.valueOf(obj)));
				} catch (final NumberFormatException ex) {
					getLogger().warning("[LogBlock] Config error: '" + obj + "' is not a number");
				}
		return ints;
	}
}

class WorldConfig
{
	public final String table;
	public final boolean logBlockPlacings, logBlockBreaks, logSignTexts, logExplosions, logFire, logLeavesDecay, logLavaFlow, logWaterFlow, logChestAccess, logButtonsAndLevers, logKills, logChat, logSnowForm, logSnowFade, logDoors, logCakes, logEndermen;

	public WorldConfig(File file) throws IOException {
		final Map<String, Object> def = new HashMap<String, Object>();
		def.put("table", "lb-" + file.getName().substring(0, file.getName().length() - 4));
		def.put("logBlockCreations", true);
		def.put("logBlockDestroyings", true);
		def.put("logSignTexts", true);
		def.put("logExplosions", true);
		def.put("logFire", true);
		def.put("logLeavesDecay", false);
		def.put("logLavaFlow", false);
		def.put("logWaterFlow", false);
		def.put("logChestAccess", false);
		def.put("logButtonsAndLevers", false);
		def.put("logKills", false);
		def.put("logChat", false);
		def.put("logSnowForm", false);
		def.put("logSnowFade", false);
		def.put("logDoors", false);
		def.put("logCakes", false);
		def.put("logEndermen", false);
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (final Entry<String, Object> e : def.entrySet())
			if (config.get(e.getKey()) == null)
				config.set(e.getKey(), e.getValue());
		config.save(file);
		table = config.getString("table");
		logBlockPlacings = config.getBoolean("logBlockCreations", true);
		logBlockBreaks = config.getBoolean("logBlockDestroyings", true);
		logSignTexts = config.getBoolean("logSignTexts", false);
		logExplosions = config.getBoolean("logExplosions", false);
		logFire = config.getBoolean("logFire", false);
		logLeavesDecay = config.getBoolean("logLeavesDecay", false);
		logLavaFlow = config.getBoolean("logLavaFlow", false);
		logWaterFlow = config.getBoolean("logWaterFlow", false);
		logChestAccess = config.getBoolean("logChestAccess", false);
		logButtonsAndLevers = config.getBoolean("logButtonsAndLevers", false);
		logKills = config.getBoolean("logKills", false);
		logChat = config.getBoolean("logChat", false);
		logSnowForm = config.getBoolean("logSnowForm", false);
		logSnowFade = config.getBoolean("logSnowFade", false);
		logDoors = config.getBoolean("logDoors", false);
		logCakes = config.getBoolean("logCakes", false);
		logEndermen = config.getBoolean("logEndermen", false);
	}
}
