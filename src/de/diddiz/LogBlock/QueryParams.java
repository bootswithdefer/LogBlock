package de.diddiz.LogBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import de.diddiz.util.BukkitUtils;
import de.diddiz.util.Utils;

public class QueryParams implements Cloneable
{
	private static final HashSet<Integer> keywords = new HashSet<Integer>(Arrays.asList("player".hashCode(), "area".hashCode(), "selection".hashCode(), "sel".hashCode(), "block".hashCode(), "type".hashCode(), "sum".hashCode(), "destroyed".hashCode(), "created".hashCode(), "chestaccess".hashCode(), "all".hashCode(), "time".hashCode(), "since".hashCode(), "before".hashCode(), "limit".hashCode(), "world".hashCode(), "asc".hashCode(), "desc".hashCode(), "last".hashCode()));
	private final LogBlock logblock;
	List<String> players = new ArrayList<String>();
	List<Integer> types = new ArrayList<Integer>();
	Location loc = null;
	int radius = -1;
	Selection sel = null;
	int minutes = 0;
	SummarizationMode sum = SummarizationMode.NONE;
	BlockChangeType bct = BlockChangeType.BOTH;
	int limit = 15;
	World world = null;
	Order order = Order.DESC;
	boolean selectFullBlockData = false;
	boolean prepareToolQuery = false;

	public QueryParams(LogBlock logblock) {
		this.logblock = logblock;
	}

	public QueryParams(LogBlock logblock, CommandSender sender, List<String> args) throws IllegalArgumentException {
		this.logblock = logblock;
		parseArgs(sender, args);
	}

	private void merge(QueryParams params) {
		players = params.players;
		types = params.types;
		loc = params.loc;
		radius = params.radius;
		sel = params.sel;
		if (minutes == 0)
			minutes = params.minutes;
		sum = params.sum;
		bct = params.bct;
		limit = params.limit;
		world = params.world;
		order = params.order;
	}

	public void parseArgs(CommandSender sender, List<String> args) throws IllegalArgumentException {
		if (args == null || args.size() == 0)
			throw new IllegalArgumentException("No parameters specified.");
		Player player = null;
		if (sender instanceof Player)
			player = (Player)sender;
		final String name = BukkitUtils.getSenderName(sender);
		final Session session;
		if (!prepareToolQuery)
			session = logblock.getSession(name);
		else
			session = null;
		if (player != null && world == null)
			world = player.getWorld();
		for (int i = 0; i < args.size(); i++) {
			final String param = args.get(i).toLowerCase();
			final String[] values = getValues(args, i + 1);
			if (param.equals("last")) {
				if (session.lastQuery == null)
					throw new IllegalArgumentException("This is your first command, you can't use last.");
				merge(session.lastQuery);
			} else if (param.equals("player")) {
				if (values == null || values.length < 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				for (final String playerName : values)
					if (playerName.length() > 0)
						players.add(playerName);
			} else if (param.equals("block") || param.equals("type")) {
				if (values == null || values.length < 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				for (final String blockName : values) {
					final Material mat = Material.matchMaterial(blockName);
					if (mat == null)
						throw new IllegalArgumentException("No material matching: '" + blockName + "'");
					types.add(mat.getId());
				}
			} else if (param.equals("area")) {
				if (player == null && !prepareToolQuery)
					throw new IllegalArgumentException("You have to ba a player to use area");
				if (values == null) {
					radius = logblock.getConfig().defaultDist;
					if (!prepareToolQuery)
						loc = player.getLocation();
				} else {
					if (!Utils.isInt(values[0]))
						throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
					radius = Integer.parseInt(values[0]);
					if (!prepareToolQuery)
						loc = player.getLocation();
				}
			} else if (param.equals("selection") || param.equals("sel")) {
				if (player == null)
					throw new IllegalArgumentException("You have to ba a player to use selection");
				final Plugin we = player.getServer().getPluginManager().getPlugin("WorldEdit");
				if (we == null)
					throw new IllegalArgumentException("WorldEdit plugin not found");
				sel = ((WorldEditPlugin)we).getSelection(player);
				if (sel == null)
					throw new IllegalArgumentException("No selection defined");
				if (!(sel instanceof CuboidSelection))
					throw new IllegalArgumentException("You have to define a cuboid selection");
				world = sel.getWorld();
			} else if (param.equals("time") || param.equals("since")) {
				if (values == null)
					minutes = logblock.getConfig().defaultTime;
				else
					minutes = Utils.parseTimeSpec(values);
				if (minutes == -1)
					throw new IllegalArgumentException("Faile to parse time spec for '" + param + "'");
			} else if (param.equals("before")) {
				if (values == null)
					minutes = logblock.getConfig().defaultTime * -1;
				else
					minutes = Utils.parseTimeSpec(values) * -1;
				if (minutes == 1)
					throw new IllegalArgumentException("Faile to parse time spec for '" + param + "'");
			} else if (param.equals("sum")) {
				if (values == null || values.length != 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				if (values[0].startsWith("p"))
					sum = SummarizationMode.PLAYERS;
				else if (values[0].startsWith("b"))
					sum = SummarizationMode.TYPES;
				else if (values[0].startsWith("n"))
					sum = SummarizationMode.NONE;
				else
					throw new IllegalArgumentException("Wrong summarization mode");
			} else if (param.equals("created"))
				bct = BlockChangeType.CREATED;
			else if (param.equals("destroyed"))
				bct = BlockChangeType.DESTROYED;
			else if (param.equals("chestaccess"))
				bct = BlockChangeType.CHESTACCESS;
			else if (param.equals("all"))
				bct = BlockChangeType.ALL;
			else if (param.equals("limit")) {
				if (values.length != 1)
					throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
				if (!Utils.isInt(values[0]))
					throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
				limit = Integer.parseInt(values[0]);
			} else if (param.equals("world")) {
				if (values.length != 1)
					throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
				if (sender.getServer().getWorld(values[0]) == null)
					throw new IllegalArgumentException("There is no world called '" + values[0] + "'");
				world = sender.getServer().getWorld(values[0]);
			} else if (param.equals("asc"))
				order = Order.ASC;
			else if (param.equals("desc"))
				order = Order.DESC;
			else
				throw new IllegalArgumentException("Not a valid argument: '" + param + "'");
			if (values != null)
				i += values.length;
		}
		if (types.size() > 0)
			for (final Set<Integer> equivalent : BukkitUtils.getBlockEquivalents()) {
				boolean found = false;
				for (final Integer type : types)
					if (equivalent.contains(type)) {
						found = true;
						break;
					}
				if (found)
					for (final Integer type : equivalent)
						if (!types.contains(type))
							types.add(type);
			}
		if (!prepareToolQuery) {
			if (world == null)
				throw new IllegalArgumentException("No world specified");
			if (!logblock.getConfig().tables.containsKey(world.getName().hashCode()))
				throw new IllegalArgumentException("This world ('" + world.getName() + "') isn't logged");
		}
		if (session != null)
			session.lastQuery = clone();
	}

	public void setPlayer(String playerName) {
		players.clear();
		players.add(playerName);
	}

	public String getTable() {
		return logblock.getConfig().tables.get(world.getName().hashCode());
	}

	public String getTitle() {
		final StringBuilder title = new StringBuilder();
		if (!types.isEmpty()) {
			for (int i = 0; i < types.size(); i++)
				title.append(BukkitUtils.getMaterialName(types.get(i)) + ", ");
			title.deleteCharAt(title.length() - 2);
		} else
			title.append("Block ");
		if (bct == BlockChangeType.CREATED)
			title.append("creations ");
		else if (bct == BlockChangeType.DESTROYED)
			title.append("destructions ");
		else
			title.append("changes ");
		if (!players.isEmpty()) {
			title.append("from player ");
			for (int i = 0; i < players.size(); i++)
				title.append(players.get(i) + ", ");
			title.deleteCharAt(title.length() - 2);
		}
		if (minutes > 0)
			title.append("in the last " + minutes + " minutes ");
		if (minutes < 0)
			title.append("up to " + minutes + " minutes ago ");
		if (loc != null && radius > 0)
			title.append("within " + radius + " blocks of you ");
		if (loc != null && radius == 0)
			title.append("at " + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() + " ");
		else if (sel != null)
			title.append("inside selection ");
		title.append("in " + getfriendlyWorldname());
		title.setCharAt(0, String.valueOf(title.charAt(0)).toUpperCase().toCharArray()[0]);
		return title.toString();
	}

	public String getOrderBy() {
		return "ORDER BY date " + order + ", id " + order + " ";
	}

	public String getLimit() {
		if (limit == -1)
			return "";
		return "LIMIT " + limit;
	}

	public String getWhere() {
		return getWhere(bct);
	}

	public String getWhere(BlockChangeType blockChangeType) {
		final StringBuilder where = new StringBuilder("WHERE ");
		switch (blockChangeType) {
			case ALL:
				if (!types.isEmpty()) {
					where.append('(');
					for (final int type : types)
						where.append("type = " + type + " OR replaced = " + type + " OR ");
					where.delete(where.length() - 4, where.length() - 1);
					where.append(") AND ");
				}
				break;
			case BOTH:
				where.append("type <> replaced AND ");
				if (!types.isEmpty()) {
					where.append('(');
					for (final int type : types)
						where.append("type = " + type + " OR replaced = " + type + " OR ");
					where.delete(where.length() - 4, where.length());
					where.append(") AND ");
				}
				break;
			case CREATED:
				where.append("type <> replaced AND ");
				if (!types.isEmpty()) {
					where.append('(');
					for (final int type : types)
						where.append("type = " + type + " OR ");
					where.delete(where.length() - 4, where.length());
					where.append(") AND ");
				} else
					where.append("type > 0 AND ");
				break;
			case DESTROYED:
				where.append("type <> replaced AND ");
				if (!types.isEmpty()) {
					where.append('(');
					for (final int type : types)
						where.append("replaced = " + type + " OR ");
					where.delete(where.length() - 4, where.length());
					where.append(") AND ");
				} else
					where.append("replaced > 0 AND ");
				break;
			case CHESTACCESS:
				where.append("type = replaced AND (type = 23 OR type = 54 OR type = 61) AND ");
				break;
		}
		if (!players.isEmpty() && sum != SummarizationMode.PLAYERS) {
			where.append('(');
			for (final String playerName : players)
				where.append("playername = '" + playerName + "' OR ");
			where.delete(where.length() - 4, where.length());
			where.append(") AND ");
		}
		if (loc != null) {
			if (radius == 0)
				where.append("x = '" + loc.getBlockX() + "' AND y = '" + loc.getBlockY() + "' AND z = '" + loc.getBlockZ() + "' AND ");
			else if (radius > 0)
				where.append("x > '" + (loc.getBlockX() - radius) + "' AND x < '" + (loc.getBlockX() + radius) + "' AND z > '" + (loc.getBlockZ() - radius) + "' AND z < '" + (loc.getBlockZ() + radius) + "' AND ");
		} else if (sel != null)
			where.append("x >= '" + sel.getMinimumPoint().getBlockX() + "' AND x <= '" + sel.getMaximumPoint().getBlockX() + "' AND y >= '" + sel.getMinimumPoint().getBlockY() + "' AND y <= '" + sel.getMaximumPoint().getBlockY() + "' AND z >= '" + sel.getMinimumPoint().getBlockZ() + "' AND z <= '" + sel.getMaximumPoint().getBlockZ() + "' AND ");
		if (minutes > 0)
			where.append("date > date_sub(now(), INTERVAL " + minutes + " MINUTE) AND ");
		if (minutes < 0)
			where.append("date < date_sub(now(), INTERVAL " + minutes * -1 + " MINUTE) AND ");
		if (where.length() > 6)
			where.delete(where.length() - 4, where.length());
		else
			where.delete(0, where.length());
		return where.toString();
	}

	public String getQuery() {
		if (sum == SummarizationMode.NONE) {
			final StringBuilder select = new StringBuilder("SELECT ");
			final StringBuilder from = new StringBuilder("FROM `" + getTable() + "` ");
			if (selectFullBlockData)
				select.append("replaced, type, data, x, y, z ");
			else
				select.append("date, replaced, type, playername");
			if (!selectFullBlockData || players.size() > 0)
				from.append("INNER JOIN `lb-players` USING (playerid) ");
			if (types.size() == 0 || types.contains(63) || types.contains(68)) {
				select.append(", signtext");
				from.append("LEFT JOIN `" + getTable() + "-sign` USING (id) ");
			}
			if (types.size() == 0 || types.contains(23) || types.contains(54) || types.contains(61)) {
				select.append(", itemtype, itemamount, itemdata");
				from.append("LEFT JOIN `" + getTable() + "-chest` USING (id) ");
			}
			return select.toString() + " " + from.toString() + getWhere() + getOrderBy() + getLimit();
		} else if (sum == SummarizationMode.TYPES)
			return "SELECT type, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT type, count(type) AS created, 0 AS destroyed FROM `" + getTable() + "` INNER JOIN `lb-players` USING (playerid) " + getWhere() + "AND type > 0 GROUP BY type) UNION (SELECT replaced AS type, 0 AS created, count(replaced) AS destroyed FROM `" + getTable() + "` INNER JOIN `lb-players` USING (playerid) " + getWhere() + "AND replaced > 0 GROUP BY replaced)) AS t GROUP BY type ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
		else
			return "SELECT playername, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(type) AS created, 0 AS destroyed FROM `" + getTable() + "` " + getWhere() + "AND type > 0 GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(replaced) AS destroyed FROM `" + getTable() + "` " + getWhere() + "AND replaced > 0 GROUP BY playerid)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
	}

	private String[] getValues(List<String> args, int offset) {
		int i;
		for (i = offset; i < args.size(); i++)
			if (isKeyWord(args.get(i)))
				break;
		if (i == offset)
			return null;
		final String[] values = new String[i - offset];
		for (int j = offset; j < i; j++)
			values[j - offset] = args.get(j).replace("\"", "");
		return values;
	}

	private String getfriendlyWorldname() {
		String worldName = world.getName();
		worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
		return worldName.substring(worldName.lastIndexOf('\\') + 1);
	}

	protected QueryParams clone() {
		try {
			return (QueryParams)super.clone();
		} catch (final CloneNotSupportedException ex) {}
		return null;
	}

	static boolean isKeyWord(String param) {
		if (keywords.contains(param.toLowerCase().hashCode()))
			return true;
		return false;
	}

	public static enum SummarizationMode {
		NONE, TYPES, PLAYERS
	}

	public static enum BlockChangeType {
		BOTH, CREATED, DESTROYED, CHESTACCESS, ALL
	}

	public static enum Order {
		ASC, DESC
	}
}
