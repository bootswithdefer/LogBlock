package de.diddiz.LogBlock;

import static de.diddiz.util.Utils.download;
import static de.diddiz.util.Utils.downloadIfNotExists;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.World;
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
import de.diddiz.util.MySQLConnectionPool;

// TODO Add painting logging
// TODO Add Button, lever etc logging

public class LogBlock extends JavaPlugin
{
	private Logger log;
	private Config config;
	private MySQLConnectionPool pool;
	private Consumer consumer = null;
	private CommandsHandler commandsHandler;
	private Updater updater = null;
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

	Updater getUpdater() {
		return updater;
	}

	@Override
	public void onLoad() {
		log = getServer().getLogger();
		try {
			updater = new Updater(this);
			log.info("[LogBlock] Version check: " + updater.checkVersion());
			config = new Config(this);
			downloadIfNotExists(log, new File("lib/mysql-connector-java-bin.jar"), new URL("http://diddiz.insane-architects.net/download/mysql-connector-java-bin.jar"));
			log.info("[LogBlock] Connecting to " + config.user + "@" + config.url + "...");
			pool = new MySQLConnectionPool(config.url, config.user, config.password);
			getConnection().close();
			if (updater.update())
				config = new Config(this);
			updater.checkTables();
		} catch (final Exception ex) {
			log.log(Level.SEVERE, "[LogBlock] Error while loading: ", ex);
			errorAtLoading = true;
			return;
		}
		consumer = new Consumer(this);
	}

	@Override
	public void onEnable() {
		final PluginManager pm = getServer().getPluginManager();
		if (errorAtLoading) {
			pm.disablePlugin(this);
			return;
		}
		if (pm.getPlugin("WorldEdit") == null && !new File("lib/WorldEdit.jar").exists() && !new File("WorldEdit.jar").exists())
			try {
				download(log, new URL("http://diddiz.insane-architects.net/download/WorldEdit.jar"), new File("lib/WorldEdit.jar"));
				log.info("[LogBlock] You've to restart/reload your server now.");
				pm.disablePlugin(this);
				return;
			} catch (final Exception ex) {
				log.warning("[LogBlock] Failed to download WorldEdit. You may have to download it manually. You don't have to install it, just place the jar in the lib folder.");
			}
		if (config.logChestAccess && pm.getPlugin("BukkitContrib") == null)
			try {
				download(log, new URL("http://bit.ly/autoupdateBukkitContrib"), new File("plugins/BukkitContrib.jar"));
				pm.loadPlugin(new File("plugins/BukkitContrib.jar"));
				pm.enablePlugin(pm.getPlugin("BukkitContrib"));
			} catch (final Exception ex) {
				log.warning("[LogBlock] Failed to install BukkitContrib, you may have to restart your server or install it manually.");
			}
		commandsHandler = new CommandsHandler(this);
		getCommand("lb").setExecutor(commandsHandler);
		if (pm.getPlugin("Permissions") != null) {
			permissions = ((Permissions)pm.getPlugin("Permissions")).getHandler();
			log.info("[LogBlock] Permissions found.");
		} else
			log.info("[LogBlock] Permissions plugin not found. Using default permissions.");
		if (config.keepLogDays >= 0) {
			final QueryParams params = new QueryParams(this);
			params.minutes = config.keepLogDays * -1440;
			params.bct = BlockChangeType.ALL;
			for (final World world : getServer().getWorlds())
				if (config.tables.containsKey(world.getName().hashCode())) {
					params.world = world;
					try {
						commandsHandler.new CommandClearLog(new ConsoleCommandSender(getServer()), params.clone(), true);
					} catch (final Exception ex) {
						log.severe("Failed to schedule ClearLog: " + ex.getMessage());
					}
				}
		}
		final LBBlockListener lbBlockListener = new LBBlockListener(this);
		final LBPlayerListener lbPlayerListener = new LBPlayerListener(this);
		final LBEntityListener lbEntityListener = new LBEntityListener(this);
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
			if (pm.getPlugin("BukkitContrib") != null)
				pm.registerEvent(Type.CUSTOM_EVENT, new LBChestAccessListener(this), Priority.Monitor, this);
			else
				log.warning("[LogBlock] BukkitContrib not found. Can't log chest accesses.");
		if (config.logLavaFlow)
			pm.registerEvent(Type.BLOCK_FROMTO, lbBlockListener, Priority.Monitor, this);
		if (config.logButtonsAndLevers)
			pm.registerEvent(Type.PLAYER_INTERACT, lbPlayerListener, Priority.Monitor, this);
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
		log.info("Logblock v" + getDescription().getVersion() + " by DiddiZ enabled.");
	}

	@Override
	public void onDisable() {
		if (timer != null)
			timer.cancel();
		getServer().getScheduler().cancelTasks(this);
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

	boolean hasPermission(CommandSender sender, String permission) {
		if (permissions != null && sender instanceof Player)
			return permissions.permission((Player)sender, permission);
		if (permission.equals("logblock.lookup") || permission.equals("logblock.hide") || permission.equals("logblock.rollback") || permission.equals("logblock.tp") || permission.equals("logblock.clearlog"))
			return sender.isOp();
		return true;
	}

	public Connection getConnection() {
		try {
			return pool.getConnection();
		} catch (final Exception ex) {
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
