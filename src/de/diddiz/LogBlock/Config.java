package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.parseTimeSpec;
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
import java.util.zip.DataFormatException;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.util.config.Configuration;

public class Config
{
	public final Map<Integer, WorldConfig> worlds;
	public final String url, user, password;
	public final int delayBetweenRuns, forceToProcessAtLeast, timePerRun;
	public final boolean useBukkitScheduler;
	public final int keepLogDays;
	public final boolean dumpDeletedLog;
	public boolean logBlockPlacings, logBlockBreaks, logSignTexts, logExplosions, logFire, logLeavesDecay, logLavaFlow, logWaterFlow, logChestAccess, logButtonsAndLevers, logKills, logChat;
	public final boolean logCreeperExplosionsAsPlayerWhoTriggeredThese;
	public final LogKillsLevel logKillsLevel;
	public final Set<Integer> dontRollback, replaceAnyway;
	public final int rollbackMaxTime, rollbackMaxArea;
	public final QueryParams toolQuery, toolBlockQuery;
	public final int defaultDist, defaultTime;
	public final int linesPerPage, linesLimit;
	public final int toolID, toolblockID;
	public final boolean askRollbacks, askRedos, askClearLogs;
	public final Set<Integer> hiddenPlayers, hiddenBlocks;

	public static enum LogKillsLevel {
		PLAYERS, MONSTERS, ANIMALS
	}

	Config(LogBlock logblock) throws DataFormatException, IOException {
		final Configuration config = logblock.getConfiguration();
		config.load();
		final List<String> keys = config.getKeys(null);
		List<String> subkeys;
		if (!keys.contains("version"))
			config.setProperty("version", logblock.getDescription().getVersion());
		if (!keys.contains("loggedWorlds"))
			config.setProperty("loggedWorlds", Arrays.asList(new String[]{"world", "world_nether"}));
		if (!keys.contains("tables"))
			config.setProperty("tables", Arrays.asList(new String[]{"lb-main", "lb-nether"}));
		subkeys = config.getKeys("mysql");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("host"))
			config.setProperty("mysql.host", "localhost");
		if (!subkeys.contains("port"))
			config.setProperty("mysql.port", 3306);
		if (!subkeys.contains("database"))
			config.setProperty("mysql.database", "minecraft");
		if (!subkeys.contains("user"))
			config.setProperty("mysql.user", "username");
		if (!subkeys.contains("password"))
			config.setProperty("mysql.password", "pass");
		subkeys = config.getKeys("consumer");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("delayBetweenRuns"))
			config.setProperty("consumer.delayBetweenRuns", 6);
		if (!subkeys.contains("forceToProcessAtLeast"))
			config.setProperty("consumer.forceToProcessAtLeast", 0);
		if (!subkeys.contains("timePerRun"))
			config.setProperty("consumer.timePerRun", 100);
		if (!subkeys.contains("useBukkitScheduler"))
			config.setProperty("consumer.useBukkitScheduler", true);
		subkeys = config.getKeys("clearlog");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("dumpDeletedLog"))
			config.setProperty("clearlog.dumpDeletedLog", false);
		if (!subkeys.contains("keepLogDays"))
			config.setProperty("clearlog.keepLogDays", -1);
		subkeys = config.getKeys("logging");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("logCreeperExplosionsAsPlayerWhoTriggeredThese"))
			config.setProperty("logging.logCreeperExplosionsAsPlayerWhoTriggeredThese", false);
		if (!subkeys.contains("logKillsLevel"))
			config.setProperty("logging.logKillsLevel", "PLAYERS");
		if (!subkeys.contains("hiddenPlayers"))
			config.setProperty("logging.hiddenPlayers", new ArrayList<String>());
		if (!subkeys.contains("hiddenBlocks"))
			config.setProperty("logging.hiddenBlocks", Arrays.asList(new Integer[]{0}));
		subkeys = config.getKeys("rollback");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("dontRollback"))
			config.setProperty("rollback.dontRollback", Arrays.asList(new Integer[]{10, 11, 46, 51}));
		if (!subkeys.contains("replaceAnyway"))
			config.setProperty("rollback.replaceAnyway", Arrays.asList(new Integer[]{8, 9, 10, 11, 51}));
		if (!subkeys.contains("maxTime"))
			config.setProperty("rollback.maxTime", "2 days");
		if (!subkeys.contains("maxArea"))
			config.setProperty("rollback.maxArea", 50);
		subkeys = config.getKeys("lookup");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("defaultDist"))
			config.setProperty("lookup.defaultDist", 20);
		if (!subkeys.contains("defaultTime"))
			config.setProperty("lookup.defaultTime", "30 minutes");
		if (!subkeys.contains("toolID"))
			config.setProperty("lookup.toolID", 270);
		if (!subkeys.contains("toolblockID"))
			config.setProperty("lookup.toolblockID", 7);
		if (!subkeys.contains("toolQuery"))
			config.setProperty("lookup.toolQuery", "area 0 all sum none limit 15 desc silent");
		if (!subkeys.contains("toolBlockQuery"))
			config.setProperty("lookup.toolBlockQuery", "area 0 all sum none limit 15 desc silent");
		if (!subkeys.contains("linesPerPage"))
			config.setProperty("lookup.linesPerPage", 15);
		if (!subkeys.contains("linesLimit"))
			config.setProperty("lookup.linesLimit", 1500);
		subkeys = config.getKeys("questioner");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("askRollbacks"))
			config.setProperty("questioner.askRollbacks", true);
		if (!subkeys.contains("askRedos"))
			config.setProperty("questioner.askRedos", true);
		if (!subkeys.contains("askClearLogs"))
			config.setProperty("questioner.askClearLogs", true);
		if (!config.save())
			throw new IOException("Error while writing to config.yml");
		url = "jdbc:mysql://" + config.getString("mysql.host") + ":" + config.getString("mysql.port") + "/" + config.getString("mysql.database");
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
		try {
			logKillsLevel = LogKillsLevel.valueOf(config.getString("logging.logKillsLevel"));
		} catch (final IllegalArgumentException ex) {
			throw new DataFormatException("lookup.toolblockID doesn't appear to be a valid log level. Allowed are 'PLAYERS', 'MONSTERS' and 'ANIMALS'");
		}
		hiddenPlayers = new HashSet<Integer>();
		for (final String playerName : config.getStringList("logging.hiddenPlayers", new ArrayList<String>()))
			hiddenPlayers.add(playerName.hashCode());
		hiddenBlocks = new HashSet<Integer>();
		for (final String blocktype : config.getStringList("logging.hiddenBlocks", new ArrayList<String>())) {
			final Material mat = Material.matchMaterial(blocktype);
			if (mat != null)
				hiddenBlocks.add(mat.getId());
			else
				throw new DataFormatException("Not a valid material: '" + blocktype + "'");
		}
		dontRollback = new HashSet<Integer>(config.getIntList("rollback.dontRollback", null));
		replaceAnyway = new HashSet<Integer>(config.getIntList("rollback.replaceAnyway", null));
		rollbackMaxTime = parseTimeSpec(config.getString("rollback.maxTime").split(" "));
		rollbackMaxArea = config.getInt("rollback.maxArea", 50);
		try {
			toolQuery = new QueryParams(logblock);
			toolQuery.prepareToolQuery = true;
			toolQuery.parseArgs(new ConsoleCommandSender(logblock.getServer()), Arrays.asList(config.getString("lookup.toolQuery").split(" ")));
		} catch (final IllegalArgumentException ex) {
			throw new DataFormatException("Error at lookup.toolQuery: " + ex.getMessage());
		}
		try {
			toolBlockQuery = new QueryParams(logblock);
			toolBlockQuery.prepareToolQuery = true;
			toolBlockQuery.parseArgs(new ConsoleCommandSender(logblock.getServer()), Arrays.asList(config.getString("lookup.toolBlockQuery").split(" ")));
		} catch (final IllegalArgumentException ex) {
			throw new DataFormatException("Error at lookup.toolBlockQuery: " + ex.getMessage());
		}
		defaultDist = config.getInt("lookup.defaultDist", 20);
		defaultTime = parseTimeSpec(config.getString("lookup.defaultTime").split(" "));
		toolID = config.getInt("lookup.toolID", 270);
		if (Material.getMaterial(toolID) == null || Material.getMaterial(toolID).isBlock())
			throw new DataFormatException("lookup.toolID doesn't appear to be a valid item id");
		toolblockID = config.getInt("lookup.toolblockID", 7);
		if (Material.getMaterial(toolblockID) == null || !Material.getMaterial(toolblockID).isBlock() || toolblockID == 0)
			throw new DataFormatException("lookup.toolblockID doesn't appear to be a valid block id");
		linesPerPage = config.getInt("lookup.linesPerPage", 15);
		linesLimit = config.getInt("lookup.linesLimit", 1500);
		askRollbacks = config.getBoolean("questioner.askRollbacks", true);
		askRedos = config.getBoolean("questioner.askRedos", true);
		askClearLogs = config.getBoolean("questioner.askClearLogs", true);
		final List<String> worldNames = config.getStringList("loggedWorlds", null);
		worlds = new HashMap<Integer, WorldConfig>();
		if (worldNames == null || worldNames.size() == 0)
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
		}

	}
}

class WorldConfig
{
	public final String table;
	public final boolean logBlockPlacings, logBlockBreaks, logSignTexts, logExplosions, logFire, logLeavesDecay, logLavaFlow, logWaterFlow, logChestAccess, logButtonsAndLevers, logKills, logChat;

	public WorldConfig(File file) {
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
		final Configuration config = new Configuration(file);
		config.load();
		final List<String> keys = config.getKeys(null);
		for (final Entry<String, Object> e : def.entrySet())
			if (!keys.contains(e.getKey()))
				config.setProperty(e.getKey(), e.getValue());
		config.save();
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
	}
}
