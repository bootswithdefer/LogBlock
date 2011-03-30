package de.diddiz.LogBlock;

import java.util.Arrays;
import java.util.List;

import org.bukkit.util.config.Configuration;

public class Config {
	List<String> worldNames;
	List<String> worldTables;
	String dbDriver;
	String dbUrl;
	String dbUsername;
	String dbPassword;
	int keepLogDays;
	int delay;
	int defaultDist;
	int defaultTime;
	int toolID;
	int toolblockID;
	boolean toolblockRemove;
	boolean logSignTexts;
	boolean logExplosions;
	boolean logFire;
	boolean logLeavesDecay;
	boolean logChestAccess;
	String logTNTExplosionsAs;
	String logCreeperExplosionsAs;
	String logFireballExplosionsAs;
	String logFireAs;
	String logLeavesDecayAs;
	boolean usePermissions;

	Config (LogBlock logblock) {
		Configuration config = logblock.getConfiguration();
		config.load();
		List<String> keys = config.getKeys(null);
		if (!keys.contains("version"))
			config.setProperty("version", logblock.getDescription().getVersion());
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
		if (!keys.contains("logLeavesDecay"))
			config.setProperty("logLeavesDecay", false);
		if (!keys.contains("logTNTExplosionsAs"))
			config.setProperty("logTNTExplosionsAs", "TNT");
		if (!keys.contains("logCreeperExplosionsAs"))
			config.setProperty("logCreeperExplosionsAs", "Creeper");
		if (!keys.contains("logFireballExplosionsAs"))
			config.setProperty("logFireballExplosionsAs", "Ghast");
		if (!keys.contains("logFireAs"))
			config.setProperty("logFireAs", "Fire");
		if (!keys.contains("logLeavesDecayAs"))
			config.setProperty("logLeavesDecayAs", "LeavesDecay");
		if (!keys.contains("usePermissions"))
			config.setProperty("usePermissions", false);
		if (!config.save())
			LogBlock.log.severe("[LogBlock] Error while writing to config.yml");
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
		logChestAccess = config.getBoolean("logChestAccess", false);
		logLeavesDecay = config.getBoolean("logLeavesDecay", false);
		logTNTExplosionsAs = config.getString("logTNTExplosionsAs");
		logCreeperExplosionsAs = config.getString("logCreeperExplosionsAs");
		logFireballExplosionsAs = config.getString("logFireballExplosionsAs");
		logFireAs = config.getString("logFireAs");
		logLeavesDecayAs = config.getString("logLeavesDecayAs");
		usePermissions = config.getBoolean("usePermissions", false);
	}
}
