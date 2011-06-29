package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.equalTypes;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;

public class WorldEditor implements Runnable
{
	private final Logger log;
	private final LogBlock logblock;
	private final Config config;
	private final Queue<Edit> edits = new LinkedBlockingQueue<Edit>();
	private final World world;
	private int taskID;
	private int successes = 0, errors = 0, blacklistCollisions = 0;
	private long elapsedTime = 0;

	public WorldEditor(LogBlock logblock, World world) {
		log = logblock.getServer().getLogger();
		this.logblock = logblock;
		config = logblock.getConfig();
		this.world = world;
	}

	public int getSize() {
		return edits.size();
	}

	public int getSuccesses() {
		return successes;
	}

	public int getErrors() {
		return errors;
	}

	public int getBlacklistCollisions() {
		return blacklistCollisions;
	}

	public void queueEdit(int x, int y, int z, int replaced, int type, byte data, String signtext, short itemType, short itemAmount, byte itemData) {
		edits.add(new Edit(new Location(world, x, y, z), null, replaced, type, data, signtext, new ChestAccess(itemType, itemAmount, itemData)));
	}

	public long getElapsedTime() {
		return elapsedTime;
	}

	synchronized public void start() throws WorldEditorException {
		final long start = System.currentTimeMillis();
		taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, this, 0, 1);
		if (taskID == -1)
			throw new WorldEditorException("Failed to schedule task");
		try {
			this.wait();
		} catch (final InterruptedException ex) {
			throw new WorldEditorException("Interrupted");
		}
		elapsedTime = System.currentTimeMillis() - start;
	}

	@Override
	public synchronized void run() {
		int counter = 0;
		while (!edits.isEmpty() && counter < 1000) {
			switch (edits.poll().perform()) {
				case SUCCESS:
					successes++;
					break;
				case ERROR:
					errors++;
					break;
				case BLACKLISTED:
					blacklistCollisions++;
					break;
			}
			counter++;
		}
		if (edits.isEmpty()) {
			logblock.getServer().getScheduler().cancelTask(taskID);
			notify();
		}
	}

	private static enum PerformResult {
		ERROR, SUCCESS, BLACKLISTED, NO_ACTION
	}

	private class Edit extends BlockChange
	{
		public Edit(Location loc, String playerName, int replaced, int type, byte data, String signtext, ChestAccess ca) {
			super(loc, playerName, replaced, type, data, signtext, ca);
		}

		PerformResult perform() {
			if (config.dontRollback.contains(replaced))
				return PerformResult.BLACKLISTED;
			try {
				final Block block = loc.getBlock();
				final BlockState state = block.getState();
				if (!world.isChunkLoaded(block.getChunk()))
					world.loadChunk(block.getChunk());
				if (type == replaced) {
					if (type == 0) {
						if (!block.setTypeId(0))
							return PerformResult.ERROR;
					} else if (type == 23 || type == 54 || type == 61) {
						if (!(state instanceof ContainerBlock))
							return PerformResult.NO_ACTION;
						final Inventory inv = ((ContainerBlock)block.getState()).getInventory();
						if (ca != null)
							if (ca.itemAmount > 0)
								inv.removeItem(new ItemStack(ca.itemType, ca.itemAmount, (short)0, ca.itemData));
							else if (ca.itemAmount < 0)
								inv.addItem(new ItemStack(ca.itemType, ca.itemAmount * -1, (short)0, ca.itemData));
						if (!state.update())
							return PerformResult.ERROR;
					} else
						return PerformResult.NO_ACTION;
					return PerformResult.SUCCESS;
				}
				if (!(equalTypes(block.getTypeId(), type) || config.replaceAnyway.contains(block.getTypeId())))
					return PerformResult.NO_ACTION;
				if (state instanceof ContainerBlock) {
					((ContainerBlock)state).getInventory().clear();
					state.update();
				}
				if (!block.setTypeIdAndData(replaced, data, true))
					return PerformResult.ERROR;
				final int curtype = block.getTypeId();
				if (signtext != null && (curtype == 63 || curtype == 68)) {
					final Sign sign = (Sign)block.getState();
					final String[] lines = signtext.split("\0", 4);
					if (lines.length < 4)
						return PerformResult.NO_ACTION;
					for (int i = 0; i < 4; i++)
						sign.setLine(i, lines[i]);
					if (!sign.update())
						return PerformResult.ERROR;
				} else if (curtype == 26) {
					final Bed bed = (Bed)block.getState().getData();
					final Block secBlock = bed.isHeadOfBed() ? block.getFace(bed.getFacing().getOppositeFace()) : block.getFace(bed.getFacing());
					if (secBlock.getTypeId() == 0 && !secBlock.setTypeIdAndData(26, (byte)(bed.getData() ^ 8), true))
						return PerformResult.ERROR;
				} else if (curtype == 64 || curtype == 71) {
					final byte blockData = block.getData();
					final Block secBlock = (blockData & 8) == 8 ? block.getFace(BlockFace.DOWN) : block.getFace(BlockFace.UP);
					if (secBlock.getTypeId() == 0 && !secBlock.setTypeIdAndData(curtype, (byte)(blockData ^ 8), true))
						return PerformResult.ERROR;
				} else if (curtype == 18 && (block.getData() & 8) > 0)
					block.setData((byte)(block.getData() & 0xF7));
				return PerformResult.SUCCESS;
			} catch (final Exception ex) {
				log.severe("[LogBlock Rollback] " + ex.toString());
				return PerformResult.ERROR;
			}
		}
	}

	@SuppressWarnings("serial")
	public static class WorldEditorException extends Exception
	{
		public WorldEditorException(String msg) {
			super(msg);
		}
	}
}
