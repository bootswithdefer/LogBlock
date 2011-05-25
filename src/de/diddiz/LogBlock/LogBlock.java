package de.diddiz.LogBlock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import de.diddiz.LogBlock.QueryParams.BlockChangeType;
import de.diddiz.util.ConnectionPool;
import de.diddiz.util.Utils;

// TODO Add painting logging
// TODO Add Button, lever etc logging

public class LogBlock extends JavaPlugin
{
	private Logger log;
	private Config config;
	private ConnectionPool pool;
	private Consumer consumer = null;
	private CommandsHandler commandsHandler;
	private Timer timer = null;
	private PermissionHandler permissions = null;
	private boolean errorAtLoading = false;
	private final Map<Integer, Session> sessions = new HashMap<Integer, Session>();

	public Config getConfig() {
		return config;
	}

	public Consumer getConsumer() {
		return consumer;
	}

	public CommandsHandler getCommandsHandler() {
		return commandsHandler;
	}

	@Override
	public void onLoad() {
		log = getServer().getLogger();
		try {
			config = new Config(this);
		} catch (final Exception ex) {
			log.log(Level.SEVERE, "[LogBlock] Exception while reading config:", ex);
			errorAtLoading = true;
			return;
		}
		final File file = new File("lib/mysql-connector-java-bin.jar");
		try {
			if (!file.exists() || file.length() == 0) {
				log.info("[LogBlock] Downloading " + file.getName() + "...");
				Utils.download(new URL("http://diddiz.insane-architects.net/download/mysql-connector-java-bin.jar"), file);
			}
			if (!file.exists() || file.length() == 0)
				throw new FileNotFoundException(file.getAbsolutePath() + file.getName());
		} catch (final IOException e) {
			log.log(Level.SEVERE, "[LogBlock] Error while downloading " + file.getName() + ".");
			errorAtLoading = true;
			return;
		}
		try {
			log.info("[LogBlock] Connecting to " + config.user + "@" + config.url + "...");
			pool = new ConnectionPool(config.url, config.user, config.password);
			getConnection().close();
		} catch (final Exception ex) {
			log.log(Level.SEVERE, "[LogBlock] Exception while checking database connection", ex);
			errorAtLoading = true;
			return;
		}
		if (!checkTables()) {
			log.log(Level.SEVERE, "[LogBlock] Errors while checking tables. They may not exist.");
			errorAtLoading = true;
			return;
		}
		consumer = new Consumer(this);
	}

	@Override
	public void onEnable() {
		if (errorAtLoading) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		commandsHandler = new CommandsHandler(this);
		getCommand("lb").setExecutor(commandsHandler);
		if (getServer().getPluginManager().getPlugin("Permissions") != null) {
			permissions = ((Permissions)getServer().getPluginManager().getPlugin("Permissions")).getHandler();
			log.info("[LogBlock] Permissions found.");
		} else
			log.info("[LogBlock] Permissions plugin not found. Using default permissions.");
		if (config.keepLogDays >= 0) {
			final QueryParams params = new QueryParams(this);
			params.minutes = config.keepLogDays * -1440;
			params.bct = BlockChangeType.ALL;
			try {
				commandsHandler.new CommandClearLog(new ConsoleCommandSender(getServer()), params);
			} catch (final Exception ex) {
				log.severe("Failed to schedule ClearLog: " + ex.getMessage());
			}
		}
		final LBBlockListener lbBlockListener = new LBBlockListener(this);
		final LBPlayerListener lbPlayerListener = new LBPlayerListener(this);
		final LBEntityListener lbEntityListener = new LBEntityListener(this);
		final PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_INTERACT, new LBToolListener(this), Priority.Normal, this);
		if (config.logBlockCreations) {
			pm.registerEvent(Type.BLOCK_PLACE, lbBlockListener, Priority.Monitor, this);
			pm.registerEvent(Type.PLAYER_BUCKET_EMPTY, lbPlayerListener, Priority.Monitor, this);
		}
		if (config.logBlockDestroyings) {
			pm.registerEvent(Type.BLOCK_BREAK, lbBlockListener, Priority.Monitor, this);
			pm.registerEvent(Type.PLAYER_BUCKET_FILL, lbPlayerListener, Priority.Monitor, this);
		}
		if (config.logSignTexts)
			pm.registerEvent(Type.SIGN_CHANGE, lbBlockListener, Priority.Monitor, this);
		if (config.logFire)
			pm.registerEvent(Type.BLOCK_BURN, lbBlockListener, Priority.Monitor, this);
		if (config.logExplosions)
			pm.registerEvent(Type.ENTITY_EXPLODE, lbEntityListener, Priority.Monitor, this);
		if (config.logLeavesDecay)
			pm.registerEvent(Type.LEAVES_DECAY, lbBlockListener, Priority.Monitor, this);
		if (config.logChestAccess)
			if (getServer().getPluginManager().getPlugin("BukkitContrib") != null)
				getServer().getPluginManager().registerEvent(Type.CUSTOM_EVENT, new LBChestAccessListener(this), Priority.Monitor, this);
			else
				log.warning("[LogBlock] BukkitContrib not found. Can't log chest accesses.");
		if (config.logLavaFlow)
			pm.registerEvent(Type.BLOCK_FROMTO, lbBlockListener, Priority.Monitor, this);
		if (config.logKills)
			pm.registerEvent(Type.ENTITY_DAMAGE, lbEntityListener, Priority.Monitor, this);
		if (config.useBukkitScheduler) {
			if (getServer().getScheduler().scheduleAsyncRepeatingTask(this, consumer, config.delayBetweenRuns * 20, config.delayBetweenRuns * 20) > 0)
				log.info("[LogBlock] Scheduled consumer with bukkit scheduler.");
			else {
				log.warning("[LogBlock] Failed to schedule consumer with bukkit scheduler. Now trying schedule with timer.");
				timer = new Timer();
				timer.scheduleAtFixedRate(consumer, config.delayBetweenRuns * 1000, config.delayBetweenRuns * 1000);
			}
		} else {
			timer = new Timer();
			timer.scheduleAtFixedRate(consumer, config.delayBetweenRuns * 1000, config.delayBetweenRuns * 1000);
			log.info("[LogBlock] Scheduled consumer with timer.");
		}
		log.info("Logblock v" + getDescription().getVersion() + " enabled.");
	}

	@Override
	public void onDisable() {
		if (timer != null)
			timer.cancel();
		if (consumer != null && consumer.getQueueSize() > 0) {
			log.info("[LogBlock] Waiting for consumer ...");
			final Thread thread = new Thread(consumer);
			while (consumer.getQueueSize() > 0) {
				log.info("[LogBlock] Remaining queue size: " + consumer.getQueueSize());
				thread.run();
			}
		}
		if (pool != null)
			pool.close();
		log.info("LogBlock disabled.");
	}

	private boolean checkTables() {
		final Connection conn = getConnection();
		Statement state = null;
		if (conn == null)
			return false;
		try {
			final DatabaseMetaData dbm = conn.getMetaData();
			state = conn.createStatement();
			if (!dbm.getTables(null, null, "lb-players", null).next()) {
				log.log(Level.INFO, "[LogBlock] Crating table lb-players.");
				state.execute("CREATE TABLE `lb-players` (playerid SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL DEFAULT '-', PRIMARY KEY (playerid), UNIQUE (playername))");
				if (!dbm.getTables(null, null, "lb-players", null).next())
					return false;
			}
			for (final String table : config.tables.values()) {
				if (!dbm.getTables(null, null, table, null).next()) {
					log.log(Level.INFO, "[LogBlock] Crating table " + table + ".");
					state.execute("CREATE TABLE `" + table + "` (id INT NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, replaced TINYINT UNSIGNED NOT NULL, type TINYINT UNSIGNED NOT NULL, data TINYINT UNSIGNED NOT NULL, x SMALLINT NOT NULL, y TINYINT UNSIGNED NOT NULL, z SMALLINT NOT NULL, PRIMARY KEY (id), KEY coords (y, x, z), KEY date (date))");
					if (!dbm.getTables(null, null, table, null).next())
						return false;
				}
				if (!dbm.getTables(null, null, table + "-sign", null).next()) {
					log.log(Level.INFO, "[LogBlock] Crating table " + table + "-sign.");
					state.execute("CREATE TABLE `" + table + "-sign` (id INT NOT NULL, signtext TEXT, PRIMARY KEY (id));");
					if (!dbm.getTables(null, null, table + "-sign", null).next())
						return false;
				}
				if (!dbm.getTables(null, null, table + "-chest", null).next()) {
					log.log(Level.INFO, "[LogBlock] Crating table " + table + "-chest.");
					state.execute("CREATE TABLE `" + table + "-chest` (id INT NOT NULL, itemtype SMALLINT UNSIGNED NOT NULL, itemamount SMALLINT NOT NULL, itemdata TINYINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
					if (!dbm.getTables(null, null, table + "-chest", null).next())
						return false;
				}
				if (config.logKills && !dbm.getTables(null, null, table + "-kills", null).next()) {
					log.log(Level.INFO, "[LogBlock] Crating table " + table + "-kills.");
					state.execute("CREATE TABLE `" + table + "-kills` (id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer SMALLINT UNSIGNED, victim SMALLINT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, PRIMARY KEY (id));");
					if (!dbm.getTables(null, null, table + "-kills", null).next())
						return false;
				}
			}
			return true;
		} catch (final SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] SQL exception while checking tables", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				conn.close();
			} catch (final SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
			}
		}
		return false;
	}

	boolean hasPermission(CommandSender sender, String permission) {
		if (permissions != null && sender instanceof Player)
			return permissions.permission((Player)sender, permission);
		if (permission.equals("logblock.lookup") || permission.equals("logblock.hide") || permission.equals("logblock.rollback") || permission.equals("logblock.tp") || permission.equals("logblock.clearlog"))
			return sender.isOp();
		return true;
	}

	Connection getConnection() {
		try {
			return pool.getConnection();
		} catch (final SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] Error while fetching connection", ex);
			return null;
		}
	}

	public Session getSession(String playerName) {
		Session session = sessions.get(playerName.hashCode());
		if (session == null) {
			session = new Session(this);
			sessions.put(playerName.hashCode(), session);
		}
		return session;
	}
}
