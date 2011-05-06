package de.diddiz.LogBlock;

import java.io.File;
import java.io.FileNotFoundException;
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
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import de.diddiz.util.ConnectionPool;
import de.diddiz.util.Download;

public class LogBlock extends JavaPlugin
{
	private Logger log;
	private Config config;
	private ConnectionPool pool;
	private Consumer consumer = null;
	private Timer timer = null;
	private PermissionHandler permissions = null;
	private boolean errorAtLoading = false;
	private Map<Integer, Session> sessions = new HashMap<Integer, Session>();

	public Config getConfig() {
		return config;
	}

	public Consumer getConsumer() {
		return consumer;
	}

	@Override
	public void onLoad() {
		log = getServer().getLogger();
		try	{
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
				Download.download(new URL("http://diddiz.insane-architects.net/download/mysql-connector-java-bin.jar"), file);
			}
			if (!file.exists() || file.length() == 0)
				throw new FileNotFoundException(file.getAbsolutePath() + file.getName());
		} catch (final Exception e) {
			log.log(Level.SEVERE, "[LogBlock] Error while downloading " + file.getName() + ".");
			errorAtLoading = true;
			return;
		}
		try {
			log.info("[LogBlock] Connecting to " + config.user + "@" + config.url + "...");
			pool = new ConnectionPool(config.url, config.user, config.password);
			final Connection conn = getConnection();
			conn.close();
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
		getCommand("lb").setExecutor(new CommandsHandler(this));
		if (getServer().getPluginManager().getPlugin("Permissions") != null) {
			permissions = ((Permissions)getServer().getPluginManager().getPlugin("Permissions")).getHandler();
			log.info("[LogBlock] Permissions enabled");
		} else
			log.info("[LogBlock] Permissions plugin not found. Using default permissions.");
		if (config.keepLogDays >= 0)
			new Thread(new ClearLog(this)).start();
		final LBBlockListener lbBlockListener = new LBBlockListener(this);
		final LBPlayerListener lbPlayerListener = new LBPlayerListener(this);
		final LBEntityListener lbEntityListener = new LBEntityListener(this);
		final PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_INTERACT, new LBToolPlayerListener(this), Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_JOIN, lbPlayerListener, Priority.Monitor, this);
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
			pm.registerEvent(Type.PLAYER_INTERACT, lbPlayerListener, Priority.Monitor, this);
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
			pool.closeConnections();
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
			if (!dbm.getTables(null, null, "lb-players", null).next())	{
				log.log(Level.INFO, "[LogBlock] Crating table lb-players.");
				state.execute("CREATE TABLE `lb-players` (playerid SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL DEFAULT '-', PRIMARY KEY (playerid), UNIQUE (playername))");
				if (!dbm.getTables(null, null, "lb-players", null).next())
					return false;
			}
			state.execute("INSERT IGNORE INTO `lb-players` (playername) VALUES ('TNT'), ('Creeper'), ('Fire'), ('LeavesDecay'), ('Ghast'), ('LavaFlow'), ('Environment'), ('Chicken'), ('Cow'), ('Giant'), ('Pig'), ('PigZombie'), ('Sheep'), ('Skeleton'), ('Slime'), ('Spider'), ('Squid'), ('Wolf'), ('Zombie')");
			for (final String table : config.tables.values()) {
				if (!dbm.getTables(null, null, table, null).next())	{
					log.log(Level.INFO, "[LogBlock] Crating table " + table + ".");
					state.execute("CREATE TABLE `" + table + "` (id INT NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL DEFAULT '0000-00-00 00:00:00', playerid SMALLINT UNSIGNED NOT NULL DEFAULT '0', replaced TINYINT UNSIGNED NOT NULL DEFAULT '0', type TINYINT UNSIGNED NOT NULL DEFAULT '0', data TINYINT UNSIGNED NOT NULL DEFAULT '0', x SMALLINT NOT NULL DEFAULT '0', y TINYINT UNSIGNED NOT NULL DEFAULT '0', z SMALLINT NOT NULL DEFAULT '0', PRIMARY KEY (id), KEY coords (y, x, z), KEY date (date));");
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
					state.execute("CREATE TABLE `" + table + "-chest` (id INT NOT NULL, intype SMALLINT UNSIGNED NOT NULL DEFAULT '0', inamount TINYINT UNSIGNED NOT NULL DEFAULT '0', outtype SMALLINT UNSIGNED NOT NULL DEFAULT '0', outamount TINYINT UNSIGNED NOT NULL DEFAULT '0', PRIMARY KEY (id));");
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
				if (conn != null)
					conn.close();
			} catch (final SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
			}
		}
		return false;
	}

	boolean checkPermission(CommandSender sender, String permission) {
		if (permissions != null && sender instanceof Player)
			return permissions.permission((Player)sender, permission);
		else {
			if (permission.equals("logblock.area"))
				return sender.isOp();
			else if (permission.equals("logblock.hide"))
				return sender.isOp();
			else if (permission.equals("logblock.rollback"))
				return sender.isOp();
			return true;
		}
	}

	static int parseTimeSpec(String timespec) {
		final String[] split = timespec.split(" ");
		if (split.length != 2)
			return 0;
		return parseTimeSpec(split[0], split[1]);
	}

	static int parseTimeSpec(String time, String unit) {
		int min;
		try {
			min = Integer.parseInt(time);
		} catch (final NumberFormatException ex) {
			return 0;
		}
		if (unit.startsWith("hour"))
			min *= 60;
		else if (unit.startsWith("day"))
			min *= 60*24;
		return min;
	}

	public Connection getConnection() {
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
			session = new Session();
			sessions.put(playerName.hashCode(), session);
		}
		return session;
	}
}
