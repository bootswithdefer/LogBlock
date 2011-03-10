package de.diddiz.LogBlock;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerItemEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import bootswithdefer.JDCBPool.JDCConnectionDriver;

import com.nijikokun.bukkit.Permissions.Permissions;

public class LogBlock extends JavaPlugin
{
	static Logger log;
	private List<String> worldNames;
	private List<String> worldTables;
	private boolean usePermissions = false;
	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbUrl = "";
	private String dbUsername = "";
	private String dbPassword = "";
	private int delay = 6;
	private int defaultDist = 20;
	private int toolID = 270;
	private int toolblockID = 7;
	private int keepLogDays = -1;
	private boolean toolblockRemove = true;
	private boolean logExplosions = false;
	private boolean logFire = false;
	private Consumer consumer = null;
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	
	@Override
	public void onEnable()
	{
		log = getServer().getLogger();
		try	{
			if (!new File (getDataFolder(), "config.yml").exists())	{
	        	log.log(Level.SEVERE, "[LogBlock] Config not found");
	        	getServer().getPluginManager().disablePlugin(this);
	        	return;
			}
			getConfiguration().load();
			dbDriver = getConfiguration().getString("driver", "com.mysql.jdbc.Driver");
			dbUrl = getConfiguration().getString("url", "jdbc:mysql://localhost:3306/db");
			dbUsername = getConfiguration().getString("username", "user");
			dbPassword = getConfiguration().getString("password", "pass");
			delay = getConfiguration().getInt("delay", 6);
			toolID = getConfiguration().getInt("tool-id", 270);
			toolblockID = getConfiguration().getInt("tool-block-id", 7);
			toolblockRemove = getConfiguration().getBoolean("tool-block-remove", true);
			defaultDist = getConfiguration().getInt("default-distance", 20);
			keepLogDays = getConfiguration().getInt("keepLogDays", -1);
			worldNames = getConfiguration().getStringList("worldNames", null);
			worldTables = getConfiguration().getStringList("worldTables", null);
			logExplosions = getConfiguration().getBoolean("logExplosions", false);
			logFire = getConfiguration().getBoolean("logFire", false);
			if (getConfiguration().getBoolean("usePermissions", false))	{
				if (getServer().getPluginManager().getPlugin("Permissions") != null) {
					usePermissions = true;
					log.info("[LogBlock] Permissions enabled");
				} else
					log.info("[LogBlock] Permissions plugin not found. Use default permissions.");
			}
        } catch (Exception e) {
        	log.log(Level.SEVERE, "[LogBlock] Exception while reading config.yml", e);
        	getServer().getPluginManager().disablePlugin(this);
        	return;
		}	
        try	{
			new JDCConnectionDriver(dbDriver, dbUrl, dbUsername, dbPassword);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "[LogBlock] Exception while creation database connection", ex);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (worldNames == null || worldTables == null || worldNames.size() == 0 || worldNames.size() != worldTables.size()) {
			log.log(Level.SEVERE, "[LogBlock] worldNames or worldTables not set porperly");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		for (int i = 0; i < worldNames.size(); i++) {
			if (!checkTables(worldTables.get(i))) {
				log.log(Level.SEVERE, "[LogBlock] Errors while checking tables. They may not exist.");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		}
		if (keepLogDays >= 0)
			dropOldLogs();
		LBLBlockListener lblBlockListener = new LBLBlockListener();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_ITEM, new LBLPlayerListener(), Event.Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_RIGHTCLICKED, lblBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_PLACED, lblBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_BREAK, lblBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.SIGN_CHANGE, lblBlockListener, Event.Priority.Monitor, this);
		if (logFire)
			pm.registerEvent(Type.BLOCK_BURN, lblBlockListener, Event.Priority.Monitor, this);
		if (logExplosions) 
			pm.registerEvent(Type.ENTITY_EXPLODE, new LBLEntityListener(), Event.Priority.Monitor, this);
		consumer = new Consumer();
		new Thread(consumer).start();
		log.info("Logblock v" + getDescription().getVersion() + " enabled.");
	}
    
	@Override
	public void onDisable()
	{
		if (consumer != null) {
			consumer.stop();
			consumer = null;
		}
		log.info("LogBlock disabled.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)	{
		if (cmd.getName().equalsIgnoreCase("lb")) {
			if ((sender instanceof Player)) {
				Player player = (Player)sender;
				Connection conn = getConnection();
				if (conn != null) {
					String table = GetTable(player.getWorld().getName());
					if (table != null) {
						if (CheckPermission(player,"logblock.area")) {
							if (args.length == 0) {
								player.sendMessage(ChatColor.RED + "No argument. Type /mt help for help");
							} else if (args[0].equalsIgnoreCase("area")) {
								int radius = defaultDist;
								if (args.length == 2 && isInt(args[1]))
									radius = Integer.parseInt(args[1]);
								AreaStats th = new AreaStats(conn, player, radius, table);
								new Thread(th).start();
							} else if (args[0].equalsIgnoreCase("world")) {
								PlayerWorldStats th = new PlayerWorldStats(conn, player, table);
								new Thread(th).start();
							} else if (args[0].equalsIgnoreCase("player")) {
								if (args.length >= 2) {
									int radius = defaultDist;
									if (args.length == 3 && isInt(args[2]))
										radius = Integer.parseInt(args[2]);
									PlayerAreaStats th = new PlayerAreaStats(conn, player, args[1], radius, table);
									new Thread(th).start();
								} else
									player.sendMessage(ChatColor.RED + "Usage: /lb player [name] <radius>");
							} else if (args[0].equalsIgnoreCase("block")) {
								if (args.length >= 2) {
									if (Material.matchMaterial(args[1]) != null) {
										int type = Material.matchMaterial(args[1]).getId();
										int radius = defaultDist;
										if (args.length == 3 && isInt(args[2]))
											radius = Integer.parseInt(args[2]);
										AreaBlockSearch th = new AreaBlockSearch(conn, player, type, radius, table);
										new Thread(th).start();
									} else
										player.sendMessage(ChatColor.RED + "Can't find any item like '" + args[1] + "'");
								} else
									player.sendMessage(ChatColor.RED + "Usage: /lb block [type] <radius>");
							} else if (args[0].equalsIgnoreCase("rollback")) {
								if (args.length == 5) {
									int minutes = parseTimeSpec(args[3], args[4]);
									if (args[1].equalsIgnoreCase("player")) {
										player.sendMessage(ChatColor.GREEN + "Rolling back " + args[2] + " by " + minutes + " minutes.");
										Rollback rb = new Rollback(player, conn, args[2], minutes, table);
										new Thread(rb).start();
									} else if (args[1].equalsIgnoreCase("area")) {
									
										if (isInt(args[2])) {
											player.sendMessage(ChatColor.GREEN + "Rolling back area within " + args[2] + " blocks of you by " + minutes + " minutes.");
											Rollback rb = new Rollback(player, conn, Integer.parseInt(args[2]), minutes, table);
											new Thread(rb).start();
										} else
											player.sendMessage(ChatColor.RED + "Can't cast into an int: " + args[2]);
									} else
										player.sendMessage(ChatColor.RED + "Wrong argument. Try player or area.");
								} else {
									player.sendMessage(ChatColor.RED + "Usage:");
									player.sendMessage(ChatColor.RED + "/lb rollback player [name] [time] [minutes|hours|days]");
									player.sendMessage(ChatColor.RED + "/lb rollback area [radius] [time] [minutes|hours|days]");
								}
							} else if (args[0].equalsIgnoreCase("help")) {
								player.sendMessage("§dLogBlock Commands:");
								player.sendMessage("§d/lb area <radius>");
								player.sendMessage("§d/lb world");
								player.sendMessage("§d/lb player [name] <radius>");
								player.sendMessage("§d/lb block [type] <radius>");
								player.sendMessage("§d/lb rollback area [radius] [time] [minutes|hours|days]");
								player.sendMessage("§d/lb rollback player [name] [time] [minutes|hours|days]");
							} else
								player.sendMessage(ChatColor.RED + "Wrong argument. Type /mt help for help");
						} else
							player.sendMessage(ChatColor.RED + "You aren't allowed to do this");
					} else
						player.sendMessage(ChatColor.RED + "This world isn't logged");
				} else
					player.sendMessage(ChatColor.RED + "Can't create SQL connection.");
			} else
				sender.sendMessage("You aren't a player");
			return true;
		} else
			return false;
	}

	private Connection getConnection()
	{
		try {
			return DriverManager.getConnection("jdbc:jdc:jdcpool");
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
			return null;
		}
	}
	
	private boolean checkTables(String table)
	{
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, table, null);
			if (!rs.next())	{
				log.log(Level.SEVERE, "[LogBlock] Crating table " + table + ".");
				conn.createStatement().execute("CREATE TABLE `" + table + "` (`id` int(11) NOT NULL AUTO_INCREMENT, `date` datetime NOT NULL DEFAULT '0000-00-00 00:00:00', `player` varchar(32) NOT NULL DEFAULT '-', `replaced` int(11) NOT NULL DEFAULT '0', `type` int(11) NOT NULL DEFAULT '0', `data` TINYINT NOT NULL DEFAULT '0', `x` int(11) NOT NULL DEFAULT '0', `y` int(11) NOT NULL DEFAULT '0',`z` int(11) NOT NULL DEFAULT '0', PRIMARY KEY (`id`), KEY `coords` (`y`,`x`,`z`), KEY `type` (`type`), KEY `data` (`data`), KEY `replaced` (`replaced`), KEY `player` (`player`));");
			}
			rs = dbm.getTables(null, null, table + "-extra", null);
			if (!rs.next())	{
				log.log(Level.SEVERE, "[LogBlock] Crating table " + table + "-extra.");
				conn.createStatement().execute("CREATE TABLE `" + table + "-extra` (`id` int(11) NOT NULL, `extra` text, PRIMARY KEY (`id`));");
				return checkTables(table);
			}
			return true;
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
			}
		}
		return false;
	}
	
	private void dropOldLogs()
	{
		Connection conn = null;
		Statement state = null;
		try {
			conn = getConnection();
			state = conn.createStatement();
			for (String table : worldTables) {
				int deleted = state.executeUpdate("DELETE FROM `" + table + "` WHERE date < date_sub(now(), INTERVAL " + keepLogDays + " DAY)");
				log.info("[LogBlock] Cleared out table " + table + ". Deleted " + deleted + " entries.");
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
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
	}
	
	private String GetTable (String worldName) {
		int idx = worldNames.indexOf(worldName);
		if (idx == -1)
			return null;
		return worldTables.get(idx);
	}

	
	private void showBlockHistory(Player player, Block b)
	{
		player.sendMessage("§3Block history (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + "): ");
		boolean hist = false;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Timestamp date;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd hh:mm:ss");
		int idx = worldNames.indexOf(player.getWorld().getName());
		if (idx == -1)
			return;
		String table = worldTables.get(idx);
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * from `" + table + "` left join `" + table + "-extra` using (id) where y = ? and x = ? and z = ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, b.getY());
			ps.setInt(2, b.getX());
			ps.setInt(3, b.getZ());
			rs = ps.executeQuery();
			while (rs.next())
			{
				date = rs.getTimestamp("date");
				String datestr = formatter.format(date);
				String msg = datestr + " " + rs.getString("player") + " ";
				if (rs.getInt("type") == 0)
					msg = msg + "destroyed " + Material.getMaterial(rs.getInt("replaced")).toString().toLowerCase().replace('_', ' ');
				else if (rs.getInt("replaced") == 0)
				{
					if (rs.getInt("type") == 63)
						msg = msg + "created " + rs.getString("extra");
					else
						msg = msg + "created " + Material.getMaterial(rs.getInt("type")).toString().toLowerCase().replace('_', ' ');
				}
				else
					msg = msg + "replaced " + Material.getMaterial(rs.getInt("replaced")).toString().toLowerCase().replace('_', ' ') + " with " + Material.getMaterial(rs.getInt("type")).toString().toLowerCase().replace('_', ' ');
				player.sendMessage("§6" + msg);
				hist = true;
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
			}
		}
		if (!hist)
			player.sendMessage("§3None.");
	}

	private void queueBlock(String playerName, Block block, int typeBefore, int typeAfter, byte data, String extra)
	{
		if (block == null || typeBefore < 0 || typeAfter < 0)
			return;
		String table = GetTable(block.getWorld().getName());
		if (table == null)
			return;
		BlockRow row = new BlockRow(table, playerName, typeBefore, typeAfter, data, block.getX(), block.getY(), block.getZ());
		if (extra != null)
			row.addExtra(extra);
		if (!bqueue.offer(row))
			log.info("[LogBlock] failed to queue block for " + playerName);
	}
	
    private boolean CheckPermission(Player player, String permission)
    {
    	if (usePermissions)
    		return Permissions.Security.permission(player, permission);
    	else {
    		if (permission.equals("logblock.lookup"))
    			return true;
    		else if (permission.equals("logblock.area"))
    			return player.isOp();
    		else if (permission.equals("logblock.rollback"))
    			return player.isOp();
    	}
    	return false;
    }
	
	private int parseTimeSpec(String time, String unit)
	{
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

	private class LBLPlayerListener extends PlayerListener
	{
		public void onPlayerItem(PlayerItemEvent event)
		{
			if (!event.isCancelled()) {
				if (event.getMaterial() == Material.WATER_BUCKET)
					queueBlock(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()), 0, 9, (byte)0, null);
				else if (event.getMaterial() == Material.LAVA_BUCKET)
					queueBlock(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()), 0, 11, (byte)0, null);
				else if (event.getMaterial() == Material.FLINT_AND_STEEL)
					queueBlock(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()), 0, 51, (byte)0, null);
			}
		}
	}
		
	private class LBLBlockListener extends BlockListener
	{ 
		public void onBlockRightClick(BlockRightClickEvent event)
		{
			if (event.getItemInHand().getTypeId()== toolID)
				showBlockHistory(event.getPlayer(), event.getBlock());
		}
		
	    public void onBlockPlace(BlockPlaceEvent event)
	    {
	    	if (!event.isCancelled()) {
		    	if (event.getItemInHand().getTypeId() == toolblockID && CheckPermission(event.getPlayer(), "logblock.lookup"))
		    	{
		    		showBlockHistory(event.getPlayer(), event.getBlockPlaced());
					if (toolblockRemove)
						event.setCancelled(true);
		    	}
		    	else
		    		queueBlock(event.getPlayer().getName(), event.getBlockPlaced(), event.getBlockReplacedState().getTypeId(), event.getBlockPlaced().getTypeId(), event.getBlockPlaced().getData(), null);
	    	}
	    }
	    
	    public void onBlockBreak(BlockBreakEvent event)
	    {
	    	if (!event.isCancelled())
	    		queueBlock(event.getPlayer().getName(), event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData(), null);
	    }
	    
	    public void onSignChange(SignChangeEvent event) {
	    	if (!event.isCancelled())
	    		queueBlock(event.getPlayer().getName(), event.getBlock(), 0, event.getBlock().getTypeId(), event.getBlock().getData(), "sign [" + event.getLine(0) + "] [" + event.getLine(1) + "] [" + event.getLine(2) + "] [" + event.getLine(3) + "]");
			;
	    }
	    
	    public void onBlockBurn(BlockBurnEvent event) {
	    	if (!event.isCancelled())
	    		queueBlock("environment", event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData(), null);
	    }
	}

	private class LBLEntityListener extends EntityListener
	{
		public void onEntityExplode(EntityExplodeEvent event) {
	    	if (!event.isCancelled()) {	
	    		for (Block block : event.blockList())
	    			queueBlock("environment", block, block.getTypeId(), 0, block.getData(), null);
	    	}
		}
	}
	
    private boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
	
	private class Consumer implements Runnable
	{
		private boolean stop = false;
		
		Consumer() {
			stop = false;
		}
		
		public void stop() {
			stop = true;
		}
		
		public void run() {
			PreparedStatement ps = null;
			Connection conn = null;
			BlockRow b;
			while (!stop) {
				long start = System.currentTimeMillis()/1000L;
				int count = 0;
				if (bqueue.size() > 100)
					log.info("[LogBlock] Queue overloaded. Size: " + bqueue.size());				
				try {
					conn = getConnection();
					conn.setAutoCommit(false);
					while (count < 100 && start+delay > (System.currentTimeMillis()/1000L))
					{
						b = bqueue.poll(1L, TimeUnit.SECONDS);

						if (b == null)
							continue;
						ps = conn.prepareStatement("INSERT INTO `" + b.table + "` (date, player, replaced, type, data, x, y, z) VALUES (now(),?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
						ps.setString(1, b.name);
						ps.setInt(2, b.replaced);
						ps.setInt(3, b.type);
						ps.setByte(4, b.data);
						ps.setInt(5, b.x);
						ps.setInt(6, b.y);
						ps.setInt(7, b.z);
						ps.executeUpdate();
						
						if (b.extra != null) {
							ResultSet keys = ps.getGeneratedKeys();
							keys.next();
							int key = keys.getInt(1);
							
							ps = conn.prepareStatement("INSERT INTO `" + b.table + "-extra` (id, extra) values (?,?)");
							ps.setInt(1, key);
							ps.setString(2, b.extra);
							ps.executeUpdate();
						}
						count++;
					}
					conn.commit();
				} catch (InterruptedException ex) {
					log.log(Level.SEVERE, "[LogBlock] Interrupted exception", ex);
				} catch (SQLException ex) {
					log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
				} finally {
					try {
						if (ps != null)
							ps.close();
						if (conn != null)
							conn.close();
					} catch (SQLException ex) {
						log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
					}
				}
			}
		}
	}
	
	private class BlockRow
	{
		public String table;
		public String name;
		public int replaced, type;
		public byte data;
		public int x, y, z;
		public String extra;
		
		BlockRow(String table, String name, int replaced, int type, byte data, int x, int y, int z)	{
			this.table = table;
			this.name = name;
			this.replaced = replaced;
			this.type = type;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
			this.extra = null;
		}

		public void addExtra(String extra) {
			this.extra = extra;
		}
		
		public String toString() {
			return("name: " + name + " before type: " + replaced + " type: " + type + " x: " + x + " y: " + y + " z: " + z);
		}
	}
}
