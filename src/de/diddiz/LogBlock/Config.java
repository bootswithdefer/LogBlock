package de.diddiz.LogBlock;

import java.util.Arrays;
import java.util.List;

import org.bukkit.util.config.Configuration;

public class Config {
	static List<String> worldNames;
	static List<String> worldTables;
	static String dbDriver;
	static String dbUrl;
	static String dbUsername;
	static String dbPassword;
	static int keepLogDays;
	static int delay;
	static int defaultDist;
	static int defaultTime;
	static int toolID;
	static int toolblockID;
	static boolean toolblockRemove;
	static boolean logSignTexts;
	static boolean logExplosions;
	static boolean logFire;
	static boolean logChestAccess;
	static boolean usePermissions;
	
	static boolean Load(Configuration config) {
		config.load();
		List<String> keys = config.getKeys(null);
		if (!keys.contains("worldNames"))
			config.setProperty("worldNames", Arrays.asList(new String[]{"world"}));
		if (!keys.contains("worldTables"))
			config.setProperty("worldTables", Arrays.asList(new String[]{"lb-main"}));
		if (!keys.contains("driver"))
			config.setProperty("driver", "com.mysql.jdbc.Driver");
		if (!keys.contains("url"))
			config.setProperty("url", "jdbc:mysql://localhost:3306/db");
		if (!keys.contains("username"))
			config.setProperty("username", "user");
		if (!keys.contains("password"))
			config.setProperty("password", "pass");
		if (!keys.contains("keepLogDays"))
			config.setProperty("keepLogDays", -1);
		if (!keys.contains("delay"))
			config.setProperty("delay", 6);
		if (!keys.contains("defaultDist"))
			config.setProperty("defaultDist", 20);
		if (!keys.contains("defaultTime"))
			config.setProperty("defaultTime", "30 minutes");
		if (!keys.contains("toolID"))
			config.setProperty("toolID", 270);
		if (!keys.contains("toolblockID"))
			config.setProperty("toolblockID", 7);
		if (!keys.contains("toolblockRemove"))
			config.setProperty("toolblockRemove", true);
		if (!keys.contains("logSignTexts"))
			config.setProperty("logSignTexts", false);
		if (!keys.contains("logExplosions"))
			config.setProperty("logExplosions", false);
		if (!keys.contains("logFire"))
			config.setProperty("logFire", false);
		if (!keys.contains("logChestAccess"))
			config.setProperty("logChestAccess", false);
		if (!keys.contains("usePermissions"))
			config.setProperty("usePermissions", false);
		if (!config.save()){
			LogBlock.log.severe("[LogBlock] Error while writing to config.yml");
			return false;
		}
		worldNames = config.getStringList("worldNames", null);
		worldTables = config.getStringList("worldTables", null);
		dbDriver = config.getString("driver");
		dbUrl = config.getString("url");
		dbUsername = config.getString("username");
		dbPassword = config.getString("password");
		keepLogDays = config.getInt("keepLogDays", -1);
		delay = config.getInt("delay", 6);
		defaultDist = config.getInt("defaultDist", 20);
		defaultTime = LogBlock.parseTimeSpec(config.getString("defaultTime"));
		toolID = config.getInt("toolID", 270);
		toolblockID = config.getInt("toolblockID", 7);
		toolblockRemove = config.getBoolean("toolblockRemove", true);
		logSignTexts = config.getBoolean("logSignTexts", false);
		logExplosions = config.getBoolean("logExplosions", false);
		logFire = config.getBoolean("logFire", false);
		logChestAccess  = config.getBoolean("logChestAccess", false);
		usePermissions = config.getBoolean("usePermissions", false);
		return true;
	}
}
