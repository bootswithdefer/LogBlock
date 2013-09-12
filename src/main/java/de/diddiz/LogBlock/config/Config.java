package de.diddiz.LogBlock.config;

import de.diddiz.LogBlock.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.zip.DataFormatException;

import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.parseTimeSpec;
import static org.bukkit.Bukkit.*;

public class Config
{
	private static LoggingEnabledMapping superWorldConfig;
	private static Map<String, WorldConfig> worldConfigs;
	public static String url, user, password;
	public static int delayBetweenRuns, forceToProcessAtLeast, timePerRun;
	public static boolean fireCustomEvents;
	public static boolean useBukkitScheduler;
	public static int queueWarningSize;
	public static boolean enableAutoClearLog;
	public static List<String> autoClearLog;
	public static int autoClearLogDelay;
	public static boolean dumpDeletedLog;
	public static boolean logCreeperExplosionsAsPlayerWhoTriggeredThese, logPlayerInfo;
	public static LogKillsLevel logKillsLevel;
	public static Set<Integer> dontRollback, replaceAnyway;
	public static int rollbackMaxTime, rollbackMaxArea;
	public static Map<String, Tool> toolsByName;
	public static Map<Integer, Tool> toolsByType;
	public static int defaultDist, defaultTime;
	public static int linesPerPage, linesLimit;
	public static boolean askRollbacks, askRedos, askClearLogs, askClearLogAfterRollback, askRollbackAfterBan;
	public static String banPermission;
	public static Set<Integer> hiddenBlocks;
	public static Set<String> hiddenPlayers;
	public static Set<String> ignoredChat;
	public static SimpleDateFormat formatter;
	public static boolean safetyIdCheck;

	public static enum LogKillsLevel
	{
		PLAYERS, MONSTERS, ANIMALS;
	}

	public static void load(LogBlock logblock) throws DataFormatException, IOException {
		final ConfigurationSection config = logblock.getConfig();
		final Map<String, Object> def = new HashMap<String, Object>();
		def.put("version", logblock.getDescription().getVersion());
		final List<String> worldNames = new ArrayList<String>();
		for (final World world : getWorlds())
			worldNames.add(world.getName());
		if (worldNames.isEmpty()) {
			worldNames.add("world");
			worldNames.add("world_nether");
			worldNames.add("world_the_end");
		}
		def.put("loggedWorlds", worldNames);
		def.put("mysql.host", "localhost");
		def.put("mysql.port", 3306);
		def.put("mysql.database", "minecraft");
		def.put("mysql.user", "username");
		def.put("mysql.password", "pass");
		def.put("consumer.delayBetweenRuns", 2);
		def.put("consumer.forceToProcessAtLeast", 200);
		def.put("consumer.timePerRun", 1000);
		def.put("consumer.fireCustomEvents", false);
		def.put("consumer.useBukkitScheduler", true);
		def.put("consumer.queueWarningSize", 1000);
		def.put("clearlog.dumpDeletedLog", false);
		def.put("clearlog.enableAutoClearLog", false);
		def.put("clearlog.auto", Arrays.asList("world \"world\" before 365 days all", "world \"world\" player lavaflow waterflow leavesdecay before 7 days all", "world world_nether before 365 days all", "world world_nether player lavaflow before 7 days all"));
		def.put("clearlog.autoClearLogDelay", "6h");
		def.put("logging.logCreeperExplosionsAsPlayerWhoTriggeredThese", false);
		def.put("logging.logKillsLevel", "PLAYERS");
		def.put("logging.logPlayerInfo", false);
		def.put("logging.hiddenPlayers", new ArrayList<String>());
		def.put("logging.hiddenBlocks", Arrays.asList(0));
		def.put("logging.ignoredChat", Arrays.asList("/register", "/login"));
		def.put("rollback.dontRollback", Arrays.asList(10, 11, 46, 51));
		def.put("rollback.replaceAnyway", Arrays.asList(8, 9, 10, 11, 51));
		def.put("rollback.maxTime", "2 days");
		def.put("rollback.maxArea", 50);
		def.put("lookup.defaultDist", 20);
		def.put("lookup.defaultTime", "30 minutes");
		def.put("lookup.linesPerPage", 15);
		def.put("lookup.linesLimit", 1500);
		try {
			formatter = new SimpleDateFormat(config.getString("lookup.dateFormat", "MM-dd HH:mm:ss"));
		} catch (IllegalArgumentException e) {
			throw new DataFormatException("Invalid specification for  date format, please see http://docs.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html : " + e.getMessage());
		}
		def.put("lookup.dateFormat", "MM-dd HH:mm:ss");
		def.put("questioner.askRollbacks", true);
		def.put("questioner.askRedos", true);
		def.put("questioner.askClearLogs", true);
		def.put("questioner.askClearLogAfterRollback", true);
		def.put("questioner.askRollbackAfterBan", false);
		def.put("questioner.banPermission", "mcbans.ban.local");
		def.put("tools.tool.aliases", Arrays.asList("t"));
		def.put("tools.tool.leftClickBehavior", "NONE");
		def.put("tools.tool.rightClickBehavior", "TOOL");
		def.put("tools.tool.defaultEnabled", true);
		def.put("tools.tool.item", 270);
		def.put("tools.tool.canDrop", true);
		def.put("tools.tool.params", "area 0 all sum none limit 15 desc silent");
		def.put("tools.tool.mode", "LOOKUP");
		def.put("tools.tool.permissionDefault", "OP");
		def.put("tools.toolblock.aliases", Arrays.asList("tb"));
		def.put("tools.toolblock.leftClickBehavior", "TOOL");
		def.put("tools.toolblock.rightClickBehavior", "BLOCK");
		def.put("tools.toolblock.defaultEnabled", true);
		def.put("tools.toolblock.item", 7);
		def.put("tools.toolblock.canDrop", false);
		def.put("tools.toolblock.params", "area 0 all sum none limit 15 desc silent");
		def.put("tools.toolblock.mode", "LOOKUP");
		def.put("tools.toolblock.permissionDefault", "OP");
		def.put("safety.id.check", true);
		for (final Entry<String, Object> e : def.entrySet())
			if (!config.contains(e.getKey()))
				config.set(e.getKey(), e.getValue());
		logblock.saveConfig();
		url = "jdbc:mysql://" + config.getString("mysql.host") + ":" + config.getInt("mysql.port") + "/" + getStringIncludingInts(config, "mysql.database") + "?useUnicode=true&characterEncoding=utf-8";
		user = getStringIncludingInts(config, "mysql.user");
		password = getStringIncludingInts(config, "mysql.password");
		delayBetweenRuns = config.getInt("consumer.delayBetweenRuns", 2);
		forceToProcessAtLeast = config.getInt("consumer.forceToProcessAtLeast", 0);
		timePerRun = config.getInt("consumer.timePerRun", 1000);
		fireCustomEvents = config.getBoolean("consumer.fireCustomEvents", false);
		useBukkitScheduler = config.getBoolean("consumer.useBukkitScheduler", true);
		queueWarningSize = config.getInt("consumer.queueWarningSize", 1000);
		enableAutoClearLog = config.getBoolean("clearlog.enableAutoClearLog");
		autoClearLog = config.getStringList("clearlog.auto");
		dumpDeletedLog = config.getBoolean("clearlog.dumpDeletedLog", false);
		autoClearLogDelay = parseTimeSpec(config.getString("clearlog.autoClearLogDelay").split(" "));
		logCreeperExplosionsAsPlayerWhoTriggeredThese = config.getBoolean("logging.logCreeperExplosionsAsPlayerWhoTriggeredThese", false);
		logPlayerInfo = config.getBoolean("logging.logPlayerInfo", true);
		try {
			logKillsLevel = LogKillsLevel.valueOf(config.getString("logging.logKillsLevel").toUpperCase());
		} catch (final IllegalArgumentException ex) {
			throw new DataFormatException("lookup.toolblockID doesn't appear to be a valid log level. Allowed are 'PLAYERS', 'MONSTERS' and 'ANIMALS'");
		}
		hiddenPlayers = new HashSet<String>();
		for (final String playerName : config.getStringList("logging.hiddenPlayers"))
			hiddenPlayers.add(playerName.toLowerCase().trim());
		hiddenBlocks = new HashSet<Integer>();
		for (final Object blocktype : config.getList("logging.hiddenBlocks")) {
			final Material mat = Material.matchMaterial(String.valueOf(blocktype));
			if (mat != null)
				hiddenBlocks.add(mat.getId());
			else
				throw new DataFormatException("Not a valid material: '" + blocktype + "'");
		}
		ignoredChat = new HashSet<String>();
		for (String chatCommand : config.getStringList("logging.ignoredChat")) {
			ignoredChat.add(chatCommand);
		}
		dontRollback = new HashSet<Integer>(config.getIntegerList("rollback.dontRollback"));
		replaceAnyway = new HashSet<Integer>(config.getIntegerList("rollback.replaceAnyway"));
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
		safetyIdCheck = config.getBoolean("safety.id.check", true);
		banPermission = config.getString("questioner.banPermission");
		final List<Tool> tools = new ArrayList<Tool>();
		final ConfigurationSection toolsSec = config.getConfigurationSection("tools");
		for (final String toolName : toolsSec.getKeys(false))
			try {
				final ConfigurationSection tSec = toolsSec.getConfigurationSection(toolName);
				final List<String> aliases = tSec.getStringList("aliases");
				final ToolBehavior leftClickBehavior = ToolBehavior.valueOf(tSec.getString("leftClickBehavior").toUpperCase());
				final ToolBehavior rightClickBehavior = ToolBehavior.valueOf(tSec.getString("rightClickBehavior").toUpperCase());
				final boolean defaultEnabled = tSec.getBoolean("defaultEnabled", false);
				final int item = tSec.getInt("item", 0);
				final boolean canDrop = tSec.getBoolean("canDrop", false);
				final QueryParams params = new QueryParams(logblock);
				params.prepareToolQuery = true;
				params.parseArgs(getConsoleSender(), Arrays.asList(tSec.getString("params").split(" ")));
				final ToolMode mode = ToolMode.valueOf(tSec.getString("mode").toUpperCase());
				final PermissionDefault pdef = PermissionDefault.valueOf(tSec.getString("permissionDefault").toUpperCase());
				tools.add(new Tool(toolName, aliases, leftClickBehavior, rightClickBehavior, defaultEnabled, item, canDrop, params, mode, pdef));
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
		final List<String> loggedWorlds = config.getStringList("loggedWorlds");
		worldConfigs = new HashMap<String, WorldConfig>();
		if (loggedWorlds.isEmpty())
			throw new DataFormatException("No worlds configured");
		for (final String world : loggedWorlds)
			worldConfigs.put(world, new WorldConfig(new File(logblock.getDataFolder(), friendlyWorldname(world) + ".yml")));
		superWorldConfig = new LoggingEnabledMapping();
		for (final WorldConfig wcfg : worldConfigs.values())
			for (final Logging l : Logging.values())
				if (wcfg.isLogging(l))
					superWorldConfig.setLogging(l, true);
	}

	private static String getStringIncludingInts(ConfigurationSection cfg, String key) {
		String str = cfg.getString(key);
		if (str == null)
			str = String.valueOf(cfg.getInt(key));
		if (str == null)
			str = "No value set for '" + key + "'";
		return str;
	}

	public static boolean isLogging(World world, Logging l) {
		final WorldConfig wcfg = worldConfigs.get(world.getName());
		return wcfg != null && wcfg.isLogging(l);
	}

	public static boolean isLogging(String worldName, Logging l) {
		final WorldConfig wcfg = worldConfigs.get(worldName);
		return wcfg != null && wcfg.isLogging(l);
	}

	public static boolean isLogged(World world) {
		return worldConfigs.containsKey(world.getName());
	}

	public static WorldConfig getWorldConfig(World world) {
		return worldConfigs.get(world.getName());
	}

	public static WorldConfig getWorldConfig(String world) {
		return worldConfigs.get(world);
	}

	public static boolean isLogging(Logging l) {
		return superWorldConfig.isLogging(l);
	}

	public static Collection<WorldConfig> getLoggedWorlds() {
		return worldConfigs.values();
	}
}

class LoggingEnabledMapping
{
	private final boolean[] logging = new boolean[Logging.length];

	public void setLogging(Logging l, boolean enabled) {
		logging[l.ordinal()] = enabled;
	}

	public boolean isLogging(Logging l) {
		return logging[l.ordinal()];
	}
}
