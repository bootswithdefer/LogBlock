package de.diddiz.LogBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.util.config.Configuration;

public class Config {
	final HashMap<Integer, String> tables;
	final String url;
	final String user;
	final String password;
	final int delay;
	final boolean useBukkitScheduler;
	final int keepLogDays;
	final boolean dumpDeletedLog;
	final boolean logBlockCreations;
	final boolean logBlockDestroyings;
	final boolean logSignTexts;
	final boolean logExplosions;
	final boolean logFire;
	final boolean logLeavesDecay;
	final boolean logChestAccess;
	final List<Integer> dontRollback;
	final List<Integer> replaceAnyway;
	final int defaultDist;
	final int defaultTime;
	final int toolID;
	final int toolblockID;

	Config (LogBlock logblock) throws Exception {
		Configuration config = logblock.getConfiguration();
		config.load();
		List<String> keys = config.getKeys(null);
		List<String> subkeys;
		if (!keys.contains("version"))
			config.setProperty("version", logblock.getDescription().getVersion());
		if (!keys.contains("loggedWorlds"))
			config.setProperty("loggedWorlds", Arrays.asList(new String[]{"world", "nether"}));
		if (!keys.contains("tables"))
			config.setProperty("tables", Arrays.asList(new String[]{"lb-main", "lb-hell"}));
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
		if (!subkeys.contains("delay"))
			config.setProperty("consumer.delay", 6);
		if (!subkeys.contains("useBukkitScheduler"))
			config.setProperty("consumer.useBukkitScheduler", true);
		subkeys = config.getKeys("clearlog");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("dumpDeletedLog"))
			config.setProperty("clearlog.dumpDeletedLog", true);
		if (!subkeys.contains("keepLogDays"))
			config.setProperty("clearlog.keepLogDays", -1);
		subkeys = config.getKeys("logging");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("logBlockCreations"))
			config.setProperty("logging.logBlockCreations", true);
		if (!subkeys.contains("logBlockDestroyings"))
			config.setProperty("logging.logBlockDestroyings", true);
		if (!subkeys.contains("logSignTexts"))
			config.setProperty("logging.logSignTexts", false);
		if (!subkeys.contains("logExplosions"))
			config.setProperty("logging.logExplosions", false);
		if (!subkeys.contains("logFire"))
			config.setProperty("logging.logFire", false);
		if (!subkeys.contains("logChestAccess"))
			config.setProperty("logging.logChestAccess", false);
		if (!subkeys.contains("logLeavesDecay"))
			config.setProperty("logging.logLeavesDecay", false);
		subkeys = config.getKeys("rollback");
		if (subkeys == null)
			subkeys = new ArrayList<String>();
		if (!subkeys.contains("dontRollback"))
			config.setProperty("rollback.dontRollback", Arrays.asList(new Integer[]{10, 11, 46, 51}));
		if (!subkeys.contains("replaceAnyway"))
			config.setProperty("rollback.replaceAnyway", Arrays.asList(new Integer[]{8, 9, 10, 11, 51}));
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
		if (!config.save())
			throw new Exception("Error while writing to config.yml");
		url = "jdbc:mysql://" + config.getString("mysql.host") + ":" + config.getString("mysql.port") + "/" + config.getString("mysql.database");
		user = config.getString("mysql.user");
		password = config.getString("mysql.password");
		delay = config.getInt("consumer.delay", 6);
		useBukkitScheduler = config.getBoolean("consumer.useBukkitScheduler", true);
		keepLogDays = config.getInt("clearlog.keepLogDays", -1);
		dumpDeletedLog =  config.getBoolean("clearlog.dumpDeletedLog", true);
		logBlockCreations = config.getBoolean("logging.logBlockCreations", true);
		logBlockDestroyings = config.getBoolean("logging.logBlockDestroyings", true);
		logSignTexts = config.getBoolean("logging.logSignTexts", false);
		logExplosions = config.getBoolean("logging.logExplosions", false);
		logFire = config.getBoolean("logging.logFire", false);
		logChestAccess = config.getBoolean("logging.logChestAccess", false);
		logLeavesDecay = config.getBoolean("logging.logLeavesDecay", false);
		dontRollback = config.getIntList("rollback.dontRollback", null);
		replaceAnyway = config.getIntList("rollback.replaceAnyway", null);
		defaultDist = config.getInt("lookup.defaultDist", 20);
		defaultTime = LogBlock.parseTimeSpec(config.getString("lookup.defaultTime"));
		toolID = config.getInt("lookup.toolID", 270);
		if (Material.getMaterial(toolID) == null || Material.getMaterial(toolID).isBlock())
			throw new Exception("lookup.toolID doesn't appear to be a valid item id");
		toolblockID = config.getInt("lookup.toolblockID", 7);
		if (Material.getMaterial(toolblockID) == null || !Material.getMaterial(toolblockID).isBlock() || toolblockID == 0)
			throw new Exception("lookup.toolblockID doesn't appear to be a valid block id");
		List<String> worldNames = config.getStringList("loggedWorlds", null);
		List<String> worldTables = config.getStringList("tables", null);
		tables = new HashMap<Integer, String>();
		if (worldNames == null || worldTables == null || worldNames.size() == 0 || worldNames.size() != worldTables.size())
			throw new Exception("worldNames or worldTables not set porperly");
		for (int i = 0; i < worldNames.size(); i++) {
			tables.put(worldNames.get(i).hashCode(), worldTables.get(i));
		}
	}
}
