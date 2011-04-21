package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

public class Consumer extends TimerTask implements Runnable
{
	private final LogBlock logblock;
	private final Logger log;
	private final Config config;
	private final LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	private final LinkedBlockingQueue<KillRow> kqueue = new LinkedBlockingQueue<KillRow>();
	private final HashSet<Integer> hiddenplayers = new HashSet<Integer>();
	private final HashMap<Integer, Integer> lastAttackedEntity = new HashMap<Integer, Integer>();
	private final HashMap<Integer, Long> lastAttackTime = new HashMap<Integer, Long>();
 	

	Consumer (LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
		config = logblock.getConfig();
	}

	/**
	 * Logs a block break. The type afterwards is assumed to be o (air).
	 * @param before Blockstate of the block before actually being destroyed.
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
	 * @param after Blockstate of the block after actually being placed.
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
	 * @param before Blockstate of the block before actually being destroyed.
	 * @param after Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(String playerName, BlockState before, BlockState after) {
		queueBlockBreak(playerName, before);
		queueBlockPlace(playerName, after);
	}

	public void queueBlockReplace(String playerName, Location loc, int typeBefore, byte dataBefore, int typeAfter, byte dataAfter) {
		queueBlockBreak(playerName, loc, typeBefore, dataBefore);
		queueBlockPlace(playerName, loc, typeAfter, dataAfter);
	}

	/**
	 * @param before Blockstate of the block before actually being destroyed.
	 */
	public void queueBlockReplace(String playerName, BlockState before, int typeAfter, byte dataAfter) {
		queueBlockBreak(playerName, before);
		queueBlockPlace(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), typeAfter, dataAfter);
	}

	/**
	 * @param after Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(String playerName, int typeBefore, byte dataBefore, BlockState after) {
		queueBlockBreak(playerName, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), typeBefore, dataBefore);
		queueBlockPlace(playerName, after);
	}

	/**
	 * Logs any block change. Don't try to combine broken and placed blocks. Queue two block changes or use the queueBLockReplace methods.
	 */
	public void queueBlock(String playerName, Location loc, int typeBefore, int typeAfter, byte data) {
		queueBlock(playerName, loc, typeBefore, typeAfter, data, null, null);
	}

	/**
	 * @param container The respective container. Must be an instance of Chest, Dispencer or Furnace.
	 * @param inType Type id of the item traded in. May be 0 to indicate nothing traded in.
	 * @param outType Type id of the item traded out. May be 0 to indicate nothing traded out.
	 */
	public void queueChestAccess(String playerName, BlockState container, short inType, byte inAmount, short outType, byte outAmount) {
		if (!(container instanceof Chest) && !(container instanceof Furnace) && !(container instanceof Dispenser))
			return;
		queueChestAccess(playerName, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getTypeId(), inType, inAmount, outType, outAmount);
	}

	/**
	 * @param type Type id of the container. Must be 63 or 68.
	 * @param inType Type id of the item traded in. May be 0 to indicate nothing traded in.
	 * @param outType Type id of the item traded out. May be 0 to indicate nothing traded out.
	 */
	public void queueChestAccess(String playerName, Location loc, int type, short inType, byte inAmount, short outType, byte outAmount) {
		queueBlock(playerName, loc, type, type, (byte)0, null, new ChestAccess(inType, inAmount, outType, outAmount));
	}

	public void queueSign(String playerName, Sign sign) {
		queueSign(playerName, new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), "sign [" + sign.getLine(0) + "] [" + sign.getLine(1) + "] [" + sign.getLine(2) + "] [" + sign.getLine(3) + "]");
	}

	/**
	 * @param type Type of the sign. Must be 63 or 68.
	 * @param signtext The whole text on the sign in a format like: sign [] [] [] []
	 */
	public void queueSign(String playerName, Location loc, int type, byte data, String signtext) {
		if (type != 63 && type != 68)
			return;
		queueBlock(playerName, loc, 0, type, data, signtext, null);
	}

	private void queueBlock(String playerName, Location loc, int typeBefore, int typeAfter, byte data, String signtext, ChestAccess ca) {
		if (playerName == null || loc == null || typeBefore < 0 || typeAfter < 0)
			return;
		if (hiddenplayers.contains(playerName.hashCode()))
			return;
		if (!config.tables.containsKey(loc.getWorld().getName().hashCode()))
			return;
		if (playerName.length() > 32)
			playerName = playerName.substring(0, 32);
		if (signtext != null)
			signtext = signtext.replace("\\", "\\\\").replace("'", "\\'");
		BlockRow row = new BlockRow(loc.getWorld().getName().hashCode(), playerName, typeBefore, typeAfter, data, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), signtext, ca);
		if (!bqueue.offer(row))
			log.info("[LogBlock] Failed to queue block for " + playerName);
	}

	/**
	 * @param killer Can' be null
	 * @param victim Can' be null
	 */
	public void queueKill(Entity killer, Entity victim) {
		if (killer  == null || victim  == null)
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
	 * @param world World the victim was inside.
	 * @param killerName Name of the killer. Can be null.
	 * @param victimName Name of the victim. Can't be null.
	 * @param weapon Item id of the weapon. 0 for no weapon.
	 */
	public void queueKill(World world, String killerName, String victimName, int weapon) {
		if (victimName == null || !config.tables.containsKey(world.getName().hashCode()))
			return;
		kqueue.add(new KillRow(world.getName().hashCode(), killerName, victimName, weapon));
	}

	int getQueueSize() {
		return bqueue.size() + kqueue.size();
	}

	boolean hide(Player player) {
		int hash = player.getName().hashCode();
		if (hiddenplayers.contains(hash)) {
			hiddenplayers.remove(hash);
			return false;
		} else {
			hiddenplayers.add(hash);
			return true;
		}
	}

	public synchronized void run() {
		Connection conn = logblock.getConnection();
		if (conn == null)
			return;
		Statement state = null;
		BlockRow b; KillRow k; String table;
		int count = 0;
		if (bqueue.size() > 100)
			log.info("[LogBlock Consumer] Queue overloaded. Size: " + bqueue.size());				
		try {
			conn.setAutoCommit(false);
			state = conn.createStatement();
			long start = System.currentTimeMillis();
			while (count < 1000 && !bqueue.isEmpty() && (System.currentTimeMillis() - start < 100 || count < 100)) {
				b = bqueue.poll();
				if (b == null)
					continue;
				table = config.tables.get(b.worldHash);
				state.execute("INSERT INTO `" + table + "` (date, playerid, replaced, type, data, x, y, z) SELECT now(), playerid, " + b.replaced + ", " + b.type + ", " + b.data + ", '" + b.x + "', " + b.y + ", '" + b.z + "' FROM `lb-players` WHERE playername = '" + b.name + "'", Statement.RETURN_GENERATED_KEYS);
				if (b.signtext != null) {
					ResultSet keys = state.getGeneratedKeys();
					if (keys.next())
						state.execute("INSERT INTO `" + table + "-sign` (id, signtext) values (" + keys.getInt(1) + ", '" + b.signtext + "')");
					else
						log.severe("[LogBlock Consumer] Failed to get generated keys");
				} else if (b.ca != null) {
					ResultSet keys = state.getGeneratedKeys();
					if (keys.next())
						state.execute("INSERT INTO `" + table + "-chest` (id, intype, inamount, outtype, outamount) values (" + keys.getInt(1) + ", " + b.ca.inType + ", " + b.ca.inAmount + ", " + b.ca.outType + ", " + b.ca.outAmount + ")");
					else
						log.severe("[LogBlock Consumer] Failed to get generated keys");
				}
				count++;
			}
			conn.commit();
			while (!kqueue.isEmpty() && count < 1000 && (System.currentTimeMillis() - start < 100 || count < 100)) {
				k = kqueue.poll();
				if (k == null)
					continue;
				state.execute("INSERT INTO `" + config.tables.get(k.worldHash) + "-kills` (date, killer, victim, weapon) SELECT now(), playerid, (SELECT playerid FROM `lb-players` WHERE playername = '" + k.victim + "'), " + k.weapon + " FROM `lb-players` WHERE playername = '" + k.killer + "'");
			}
			conn.commit();
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception", ex);
		} finally {
			try {
				if (conn != null)
					conn.close();
				if (state != null)
					state.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception on close", ex);
			}
		}
	}

	private String getEntityName(Entity entity) {
		if (entity instanceof Player)
			return ((Player)entity).getName();
		if (entity instanceof TNTPrimed)
			return "TNT";
		return entity.getClass().getSimpleName().substring(5);
	}

	private class ChestAccess
	{
		public short inType, outType;
		public byte inAmount, outAmount;

		ChestAccess(short inType, byte inAmount, short outType, byte outAmount) {
			this.inType = inType;
			this.inAmount = inAmount;
			this.outType = outType;
			this.outAmount = outAmount;
		}
	}

	private class BlockRow
	{
		public final int worldHash;
		public final String name;
		public final int replaced, type;
		public final byte data;
		public final int x, y, z;
		public final String signtext;
		public final ChestAccess ca;

		BlockRow(int worldHash, String name, int replaced, int type, byte data, int x, int y, int z, String signtext, ChestAccess ca)	{
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

	private class KillRow
	{
		public final int worldHash;
		public final String killer;
		public final String victim;
		public final int weapon;

		KillRow(int worldHash, String attacker, String defender, int weapon) {
			this.worldHash = worldHash;
			this.killer = attacker;
			this.victim = defender;
			this.weapon = weapon;
		}

		@Override
		public int hashCode() {
			return killer.hashCode() * 31 + victim.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			KillRow k = (KillRow)obj;
			if (!killer.equals(k.killer))
				return false;
			if (!victim.equals(k.victim))
				return false;
			return true;
		}
	}
}
