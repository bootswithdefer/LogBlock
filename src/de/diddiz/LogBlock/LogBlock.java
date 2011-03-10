package de.diddiz.LogBlock;

import java.io.File;
import java.io.FileWriter;
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

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerItemEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import bootswithdefer.JDCBPool.JDCConnectionDriver;
import com.nijikokun.bukkit.Permissions.Permissions;

public class LogBlock extends JavaPlugin
{
	private LBLPlayerListener lblPlayerListener = new LBLPlayerListener();
	private LBLBlockListener lblBlockListener = new LBLBlockListener();
	static final Logger log = Logger.getLogger("Minecraft");
	List<World> worlds = getServer().getWorlds();
	private boolean usePermissions = false;
	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbUrl = "";
	private String dbUsername = "";
	private String dbPassword = "";
	private String name = "[LogBlock]";
	private int delay = 6;
	private int defaultDist = 20;
	private int toolID = 270;
	private int toolblockID = 7;
	private int keepLogDays = -1;
	private boolean toolblockRemove = true;
	private Consumer consumer = null;
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	
	public LogBlock(PluginLoader pluginLoader, Server instance,	PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader)
	{
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
	}
	
	@Override
	public void onEnable()
	{
		try
		{
			File file = new File (getDataFolder(), "config.yml");
			if (!file.exists())
			{
				file.getParentFile().mkdirs();
				FileWriter writer = new FileWriter(file);
				String crlf = System.getProperty("line.separator");
				writer.write("driver : com.mysql.jdbc.Driver" + crlf
						+ "url : jdbc:mysql://localhost:3306/db" + crlf
						+ "username : user" + crlf
						+ "password : pass" + crlf
						+ "delay : 6" + crlf
						+ "tool-id : 270" + crlf
						+ "tool-block-id : 7" + crlf
						+ "tool-block-remove : true" + crlf
						+ "default-distance : 20" + crlf
						+ "usePermissions : false" + crlf
						+ "keepLogDays : -1");
				writer.close();
				log.info(name + " Config created");
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
			if (getConfiguration().getBoolean("usePermissions", false))
			{
				if (getServer().getPluginManager().getPlugin("Permissions") != null)
				{
					usePermissions = true;
					log.info(name + " Permissions enabled");
				}
				else
					log.info(name + " Permissions plugin not found. Use default permissions.");
			}
        }
		catch (Exception e)
		{
        	log.log(Level.SEVERE, name + " Exception while reading config.yml", e);
        	getServer().getPluginManager().disablePlugin(this);
        	return;
		}	
		try
		{
			new JDCConnectionDriver(dbDriver, dbUrl, dbUsername, dbPassword);
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, name + ": exception while creation database connection pool", ex);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (!checkTables())
		{
			log.log(Level.SEVERE, name + " Errors while loading, check logs for more information.");
			return;
		}
		if (keepLogDays >= 0)
			dropOldLogs();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_COMMAND, lblPlayerListener, Event.Priority.Normal, this);
		pm.registerEvent(Type.BLOCK_RIGHTCLICKED, lblBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_PLACED, lblBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_BREAK, lblBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.SIGN_CHANGE, lblBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_ITEM, lblPlayerListener, Event.Priority.Monitor, this);
		consumer = new Consumer();
		new Thread(consumer).start();
		log.info("Logblock v" + getDescription().getVersion() + " enabled.");
	}
    
	@Override
	public void onDisable()
	{
		if (consumer != null)
		{
			consumer.stop();
			consumer = null;
		}
		log.info("LogBlock disabled.");
	}
	
	private Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection("jdbc:jdc:jdcpool");
	}
	
	private boolean checkTables()
	{
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, "blocks", null);
			if (!rs.next())
			{
				log.log(Level.SEVERE, name + " blocks table doesn't exist.");
				return false;
			}
			rs = dbm.getTables(null, null, "extra", null);
			if (!rs.next())
			{
				log.log(Level.SEVERE, name + " extra table doesn't exist.");
				return false;
			}
			return true;
		} catch (SQLException ex) {
			log.log(Level.SEVERE, name + " SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception on close", ex);
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
			int deleted = state.executeUpdate("DELETE FROM blocks WHERE date < date_sub(now(), INTERVAL " + keepLogDays + " DAY)");
			log.info(name + "Cleared out database. Deleted " + deleted + " entries.");
		} catch (SQLException ex) {
			log.log(Level.SEVERE, name + " SQL exception", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception on close", ex);
			}
		}
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
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * from blocks left join extra using (id) where y = ? and x = ? and z = ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
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
			log.log(Level.SEVERE, name + " SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception on close", ex);
			}
		}
		if (!hist)
			player.sendMessage("§3None.");
	}

	private void queueBlock(String playerName, Block block, int typeBefore, int typeAfter, byte data)
	{
		if (block == null || typeBefore < 0 || typeAfter < 0)
			return;
		BlockRow row = new BlockRow(playerName, typeBefore, typeAfter, data, block.getX(), block.getY(), block.getZ());
		boolean result = bqueue.offer(row);
		if (!result)
			log.info(name + " failed to queue block for " + playerName);
	}
	
	private void queueSign(String playerName, Block block, String[] signText)
	{
		BlockRow row = new BlockRow(playerName, 0, 63, block.getData(), block.getX(), block.getY(), block.getZ());
		String text = "sign";
		for (int i = 0; i < 4; i++)
			text += " [" + signText[i] + "]";
		row.addExtra(text);
		boolean result = bqueue.offer(row);
		if (!result)
			log.info(name + " failed to queue block for " + playerName);
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
	
	private int parseTimeSpec(String ts)
	{
		String[] split = ts.split(" ");
		
		if (split.length < 2)
			return 0;
			
		int min;
		try {
			min = Integer.parseInt(split[0]);
		} catch (NumberFormatException ex) {
			return 0;
		}
		
		if (split[1].startsWith("hour"))
			min *= 60;
		else if (split[1].startsWith("day"))
			min *= (60*24);
		return min;
	}

	private class LBLPlayerListener extends PlayerListener
	{
		public void onPlayerCommand(PlayerChatEvent event) {
			if (event.isCancelled())
				return;
			String[] split = event.getMessage().split(" ");
			Player player = event.getPlayer();
			if (split[0].equalsIgnoreCase("/lb")) {
				event.setCancelled(true);
				if (!CheckPermission(event.getPlayer(),"logblock.area"))
				{
					event.getPlayer().sendMessage("§cInsufficient permissions");
					return;
				}
				Connection conn;
				try	{
					conn = getConnection();
				}
				catch (SQLException ex)	{
					log.log(Level.SEVERE, name + " SQL exception", ex);
					player.sendMessage("§cError, check server logs.");
					return;
				}
				if (split.length == 1) {
					AreaStats th = new AreaStats(conn, player, defaultDist);
					new Thread(th).start();
					return;
				}
				else if (split.length == 2) {
					if (split[1].equalsIgnoreCase("world")) {
						PlayerWorldStats th = new PlayerWorldStats(conn, player);
						new Thread(th).start();
						return;
					}
					player.sendMessage("§cIncorrect usage.");
					return;
				}
				else if (split.length == 3) {
					if (split[1].equalsIgnoreCase("player")) {
						PlayerAreaStats th = new PlayerAreaStats(conn, player, split[2], defaultDist);
						new Thread(th).start();
						return;
					}
					else if (split[1].equalsIgnoreCase("area")) {
						AreaStats th = new AreaStats(conn, player, Integer.parseInt(split[2]));
						new Thread(th).start();
						return;
					}
					else if (split[1].equalsIgnoreCase("block")) {
						int type;
						if (Material.matchMaterial(split[2]) != null)
							type = Material.matchMaterial(split[2]).getId();
						else
							type = Integer.parseInt(split[2]);
						AreaBlockSearch th = new AreaBlockSearch(conn, player, type, defaultDist);
						new Thread(th).start();
						return;
					}
				}
				player.sendMessage("§cIncorrect usage.");
			}
			if (split[0].equalsIgnoreCase("/rollback"))
			{
				event.setCancelled(true);
				if (!CheckPermission(event.getPlayer(),"logblock.rollback"))
					{
						event.getPlayer().sendMessage("§cInsufficient permissions");
						return;
					}
				int minutes;
				String name;
				if (split.length < 3)
				{
					player.sendMessage("§cUsage: /rollback [player] [time spec]");
					return;
				}
				name = split[1];
				minutes = parseTimeSpec(event.getMessage().substring(event.getMessage().indexOf(' ', 11) + 1));
				
				player.sendMessage("§cRolling back " + name + " by " + minutes + " minutes.");

				Connection conn;
				try {
					conn = getConnection();
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception", ex);
					player.sendMessage("§cError, check server logs.");
					return;
				}
				Rollback rb = new Rollback(conn, name, minutes);
				player.sendMessage("§cEdit count: " + rb.count());
				new Thread(rb).start();
				return;
			}
		}
		
		public void onPlayerItem(PlayerItemEvent event)
		{
			if (event.getPlayer().getWorld() == worlds.get(0)) {
				if (event.getMaterial() == Material.WATER_BUCKET)
					queueBlock(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()), 0, 9, (byte)0);
				else if (event.getMaterial() == Material.LAVA_BUCKET)
					queueBlock(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()), 0, 11, (byte)0);
				else if (event.getMaterial() == Material.FLINT_AND_STEEL)
					queueBlock(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()), 0, 51, (byte)0);
			}
		}
	}
		
	private class LBLBlockListener extends BlockListener
	{ 
		public void onBlockRightClick(BlockRightClickEvent event)
		{
			if (event.getItemInHand().getTypeId() == toolID && CheckPermission(event.getPlayer(), "logblock.lookup") && event.getPlayer().getWorld() == worlds.get(0))
				showBlockHistory(event.getPlayer(), event.getBlock());
		}
		
	    public void onBlockPlace(BlockPlaceEvent event)
	    {
	    	if (event.getPlayer().getWorld() == worlds.get(0)) {
		    	if (event.getItemInHand().getTypeId() == toolblockID && CheckPermission(event.getPlayer(), "logblock.lookup"))
		    	{
		    		showBlockHistory(event.getPlayer(), event.getBlockPlaced());
					if (toolblockRemove)
						event.setCancelled(true);
		    	}
		    	else
		    		queueBlock(event.getPlayer().getName(), event.getBlockPlaced(), event.getBlockReplacedState().getTypeId(), event.getBlockPlaced().getTypeId(), event.getBlockPlaced().getData());
	    	}
	    }
	    
	    public void onBlockBreak(BlockBreakEvent event)
	    {
	    	if (event.getPlayer().getWorld() == worlds.get(0))
	    		queueBlock(event.getPlayer().getName(), event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData());
	    }
	    
	    public void onSignChange(SignChangeEvent event) {
	    	if (event.getPlayer().getWorld() == worlds.get(0))
	    		queueSign(event.getPlayer().getName(), event.getBlock(), event.getLines());
	    }
	}

	private class Consumer implements Runnable
	{
		private boolean stop = false;
		Consumer() { stop = false; }
		public void stop() { stop = true; }
		public void run()
		{
			PreparedStatement ps = null;
			Connection conn = null;
			BlockRow b;
			
			while (!stop)
			{
			   long start = System.currentTimeMillis()/1000L;
				int count = 0;
				
				if (bqueue.size() > 100)
					log.info(name + " queue size " + bqueue.size());
									
				try {
					conn = getConnection();
					conn.setAutoCommit(false);
					while (count < 100 && start+delay > (System.currentTimeMillis()/1000L))
					{
						b = bqueue.poll(1L, TimeUnit.SECONDS);

						if (b == null)
							continue;
						ps = conn.prepareStatement("INSERT INTO blocks (date, player, replaced, type, data, x, y, z) VALUES (now(),?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
						ps.setString(1, b.name);
						ps.setInt(2, b.replaced);
						ps.setInt(3, b.type);
						ps.setByte(4, b.data);
						ps.setInt(5, b.x);
						ps.setInt(6, b.y);
						ps.setInt(7, b.z);
						ps.executeUpdate();
						
						if (b.extra != null)
						{
							ResultSet keys = ps.getGeneratedKeys();
							keys.next();
							int key = keys.getInt(1);
							
							ps = conn.prepareStatement("INSERT INTO extra (id, extra) values (?,?)");
							ps.setInt(1, key);
							ps.setString(2, b.extra);
							ps.executeUpdate();
						}
						
						count++;
					}
					conn.commit();
				} catch (InterruptedException ex) {
					log.log(Level.SEVERE, name + " interrupted exception", ex);
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception", ex);
				} finally {
					try {
						if (ps != null)
							ps.close();
						if (conn != null)
							conn.close();
					} catch (SQLException ex) {
						log.log(Level.SEVERE, name + " SQL exception on close", ex);
					}
				}
			}
		}
	}
	
	private class BlockRow
	{
		public String name;
		public int replaced, type;
		public byte data;
		public int x, y, z;
		public String extra;
		
		BlockRow(String name, int replaced, int type, byte data, int x, int y, int z)
		{
			this.name = name;
			this.replaced = replaced;
			this.type = type;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
			this.extra = null;
		}

		public void addExtra(String extra)
		{
			this.extra = extra;
		}
		
		public String toString()
		{
			return("name: " + name + " before type: " + replaced + " type: " + type + " x: " + x + " y: " + y + " z: " + z);
		}
	}
	
	private class Rollback implements Runnable
	{
		private LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();

		Rollback(Connection conn, String name, int minutes)
		{
			String query = "select type, data, replaced, x, y, z from blocks where player = ? and date > date_sub(now(), interval ? minute) order by date desc";
			PreparedStatement ps = null;
			ResultSet rs = null;
			edits.clear();
			
			try {
				conn.setAutoCommit(false);
				ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, name);
				ps.setInt(2, minutes);
				rs = ps.executeQuery();

				while (rs.next())
				{
					Edit e = new Edit(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					edits.offer(e);
				}
			} catch (SQLException ex) {
				log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			} finally {
				try {
					if (rs != null)
						rs.close();
					if (ps != null)
						ps.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					log.log(Level.SEVERE, this.getClass().getName() + " SQL exception on close", ex);
				}
			}
			
		}
		
		public int count()
		{
			return edits.size();
		}
		
		public void run()
		{
			Edit e = edits.poll();

			while (e != null)
			{
				e.perform();
				e.log();
				e = edits.poll();
			}
		}
		
		private class Edit
		{
			int type, replaced;
			int x, y, z;
			byte data;
			
			Edit(int type, int replaced, byte data, int x, int y, int z)
			{
				this.type = type;
				this.replaced = replaced;
				this.data = data;
				this.x = x;
				this.y = y;
				this.z = z;
			}
			
			public void perform()
			{
				Block block = getServer().getWorlds().get(0).getBlockAt(x, y, z);
				if (block.getTypeId() == type || (block.getTypeId() >= 8 && block.getTypeId() <= 11))
				{
					if (block.setTypeId(replaced)) {
						block.setData(data);
						log.info("R (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
					}
					else
						log.info("r (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
				}
			}
			
			public void log()
			{
				int current = getServer().getWorlds().get(0).getBlockTypeIdAt(x, y, z);
				if (current == type)
					log.info("+ (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
				else
					log.info("- (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
			}
		}
	}
}
