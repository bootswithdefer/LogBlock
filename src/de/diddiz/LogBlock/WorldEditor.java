package de.diddiz.LogBlock;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import de.diddiz.util.BukkitUtils;

public class WorldEditor implements Runnable
{
	public static class WorldEditorException extends Exception
	{
		private static final long serialVersionUID = 7509084196124728986L;

		public WorldEditorException(String msg) {
			super(msg);
		}
	}

	private final Logger log;
	private final LogBlock logblock;
	private final Config config;
	private final LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();
	private final World world;
	private int taskID;
	private int successes = 0;
	private int errors = 0;
	private int blacklistCollisions = 0;

	WorldEditor(LogBlock logblock, World world) {
		log = logblock.getServer().getLogger();
		this.logblock = logblock;
		config = logblock.getConfig();
		this.world = world;
	}

	int getSize() {
		return edits.size();
	}

	int getSuccesses() {
		return successes;
	}

	int getErrors() {
		return errors;
	}

	int getBlacklistCollisions() {
		return blacklistCollisions;
	}

	void queueBlockChange(int type, int replaced, byte data, int x, int y, int z, String signtext, short itemType, short itemAmount, byte itemData) {
		edits.add(new Edit(type, replaced, data, x, y, z, signtext, itemType, itemAmount, itemData));
	}

	synchronized void start() throws WorldEditorException {
		taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, this, 0, 1);
		if (taskID == -1)
			throw new WorldEditorException("Failed to schedule task");
		try {
			this.wait();
		} catch (final InterruptedException ex) {
			throw new WorldEditorException("Interrupted");
		}
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

		// TODO Fix doors and beds
		private PerformResult perform() {
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
				if (!(BukkitUtils.equalTypes(block.getTypeId(), type) || config.replaceAnyway.contains(block.getTypeId())))
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
					for (int i = 0; i < 4; i++)
						sign.setLine(i, lines[i]);
					if (!sign.update())
						return PerformResult.ERROR;
				}
				return PerformResult.SUCCESS;
			} catch (final Exception ex) {
				log.severe("[LogBlock Rollback] " + ex.toString());
				return PerformResult.ERROR;
			}
		}
	}
}
