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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

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

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)	{
		if (!cmd.getName().equalsIgnoreCase("lb"))
			return false;
		if (!(sender instanceof Player)) {
			sender.sendMessage("You aren't a player");
			return true;
		}
		final Player player = (Player)sender;
		if (args.length == 0) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock v" + getDescription().getVersion() + " by DiddiZ");
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Type /lb help for help");
		} else if (args[0].equalsIgnoreCase("tool")) {
			if (checkPermission(player, "logblock.tool")) {
				if (player.getInventory().contains(config.toolID))
					player.sendMessage(ChatColor.RED + "You have alredy a tool");
				else {
					final int free = player.getInventory().firstEmpty();
					if (free >= 0) {
						player.getInventory().setItem(free, player.getItemInHand());
						player.setItemInHand(new ItemStack(config.toolID, 1));
						player.sendMessage(ChatColor.GREEN + "Here is your tool.");
					} else
						player.sendMessage(ChatColor.RED + "You have no empty slot in your inventory");
				}
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("toolblock")) {
			if (checkPermission(player, "logblock.toolblock")) {
				if (player.getInventory().contains(config.toolblockID))
					player.sendMessage(ChatColor.RED + "You have alredy a tool");
				else {
					final int free = player.getInventory().firstEmpty();
					if (free >= 0) {
						player.getInventory().setItem(free, player.getItemInHand());
						player.setItemInHand(new ItemStack(config.toolblockID, 1));
						player.sendMessage(ChatColor.GREEN + "Here's your tool.");
					} else
						player.sendMessage(ChatColor.RED + "You have no empty slot in your inventory");
				}
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("hide")) {
			if (checkPermission(player, "logblock.hide")) {
				if (consumer.hide(player))
					player.sendMessage(ChatColor.GREEN + "You are now hided and won't appear in any log. Type '/lb hide' again to unhide");
				else
					player.sendMessage(ChatColor.GREEN + "You aren't hided anylonger.");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("savequeue")) {
			if (checkPermission(player, "logblock.rollback")) {
				player.sendMessage(ChatColor.DARK_AQUA + "Current queue size: " + consumer.getQueueSize());
				final Thread thread = new Thread(consumer);
				while (consumer.getQueueSize() > 0) {
					thread.run();
				}
				player.sendMessage(ChatColor.GREEN + "Queue saved successfully");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("area")) {
			if (checkPermission(player,"logblock.area")) {
				int radius = config.defaultDist;
				if (args.length == 2 && isInt(args[1]))
					radius = Integer.parseInt(args[1]);
				new Thread(new AreaStats(this, player, radius)).start();
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("world")) {
			if (checkPermission(player,"logblock.area"))
				new Thread(new AreaStats(this, player, Short.MAX_VALUE)).start();
			else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("player")) {
			if (checkPermission(player,"logblock.area")) {
				if (args.length == 2 || args.length == 3) {
					int radius = config.defaultDist;
					if (args.length == 3 && isInt(args[2]))
						radius = Integer.parseInt(args[2]);
					new Thread(new PlayerAreaStats(this, player, args[1], radius)).start();
				} else
					player.sendMessage(ChatColor.RED + "Usage: /lb player [name] <radius>");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("block")) {
			if (checkPermission(player,"logblock.area")) {
				if (args.length == 2 || args.length == 3) {
					final Material mat = Material.matchMaterial(args[1]);
					int radius = config.defaultDist;
					if (args.length == 3 && isInt(args[2]))
						radius = Integer.parseInt(args[2]);
					if (mat != null)
						new Thread(new AreaBlockSearch(this, player, mat.getId(), radius)).start();
					else
						player.sendMessage(ChatColor.RED + "Can't find any item like '" + args[1] + "'");
				} else
					player.sendMessage(ChatColor.RED + "Usage: /lb block [type] <radius>");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("rollback") || args[0].equalsIgnoreCase("undo")) {
			if (checkPermission(player,"logblock.rollback")) {
				if (args.length >= 2) {
					int minutes = config.defaultTime;
					if (args[1].equalsIgnoreCase("player")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							player.sendMessage(ChatColor.GREEN + "Rolling back " + args[2] + " by " + minutes + " minutes.");
							getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, args[2], -1, null, minutes, false));
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb rollback player [name] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("area")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							if (isInt(args[2])) {
								player.sendMessage(ChatColor.GREEN + "Rolling back area within " + args[2] + " blocks of you by " + minutes + " minutes.");
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, null, Integer.parseInt(args[2]), null, minutes, false));
							} else
								player.sendMessage(ChatColor.RED + "Can't parse to an int: " + args[2]);
						} else
							player.sendMessage(ChatColor.RED + "Usage /lb rollback area [radius] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("playerarea")) {
						if (args.length == 4 || args.length == 6) {
							if (args.length == 6)
								minutes = parseTimeSpec(args[4], args[5]);
							if (isInt(args[3])) {
								player.sendMessage(ChatColor.GREEN + "Rolling back " + args[2] + " within " + args[3] + " blocks by " + minutes + " minutes.");
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, args[2], Integer.parseInt(args[3]), null, minutes, false));
							} else
								player.sendMessage(ChatColor.RED + "Can't parse to an int: " + args[3]);
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb rollback playerarea [player] [radius] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("selection")) {
						if (args.length == 2 || args.length == 4) {
							if (args.length == 4)
								minutes = parseTimeSpec(args[2], args[3]);
							final Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
							if (we != null) {
								final Selection sel = ((WorldEditPlugin)we).getSelection(player);
								if (sel != null) {
									if (sel instanceof CuboidSelection) {
										player.sendMessage(ChatColor.GREEN + "Rolling back selection by " + minutes + " minutes.");
										getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, null, -1, sel, minutes, false));
									} else
										player.sendMessage(ChatColor.RED + "You have to define a cuboid selection");
								} else
									player.sendMessage(ChatColor.RED + "No selection defined");
							} else
								player.sendMessage(ChatColor.RED + "WorldEdit plugin not found");
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb rollback selection <time> <minutes|hours|days>");
					} else
						player.sendMessage(ChatColor.RED + "Wrong rollback mode");
				} else {
					player.sendMessage(ChatColor.RED + "Usage: ");
					player.sendMessage(ChatColor.RED + "/lb rollback player [name] <time> <minutes|hours|days>");
					player.sendMessage(ChatColor.RED + "/lb rollback area [radius] <time> <minutes|hours|days>");
					player.sendMessage(ChatColor.RED + "/lb rollback playerarea [name] [radius] <time> <minutes|hours|days>");
					player.sendMessage(ChatColor.RED + "/lb rollback selection <time> <minutes|hours|days>");
				}
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("redo")) {
			if (checkPermission(player,"logblock.rollback")) {
				if (args.length >= 2) {
					int minutes = config.defaultTime;
					if (args[1].equalsIgnoreCase("player")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							player.sendMessage(ChatColor.GREEN + "Redoing " + args[2] + " for " + minutes + " minutes.");
							getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, args[2], -1, null, minutes, true));
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb redo player [name] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("area")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							if (isInt(args[2])) {
								player.sendMessage(ChatColor.GREEN + "Redoing area within " + args[2] + " blocks of you for " + minutes + " minutes.");
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, null, Integer.parseInt(args[2]), null, minutes, true));
							} else
								player.sendMessage(ChatColor.RED + "Can't parse to an int: " + args[2]);
						} else
							player.sendMessage(ChatColor.RED + "Usage /lb redo area [radius] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("playerarea")) {
						if (args.length == 4 || args.length == 6) {
							if (args.length == 6)
								minutes = parseTimeSpec(args[4], args[5]);
							if (isInt(args[3])) {
								player.sendMessage(ChatColor.GREEN + "Redoing " + args[2] + " within " + args[3] + " blocks for " + minutes + " minutes.");
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, args[2], Integer.parseInt(args[3]), null, minutes, true));
							} else
								player.sendMessage(ChatColor.RED + "Can't parse to an int: " + args[3]);
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb redo playerarea [player] [radius] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("selection")) {
						if (args.length == 2 || args.length == 4) {
							if (args.length == 4)
								minutes = parseTimeSpec(args[2], args[3]);
							final Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
							if (we != null) {
								final Selection sel = ((WorldEditPlugin)we).getSelection(player);
								if (sel != null) {
									if (sel instanceof CuboidSelection) {
										player.sendMessage(ChatColor.GREEN + "Redoing selection for " + minutes + " minutes.");
										getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(this, player, null, -1, sel, minutes, true));
									} else
										player.sendMessage(ChatColor.RED + "You have to define a cuboid selection");
								} else
									player.sendMessage(ChatColor.RED + "No selection defined");
							} else
								player.sendMessage(ChatColor.RED + "WorldEdit plugin not found");
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb redo selection <time> <minutes|hours|days>");
					} else
						player.sendMessage(ChatColor.RED + "Wrong redo mode");
				} else {
					player.sendMessage(ChatColor.RED + "Usage:");
					player.sendMessage(ChatColor.RED + "/lb redo player [name] <time> <minutes|hours|days>");
					player.sendMessage(ChatColor.RED + "/lb redo area [radius] <time> <minutes|hours|days>");
					player.sendMessage(ChatColor.RED + "/lb redo playerarea [name] [radius] <time> <minutes|hours|days>");
					player.sendMessage(ChatColor.RED + "/lb redo selection <time> <minutes|hours|days>");
				}
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("writelogfile")) {
			if (checkPermission(player,"logblock.rollback")) {
				if (args.length == 2)
					new Thread(new WriteLogFile(this, player, args[1])).start();
				else
					player.sendMessage(ChatColor.RED + "Usage: /lb writelogfile [name]");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("me")) {
			if (checkPermission(player,"logblock.me"))
				new Thread(new PlayerAreaStats(this, player, player.getName(), Short.MAX_VALUE)).start();
			else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("help")) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock Commands:");
			if (checkPermission(player, "logblock.me"))
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb me");
			if (checkPermission(player, "logblock.area")) {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb area <radius>");
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb world");
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb player [name] <radius>");
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb block [type] <radius>");
			}
			if (checkPermission(player, "logblock.rollback")) {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb rollback [rollback mode]");
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb redo [redo mode]");
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb writelogfile [player]");
			}
			if (checkPermission(player, "logblock.hide"))
				player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb hide");
		} else
			player.sendMessage(ChatColor.RED + "Wrong argument. Type /lb help for help");
		return true;
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

	private boolean isInt(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (final NumberFormatException ex) {
			return false;
		}
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
