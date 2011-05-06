package de.diddiz.LogBlock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

public class QueryParams
{
	private final LogBlock logblock;
	private final List<String> players = new ArrayList<String>();
	private final List<Integer> types = new ArrayList<Integer>();
	private Location loc = null;
	private int radius = -1;
	private Selection sel = null;
	private int minutes = -1;
	private SummarizationMode sum = SummarizationMode.NONE;;
	private BlockChangeType bct = BlockChangeType.BOTH;
	private int limit = 15;
	private World world = null;
	private Order order = Order.DESC;

	public QueryParams(LogBlock logblock) {
		this.logblock = logblock;
	}

	public void merge (QueryParams params) {
		loc = params.getLoc();
		radius = params.getRadius();
		sel = params.getSel();
		minutes = params.getMinutes();
		sum = params.getSummarizationMode();
		bct = params.getBlockChangeType();
		limit = params.limit;
		world = params.getWorld();
		order = params.getOrder();
	}

	public void parseArgs(CommandSender sender, List<String> args) throws Exception {
		Player player = null;
		if (sender instanceof Player)
			player = (Player)sender;
		String name = "Console";
		if (player != null)
			name = player.getName();
		final Session session = logblock.getSession(name);
		if (!args.isEmpty() && args.get(0).equalsIgnoreCase("last") && session.getLastQuery() != null)
			merge(session.getLastQuery());
		if (player != null && world == null)
			world = player.getWorld();
		for (int i = 0; i < args.size(); i++) {
			final String param = args.get(i).toLowerCase();
			final String[] values = getValues(args, i +1);
			if (values != null)
				i += values.length;
			if (param.equals("player")) {
				if (values == null || values.length < 1)
					throw new Exception("No or wrong count of arguments for '" + param + "'");
				for (final String playerName : values) {
					if (playerName.length() > 0)
						players.add(playerName);
				}
			} else if (param.equals("block")) {
				if (values == null || values.length < 1)
					throw new Exception("No or wrong count of arguments for '" + param + "'");
				for (final String blockName : values) {
					final Material mat = Material.matchMaterial(blockName);
					if (mat == null)
						throw new Exception("No material matching: '" + blockName + "'");
					types.add(mat.getId());
				}
			} else if (param.equals("area")) {
				if (player == null)
					throw new Exception("You have to ba a player to use area");
				if (values == null) {
					radius = logblock.getConfig().defaultDist;
					loc = player.getLocation();
				} else {
					if (!isInt(values[0]))
						throw new Exception("Not a number: '" + values[0] + "'");
					radius = Integer.parseInt(values[0]);
					loc = player.getLocation();
				}
			} else if (param.equals("selection")) {
				if (player == null)
					throw new Exception("You have to ba a player to use selection");
				final Plugin we = player.getServer().getPluginManager().getPlugin("WorldEdit");
				if (we == null)
					throw new Exception("WorldEdit plugin not found");
				final Selection sel = ((WorldEditPlugin)we).getSelection(player);
				if (sel == null)
					throw new Exception("No selection defined");
				if (!(sel instanceof CuboidSelection))
					throw new Exception("You have to define a cuboid selection");
				this.sel = sel;
			} else if (param.equals("time")) {
				if (values == null)
					minutes = logblock.getConfig().defaultTime;
				else {
					if (values.length != 2)
						throw new Exception("Wrong count of arguments for '" + param + "'");
					if (!isInt(values[0]))
						throw new Exception("Not a number: '" + values[0] + "'");
					minutes = Integer.parseInt(values[0]);
					if (values[1].startsWith("h"))
						minutes *= 60;
					else if (values[1].startsWith("d"))
						minutes *= 60*24;
				}
			} else if (param.equals("since")) {
				throw new Exception("Since parameter not implemented yet");
			} else if (param.equals("sum")) {
				if (values == null || values.length != 1)
					throw new Exception("No or wrong count of arguments for '" + param + "'");
				if (values[0].startsWith("p"))
					sum = SummarizationMode.PLAYERS;
				else if (values[0].startsWith("b"))
					sum = SummarizationMode.TYPES;
				else if (values[0].startsWith("n"))
					sum = SummarizationMode.NONE;
				else
					throw new Exception("Wrong summarization mode");
			} else if (param.equals("created")) {
				bct = BlockChangeType.CREATED;
			} else if (param.equals("destroyed")) {
				bct = BlockChangeType.DESTROYED;
			} else if (param.equals("chestaccess")) {
				bct = BlockChangeType.CHESTACCESS;
			} else if (param.equals("limit")) {
				if (values.length != 1)
					throw new Exception("Wrong count of arguments for '" + param + "'");
				if (!isInt(values[0]))
					throw new Exception("Not a number: '" + values[0] + "'");
				limit = Integer.parseInt(values[0]);
			} else if (param.equals("world")) {
				if (values.length != 1)
					throw new Exception("Wrong count of arguments for '" + param + "'");
				if (sender.getServer().getWorld(values[0]) == null)
					throw new Exception("There is no world called '" + values[0] + "'");
				world = sender.getServer().getWorld(values[0]);
			} else if (param.equals("ASC")) {
				order = Order.ASC;
			} else if (param.equals("DESC")) {
				order = Order.DESC;
			} else
				throw new Exception("Not a valid argument: '" + param + "'");
		}
		if (world == null)
			throw new Exception("No world specified");
		if (!logblock.getConfig().tables.containsKey(world.getName().hashCode()))
			throw new Exception("This world ('" + world.getName() + "') isn't logged");
		session.setLast(this);
	}

	public void setPlayer(String playerName) {
		players.clear();
		players.add(playerName);
	}

	public List<String> getPlayers() {
		return players;
	}

	public List<Integer> getTypes() {
		return types;
	}

	public Location getLoc() {
		return loc;
	}

	public int getRadius() {
		return radius;
	}

	public Selection getSel() {
		return sel;
	}

	public int getMinutes() {
		return minutes;
	}

	public int getLimit() {
		return limit;
	}

	public Order getOrder() {
		return order;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public SummarizationMode getSummarizationMode() {
		return sum;
	}

	public void setSummarizationMode(SummarizationMode sum) {
		this.sum = sum;
	}

	public void setBlockChangeType(BlockChangeType bct) {
		this.bct = bct;
	}

	public BlockChangeType getBlockChangeType() {
		return bct;
	}

	public World getWorld() {
		return world;
	}

	public String getTable() {
		return logblock.getConfig().tables.get(world.getName().hashCode());
	}

	public String getTitle() {
		final StringBuffer buffer = new StringBuffer();
		if (!types.isEmpty()) {
			for (int i = 0; i < types.size(); i++) {
				buffer.append(getMaterialName(types.get(i)) + ", ");
			}
			buffer.deleteCharAt(buffer.length() - 2);
		} else
			buffer.append("Block ");
		if (bct == BlockChangeType.CREATED)
			buffer.append("creations ");
		else if  (bct == BlockChangeType.DESTROYED)
			buffer.append("destructions ");
		else
			buffer.append("changes ");
		if (!players.isEmpty()) {
			buffer.append("from player ");
			for (int i = 0; i < players.size(); i++) {
				buffer.append(players.get(i) + ", ");
			}
			buffer.deleteCharAt(buffer.length() - 2);
		}
		if (loc != null && radius >= 0)
			buffer.append("within " + radius + " blocks of you:");
		else if (sel != null)
			buffer.append("in selection:");
		else
			buffer.append("in entire world:");
		buffer.setCharAt(0,String.valueOf(buffer.charAt(0)).toUpperCase().toCharArray()[0]);
		return buffer.toString();
	}

	public String getQuery() {
		final StringBuffer where = new StringBuffer();
		switch (bct) {
		case ALL:
			if (!types.isEmpty()) {
				where.append('(');
				for (final int type: types) {
					where.append("type = " + type + " OR replaced = " + type + " OR ");
				}
				where.delete(where.length() - 5, where.length() - 1);
				where.append(") AND ");
			}
			break;
		case BOTH:
			where.append("type <> replaced AND ");
			if (!types.isEmpty()) {
				where.append('(');
				for (final int type: types) {
					where.append("type = " + type + " OR replaced = " + type + " OR ");
				}
				where.delete(where.length() - 5, where.length() - 1);
				where.append(") AND ");
			}
			break;
		case CREATED:
			where.append("type <> replaced AND ");
			if (!types.isEmpty()) {
				where.append('(');
				for (final int type: types) {
					where.append("type = " + type + " OR ");
				}
				where.delete(where.length() - 5, where.length() - 1);
				where.append(") AND ");
			} else
				where.append("type > 0 AND ");
			break;
		case DESTROYED:
			where.append("type <> replaced AND ");
			if (!types.isEmpty()) {
				where.append('(');
				for (final int type: types) {
					where.append("replaced = " + type + " OR ");
				}
				where.delete(where.length() - 5, where.length() - 1);
				where.append(") AND ");
			} else
				where.append("replaced > 0 AND ");
			break;
		case CHESTACCESS:
			where.append("type = replaced AND (type = 23 OR type = 54 OR type = 61) ");
			break;
		}
		if (!players.isEmpty() && sum != SummarizationMode.PLAYERS) {
			for (final String playerName: players) {
				where.append("playername = '" + playerName + "' AND ");
			}
		}
		if (loc != null && radius >= 0)
			where.append("x > '" + (loc.getBlockX() - radius) + "' AND x < '" + (loc.getBlockX() + radius) + "' AND z > '" + (loc.getBlockZ() - radius) + "' AND z < '" + (loc.getBlockZ() + radius) + "' AND ");
		else if (sel != null)
			where.append("x >= '"+ sel.getMinimumPoint().getBlockX() + "' AND x <= '" + sel.getMaximumPoint().getBlockX() + "' AND y >= '" + sel.getMinimumPoint().getBlockY() + "' AND y <= '" + sel.getMaximumPoint().getBlockY() + "' AND z >= '" + sel.getMinimumPoint().getBlockZ() + "' AND z <= '" + sel.getMaximumPoint().getBlockZ() + "' AND ");
		if (minutes >= 0)
			where.append("date > date_sub(now(), INTERVAL " + minutes + " MINUTE) AND ");
		where.delete(where.length() - 5, where.length() - 1);
		if (sum == SummarizationMode.NONE) {
			final StringBuffer sql = new StringBuffer("SELECT date, replaced, type, playername FROM `" + getTable() + "` INNER JOIN `lb-players` USING (playerid) ");
			if (bct == BlockChangeType.ALL)
				sql.append("LEFT JOIN `" + getTable() + "-sign` USING (id) ");
			if (bct == BlockChangeType.ALL || bct == BlockChangeType.CHESTACCESS)
				sql.append("LEFT JOIN `" + getTable() + "-chest` USING (id) ");
			sql.append("WHERE " + where + "ORDER BY date " + order + ", id " + order + " ");
			if (limit > 0)
				sql.append("LIMIT " + limit);
			return sql.toString();
		} else if (sum == SummarizationMode.TYPES)
			return "SELECT type, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT type, count(type) AS created, 0 AS destroyed FROM `" + getTable() + "` INNER JOIN `lb-players` USING (playerid) WHERE " + where + "GROUP BY type) UNION (SELECT replaced AS type, 0 AS created, count(replaced) AS destroyed FROM `" + getTable() + "` INNER JOIN `lb-players` USING (playerid) WHERE " + where + "GROUP BY replaced)) AS t GROUP BY type ORDER BY SUM(created) + SUM(destroyed) DESC LIMIT 15";
		else {
			return "SELECT playername, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(type) AS created, 0 AS destroyed FROM `" + getTable() + "` WHERE " + where + "GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(replaced) AS destroyed FROM `" + getTable() + "` WHERE " + where + "GROUP BY playerid)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) DESC LIMIT 15";
		}
	}

	private String getMaterialName(int type) {
		return Material.getMaterial(type).toString().toLowerCase().replace('_', ' ');
	}

	private String[] getValues(List<String> args, int offset) {
		int i;
		for (i = offset; i < args.size(); i++) {
			if (isKeyWord(args.get(i)))
				break;
		}
		if (i == offset)
			return null;
		else {
			final String[] values = new String[i - offset];
			System.arraycopy(args.toArray(), offset, values, 0, i - offset);
			return values;
		}
	}

	private boolean isInt(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (final Exception ex) {
			return false;
		}
	}

	public static boolean isKeyWord(String param) {
		final String key = new String(param).toLowerCase();
		if (key.equals("player") || key.equals("area") || key.equals("selection") || key.equals("block") || key.equals("sum") || key.equals("destroyed") || key.equals("created") || key.equals("time") || key.equals("since") || key.equals("limit") || key.equals("world") || key.equals("asc") || key.equals("desc"))
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
