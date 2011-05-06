package de.diddiz.LogBlock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.diddiz.LogBlock.QueryParams.BlockChangeType;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class CommandsHandler implements CommandExecutor
{
	private final Logger log;
	private final LogBlock logblock;
	private final Config config;

	public CommandsHandler(LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
		config = logblock.getConfig();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)	{
		if (!cmd.getName().equalsIgnoreCase("lb"))
			return false;
		if (args.length == 0) {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock v" + logblock.getDescription().getVersion() + " by DiddiZ");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Type /lb help for help");
		} else if (args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock Commands:");
			if (logblock.checkPermission(sender, "logblock.me"))
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb me");
			if (logblock.checkPermission(sender, "logblock.area")) {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb area <radius>");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb world");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb player [name] <radius>");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb block [type] <radius>");
			}
			if (logblock.checkPermission(sender, "logblock.rollback")) {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb rollback [rollback mode]");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb redo [redo mode]");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb writelogfile [player]");
			}
			if (logblock.checkPermission(sender, "logblock.hide"))
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "/lb hide");
		} else if (args[0].equalsIgnoreCase("tool")) {
			if (sender instanceof Player) {
				if (logblock.checkPermission(sender, "logblock.tool"))
					giveTool((Player)sender, config.toolID);
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
			} else
				sender.sendMessage(ChatColor.RED + "You have to be a player.");
		} else if (args[0].equalsIgnoreCase("toolblock")) {
			if (sender instanceof Player) {
				if (logblock.checkPermission(sender, "logblock.toolblockID"))
					giveTool((Player)sender, config.toolblockID);
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
			} else
				sender.sendMessage(ChatColor.RED + "You have to be a player.");
		} else if (args[0].equalsIgnoreCase("hide")) {
			if (sender instanceof Player) {
				if (logblock.checkPermission(sender, "logblock.hide")) {
					if (logblock.getConsumer().hide((Player)sender))
						sender.sendMessage(ChatColor.GREEN + "You are now hided and won't appear in any log. Type '/lb hide' again to unhide");
					else
						sender.sendMessage(ChatColor.GREEN + "You aren't hided anylonger.");
				} else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
			} else
				sender.sendMessage(ChatColor.RED + "You have to be a player.");
		} else if (args[0].equalsIgnoreCase("savequeue")) {
			if (logblock.checkPermission(sender, "logblock.rollback"))
				try {
					new Thread(new CommandSaveQueue(sender, null)).run();
				} catch (Exception ex) {
					sender.sendMessage(ex.getMessage());
				}
			else
				sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("rollback") || args[0].equalsIgnoreCase("undo")) {
			if (logblock.checkPermission(sender, "logblock.rollback")) {
				try {
					final List<String> argsList = Arrays.asList(args);
					argsList.remove(1);
					final QueryParams params = new QueryParams(logblock);
					params.parseArgs(sender, argsList);
					params.setLimit(-1);
					params.setOrder(QueryParams.Order.DESC);
					params.setSummarizationMode(QueryParams.SummarizationMode.NONE);
					new Thread(new CommandRollback(sender, params)).run();
				} catch (final Exception ex) {
					sender.sendMessage(ex.getMessage());
				}
			} else
				sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("redo")) {
			if (logblock.checkPermission(sender, "logblock.rollback")) {
				try {
					final List<String> argsList = Arrays.asList(args);
					argsList.remove(1);
					final QueryParams params = new QueryParams(logblock);
					params.parseArgs(sender, argsList);
					params.setLimit(-1);
					params.setOrder(QueryParams.Order.ASC);
					params.setSummarizationMode(QueryParams.SummarizationMode.NONE);
					new Thread(new CommandRedo(sender, params)).run();
				} catch (final Exception ex) {
					sender.sendMessage(ex.getMessage());
				}
			} else
				sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
		} else if (args[0].equalsIgnoreCase("me")) {
			if (sender instanceof Player) {
				if (logblock.checkPermission(sender, "logblock.rollback")) {
					final QueryParams params = new QueryParams(logblock);
					params.setPlayer(((Player)sender).getName());
					params.setSummarizationMode(SummarizationMode.TYPES);
				} else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
			} else
				sender.sendMessage(ChatColor.RED + "You have to be a player.");
		} else if (args[0].equalsIgnoreCase("writelogfile")) {
			if (logblock.checkPermission(sender,"logblock.rollback")) {
				try {
					final List<String> argsList = Arrays.asList(args);
					argsList.remove(1);
					final QueryParams params = new QueryParams(logblock);
					params.parseArgs(sender, argsList);
					params.setLimit(-1);
					params.setBlockChangeType(BlockChangeType.ALL);
					params.setSummarizationMode(SummarizationMode.NONE);
					new Thread(new CommandWriteLogFile(sender, params)).run();
				} catch (final Exception ex) {
					sender.sendMessage(ex.getMessage());
				}
			} else
				sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
		} else if (args[0].equalsIgnoreCase("tp")) {
			if (sender instanceof Player) {
				if (logblock.checkPermission(sender,"logblock.tp")) {
					try {
						final List<String> argsList = Arrays.asList(args);
						argsList.remove(1);
						final QueryParams params = new QueryParams(logblock);
						params.parseArgs(sender, argsList);
						params.setLimit(1);
						new Thread(new CommandTeleport(sender, null)).run();
					} catch (final Exception ex) {
						sender.sendMessage(ex.getMessage());
					}
				} else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
			} else
				sender.sendMessage(ChatColor.RED + "You have to be a player.");
		} else if (QueryParams.isKeyWord(args[0])) {
			try {
				final QueryParams params = new QueryParams(logblock);
				params.parseArgs(sender, Arrays.asList(args));
				new Thread(new CommandLookup(sender, params)).run();
			} catch (final Exception ex) {
				log.log(Level.SEVERE, "Error at Command", ex);
				sender.sendMessage(ex.getMessage());
			}
		}
		return true;
	}

	private abstract class LBCommand implements Runnable
	{
		protected final CommandSender sender;
		protected final QueryParams params;
		protected Connection conn = null;
		protected Statement state = null;
		protected ResultSet rs = null;

		LBCommand(CommandSender sender, QueryParams params) throws Exception {
			this.sender = sender;
			this.params = params;
			try {
				conn = logblock.getConnection();
				conn.setAutoCommit(false);
				state = conn.createStatement();
				log.info("Query: " + params.getQuery());
				rs = state.executeQuery(params.getQuery());
			} catch (SQLException ex) {
				close();
				log.log(Level.SEVERE, "[LogBlock CommandsHandler] Error while executing query", ex);
				throw new Exception("Error while executing query");
			}
		}

		protected void close() {
			try {
				if (conn != null)
					conn.close();
				if (state != null)
					state.close();
				if (rs != null)
					rs.close();
			} catch (final SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock CommandHandler] SQL exception on close", ex);
			}
		}
	}

	private class CommandLookup extends LBCommand
	{
		CommandLookup(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				final SummarizationMode sum = params.getSummarizationMode();
				final HistoryFormatter histformatter = new HistoryFormatter(sum);
				sender.sendMessage(ChatColor.DARK_AQUA + params.getTitle());
				if (rs.next()) {
					rs.beforeFirst();
					if (sum == SummarizationMode.TYPES)
						sender.sendMessage(ChatColor.GOLD + String.format("%-6s %-6s %s", "Creat", "Destr", "Block"));
					else if (sum == SummarizationMode.PLAYERS)
						sender.sendMessage(ChatColor.GOLD + String.format("%-6d %-6d %s", rs.getInt("created"), rs.getInt("destroyed"), rs.getString("playername")));
					while (rs.next())
						sender.sendMessage(ChatColor.GOLD + histformatter.format(rs));
				} else
					sender.sendMessage(ChatColor.DARK_AQUA + "No results found.");
			} catch (final SQLException ex) {
				sender.sendMessage(ChatColor.RED + "SQL exception");
				log.log(Level.SEVERE, "[LogBlock Lookup] SQL exception", ex);
			} finally {
				close();
			}
		}
	}

	private class CommandWriteLogFile extends LBCommand
	{
		CommandWriteLogFile(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				final File file = new File ("plugins/LogBlock/log/" + params.getTitle() + ".log");
				final FileWriter writer = new FileWriter(file);
				final String newline = System.getProperty("line.separator");
				final HistoryFormatter histformatter = new HistoryFormatter(params.getSummarizationMode());
				file.getParentFile().mkdirs();
				sender.sendMessage(ChatColor.GREEN + "Creating " + file.getName());
				while (rs.next()) {
					writer.write(histformatter.format(rs) + newline);
				}
				writer.close();
				sender.sendMessage(ChatColor.GREEN + "Done");
			} catch (final SQLException ex) {
				sender.sendMessage(ChatColor.RED + "SQL exception");
				log.log(Level.SEVERE, "[LogBlock WriteLogFile] SQL exception", ex);
			} catch (final IOException ex) {
				sender.sendMessage(ChatColor.RED + "IO exception");
				log.log(Level.SEVERE, "[LogBlock WriteLogFile] IO exception", ex);
			} finally {
				close();
			}
		}
	}

	private class CommandSaveQueue extends LBCommand
	{
		CommandSaveQueue(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			final Consumer consumer = logblock.getConsumer();
			sender.sendMessage(ChatColor.DARK_AQUA + "Current queue size: " + consumer.getQueueSize());
			while (consumer.getQueueSize() > 0) {
				consumer.run();
			}
			sender.sendMessage(ChatColor.GREEN + "Queue saved successfully");
		}
	}

	private class CommandTeleport extends LBCommand
	{
		CommandTeleport(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				if (rs.next()) {
					final Player player = (Player)sender;
					player.teleport(new Location(params.getWorld(), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
				} else
					sender.sendMessage(ChatColor.RED + "Query returned no result");
			} catch (final SQLException ex) {
				sender.sendMessage(ChatColor.RED + "SQL exception");
				log.log(Level.SEVERE, "[LogBlock WriteLogFile] SQL exception", ex);
			} finally {
				close();
			}
		}
	}

	private class CommandRollback extends LBCommand
	{
		CommandRollback(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				final WorldEditor editor = new WorldEditor(logblock, this, params.getWorld());
				while (rs.next()) {
					editor.queueBlockChange(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
				}
				final int changes = editor.getSize();
				sender.sendMessage(ChatColor.GREEN + "" + changes + " Changes found.");
				final long start = System.currentTimeMillis();
				if (!editor.start()) {
					sender.sendMessage(ChatColor.RED + "Failed to schedule rollback task");
					return;
				}
				synchronized (this) {
					try {
						this.wait();
					} catch (final InterruptedException e) {
						sender.sendMessage(ChatColor.RED + "Rollback Interrupted");
						log.severe("[LogBlock Rollback] Interrupted");
					}
				}
				sender.sendMessage(ChatColor.GREEN + "Rollback finished successfully");
				sender.sendMessage(ChatColor.GREEN + "Undid " + editor.getSuccesses() + " of " + changes + " changes (" + editor.getErrors() + " errors, " + editor.getBlacklistCollisions() + " blacklist collisions)");
				sender.sendMessage(ChatColor.GREEN + "Took: " + (System.currentTimeMillis() - start) + "ms");
			} catch (final SQLException ex) {
				sender.sendMessage(ChatColor.RED + "SQL exception");
				log.log(Level.SEVERE, "[LogBlock Rollback] SQL exception", ex);
			} finally {
				close();
			}
		}
	}

	private class CommandRedo extends LBCommand
	{
		CommandRedo(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				final WorldEditor editor = new WorldEditor(logblock, this, params.getWorld());
				while (rs.next()) {
					editor.queueBlockChange(rs.getInt("replaced"), rs.getInt("type"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
				}
				final int changes = editor.getSize();
				sender.sendMessage(ChatColor.GREEN + "" + changes + " Changes found.");
				final long start = System.currentTimeMillis();
				if (!editor.start()) {
					sender.sendMessage(ChatColor.RED + "Failed to schedule redo task");
					return;
				}
				synchronized (this) {
					try {
						this.wait();
					} catch (final InterruptedException e) {
						sender.sendMessage(ChatColor.RED + "Redo Interrupted");
						log.severe("[LogBlock Redo] Interrupted");
					}
				}
				sender.sendMessage(ChatColor.GREEN + "Redo finished successfully");
				sender.sendMessage(ChatColor.GREEN + "Redid " + editor.getSuccesses() + " of " + changes + " changes (" + editor.getErrors() + " errors, " + editor.getBlacklistCollisions() + " blacklist collisions)");
				sender.sendMessage(ChatColor.GREEN + "Took: " + (System.currentTimeMillis() - start) + "ms");
			} catch (final SQLException ex) {
				sender.sendMessage(ChatColor.RED + "SQL exception");
				log.log(Level.SEVERE, "[LogBlock Redo] SQL exception", ex);
			} finally {
				close();
			}
		}
	}

	private String getMaterialName(int type) {
		return Material.getMaterial(type).toString().toLowerCase().replace('_', ' ');
	}

	private class HistoryFormatter
	{
		private final SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		private final SummarizationMode sum;

		HistoryFormatter(SummarizationMode sum) {
			this.sum = sum;
		}

		String format(ResultSet rs) {
			try {
				if (sum == SummarizationMode.NONE) {
					final StringBuffer msg = new StringBuffer(formatter.format(rs.getTimestamp("date")) + " " + rs.getString("playername") + " ");
					final int type = rs.getInt("type");
					final int replaced = rs.getInt("replaced");
					if ((type == 63 || type == 68) && rs.getString("signtext") != null)
						msg.append("created " + rs.getString("signtext"));
					else if (type == replaced) {
						if (type == 23 || type == 54 || type == 61)
							msg.append("looked inside " + getMaterialName(type));
					} else if (type == 0)
						msg.append("destroyed " + getMaterialName(replaced));
					else if (replaced == 0)
						msg.append("created " + getMaterialName(type));
					else
						msg.append("replaced " + getMaterialName(replaced) + " with " + getMaterialName(type));
					return msg.toString();
				} else if (sum == SummarizationMode.TYPES)
					return fillWithSpaces(rs.getInt("created")) + fillWithSpaces(rs.getInt("destroyed")) + Material.getMaterial(rs.getInt("type")).toString().toLowerCase().replace('_', ' ');
				else
					return fillWithSpaces(rs.getInt("created")) + fillWithSpaces(rs.getInt("destroyed")) + rs.getString("playername");
			} catch (final Exception ex) {
				return null;
			}
		}

		private String fillWithSpaces(Integer number) {
			final StringBuffer filled = new StringBuffer(number.toString());
			final int neededSpaces = (36 - filled.length() * 6) / 4;
			for (int i = 0; i < neededSpaces; i++)
				filled.append(' ');;
				return filled.toString();
		}
	}

	private void giveTool(Player player, int tool) {
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
	}
}
