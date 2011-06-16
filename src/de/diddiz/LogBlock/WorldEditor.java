package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.equalTypes;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
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

	public void queueBlockChange(int type, int replaced, byte data, int x, int y, int z, String signtext, short itemType, short itemAmount, byte itemData) {
		edits.add(new Edit(type, replaced, data, x, y, z, signtext, itemType, itemAmount, itemData));
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

	private class Edit
	{
		private final int type, replaced;
		private final int x, y, z;
		private final byte data;
		private final String signtext;
		public final short itemType, itemAmount;
		public final byte itemData;

		Edit(int type, int replaced, byte data, int x, int y, int z, String signtext, short itemType, short itemAmount, byte itemData) {
			this.type = type;
			this.replaced = replaced;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
			this.signtext = signtext;
			this.itemType = itemType;
			this.itemAmount = itemAmount;
			this.itemData = itemData;
		}

		PerformResult perform() {
			if (config.dontRollback.contains(replaced))
				return PerformResult.BLACKLISTED;
			try {
				final Block block = world.getBlockAt(x, y, z);
				final BlockState state = block.getState();
				if (!world.isChunkLoaded(block.getChunk()))
					world.loadChunk(block.getChunk());
				if (type == replaced)
					if (type == 0) {
						if (block.setTypeId(0))
							return PerformResult.SUCCESS;
						return PerformResult.ERROR;
					} else if (type == 23 || type == 54 || type == 61) {
						if (!(state instanceof ContainerBlock))
							return PerformResult.NO_ACTION;
						final Inventory inv = ((ContainerBlock)block.getState()).getInventory();
						if (itemType != 0)
							if (itemAmount > 0)
								inv.removeItem(new ItemStack(itemType, itemAmount, (short)0, itemData));
							else if (itemAmount < 0)
								inv.addItem(new ItemStack(itemType, itemAmount * -1, (short)0, itemData));
						if (!state.update())
							return PerformResult.ERROR;
						return PerformResult.SUCCESS;
					} else
						return PerformResult.NO_ACTION;
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
					final String[] lines = signtext.split("\0");
					if (lines.length < 4)
						return PerformResult.ERROR;
					for (int i = 0; i < 4; i++)
						sign.setLine(i, lines[i]);
					if (!sign.update())
						return PerformResult.ERROR;
				} else if (curtype == 26) {
					final Bed bed = (Bed)block.getState().getData();
					final Block secBlock;
					if (!bed.isHeadOfBed())
						secBlock = block.getFace(bed.getFacing());
					else
						secBlock = block.getFace(bed.getFacing().getOppositeFace());
					if (secBlock.getTypeId() != 0)
						return PerformResult.SUCCESS;
					if (!secBlock.setTypeIdAndData(26, (byte)(bed.getData() ^ 8), true))
						return PerformResult.ERROR;
				} else if (curtype == 64 || curtype == 71) {
					final Block secBlock;
					final byte blockData = block.getData();
					if ((blockData & 8) == 8)
						secBlock = block.getFace(BlockFace.DOWN);
					else
						secBlock = block.getFace(BlockFace.UP);
					if (secBlock.getTypeId() != 0)
						return PerformResult.SUCCESS;
					if (!secBlock.setTypeIdAndData(curtype, (byte)(blockData ^ 8), true))
						return PerformResult.ERROR;
				}
				return PerformResult.SUCCESS;
			} catch (final Exception ex) {
				log.severe("[LogBlock Rollback] " + ex.toString());
				return PerformResult.ERROR;
			}
		}
	}

	public static class WorldEditorException extends Exception
	{
		private static final long serialVersionUID = 7509084196124728986L;

		public WorldEditorException(String msg) {
			super(msg);
		}
	}
}
