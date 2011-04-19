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
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

public class Consumer extends TimerTask implements Runnable
{
	private final LogBlock logblock;
	private final Logger log;
	private final Config config;
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	private LinkedBlockingQueue<KillRow> kqueue = new LinkedBlockingQueue<KillRow>();
	private HashSet<Integer> hiddenplayers = new HashSet<Integer>();
	private HashMap<Integer, Integer> lastAttackedEntity = new HashMap<Integer, Integer>();
	private HashMap<Integer, Long> lastAttackTime = new HashMap<Integer, Long>();
 	

	Consumer (LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
		config = logblock.getConfig();
	}

	public void queueBlockDestroy(Player player, BlockState before) {
		queueBlockDestroy(player.getName(), before);
	}

	public void queueBlockDestroy(String playerName, BlockState before) {
		queueBlock(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), 0, before.getRawData());
	}

	public void queueBlockPlace(Player player, Location loc, int type, byte data) {
		queueBlockPlace(player.getName(), loc, type, data);
	}

	public void queueBlockPlace(String playerName, Location loc, int type, byte data) {
		queueBlock(playerName, loc, 0, type, data);
	}

	public void queueBlockReplace(Player player, BlockState before, int typeAfter, byte dataAfter) {
		queueBlockReplace(player.getName(), before, typeAfter, dataAfter);
	}

	public void queueBlockReplace(String playerName, BlockState before, int typeAfter, byte dataAfter) {
		queueBlockDestroy(playerName, before);
		queueBlockPlace(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), typeAfter, dataAfter);
	}

	public void queueBlock(String playerName, Location loc, int typeBefore, int typeAfter, byte data) {
		queueBlock(playerName, loc, typeBefore, typeAfter, data, null, null);
	}

	public void queueChestAccess(Player player, BlockState block, short inType, byte inAmount, short outType, byte outAmount) {
		queueChestAccess(player.getName(), new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()), block.getTypeId(), inType, inAmount, outType, outAmount);
	}

	public void queueChestAccess(String playerName, Location loc, int type, short inType, byte inAmount, short outType, byte outAmount) {
		queueBlock(playerName, loc, type, type, (byte)0, null, new ChestAccess(inType, inAmount, outType, outAmount));
	}

	public void queueSign(Player player, Sign sign) {
		queueSign(player.getName(), new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), "sign [" + sign.getLine(0) + "] [" + sign.getLine(1) + "] [" + sign.getLine(2) + "] [" + sign.getLine(3) + "]");
	}

	public void queueSign(String playerName, Location loc, int type, byte data, String signtext) {
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
		queueKill(victim.getWorld().getName(), getEntityName(killer), getEntityName(victim), weapon);
	}

	public void queueKill(String worldName, String killerName, String victimName, int weapon) {
		if (victimName == null || !config.tables.containsKey(worldName.hashCode()))
			return;
		kqueue.add(new KillRow(worldName.hashCode(), killerName, victimName, weapon));
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
