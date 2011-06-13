package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.compressInventory;
import static de.diddiz.util.BukkitUtils.getEntityName;
import static de.diddiz.util.BukkitUtils.rawData;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Consumer extends TimerTask
{
	private final LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	private final Config config;
	private final Set<Integer> hiddenplayers;
	private final LinkedBlockingQueue<KillRow> kqueue = new LinkedBlockingQueue<KillRow>();
	private final HashMap<Integer, Integer> lastAttackedEntity = new HashMap<Integer, Integer>();
	private final HashMap<Integer, Long> lastAttackTime = new HashMap<Integer, Long>();
	private final Logger log;
	private final LogBlock logblock;
	private final HashSet<Integer> players = new HashSet<Integer>();

	Consumer(LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
		config = logblock.getConfig();
		hiddenplayers = config.hiddenPlayers;
	}

	/**
	 * Logs any block change. Don't try to combine broken and placed blocks. Queue two block changes or use the queueBLockReplace methods.
	 */
	public void queueBlock(String playerName, Location loc, int typeBefore, int typeAfter, byte data) {
		queueBlock(playerName, loc, typeBefore, typeAfter, data, null, null);
	}

	/**
	 * Logs a block break. The type afterwards is assumed to be o (air).
	 * 
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 */
	public void queueBlockBreak(String playerName, BlockState before) {
		queueBlockBreak(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData());
	}

	/**
	 * Logs a block break. The block type afterwards is assumed to be o (air).
	 */
	public void queueBlockBreak(String playerName, Location loc, int typeBefore, byte dataBefore) {
		queueBlock(playerName, loc, typeBefore, 0, dataBefore);
	}

	/**
	 * Logs a block place. The block type before is assumed to be o (air).
	 * 
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockPlace(String playerName, BlockState after) {
		queueBlockPlace(playerName, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), after.getTypeId(), after.getRawData());
	}

	/**
	 * Logs a block place. The block type before is assumed to be o (air).
	 */
	public void queueBlockPlace(String playerName, Location loc, int type, byte data) {
		queueBlock(playerName, loc, 0, type, data);
	}

	/**
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(String playerName, BlockState before, BlockState after) {
		queueBlockReplace(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData(), after.getTypeId(), after.getRawData());
	}

	/**
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 */
	public void queueBlockReplace(String playerName, BlockState before, int typeAfter, byte dataAfter) {
		queueBlockReplace(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData(), typeAfter, dataAfter);
	}

	/**
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(String playerName, int typeBefore, byte dataBefore, BlockState after) {
		queueBlockReplace(playerName, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), typeBefore, dataBefore, after.getTypeId(), after.getRawData());
	}

	public void queueBlockReplace(String playerName, Location loc, int typeBefore, byte dataBefore, int typeAfter, byte dataAfter) {
		if (dataBefore == 0)
			queueBlock(playerName, loc, typeBefore, typeAfter, dataAfter);
		else {
			queueBlockBreak(playerName, loc, typeBefore, dataBefore);
			queueBlockPlace(playerName, loc, typeAfter, dataAfter);
		}
	}

	/**
	 * @param container
	 * The respective container. Must be an instance of Chest, Dispencer or Furnace.
	 */
	public void queueChestAccess(String playerName, BlockState container, short itemType, short itemAmount, byte itemData) {
		if (!(container instanceof ContainerBlock))
			return;
		queueChestAccess(playerName, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getTypeId(), itemType, itemAmount, itemData);
	}

	/**
	 * @param type
	 * Type id of the container. Must be 63 or 68.
	 */
	public void queueChestAccess(String playerName, Location loc, int type, short itemType, short itemAmount, byte itemData) {
		queueBlock(playerName, loc, type, type, (byte)0, null, new ChestAccess(itemType, itemAmount, itemData));
	}

	/**
	 * Logs a container block break. The block type before is assumed to be o (air). All content is assumed to be taken.
	 * 
	 * @param container
	 * Must be instanceof ContainerBlock
	 */
	public void queueContainerBreak(String playerName, BlockState container) {
		if (!(container instanceof ContainerBlock))
			return;
		queueContainerBreak(playerName, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getTypeId(), container.getRawData(), ((ContainerBlock)container).getInventory());
	}

	/**
	 * Logs a container block break. The block type before is assumed to be o (air). All content is assumed to be taken.
	 */
	public void queueContainerBreak(String playerName, Location loc, int type, byte data, Inventory inv) {
		final ItemStack[] items = compressInventory(inv.getContents());
		for (final ItemStack item : items)
			queueChestAccess(playerName, loc, type, (short)item.getTypeId(), (short)(item.getAmount() * -1), rawData(item));
		queueBlockBreak(playerName, loc, type, data);
	}

	/**
	 * @param killer
	 * Can' be null
	 * @param victim
	 * Can' be null
	 */
	public void queueKill(Entity killer, Entity victim) {
		if (killer == null || victim == null)
			return;
		if (lastAttackedEntity.containsKey(killer.getEntityId()) && lastAttackedEntity.get(killer.getEntityId()) == victim.getEntityId() && System.currentTimeMillis() - lastAttackTime.get(killer.getEntityId()) < 5000)
			return;
		int weapon = 0;
		if (killer instanceof Player && ((Player)killer).getItemInHand() != null)
			weapon = ((Player)killer).getItemInHand().getTypeId();
		lastAttackedEntity.put(killer.getEntityId(), victim.getEntityId());
		lastAttackTime.put(killer.getEntityId(), System.currentTimeMillis());
		queueKill(victim.getWorld(), getEntityName(killer), getEntityName(victim), weapon);
	}

	/**
	 * @param world
	 * World the victim was inside.
	 * @param killerName
	 * Name of the killer. Can be null.
	 * @param victimName
	 * Name of the victim. Can't be null.
	 * @param weapon
	 * Item id of the weapon. 0 for no weapon.
	 */
	public void queueKill(World world, String killerName, String victimName, int weapon) {
		if (victimName == null || !config.tables.containsKey(world.getName().hashCode()))
			return;
		kqueue.add(new KillRow(world.getName().hashCode(), killerName, victimName, weapon));
	}

	/**
	 * @param type
	 * Type of the sign. Must be 63 or 68.
	 * @param lines
	 * The four lines on the sign.
	 */
	public void queueSignBreak(String playerName, Location loc, int type, byte data, String[] lines) {
		if (type != 63 && type != 68 || lines == null || lines.length != 4)
			return;
		queueBlock(playerName, loc, type, 0, data, lines[0] + "\0" + lines[1] + "\0" + lines[2] + "\0" + lines[3], null);
	}

	public void queueSignBreak(String playerName, Sign sign) {
		queueSignBreak(playerName, new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), sign.getLines());
	}

	/**
	 * @param type
	 * Type of the sign. Must be 63 or 68.
	 * @param lines
	 * The four lines on the sign.
	 */
	public void queueSignPlace(String playerName, Location loc, int type, byte data, String[] lines) {
		if (type != 63 && type != 68 || lines == null || lines.length != 4)
			return;
		queueBlock(playerName, loc, 0, type, data, lines[0] + "\0" + lines[1] + "\0" + lines[2] + "\0" + lines[3], null);
	}

	public void queueSignPlace(String playerName, Sign sign) {
		queueSignPlace(playerName, new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), sign.getLines());
	}

	@Override
	public synchronized void run() {
		final Connection conn = logblock.getConnection();
		if (conn == null)
			return;
		Statement state = null;
		BlockRow b;
		KillRow k;
		String table;
		if (getQueueSize() > 1000)
			log.info("[LogBlock Consumer] Queue overloaded. Size: " + getQueueSize());
		try {
			conn.setAutoCommit(false);
			state = conn.createStatement();
			final long start = System.currentTimeMillis();
			int count = 0;
			if (!bqueue.isEmpty()) {
				while (!bqueue.isEmpty() && (System.currentTimeMillis() - start < config.timePerRun || count < config.forceToProcessAtLeast)) {
					b = bqueue.poll();
					if (b == null)
						continue;
					if (!players.contains(b.name.hashCode()))
						if (!addPlayer(conn, state, b.name)) {
							log.warning("[LogBlock Consumer] Failed to add player " + b.name);
							continue;
						}
					table = config.tables.get(b.worldHash);
					state.execute("INSERT INTO `" + table + "` (date, playerid, replaced, type, data, x, y, z) SELECT now(), playerid, " + b.replaced + ", " + b.type + ", " + b.data + ", '" + b.x + "', " + b.y + ", '" + b.z + "' FROM `lb-players` WHERE playername = '" + b.name + "'", Statement.RETURN_GENERATED_KEYS);
					if (b.signtext != null) {
						final ResultSet keys = state.getGeneratedKeys();
						if (keys.next())
							state.execute("INSERT INTO `" + table + "-sign` (id, signtext) values (" + keys.getInt(1) + ", '" + b.signtext + "')");
						else
							log.warning("[LogBlock Consumer] Failed to get generated keys. Unable to log sign text.");
					} else if (b.ca != null) {
						final ResultSet keys = state.getGeneratedKeys();
						if (keys.next())
							state.execute("INSERT INTO `" + table + "-chest` (id, itemtype, itemamount, itemdata) values (" + keys.getInt(1) + ", " + b.ca.itemType + ", " + b.ca.itemAmount + ", " + b.ca.itemData + ")");
						else
							log.warning("[LogBlock Consumer] Failed to get generated keys. Unable to log chest access.");
					}
					count++;
					if (count % 100 == 0)
						conn.commit();
				}
				conn.commit();
			}
			if (!kqueue.isEmpty()) {
				while (!kqueue.isEmpty() && (System.currentTimeMillis() - start < config.timePerRun || count < config.forceToProcessAtLeast)) {
					k = kqueue.poll();
					if (k == null)
						continue;
					if (!players.contains(k.killer.hashCode()))
						if (!addPlayer(conn, state, k.killer)) {
							log.warning("[LogBlock Consumer] Failed to add player " + k.killer);
							continue;
						}
					state.execute("INSERT INTO `" + config.tables.get(k.worldHash) + "-kills` (date, killer, victim, weapon) SELECT now(), playerid, (SELECT playerid FROM `lb-players` WHERE playername = '" + k.victim + "'), " + k.weapon + " FROM `lb-players` WHERE playername = '" + k.killer + "'");
					count++;
					if (count % 100 == 0)
						conn.commit();
				}
				conn.commit();
			}
		} catch (final SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				conn.close();
			} catch (final SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception on close", ex);
			}
		}
	}

	int getQueueSize() {
		return bqueue.size() + kqueue.size();
	}

	boolean hide(Player player) {
		final int hash = player.getName().hashCode();
		if (hiddenplayers.contains(hash)) {
			hiddenplayers.remove(hash);
			return false;
		}
		hiddenplayers.add(hash);
		return true;
	}

	private boolean addPlayer(Connection conn, Statement state, String playerName) throws SQLException {
		state.execute("INSERT IGNORE INTO `lb-players` (playername) VALUES ('" + playerName + "')");
		conn.commit();
		final ResultSet rs = state.executeQuery("SELECT playername FROM `lb-players`");
		while (rs.next()) {
			final String name = rs.getString(1);
			if (name.equalsIgnoreCase(playerName))
				players.add(playerName.hashCode());
			else
				players.add(name.hashCode());
		}
		rs.close();
		return players.contains(playerName.hashCode());
	}

	private void queueBlock(String playerName, Location loc, int typeBefore, int typeAfter, byte data, String signtext, ChestAccess ca) {
		if (playerName == null || loc == null || typeBefore < 0 || typeAfter < 0 || hiddenplayers.contains(playerName.hashCode()) || !config.tables.containsKey(loc.getWorld().getName().hashCode()))
			return;
		if (playerName.length() > 32)
			playerName = playerName.substring(0, 32);
		if (signtext != null)
			signtext = signtext.replace("\\", "\\\\").replace("'", "\\'");
		bqueue.add(new BlockRow(loc.getWorld().getName().hashCode(), playerName, typeBefore, typeAfter, data, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), signtext, ca));
	}

	private static class BlockRow
	{
		final ChestAccess ca;
		final byte data;
		final String name;
		final int replaced, type;
		final String signtext;
		final int worldHash;
		final int x, y, z;

		BlockRow(int worldHash, String name, int replaced, int type, byte data, int x, int y, int z, String signtext, ChestAccess ca) {
			this.worldHash = worldHash;
			this.name = name;
			this.replaced = replaced;
			this.type = type;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
			this.signtext = signtext;
			this.ca = ca;
		}
	}

	private static class ChestAccess
	{
		final byte itemData;
		final short itemType, itemAmount;

		ChestAccess(short itemType, short itemAmount, byte itemData) {
			this.itemType = itemType;
			this.itemAmount = itemAmount;
			if (itemData < 0)
				this.itemData = 0;
			else
				this.itemData = itemData;
		}
	}

	private static class KillRow
	{
		final String killer;
		final String victim;
		final int weapon;
		final int worldHash;

		KillRow(int worldHash, String attacker, String defender, int weapon) {
			this.worldHash = worldHash;
			killer = attacker;
			victim = defender;
			this.weapon = weapon;
		}
	}
}
