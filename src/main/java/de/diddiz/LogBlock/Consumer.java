package de.diddiz.LogBlock;

import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import static de.diddiz.LogBlock.config.Config.*;
import static de.diddiz.util.BukkitUtils.*;
import static org.bukkit.Bukkit.getLogger;
import org.bukkit.projectiles.ProjectileSource;

public class Consumer extends TimerTask
{
	private final Queue<Row> queue = new LinkedBlockingQueue<Row>();
	private final Set<Actor> failedPlayers = new HashSet<Actor>();
	private final LogBlock logblock;
	private final Map<Actor, Integer> playerIds = new HashMap<Actor, Integer>();
	private final Lock lock = new ReentrantLock();

	Consumer(LogBlock logblock) {
		this.logblock = logblock;
		try {
			Class.forName("PlayerLeaveRow");
		} catch (final ClassNotFoundException ex) {
		}
	}

	/**
	 * Logs any block change. Don't try to combine broken and placed blocks. Queue two block changes or use the queueBLockReplace methods.
	 */
	public void queueBlock(Actor actor, Location loc, int typeBefore, int typeAfter, byte data) {
		queueBlock(actor, loc, typeBefore, typeAfter, data, null, null);
	}

	/**
	 * Logs a block break. The type afterwards is assumed to be o (air).
	 *
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 */
	public void queueBlockBreak(Actor actor, BlockState before) {
		queueBlockBreak(actor, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData());
	}

	/**
	 * Logs a block break. The block type afterwards is assumed to be o (air).
	 */
	public void queueBlockBreak(Actor actor, Location loc, int typeBefore, byte dataBefore) {
		queueBlock(actor, loc, typeBefore, 0, dataBefore);
	}

	/**
	 * Logs a block place. The block type before is assumed to be o (air).
	 *
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockPlace(Actor actor, BlockState after) {
		queueBlockPlace(actor, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), after.getBlock().getTypeId(), after.getBlock().getData());
	}

	/**
	 * Logs a block place. The block type before is assumed to be o (air).
	 */
	public void queueBlockPlace(Actor actor, Location loc, int type, byte data) {
		queueBlock(actor, loc, 0, type, data);
	}

	/**
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(Actor actor, BlockState before, BlockState after) {
		queueBlockReplace(actor, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData(), after.getTypeId(), after.getRawData());
	}

	/**
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 */
	public void queueBlockReplace(Actor actor, BlockState before, int typeAfter, byte dataAfter) {
		queueBlockReplace(actor, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData(), typeAfter, dataAfter);
	}

	/**
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(Actor actor, int typeBefore, byte dataBefore, BlockState after) {
		queueBlockReplace(actor, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), typeBefore, dataBefore, after.getTypeId(), after.getRawData());
	}

	public void queueBlockReplace(Actor actor, Location loc, int typeBefore, byte dataBefore, int typeAfter, byte dataAfter) {
		if (dataBefore == 0 && (typeBefore != typeAfter))
			queueBlock(actor, loc, typeBefore, typeAfter, dataAfter);
		else {
			queueBlockBreak(actor, loc, typeBefore, dataBefore);
			queueBlockPlace(actor, loc, typeAfter, dataAfter);
		}
	}

	/**
	 * @param container
	 * The respective container. Must be an instance of an InventoryHolder.
	 */
	public void queueChestAccess(Actor actor, BlockState container, short itemType, short itemAmount, short itemData) {
		if (!(container instanceof InventoryHolder))
			return;
		queueChestAccess(actor, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getTypeId(), itemType, itemAmount, itemData);
	}

	/**
	 * @param type
	 * Type id of the container.
	 */
	public void queueChestAccess(Actor actor, Location loc, int type, short itemType, short itemAmount, short itemData) {
		queueBlock(actor, loc, type, type, (byte)0, null, new ChestAccess(itemType, itemAmount, itemData));
	}

	/**
	 * Logs a container block break. The block type before is assumed to be o (air). All content is assumed to be taken.
	 *
	 * @param container
	 * Must be an instance of InventoryHolder
	 */
	public void queueContainerBreak(Actor actor, BlockState container) {
		if (!(container instanceof InventoryHolder))
			return;
		queueContainerBreak(actor, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getTypeId(), container.getRawData(), ((InventoryHolder)container).getInventory());
	}

	/**
	 * Logs a container block break. The block type before is assumed to be o (air). All content is assumed to be taken.
	 */
	public void queueContainerBreak(Actor actor, Location loc, int type, byte data, Inventory inv) {
		final ItemStack[] items = compressInventory(inv.getContents());
		for (final ItemStack item : items)
			queueChestAccess(actor, loc, type, (short)item.getTypeId(), (short)(item.getAmount() * -1), rawData(item));
		queueBlockBreak(actor, loc, type, data);
	}

	/**
	 * @param killer
	 * Can't be null
	 * @param victim
	 * Can't be null
	 */
	public void queueKill(Entity killer, Entity victim) {
		if (killer == null || victim == null)
			return;
		int weapon = 0;
		Actor killerActor = Actor.actorFromEntity(killer);
		// If it's a projectile kill we want to manually assign the weapon, so check for player before converting a projectile to its source
		if (killer instanceof Player && ((Player)killer).getItemInHand() != null)
			weapon = ((Player)killer).getItemInHand().getTypeId();
		if(killer instanceof Projectile) {
			ProjectileSource ps = ((Projectile)killer).getShooter();
			killerActor = Actor.actorFromProjectileSource(ps);
			weapon = itemIDfromProjectileEntity(killer);
		}

		queueKill(victim.getLocation(), killerActor, Actor.actorFromEntity(victim), weapon);
	}

	/**
	 * This form should only be used when the killer is not an entity e.g. for fall or suffocation damage
	 * @param killer
	 * Can't be null
	 * @param victim
	 * Can't be null
	 */
	public void queueKill(Actor killer, Entity victim) {
		if (killer == null || victim == null)
			return;
		queueKill(victim.getLocation(), killer, Actor.actorFromEntity(victim), 0);
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
	 * @deprecated Use {@link #queueKill(Location,String,String,int)} instead
	 */
	@Deprecated
	public void queueKill(World world, Actor killer, Actor victim, int weapon) {
		queueKill(new Location(world, 0, 0, 0), killer, victim, weapon);
	}

	/**
	 * @param location
	 * Location of the victim.
	 * @param killer
	 * Killer Actor. Can be null.
	 * @param victim
	 * Victim Actor. Can't be null.
	 * @param weapon
	 * Item id of the weapon. 0 for no weapon.
	 */
	public void queueKill(Location location, Actor killer, Actor victim, int weapon) {
		if (victim == null || !isLogged(location.getWorld()))
			return;
		queue.add(new KillRow(location, killer == null ? null : killer, victim, weapon));
	}

	/**
	 * @param type
	 * Type of the sign. Must be 63 or 68.
	 * @param lines
	 * The four lines on the sign.
	 */
	public void queueSignBreak(Actor actor, Location loc, int type, byte data, String[] lines) {
		if (type != 63 && type != 68 || lines == null || lines.length != 4)
			return;
		queueBlock(actor, loc, type, 0, data, lines[0] + "\0" + lines[1] + "\0" + lines[2] + "\0" + lines[3], null);
	}

	public void queueSignBreak(Actor actor, Sign sign) {
		queueSignBreak(actor, new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), sign.getLines());
	}

	/**
	 * @param type
	 * Type of the sign. Must be 63 or 68.
	 * @param lines
	 * The four lines on the sign.
	 */
	public void queueSignPlace(Actor actor, Location loc, int type, byte data, String[] lines) {
		if (type != 63 && type != 68 || lines == null || lines.length != 4)
			return;
		queueBlock(actor, loc, 0, type, data, lines[0] + "\0" + lines[1] + "\0" + lines[2] + "\0" + lines[3], null);
	}

	public void queueSignPlace(Actor actor, Sign sign) {
		queueSignPlace(actor, new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), sign.getLines());
	}

	public void queueChat(Actor player, String message) {
		for (String ignored : Config.ignoredChat) {
			if (message.startsWith(ignored)) {
				return;
			}
		}
		if (hiddenPlayers.contains(player.getName().toLowerCase())) {
			return;
		}
		queue.add(new ChatRow(player, message));
	}

	public void queueJoin(Player player) {
		queue.add(new PlayerJoinRow(player));
	}

	public void queueLeave(Player player) {
		queue.add(new PlayerLeaveRow(player));
	}

	@Override
	public synchronized void run() {
		if (queue.isEmpty() || !lock.tryLock())
			return;
		final Connection conn = logblock.getConnection();
		Statement state = null;
		if (Config.queueWarningSize > 0 && queue.size() >= Config.queueWarningSize) {
			getLogger().info("[Consumer] Queue overloaded. Size: " + getQueueSize());
		}

		try {
			if (conn == null)
				return;
			conn.setAutoCommit(false);
			state = conn.createStatement();
			final long start = System.currentTimeMillis();
			int count = 0;
			process:
			while (!queue.isEmpty() && (System.currentTimeMillis() - start < timePerRun || count < forceToProcessAtLeast)) {
				final Row r = queue.poll();
				if (r == null)
					continue;
				for (final Actor actor : r.getActors()) {
					if (!playerIds.containsKey(actor)) {
						if (!addPlayer(state, actor)) {
							if (!failedPlayers.contains(actor)) {
								failedPlayers.add(actor);
								getLogger().warning("[Consumer] Failed to add player " + actor.getName());
							}
							continue process;
						}
					}
				}
				if (r instanceof PreparedStatementRow) {
					PreparedStatementRow PSRow = (PreparedStatementRow) r;
					if (r instanceof MergeableRow) {
						int batchCount=count;
						// if we've reached our row target but not exceeded our time target, allow merging of up to 50% of our row limit more rows
						if (count > forceToProcessAtLeast) batchCount = forceToProcessAtLeast / 2;
						while(!queue.isEmpty()) {
							MergeableRow mRow = (MergeableRow) PSRow;
							Row s = queue.peek();
							if (s == null) break;
							if (!(s instanceof MergeableRow)) break;
							MergeableRow mRow2 = (MergeableRow) s;
							if (mRow.canMerge(mRow2)) {
								PSRow = mRow.merge((MergeableRow) queue.poll());
								count++;
								batchCount++;
								if (batchCount > forceToProcessAtLeast) break;
							} else break;
						}
					}
					PSRow.setConnection(conn);
					try {
						PSRow.executeStatements();
					} catch (final SQLException ex) {
						getLogger().log(Level.SEVERE, "[Consumer] SQL exception on insertion: ", ex);
						break;
					}
				} else {
					for (final String insert : r.getInserts())
						try {
							state.execute(insert);
						} catch (final SQLException ex) {
							getLogger().log(Level.SEVERE, "[Consumer] SQL exception on " + insert + ": ", ex);
							break process;
						}
				}

				count++;
			}
			conn.commit();
		} catch (final SQLException ex) {
			getLogger().log(Level.SEVERE, "[Consumer] SQL exception", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				if (conn != null)
					conn.close();
			} catch (final SQLException ex) {
				getLogger().log(Level.SEVERE, "[Consumer] SQL exception on close", ex);
			}
			lock.unlock();
		}
	}

	public void writeToFile() throws FileNotFoundException {
		final long time = System.currentTimeMillis();
		final Set<Actor> insertedPlayers = new HashSet<Actor>();
		int counter = 0;
		new File("plugins/LogBlock/import/").mkdirs();
		PrintWriter writer = new PrintWriter(new File("plugins/LogBlock/import/queue-" + time + "-0.sql"));
		while (!queue.isEmpty()) {
			final Row r = queue.poll();
			if (r == null)
				continue;
			for (final Actor actor : r.getActors())
				if (!playerIds.containsKey(actor) && !insertedPlayers.contains(actor)) {
					// Odd query contruction is to work around innodb auto increment behaviour - bug #492
					writer.println("INSERT IGNORE INTO `lb-players` (playername,UUID) SELECT '" + actor.getName() + "','" + actor.getUUID() + "' FROM `lb-players` WHERE NOT EXISTS (SELECT NULL FROM `lb-players` WHERE UUID = '" + actor.getUUID() + "') LIMIT 1;");
					insertedPlayers.add(actor);
				}
			for (final String insert : r.getInserts())
				writer.println(insert);
			counter++;
			if (counter % 1000 == 0) {
				writer.close();
				writer = new PrintWriter(new File("plugins/LogBlock/import/queue-" + time + "-" + counter / 1000 + ".sql"));
			}
		}
		writer.close();
	}

	int getQueueSize() {
		return queue.size();
	}

	static void hide(Player player) {
		hiddenPlayers.add(player.getName().toLowerCase());
	}

	static void unHide(Player player) {
		hiddenPlayers.remove(player.getName().toLowerCase());
	}

	static boolean toggleHide(Player player) {
		final String playerName = player.getName().toLowerCase();
		if (hiddenPlayers.contains(playerName)) {
			hiddenPlayers.remove(playerName);
			return false;
		}
		hiddenPlayers.add(playerName);
		return true;
	}

	private boolean addPlayer(Statement state, Actor actor) throws SQLException {
		// Odd query contruction is to work around innodb auto increment behaviour - bug #492
		String name = actor.getName();
		String uuid = actor.getUUID();
		state.execute("INSERT IGNORE INTO `lb-players` (playername,UUID) SELECT '" + name + "','" + uuid + "' FROM `lb-players` WHERE NOT EXISTS (SELECT NULL FROM `lb-players` WHERE UUID = '" + uuid + "') LIMIT 1;");
		final ResultSet rs = state.executeQuery("SELECT playerid FROM `lb-players` WHERE UUID = '" + uuid + "'");
		if (rs.next())
			playerIds.put(actor, rs.getInt(1));
		rs.close();
		return playerIds.containsKey(actor);
	}

	private void queueBlock(Actor actor, Location loc, int typeBefore, int typeAfter, byte data, String signtext, ChestAccess ca) {

		if (Config.fireCustomEvents) {
			// Create and call the event
			BlockChangePreLogEvent event = new BlockChangePreLogEvent(actor, loc, typeBefore, typeAfter, data, signtext, ca);
			logblock.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) return;

			// Update variables
			actor = event.getOwnerActor();
			loc = event.getLocation();
			typeBefore = event.getTypeBefore();
			typeAfter = event.getTypeAfter();
			data = event.getData();
			signtext = event.getSignText();
			ca = event.getChestAccess();
		}
		// Do this last so LogBlock still has final say in what is being added
		if (actor == null || loc == null || typeBefore < 0 || typeAfter < 0 || (Config.safetyIdCheck && (typeBefore > 255 || typeAfter > 255)) || hiddenPlayers.contains(actor.getName().toLowerCase()) || !isLogged(loc.getWorld()) || typeBefore != typeAfter && hiddenBlocks.contains(typeBefore) && hiddenBlocks.contains(typeAfter)) return;
		queue.add(new BlockRow(loc, actor, typeBefore, typeAfter, data, signtext, ca));
	}

	private String playerID(Actor actor) {
		if (actor == null)
			return "NULL";
		final Integer id = playerIds.get(actor);
		if (id != null)
			return id.toString();
		return "(SELECT playerid FROM `lb-players` WHERE UUID = '" + actor.getUUID() + "')";
	}

	private Integer playerIDAsInt(Actor actor) {
		if (actor == null) {
			return null;
		}
		return playerIds.get(actor);
	}

	private static interface Row
	{
		String[] getInserts();

		/** 
		 * 
		 * @deprecated - Names are not guaranteed to be unique. Use {@link #getActors() }
		 */
		String[] getPlayers();
		
		Actor[] getActors();
	}

	private interface PreparedStatementRow extends Row
	{

		abstract void setConnection(Connection connection);
		abstract void executeStatements() throws SQLException;
	}

	private interface MergeableRow extends PreparedStatementRow
	{
		abstract boolean isUnique();
		abstract boolean canMerge(MergeableRow row);
		abstract MergeableRow merge(MergeableRow second);
	}

	private class BlockRow extends BlockChange implements MergeableRow
	{
		private Connection connection;

		public BlockRow(Location loc, Actor actor, int replaced, int type, byte data, String signtext, ChestAccess ca) {
			super(System.currentTimeMillis() / 1000, loc, actor, replaced, type, data, signtext, ca);
		}

		@Override
		public String[] getInserts() {
			final String table = getWorldConfig(loc.getWorld()).table;
			final String[] inserts = new String[ca != null || signtext != null ? 2 : 1];
			inserts[0] = "INSERT INTO `" + table + "` (date, playerid, replaced, type, data, x, y, z) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(actor) + ", " + replaced + ", " + type + ", " + data + ", '" + loc.getBlockX() + "', " + loc.getBlockY() + ", '" + loc.getBlockZ() + "');";
			if (signtext != null) {
				inserts[1] = "INSERT INTO `" + table + "-sign` (id, signtext) values (LAST_INSERT_ID(), '" + signtext.replace("\\", "\\\\").replace("'", "\\'") + "');";
			}
			else if (ca != null)
				inserts[1] = "INSERT INTO `" + table + "-chest` (id, itemtype, itemamount, itemdata) values (LAST_INSERT_ID(), " + ca.itemType + ", " + ca.itemAmount + ", " + ca.itemData + ");";
			return inserts;
		}

		@Override
		public String[] getPlayers() {
			return new String[]{actor.getName()};
		}
		
		@Override
		public Actor[] getActors() {
			return new Actor[]{actor};
		}

		@Override
		public void setConnection(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void executeStatements() throws SQLException {
			final String table = getWorldConfig(loc.getWorld()).table;

			PreparedStatement ps1 = null;
			PreparedStatement ps = null;
			try {
				ps1 = connection.prepareStatement("INSERT INTO `" + table + "` (date, playerid, replaced, type, data, x, y, z) VALUES(FROM_UNIXTIME(?), " + playerID(actor) + ", ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				ps1.setLong(1, date );
				ps1.setInt(2, replaced);
				ps1.setInt(3, type);
				ps1.setInt(4, data);
				ps1.setInt(5, loc.getBlockX());
				ps1.setInt(6, loc.getBlockY());
				ps1.setInt(7, loc.getBlockZ());
				ps1.executeUpdate();
	
				int id;
				ResultSet rs = ps1.getGeneratedKeys();
				rs.next();
				id = rs.getInt(1);
	
				if (signtext != null) {
					ps = connection.prepareStatement("INSERT INTO `" + table + "-sign` (signtext, id) VALUES(?, ?)");
					ps.setString(1, signtext);
					ps.setInt(2, id);
					ps.executeUpdate();
				} else if (ca != null) {
					ps = connection.prepareStatement("INSERT INTO `" + table + "-chest` (itemtype, itemamount, itemdata, id) values (?, ?, ?, ?)");
					ps.setInt(1, ca.itemType);
					ps.setInt(2, ca.itemAmount);
					ps.setInt(3, ca.itemData);
					ps.setInt(4, id);
					ps.executeUpdate();
				}
			}
			catch (final SQLException ex) {
				if (ps1 != null) {
					getLogger().log(Level.SEVERE, "[Consumer] Troublesome query: " + ps1.toString());
				}
				if (ps != null) {
					getLogger().log(Level.SEVERE, "[Consumer] Troublesome query: " + ps.toString());
				}
				throw ex;
			}
			finally {
				// individual try/catch here, though ugly, prevents resource leaks
				if( ps1 != null ) {
					try {
						ps1.close();
					}
					catch(SQLException e) {
						// ideally should log to logger, none is available in this class
						// at the time of this writing, so I'll leave that to the plugin
						// maintainers to integrate if they wish
						e.printStackTrace();
					}
				}
				
				if( ps != null ) {
					try {
						ps.close();
					}
					catch(SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public boolean isUnique() {
			return !(signtext == null && ca == null && playerIds.containsKey(actor));
		}
		
		@Override
		public boolean canMerge(MergeableRow row) {
			return !this.isUnique() && !row.isUnique() && row instanceof BlockRow && getWorldConfig(loc.getWorld()).table.equals(getWorldConfig(((BlockRow) row).loc.getWorld()).table);
		}

		@Override
		public MergeableRow merge(MergeableRow singleRow) {
			return new MultiBlockChangeRow(this,(BlockRow) singleRow);
		}
	}

	private class MultiBlockChangeRow implements MergeableRow{
		private List<BlockRow> rows = new ArrayList<BlockRow>();
		private Connection connection;
		private Set<String> players = new HashSet<String>();
		private Set<Actor> actors = new HashSet<Actor>();
		private String table;
		
		MultiBlockChangeRow (BlockRow first, BlockRow second) {
			if (first.isUnique() || second.isUnique()) throw new IllegalArgumentException("Can't merge a unique row");
			rows.add(first);
			rows.add(second);
			actors.addAll(Arrays.asList(first.getActors()));
			actors.addAll(Arrays.asList(second.getActors()));
			players.addAll(Arrays.asList(first.getPlayers()));
			players.addAll(Arrays.asList(second.getPlayers()));
			table = getWorldConfig(first.loc.getWorld()).table;
		}

		@Override
		public void setConnection(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void executeStatements() throws SQLException {
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement("INSERT INTO `" + table + "` (date, playerid, replaced, type, data, x, y, z) VALUES(FROM_UNIXTIME(?), ?, ?, ?, ?, ?, ?, ?)");
				for (BlockRow row : rows) {
					ps.setLong(1, row.date );
					ps.setInt(2, playerIds.get(row.actor));
					ps.setInt(3, row.replaced);
					ps.setInt(4, row.type);
					ps.setInt(5, row.data);
					ps.setInt(6, row.loc.getBlockX());
					ps.setInt(7, row.loc.getBlockY());
					ps.setInt(8, row.loc.getBlockZ());
					ps.addBatch();
				}
				ps.executeBatch();
			} catch (final SQLException ex) {
				if (ps != null) {
					getLogger().log(Level.SEVERE, "[Consumer] Troublesome query: " + ps.toString());
				}
				throw ex;
			} finally {
				// individual try/catch here, though ugly, prevents resource leaks
				if( ps != null ) {
					try {
						ps.close();
					}
					catch(SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public boolean isUnique() {
			return true;
		}

		@Override
		public boolean canMerge(MergeableRow row) {
			return !row.isUnique() && row instanceof BlockRow  && table.equals(getWorldConfig(((BlockRow) row).loc.getWorld()).table);
		}

		@Override
		public MergeableRow merge(MergeableRow second) {
			if (second.isUnique()) throw new IllegalArgumentException("Can't merge a unique row");
			rows.add((BlockRow) second);
			actors.addAll(Arrays.asList(second.getActors()));
			players.addAll(Arrays.asList(second.getPlayers()));
			return this;
		}

		@Override
		public String[] getInserts() {
			List<String> l = new ArrayList<String>();
			for (BlockRow row : rows) {
				l.addAll(Arrays.asList(row.getInserts()));
			}
			return (String[]) l.toArray();
		}

		@Override
		public String[] getPlayers() {
			return (String[]) players.toArray();
		}

		@Override
		public Actor[] getActors() {
			return (Actor[]) actors.toArray();
		}
	}

	private class KillRow implements Row
	{
		final long date;
		final Actor killer, victim;
		final int weapon;
		final Location loc;

		KillRow(Location loc, Actor attacker, Actor defender, int weapon) {
			date = System.currentTimeMillis() / 1000;
			this.loc = loc;
			killer = attacker;
			victim = defender;
			this.weapon = weapon;
		}

		@Override
		public String[] getInserts() {
			return new String[]{"INSERT INTO `" + getWorldConfig(loc.getWorld()).table + "-kills` (date, killer, victim, weapon, x, y, z) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(killer) + ", " + playerID(victim) + ", " + weapon + ", " + loc.getBlockX() + ", " + (loc.getBlockY() < 0 ? 0 : loc.getBlockY()) + ", " + loc.getBlockZ() + ");"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{killer.getName(), victim.getName()};
		}

		@Override
		public Actor[] getActors() {
			return new Actor[]{killer,victim};
		}
	}

	private class ChatRow extends ChatMessage implements PreparedStatementRow
	{
		private Connection connection;

		ChatRow(Actor player, String message) {
			super(player, message);
		}

		@Override
		public String[] getInserts() {
			return new String[]{"INSERT INTO `lb-chat` (date, playerid, message) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(player) + ", '" + message.replace("\\", "\\\\").replace("'", "\\'") + "');"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{player.getName()};
		}

		@Override
		public Actor[] getActors() {
			return new Actor[]{player};
		}

		@Override
		public void setConnection(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void executeStatements() throws SQLException {
			boolean noID = false;
			Integer id;

			String sql = "INSERT INTO `lb-chat` (date, playerid, message) VALUES (FROM_UNIXTIME(?), ";
			if ((id = playerIDAsInt(player)) == null) {
				noID = true;
				sql += playerID(player) + ", ";
			} else {
				sql += "?, ";
			}
			sql += "?)";
			
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement(sql);
				ps.setLong(1, date);
				if (!noID) {
					ps.setInt(2, id);
					ps.setString(3, message);
				} else {
					ps.setString(2, message);
				}
				ps.execute();
			}
			// we intentionally do not catch SQLException, it is thrown to the caller
			finally {
				if( ps != null ) {
					try {
						ps.close();
					}
					catch(SQLException e) {
						// should print to a Logger instead if one is ever added to this class
						e.printStackTrace();
					}
				}
			}
		}
	}

	private class PlayerJoinRow implements Row
	{
		private final Actor player;
		private final long lastLogin;
		private final String ip;

		PlayerJoinRow(Player player) {
			this.player = Actor.actorFromEntity(player);
			lastLogin = System.currentTimeMillis() / 1000;
			ip = player.getAddress().toString().replace("'", "\\'");
		}

		@Override
		public String[] getInserts() {
			if (logPlayerInfo)
				return new String[]{"UPDATE `lb-players` SET lastlogin = FROM_UNIXTIME(" + lastLogin + "), firstlogin = IF(firstlogin = 0, FROM_UNIXTIME(" + lastLogin + "), firstlogin), ip = '" + ip + "', playername = '" + player.getName() + "' WHERE UUID = '" + player.getUUID() + "';"};
			return new String[]{"UPDATE `lb-players` SET playername = '" + player.getName() + "' WHERE UUID = '" + player.getUUID() + "';"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{player.getName()};
		}

		@Override
		public Actor[] getActors() {
			return new Actor[]{player};
		}
	}

	private class PlayerLeaveRow implements Row
	{;
		private final long leaveTime;
		private final Actor actor;

		PlayerLeaveRow(Player player) {
			leaveTime = System.currentTimeMillis() / 1000;
			actor = Actor.actorFromEntity(player);
		}

		@Override
		public String[] getInserts() {
			if (logPlayerInfo)
				return new String[]{"UPDATE `lb-players` SET onlinetime = onlinetime + TIMESTAMPDIFF(SECOND, lastlogin, FROM_UNIXTIME('" + leaveTime + "')), playername = '" + actor.getName() + "' WHERE lastlogin > 0 && UUID = '" + actor.getUUID() + "';"};
			return new String[]{"UPDATE `lb-players` SET playername = '" + actor.getName() + "' WHERE UUID = '" + actor.getUUID() + "';"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{actor.getName()};
		}

		@Override
		public Actor[] getActors() {
			return new Actor[]{actor};
		}
	}
}
