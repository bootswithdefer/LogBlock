package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.giveTool;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import de.diddiz.LogBlock.QueryParams.BlockChangeType;
import de.diddiz.LogBlock.QueryParams.Order;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;
import de.diddiz.LogBlockQuestioner.LogBlockQuestioner;

public class CommandsHandler implements CommandExecutor
{
	private final Logger log;
	private final LogBlock logblock;
	private final Config config;
	private final BukkitScheduler scheduler;
	private final LogBlockQuestioner questioner;

	CommandsHandler(LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
		config = logblock.getConfig();
		scheduler = logblock.getServer().getScheduler();
		questioner = (LogBlockQuestioner)logblock.getServer().getPluginManager().getPlugin("LogBlockQuestioner");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("lb"))
			return false;
		if (args.length == 0) {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock v" + logblock.getDescription().getVersion() + " by DiddiZ");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + logblock.getUpdater().checkVersion());
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Type /lb help for help");
		} else {
			final String command = args[0].toLowerCase();
			if (command.equals("help")) {
				sender.sendMessage(ChatColor.DARK_AQUA + "LogBlock Help:");
				sender.sendMessage(ChatColor.GOLD + "For the commands list type '/lb commands'");
				sender.sendMessage(ChatColor.GOLD + "For the parameters list type '/lb params'");
				sender.sendMessage(ChatColor.GOLD + "For the list of permissions you got type '/lb permissions'");
			} else if (command.equals("commands")) {
				sender.sendMessage(ChatColor.DARK_AQUA + "LogBlock Commands:");
				sender.sendMessage(ChatColor.GOLD + "/lb tool -- Gives you the lb tool");
				sender.sendMessage(ChatColor.GOLD + "/lb tool [on|off] -- Enables/Disables tool");
				sender.sendMessage(ChatColor.GOLD + "/lb tool [params] -- Sets the tool lookup query");
				sender.sendMessage(ChatColor.GOLD + "/lb toolblock -- Analog to tool");
				sender.sendMessage(ChatColor.GOLD + "/lb hide -- Hides you from log");
				sender.sendMessage(ChatColor.GOLD + "/lb rollback [params] -- Rollback");
				sender.sendMessage(ChatColor.GOLD + "/lb redo [params] -- Redo");
				sender.sendMessage(ChatColor.GOLD + "/lb tp [params] -- Teleports you to the location of griefing");
				sender.sendMessage(ChatColor.GOLD + "/lb writelogfile [params] -- Writes a log file");
				sender.sendMessage(ChatColor.GOLD + "/lb lookup [params] -- Lookup");
				sender.sendMessage(ChatColor.GOLD + "/lb me -- Displays your stats");
				sender.sendMessage(ChatColor.GOLD + "Look at diddiz.insane-architects.net/logblock for the full commands reference");
			} else if (command.equals("params")) {
				sender.sendMessage(ChatColor.DARK_AQUA + "LogBlock Query Parameters:");
				sender.sendMessage(ChatColor.GOLD + "Use doublequotes to escape a keyword: world \"world\"");
				sender.sendMessage(ChatColor.GOLD + "player [name1] <name2> <name3> -- List of players");
				sender.sendMessage(ChatColor.GOLD + "block [type1] <type2> <type3> -- List of block types");
				sender.sendMessage(ChatColor.GOLD + "created, destroyed -- Show only created/destroyed blocks");
				sender.sendMessage(ChatColor.GOLD + "chestaccess -- Show only chest accesses");
				sender.sendMessage(ChatColor.GOLD + "area <radius> -- Area around you");
				sender.sendMessage(ChatColor.GOLD + "selection, sel -- Inside current WorldEdit selection");
				sender.sendMessage(ChatColor.GOLD + "world [worldname] -- Changes the world");
				sender.sendMessage(ChatColor.GOLD + "time [number] [minutes|hours|days] -- Limits time");
				sender.sendMessage(ChatColor.GOLD + "since <dd.MM.yyyy> <HH:mm:ss> -- Limits time to a fixed point");
				sender.sendMessage(ChatColor.GOLD + "limit <row count> -- Limits the result to count of rows");
				sender.sendMessage(ChatColor.GOLD + "sum [none|blocks|players] -- Sums the result");
				sender.sendMessage(ChatColor.GOLD + "asc, desc -- Changes the order of the displayed log");
			} else if (command.equals("permissions")) {
				sender.sendMessage(ChatColor.DARK_AQUA + "You've got the following permissions:");
				if (logblock.hasPermission(sender, "logblock.tool"))
					sender.sendMessage(ChatColor.GOLD + "logblock.tool");
				if (logblock.hasPermission(sender, "logblock.toolblock"))
					sender.sendMessage(ChatColor.GOLD + "logblock.toolblock");
				if (logblock.hasPermission(sender, "logblock.me"))
					sender.sendMessage(ChatColor.GOLD + "logblock.me");
				if (logblock.hasPermission(sender, "logblock.tp"))
					sender.sendMessage(ChatColor.GOLD + "logblock.tp");
				if (logblock.hasPermission(sender, "logblock.rollback"))
					sender.sendMessage(ChatColor.GOLD + "logblock.rollback");
				if (logblock.hasPermission(sender, "logblock.clearlog"))
					sender.sendMessage(ChatColor.GOLD + "logblock.clearlog");
				if (logblock.hasPermission(sender, "logblock.hide"))
					sender.sendMessage(ChatColor.GOLD + "logblock.hide");
			} else if (command.equals("tool")) {
				if (sender instanceof Player) {
					final Player player = (Player)sender;
					if (args.length == 1) {
						if (logblock.hasPermission(player, "logblock.tool"))
							giveTool(player, config.toolID);
						else
							sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
					} else if (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("on")) {
						logblock.getSession(player.getName()).toolEnabled = true;
						player.sendMessage(ChatColor.GREEN + "Tool enabled.");
					} else if (args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("off")) {
						logblock.getSession(player.getName()).toolEnabled = false;
						player.sendMessage(ChatColor.GREEN + "Tool disabled.");
					} else if (args[1].equalsIgnoreCase("mode")) {
						final Session session = logblock.getSession(player.getName());
						if (args.length == 3) {
							final ToolMode mode;
							try {
								mode = ToolMode.valueOf(args[2].toUpperCase());
							} catch (final IllegalArgumentException ex) {
								sender.sendMessage(ChatColor.RED + "Can't find mode " + args[2]);
								return true;
							}
							if (logblock.hasPermission(player, mode.getPermission())) {
								session.toolMode = mode;
								sender.sendMessage(ChatColor.GREEN + "Tool mode set to " + args[2]);
							} else
								sender.sendMessage(ChatColor.RED + "You aren't allowed to use mode " + args[2]);
						} else
							player.sendMessage(ChatColor.RED + "No mode specified");
					} else if (logblock.hasPermission(player, "logblock.lookup"))
						try {
							final QueryParams params = new QueryParams(logblock, sender, ArgsToList(args, 1));
							logblock.getSession(player.getName()).toolQuery = params;
							sender.sendMessage(ChatColor.GREEN + "Set tool query to: " + params.getTitle());
						} catch (final Exception ex) {
							sender.sendMessage(ChatColor.RED + ex.getMessage());
						}
					else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else
					sender.sendMessage(ChatColor.RED + "You have to be a player.");
			} else if (command.equals("toolblock")) {
				if (sender instanceof Player) {
					final Player player = (Player)sender;
					if (args.length == 1) {
						if (logblock.hasPermission(player, "logblock.toolblock"))
							giveTool(player, config.toolblockID);
						else
							player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
					} else if (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("on")) {
						logblock.getSession(player.getName()).toolBlockEnabled = true;
						player.sendMessage(ChatColor.GREEN + "Tool block enabled.");
					} else if (args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("off")) {
						logblock.getSession(player.getName()).toolBlockEnabled = false;
						player.sendMessage(ChatColor.GREEN + "Tool block disabled.");
					} else if (args[1].equalsIgnoreCase("mode")) {
						final Session session = logblock.getSession(player.getName());
						if (args.length == 3) {
							final ToolMode mode;
							try {
								mode = ToolMode.valueOf(args[2].toUpperCase());
							} catch (final IllegalArgumentException ex) {
								sender.sendMessage(ChatColor.RED + "Can't find mode " + args[2]);
								return true;
							}
							if (logblock.hasPermission(player, mode.getPermission())) {
								session.toolBlockMode = mode;
								sender.sendMessage(ChatColor.GREEN + "Toolblock mode set to " + args[2]);
							} else
								sender.sendMessage(ChatColor.RED + "You aren't allowed to use mode " + args[2]);
						} else
							player.sendMessage(ChatColor.RED + "No mode specified");
					} else if (logblock.hasPermission(player, "logblock.lookup"))
						try {
							final QueryParams params = new QueryParams(logblock, sender, ArgsToList(args, 1));
							logblock.getSession(player.getName()).toolBlockQuery = params;
							sender.sendMessage(ChatColor.GREEN + "Set tool block query to: " + params.getTitle());
						} catch (final Exception ex) {
							sender.sendMessage(ChatColor.RED + ex.getMessage());
						}
					else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else
					sender.sendMessage(ChatColor.RED + "You have to be a player.");
			} else if (command.equals("hide")) {
				if (sender instanceof Player) {
					if (logblock.hasPermission(sender, "logblock.hide")) {
						if (logblock.getConsumer().hide((Player)sender))
							sender.sendMessage(ChatColor.GREEN + "You are now hided and won't appear in any log. Type '/lb hide' again to unhide");
						else
							sender.sendMessage(ChatColor.GREEN + "You aren't hidden anylonger.");
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else
					sender.sendMessage(ChatColor.RED + "You have to be a player.");
			} else if (args[0].equalsIgnoreCase("savequeue")) {
				if (logblock.hasPermission(sender, "logblock.rollback"))
					try {
						new CommandSaveQueue(sender, null);
					} catch (final Exception ex) {
						sender.sendMessage(ChatColor.RED + ex.getMessage());
					}
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
			} else if (command.equals("rollback") || command.equals("undo") || command.equals("rb")) {
				if (logblock.hasPermission(sender, "logblock.rollback"))
					try {
						final QueryParams params = new QueryParams(logblock);
						params.minutes = logblock.getConfig().defaultTime;
						params.parseArgs(sender, ArgsToList(args, 1));
						params.limit = -1;
						params.order = Order.DESC;
						params.sum = SummarizationMode.NONE;
						params.bct = BlockChangeType.ALL;
						new CommandRollback(sender, params);
					} catch (final Exception ex) {
						sender.sendMessage(ChatColor.RED + ex.getMessage());
					}
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
			} else if (command.equals("redo")) {
				if (logblock.hasPermission(sender, "logblock.rollback"))
					try {
						final QueryParams params = new QueryParams(logblock);
						params.minutes = logblock.getConfig().defaultTime;
						params.parseArgs(sender, ArgsToList(args, 1));
						params.limit = -1;
						params.order = Order.ASC;
						params.sum = SummarizationMode.NONE;
						params.bct = BlockChangeType.ALL;
						new CommandRedo(sender, params);
					} catch (final Exception ex) {
						sender.sendMessage(ChatColor.RED + ex.getMessage());
					}
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
			} else if (command.equals("me")) {
				if (sender instanceof Player) {
					if (logblock.hasPermission(sender, "logblock.me"))
						try {
							final Player player = (Player)sender;
							final QueryParams params = new QueryParams(logblock);
							params.setPlayer(player.getName());
							params.sum = SummarizationMode.TYPES;
							params.world = player.getWorld();
							new CommandLookup(sender, params);
						} catch (final Exception ex) {
							sender.sendMessage(ChatColor.RED + ex.getMessage());
						}
					else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else
					sender.sendMessage(ChatColor.RED + "You have to be a player.");
			} else if (command.equals("writelogfile")) {
				if (logblock.hasPermission(sender, "logblock.rollback"))
					try {
						final QueryParams params = new QueryParams(logblock, sender, ArgsToList(args, 1));
						params.limit = -1;
						params.bct = BlockChangeType.ALL;
						params.sum = SummarizationMode.NONE;
						new CommandWriteLogFile(sender, params);
					} catch (final Exception ex) {
						sender.sendMessage(ChatColor.RED + ex.getMessage());
					}
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
			} else if (command.equals("clearlog")) {
				if (logblock.hasPermission(sender, "logblock.clearlog"))
					try {
						final QueryParams params = new QueryParams(logblock, sender, ArgsToList(args, 1));
						params.bct = BlockChangeType.ALL;
						params.limit = -1;
						new CommandClearLog(sender, params);
					} catch (final Exception ex) {
						sender.sendMessage(ChatColor.RED + ex.getMessage());
					}
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
			} else if (command.equals("tp")) {
				if (sender instanceof Player) {
					if (logblock.hasPermission(sender, "logblock.tp"))
						try {
							new CommandTeleport(sender, new QueryParams(logblock, sender, ArgsToList(args, 1)));
						} catch (final Exception ex) {
							sender.sendMessage(ChatColor.RED + ex.getMessage());
						}
					else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
				} else
					sender.sendMessage(ChatColor.RED + "You have to be a player.");
			} else if (command.equals("lookup") || QueryParams.isKeyWord(args[0])) {
				if (logblock.hasPermission(sender, "logblock.lookup"))
					try {
						final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
						if (command.equals("lookup"))
							argsList.remove(0);
						new CommandLookup(sender, new QueryParams(logblock, sender, argsList));
					} catch (final Exception ex) {
						sender.sendMessage(ChatColor.RED + ex.getMessage());
					}
				else
					sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
			} else
				sender.sendMessage(ChatColor.RED + "Unknown command '" + args[0] + "'");
		}
		return true;
	}

	public abstract class LBCommand implements Runnable, Closeable
	{
		protected final CommandSender sender;
		protected final QueryParams params;
		protected Connection conn = null;
		protected Statement state = null;
		protected ResultSet rs = null;

		protected LBCommand(CommandSender sender, QueryParams params) throws Exception {
			this.sender = sender;
			this.params = params;
			conn = logblock.getConnection();
			state = conn.createStatement();
			if (scheduler.scheduleAsyncDelayedTask(logblock, this) == -1)
				throw new Exception("Failed to schedule the command");
		}

		@Override
		public final void close() {
			try {
				if (conn != null)
					conn.close();
				if (state != null)
					state.close();
				if (rs != null)
					rs.close();
			} catch (final SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock CommandsHandler] SQL exception on close", ex);
			}
		}
	}

	public class CommandLookup extends LBCommand
	{
		public CommandLookup(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				rs = state.executeQuery(params.getLookupQuery());
				sender.sendMessage(ChatColor.DARK_AQUA + params.getTitle());
				if (rs.next()) {
					rs.beforeFirst();
					final SummarizationMode sum = params.sum;
					final HistoryFormatter histformatter = new HistoryFormatter(sum);
					if (sum == SummarizationMode.TYPES)
						sender.sendMessage(ChatColor.GOLD + String.format("%-6s %-6s %s", "Creat", "Destr", "Block"));
					else if (sum == SummarizationMode.PLAYERS)
						sender.sendMessage(ChatColor.GOLD + String.format("%-6s %-6s %s", "Created", "Destroyed", "Playername"));
					while (rs.next())
						sender.sendMessage(ChatColor.GOLD + histformatter.format(rs, params.coords));
				} else
					sender.sendMessage(ChatColor.DARK_AQUA + "No results found.");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				log.log(Level.SEVERE, "[LogBlock Lookup] Exception: ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandWriteLogFile extends LBCommand
	{
		CommandWriteLogFile(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			File file = null;
			try {
				rs = state.executeQuery(params.getLookupQuery());
				file = new File("plugins/LogBlock/log/" + params.getTitle() + ".log");
				if (!file.canWrite()) {
					log.severe(ChatColor.RED + "No rights to create file: " + file.getAbsolutePath());
					return;
				}
				file.createNewFile();
				final FileWriter writer = new FileWriter(file);
				final String newline = System.getProperty("line.separator");
				final HistoryFormatter histformatter = new HistoryFormatter(params.sum);
				file.getParentFile().mkdirs();
				sender.sendMessage(ChatColor.GREEN + "Creating " + file.getName());
				while (rs.next())
					writer.write(histformatter.format(rs, params.coords) + newline);
				writer.close();
				sender.sendMessage(ChatColor.GREEN + "Done");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				log.log(Level.SEVERE, "[LogBlock WriteLogFile] Exception (file was " + file.getAbsolutePath() + "): ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandSaveQueue extends LBCommand
	{
		public CommandSaveQueue(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			final Consumer consumer = logblock.getConsumer();
			sender.sendMessage(ChatColor.DARK_AQUA + "Current queue size: " + consumer.getQueueSize());
			while (consumer.getQueueSize() > 0)
				consumer.run();
			sender.sendMessage(ChatColor.GREEN + "Queue saved successfully");
		}
	}

	public class CommandTeleport extends LBCommand
	{
		public CommandTeleport(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				rs = state.executeQuery("SELECT x, z FROM `" + params.getTable() + "` INNER JOIN `lb-players` USING (playerid) " + params.getWhere() + params.getOrderBy() + " LIMIT 1");
				if (rs.next()) {
					final Player player = (Player)sender;
					final int x = rs.getInt("x");
					final int z = rs.getInt("z");
					player.teleport(new Location(params.world, x + 0.5, player.getWorld().getHighestBlockYAt(x, z), z + 0.5, player.getLocation().getYaw(), 90));
				} else
					sender.sendMessage(ChatColor.RED + "Query returned no result");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				log.log(Level.SEVERE, "[LogBlock Teleport] Exception: ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandRollback extends LBCommand
	{
		public CommandRollback(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				if (logblock.getConsumer().getQueueSize() > 50)
					try {
						new CommandSaveQueue(sender, null);
					} catch (final Exception ex) {
						sender.sendMessage(ChatColor.RED + ex.getMessage());
					}
				if (!params.silent)
					sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
				rs = state.executeQuery(params.getRollbackQuery());
				final WorldEditor editor = new WorldEditor(logblock, params.world);
				while (rs.next())
					editor.queueBlockChange(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getString("signtext"), rs.getShort("itemtype"), rs.getShort("itemamount"), rs.getByte("itemdata"));
				final int changes = editor.getSize();
				if (!params.silent)
					sender.sendMessage(ChatColor.GREEN.toString() + changes + " blocks found.");
				if (changes == 0) {
					if (!params.silent)
						sender.sendMessage(ChatColor.RED + "Rollback aborted");
					return;
				}
				if (!params.silent && config.askRollbacks && questioner != null && sender instanceof Player && !questioner.ask((Player)sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
					sender.sendMessage(ChatColor.RED + "Rollback aborted");
					return;
				}
				editor.start();
				sender.sendMessage(ChatColor.GREEN + "Rollback finished successfully (" + editor.getElapsedTime() + " ms, " + editor.getSuccesses() + "/" + changes + " blocks" + (editor.getErrors() > 0 ? ChatColor.RED + ", " + editor.getErrors() + " errors" + ChatColor.GREEN : "") + (editor.getBlacklistCollisions() > 0 ? ", " + editor.getBlacklistCollisions() + " blacklist collisions" : "") + ")");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				log.log(Level.SEVERE, "[LogBlock Rollback] Exception: ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandRedo extends LBCommand
	{
		public CommandRedo(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				rs = state.executeQuery(params.getRollbackQuery());
				if (!params.silent)
					sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
				final WorldEditor editor = new WorldEditor(logblock, params.world);
				while (rs.next())
					editor.queueBlockChange(rs.getInt("replaced"), rs.getInt("type"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getString("signtext"), rs.getShort("itemtype"), (short)(rs.getShort("itemamount") * 1), rs.getByte("itemdata"));
				final int changes = editor.getSize();
				if (!params.silent)
					sender.sendMessage(ChatColor.GREEN.toString() + changes + " blocks found.");
				if (changes == 0) {
					if (!params.silent)
						sender.sendMessage(ChatColor.RED + "Redo aborted");
					return;
				}
				if (!params.silent && config.askRedos && questioner != null && sender instanceof Player && !questioner.ask((Player)sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
					sender.sendMessage(ChatColor.RED + "Redo aborted");
					return;
				}
				editor.start();
				sender.sendMessage(ChatColor.GREEN + "Redo finished successfully (" + editor.getElapsedTime() + " ms, " + editor.getSuccesses() + "/" + changes + " blocks" + (editor.getErrors() > 0 ? ChatColor.RED + ", " + editor.getErrors() + " errors" + ChatColor.GREEN : "") + (editor.getBlacklistCollisions() > 0 ? ", " + editor.getBlacklistCollisions() + " blacklist collisions" : "") + ")");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				log.log(Level.SEVERE, "[LogBlock Redo] Exception: ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandClearLog extends LBCommand
	{
		public CommandClearLog(CommandSender sender, QueryParams params) throws Exception {
			super(sender, params);
		}

		@Override
		public void run() {
			try {
				final File dumpFolder = new File(logblock.getDataFolder(), "dumb");
				final SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
				int deleted;
				final String table = params.getTable();
				final String join;
				if (params.players.size() > 0)
					join = "INNER JOIN `lb-players` USING (playerid) ";
				else
					join = "";
				rs = state.executeQuery("SELECT count(*) FROM `" + table + "` " + join + params.getWhere());
				rs.next();
				if ((deleted = rs.getInt(1)) > 0) {
					if (!params.silent && config.askClearLogs && sender instanceof Player && questioner != null) {
						sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
						sender.sendMessage(ChatColor.GREEN.toString() + deleted + " blocks found.");
						if (!questioner.ask((Player)sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
							sender.sendMessage(ChatColor.RED + "ClearLog aborted");
							return;
						}
					}
					if (config.dumpDeletedLog)
						try {
							state.execute("SELECT * FROM `" + table + "` " + join + params.getWhere() + "INTO OUTFILE '" + new File(dumpFolder, formatter.format(System.currentTimeMillis()) + " " + table + " " + params.getTitle().replace(":", ".") + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
						} catch (final SQLException ex) {
							sender.sendMessage(ChatColor.RED + "Error while dumping log. Make sure your MySQL user has access to the LogBlock folder, or disable clearlog.dumpDeletedLog");
							log.log(Level.SEVERE, "[LogBlock ClearLog] Exception while dumping", ex);
							return;
						}
					state.execute("DELETE `" + table + "` FROM `" + table + "` " + join + params.getWhere());
					sender.sendMessage(ChatColor.GREEN + "Cleared out table " + table + ". Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				if ((deleted = rs.getInt(1)) > 0) {
					if (config.dumpDeletedLog)
						state.execute("SELECT id, signtext FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + new File(dumpFolder, formatter.format(System.currentTimeMillis()) + " " + table + "-sign " + params.getTitle() + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE `" + table + "-sign` FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL;");
					sender.sendMessage(ChatColor.GREEN + "Cleared out table " + table + "-sign. Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				if ((deleted = rs.getInt(1)) > 0) {
					if (config.dumpDeletedLog)
						state.execute("SELECT id, itemtype, itemamount, itemdata FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + new File(dumpFolder, formatter.format(System.currentTimeMillis()) + " " + table + "-chest " + params.getTitle() + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE `" + table + "-chest` FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL;");
					sender.sendMessage(ChatColor.GREEN + "Cleared out table " + table + "-chest. Deleted " + deleted + " entries.");
				}
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				log.log(Level.SEVERE, "[LogBlock ClearLog] Exception: ", ex);
			} finally {
				close();
			}
		}
	}

	private static List<String> ArgsToList(String[] arr, int offset) {
		final List<String> list = new ArrayList<String>(Arrays.asList(arr));
		for (int i = 0; i < offset; i++)
			list.remove(0);
		return list;
	}
}
