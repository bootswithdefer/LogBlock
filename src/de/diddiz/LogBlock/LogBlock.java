package de.diddiz.LogBlock;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

import de.diddiz.util.ConnectionPool;
import de.diddiz.util.Download;

public class LogBlock extends JavaPlugin
{
	public static Logger log;
	public static Config config;
	public ConnectionPool pool;
	private Consumer consumer = null;
	private Timer timer = null;

	@Override
	public void onEnable() {
		log = getServer().getLogger();
		try	{
			config = new Config(this);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "[LogBlock] Exception while reading config", ex);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}	
		if (config.usePermissions)	{
			if (getServer().getPluginManager().getPlugin("Permissions") != null) 
				log.info("[LogBlock] Permissions enabled");
			else {
				config.usePermissions = false;
				log.warning("[LogBlock] Permissions plugin not found. Using default permissions.");
			}
		}
		File file = new File("lib/mysql-connector-java-bin.jar");
		try {
			if (!file.exists() || file.length() == 0) {
				log.info("[LogBlock] Downloading " + file.getName() + "...");
				Download.download(new URL("http://diddiz.insane-architects.net/download/mysql-connector-java-bin.jar"), file);
			}
			if (!file.exists() || file.length() == 0)
				throw new FileNotFoundException(file.getAbsolutePath() + file.getName());
		} catch (Exception e) {
			log.log(Level.SEVERE, "[LogBlock] Error while downloading " + file.getName() + ".");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		try {
			pool = new ConnectionPool(config.dbDriver, config.dbUrl, config.dbUsername, config.dbPassword);
			Connection conn = pool.getConnection();
			conn.close();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "[LogBlock] Exception while checking database connection", ex);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (!checkTables()) {
			log.log(Level.SEVERE, "[LogBlock] Errors while checking tables. They may not exist.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (config.keepLogDays >= 0)
			new Thread(new ClearLog(this)).start();
		LBBlockListener lbBlockListener = new LBBlockListener();
		LBPlayerListener lbPlayerListener = new LBPlayerListener();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_INTERACT, new LBToolPlayerListener(), Event.Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_INTERACT, lbPlayerListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_BUCKET_FILL, lbPlayerListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_BUCKET_EMPTY, lbPlayerListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_JOIN, lbPlayerListener, Event.Priority.Normal, this);
		pm.registerEvent(Type.BLOCK_BREAK, lbBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_PLACE, lbBlockListener, Event.Priority.Monitor, this);
		if (config.logSignTexts)
			pm.registerEvent(Type.SIGN_CHANGE, lbBlockListener, Event.Priority.Monitor, this);
		if (config.logFire)
			pm.registerEvent(Type.BLOCK_BURN, lbBlockListener, Event.Priority.Monitor, this);
		if (config.logExplosions) 
			pm.registerEvent(Type.ENTITY_EXPLODE, new LBEntityListener(), Event.Priority.Monitor, this);
		if (config.logLeavesDecay)
			pm.registerEvent(Type.LEAVES_DECAY, lbBlockListener, Event.Priority.Monitor, this);
		consumer = new Consumer(this);
		if (getServer().getScheduler().scheduleAsyncRepeatingTask(this, consumer, config.delay * 20, config.delay * 20) > 0)
			log.info("[LogBlock] Started consumer");
		else {
			log.warning("[LogBlock] Failed to schedule consumer with bukkit scheduler. Now trying timer scheduler.");
			timer = new Timer();
			timer.scheduleAtFixedRate(consumer, config.delay*1000, config.delay*1000);
		}
		log.info("Logblock v" + getDescription().getVersion() + " enabled.");
	}

	@Override
	public void onDisable() {
		if (timer != null)
			timer.cancel();
		if (consumer != null && consumer.getQueueSize() > 0) {
			log.info("[LogBlock] Waiting for consumer ...");
			Thread thread = new Thread(consumer);
			while (consumer.getQueueSize() > 0) {
				log.info("[LogBlock] Remaining queue size: " + consumer.getQueueSize());
				thread.run();
			}
		}
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
		Player player = (Player)sender;
		Connection conn = pool.getConnection();
		String table = config.tables.get(player.getWorld().getName().hashCode());
		if (conn == null) {
			player.sendMessage(ChatColor.RED + "Can't create SQL connection.");
			return true;
		} else if (table == null) {
			player.sendMessage(ChatColor.RED + "This world isn't logged");
			return true;
		}
		if (args.length == 0) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock v" + getDescription().getVersion() + " by DiddiZ");
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Type /lb help for help");
		} else if (args[0].equalsIgnoreCase("tool")) {
			if (CheckPermission(player, "logblock.tool")) {
				if (player.getInventory().contains(config.toolID))
					player.sendMessage(ChatColor.RED + "You have alredy a tool"); 
				else {
					int free = player.getInventory().firstEmpty();
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
			if (CheckPermission(player, "logblock.toolblock")) {
				if (player.getInventory().contains(config.toolblockID))
					player.sendMessage(ChatColor.RED + "You have alredy a tool"); 
				else {
					int free = player.getInventory().firstEmpty();
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
			if (CheckPermission(player, "logblock.hide")) {
				if (consumer.hide(player))
					player.sendMessage(ChatColor.GREEN + "You are now hided and won't appear in any log. Type '/lb hide' again to unhide"); 
				else
					player.sendMessage(ChatColor.GREEN + "You aren't hided anylonger."); 
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("savequeue")) {
			if (CheckPermission(player, "logblock.rollback")) {
				player.sendMessage(ChatColor.DARK_AQUA + "Current queue size: " + consumer.getQueueSize());
				Thread thread = new Thread(consumer);
				while (consumer.getQueueSize() > 0) {
					thread.run();
				}
				player.sendMessage(ChatColor.GREEN + "Queue saved successfully");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("area")) {
			if (CheckPermission(player,"logblock.area")) {
				int radius = config.defaultDist;
				if (args.length == 2 && isInt(args[1]))
					radius = Integer.parseInt(args[1]);
				new Thread(new AreaStats(conn, player, radius, table)).start();
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("world")) {
			if (CheckPermission(player,"logblock.area")) {
				new Thread(new AreaStats(conn, player, Short.MAX_VALUE, table)).start();
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("player")) {
			if (CheckPermission(player,"logblock.area")) {
				if (args.length == 2 || args.length == 3) {
					int radius = config.defaultDist;
					if (args.length == 3 && isInt(args[2]))
						radius = Integer.parseInt(args[2]);
					new Thread(new PlayerAreaStats(conn, player, args[1], radius, table)).start();
					} else
						player.sendMessage(ChatColor.RED + "Usage: /lb player [name] <radius>"); 
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("block")) {
			if (CheckPermission(player,"logblock.area")) {
				if (args.length == 2 || args.length == 3) {
					Material mat = Material.matchMaterial(args[1]);
					int radius = config.defaultDist;
					if (args.length == 3 && isInt(args[2]))
						radius = Integer.parseInt(args[2]);
					if (mat != null)
						new Thread(new AreaBlockSearch(conn, player, mat.getId(), radius, table)).start();
					else
						player.sendMessage(ChatColor.RED + "Can't find any item like '" + args[1] + "'");
				} else
					player.sendMessage(ChatColor.RED + "Usage: /lb block [type] <radius>");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("rollback") || args[0].equalsIgnoreCase("undo")) {
			if (CheckPermission(player,"logblock.rollback")) {
				if (args.length >= 2) {
					int minutes = config.defaultTime;
					if (args[1].equalsIgnoreCase("player")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							player.sendMessage(ChatColor.GREEN + "Rolling back " + args[2] + " by " + minutes + " minutes.");
							getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, args[2], -1, null, minutes, table, false));
						} else 
							player.sendMessage(ChatColor.RED + "Usage: /lb rollback player [name] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("area")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							if (isInt(args[2])) {
								player.sendMessage(ChatColor.GREEN + "Rolling back area within " + args[2] + " blocks of you by " + minutes + " minutes.");
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, null, Integer.parseInt(args[2]), null, minutes, table, false));
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
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, args[2], Integer.parseInt(args[3]), null, minutes, table, false));
							} else
								player.sendMessage(ChatColor.RED + "Can't parse to an int: " + args[3]);
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb rollback playerarea [player] [radius] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("selection")) {
						if (args.length == 2 || args.length == 4) {
							if (args.length == 4)
								minutes = parseTimeSpec(args[2], args[3]);
							Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
							if (we != null) {
								Selection sel = ((WorldEditPlugin)we).getSelection(player);
								if (sel != null) {
									if (sel instanceof CuboidSelection) {
										player.sendMessage(ChatColor.GREEN + "Rolling back selection by " + minutes + " minutes.");
										getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, null, -1, sel, minutes, table, false));
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
			if (CheckPermission(player,"logblock.rollback")) {
				if (args.length >= 2) {
					int minutes = config.defaultTime;
					if (args[1].equalsIgnoreCase("player")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							player.sendMessage(ChatColor.GREEN + "Redoing " + args[2] + " for " + minutes + " minutes.");
							getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, args[2], -1, null, minutes, table, true));
						} else 
							player.sendMessage(ChatColor.RED + "Usage: /lb redo player [name] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("area")) {
						if (args.length == 3 || args.length == 5) {
							if (args.length == 5)
								minutes = parseTimeSpec(args[3], args[4]);
							if (isInt(args[2])) {
								player.sendMessage(ChatColor.GREEN + "Redoing area within " + args[2] + " blocks of you for " + minutes + " minutes.");
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, null, Integer.parseInt(args[2]), null, minutes, table, true));
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
								getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, args[2], Integer.parseInt(args[3]), null, minutes, table, true));
							} else
								player.sendMessage(ChatColor.RED + "Can't parse to an int: " + args[3]);
						} else
							player.sendMessage(ChatColor.RED + "Usage: /lb redo playerarea [player] [radius] <time> <minutes|hours|days>");
					} else if (args[1].equalsIgnoreCase("selection")) {
						if (args.length == 2 || args.length == 4) {
							if (args.length == 4)
								minutes = parseTimeSpec(args[2], args[3]);
							Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
							if (we != null) {
								Selection sel = ((WorldEditPlugin)we).getSelection(player);
								if (sel != null) {
									if (sel instanceof CuboidSelection) {
										player.sendMessage(ChatColor.GREEN + "Redoing selection for " + minutes + " minutes.");
										getServer().getScheduler().scheduleAsyncDelayedTask(this, new Rollback(player, conn, this, null, -1, sel, minutes, table, true));
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
			if (CheckPermission(player,"logblock.rollback")) {
				if (args.length == 2) {
					new Thread(new WriteLogFile(conn, player, args[1], table)).start();
				}
				else
					player.sendMessage(ChatColor.RED + "Usage: /lb writelogfile [name]");
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("me")) {
			if (CheckPermission(player,"logblock.me")) {
				new Thread(new PlayerAreaStats(conn, player, player.getName(), Short.MAX_VALUE, table)).start();
			} else
				player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("help")) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock Commands:");
			player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb area <radius>");
			player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb world");
			player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb player [name] <radius>");
			player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb block [type] <radius>");
			player.sendMessage(ChatColor.LIGHT_PURPLE + "/lb rollback [rollback mode]");
		} else
			player.sendMessage(ChatColor.RED + "Wrong argument. Type /lb help for help");
		return true;
	}

	private boolean checkTables() {
		Connection conn = pool.getConnection();
		Statement state = null;
		if (conn == null)
			return false;
		try {
			DatabaseMetaData dbm = conn.getMetaData();
			state = conn.createStatement();
			if (!dbm.getTables(null, null, "lb-players", null).next())	{
				log.log(Level.INFO, "[LogBlock] Crating table lb-players.");
				state.execute("CREATE TABLE `lb-players` (playerid SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL DEFAULT '-', PRIMARY KEY (playerid), UNIQUE (playername))");
				if (!dbm.getTables(null, null, "lb-players", null).next())
					return false;
			}
			state.execute("INSERT IGNORE INTO `lb-players` (playername) VALUES ('" + config.logTNTExplosionsAs + "'), ('" + config.logCreeperExplosionsAs + "'), ('" + config.logFireAs + "'), ('" + config.logLeavesDecayAs + "'), ('" + config.logFireballExplosionsAs + "'), ('Environment')");
			for (String table : config.tables.values()) {
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
			}
			return true;
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] SQL exception while checking tables", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
			}
		}
		return false;
	}

	private boolean CheckPermission(Player player, String permission) {
		if (config.usePermissions)
			return Permissions.Security.permission(player, permission);
		else {
			if (permission.equals("logblock.lookup"))
				return true;
			else if (permission.equals("logblock.me"))
				return true;
			else if (permission.equals("logblock.tool"))
				return true;
			else if (permission.equals("logblock.toolblock"))
				return true;
			else if (permission.equals("logblock.area"))
				return player.isOp();
			else if (permission.equals("logblock.hide"))
				return player.isOp();
			else if (permission.equals("logblock.rollback"))
				return player.isOp();
		}
		return false;
	}

	static int parseTimeSpec(String timespec) {
		String[] split = timespec.split(" ");
		if (split.length != 2)
			return 0;
		return parseTimeSpec(split[0], split[1]);
	}

	static int parseTimeSpec(String time, String unit) {
		int min;
		try {
			min = Integer.parseInt(time);
		} catch (NumberFormatException ex) {
			return 0;
		}
		if (unit.startsWith("hour"))
			min *= 60;
		else if (unit.startsWith("day"))
			min *= (60*24);
		return min;
	}

	private class LBBlockListener extends BlockListener
	{
		public void onBlockPlace(BlockPlaceEvent event) {
			if (!event.isCancelled() && !(config.logSignTexts && (event.getBlock().getType() == Material.WALL_SIGN || event.getBlock().getType() == Material.SIGN_POST))) {
				consumer.queueBlock(event.getPlayer().getName(), event.getBlockPlaced(), event.getBlockReplacedState().getTypeId(), event.getBlockPlaced().getTypeId(), event.getBlockPlaced().getData());
			}
		}

		public void onBlockBreak(BlockBreakEvent event) {
			if (!event.isCancelled())
				consumer.queueBlock(event.getPlayer().getName(), event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData());
		}

		public void onSignChange(SignChangeEvent event) {
			if (!event.isCancelled())
				consumer.queueBlock(event.getPlayer().getName(), event.getBlock(), 0, event.getBlock().getTypeId(), event.getBlock().getData(), "sign [" + event.getLine(0) + "] [" + event.getLine(1) + "] [" + event.getLine(2) + "] [" + event.getLine(3) + "]", null);
		}

		public void onBlockBurn(BlockBurnEvent event) {
			if (!event.isCancelled())
				consumer.queueBlock(config.logFireAs, event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData());
		}

		public void onLeavesDecay(LeavesDecayEvent event) {
			if (!event.isCancelled())
				consumer.queueBlock(config.logLeavesDecayAs, event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData());
		}
	}

	private class LBEntityListener extends EntityListener
	{
		public void onEntityExplode(EntityExplodeEvent event) {
		if (!event.isCancelled()) {	
			String name;
			if (event.getEntity() instanceof TNTPrimed)
				name = config.logTNTExplosionsAs;
			else if (event.getEntity() instanceof Creeper)
				name = config.logCreeperExplosionsAs;
			else if (event.getEntity() instanceof Fireball)
				name = config.logFireballExplosionsAs;
			else
				name = "Environment";
			for (Block block : event.blockList())
				consumer.queueBlock(name, block, block.getTypeId(), 0, block.getData());
			}
		}
	}

	public class LBPlayerListener extends PlayerListener
	{
		public void onPlayerInteract(PlayerInteractEvent event) {
			if (!event.isCancelled()) {
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK && (event.getClickedBlock().getType() == Material.CHEST || event.getClickedBlock().getType() == Material.FURNACE ||event.getClickedBlock().getType() == Material.DISPENSER)) {
					consumer.queueBlock(event.getPlayer(), event.getClickedBlock(), (short)0, (byte)0, (short)0, (byte)0);
				}
			}
		}	

		public void onPlayerBucketFill(PlayerBucketFillEvent event) {
			if (!event.isCancelled()) {
				consumer.queueBlock(event.getPlayer().getName(), event.getBlockClicked(), event.getBlockClicked().getTypeId(), 0, event.getBlockClicked().getData());
			}
		}

		public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
			if (event.getBucket() == Material.WATER_BUCKET)
				consumer.queueBlock(event.getPlayer(), event.getBlockClicked().getFace(event.getBlockFace()), Material.STATIONARY_WATER.getId());
			else if (event.getBucket() == Material.LAVA_BUCKET)
				consumer.queueBlock(event.getPlayer(), event.getBlockClicked().getFace(event.getBlockFace()), Material.STATIONARY_LAVA.getId());
		}

		public void onPlayerJoin(PlayerJoinEvent event) {
			Connection conn = pool.getConnection();
			Statement state = null;
			if (conn == null)
				return;
			try {
				state = conn.createStatement();
				state.execute("INSERT IGNORE INTO `lb-players` (playername) VALUES ('" + event.getPlayer().getName() + "');");
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
			} finally {
				try {
					if (state != null)
						state.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					LogBlock.log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
				}
			}
		}
	}

	private class LBToolPlayerListener extends PlayerListener
	{
		public void onPlayerInteract(PlayerInteractEvent event) {
			if (!event.isCancelled()) {
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == LogBlock.config.toolID && CheckPermission(event.getPlayer(), "logblock.lookup")) {
					getServer().getScheduler().scheduleAsyncDelayedTask(LogBlock.this, new BlockStats(pool.getConnection(), event.getPlayer(), event.getClickedBlock()));
					event.setCancelled(true);
				} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == LogBlock.config.toolblockID && CheckPermission(event.getPlayer(), "logblock.lookup")) {
					getServer().getScheduler().scheduleAsyncDelayedTask(LogBlock.this, new BlockStats(pool.getConnection(), event.getPlayer(), event.getClickedBlock().getFace(event.getBlockFace())));
					if (config.toolblockRemove) 
						event.setCancelled(true);
				}
			}
		}
	}

	private boolean isInt(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}
}
