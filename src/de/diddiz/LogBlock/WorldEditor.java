package de.diddiz.LogBlock;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.block.Block;

public class WorldEditor implements Runnable
{
	private final Logger log;
	private final LogBlock logblock;
	private final Config config;
	private final Object caller;
	private final LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();
	private final World world;
	private int taskID;
	private int successes = 0;
	private int errors = 0;
	private int blacklistCollisions = 0;

	WorldEditor(LogBlock logblock, Object caller, World world) {
		log = logblock.getServer().getLogger();
		this.logblock = logblock;
		config = logblock.getConfig();
		this.caller = caller;
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

	public void queueBlockChange(int type, int replaced, byte data, int x, int y, int z) {
		edits.add(new Edit(type, replaced, data, x, y, z));
	}

	public boolean start() {
		taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, this, 0, 1);
		if (taskID == -1)
			return false;
		return true;
	}

	@Override
	public void run() {
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
			synchronized (caller) {
				caller.notify();
			}
		}
	}

	private boolean equalsType(int type1, int type2) {
		if (type1 == type2)
			return true;
		if ((type1 == 2 || type1 == 3) && (type2 == 2 || type2 == 3))
			return true;
		if ((type1 == 8 || type1 == 9) && (type2 == 8 || type2 == 9))
			return true;
		if ((type1 == 10 || type1 == 11) && (type2 == 10 || type2 == 11))
			return true;
		if ((type1 == 73 || type1 == 74) && (type2 == 73 || type2 == 74))
			return true;
		if ((type1 == 75 || type1 == 76) && (type2 == 75 || type2 == 76))
			return true;
		if ((type1 == 93 || type1 == 94) && (type2 == 93 || type2 == 94))
			return true;
		return false;
	}

	private enum PerformResult {
		ERROR, SUCCESS, BLACKLISTED, NO_ACTION
	}

	private class Edit
	{
		final int type, replaced;
		final int x, y, z;
		final byte data;

		Edit(int type, int replaced, byte data, int x, int y, int z) {
			this.type = type;
			this.replaced = replaced;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		private PerformResult perform() {
			if (config.dontRollback.contains(replaced))
				return PerformResult.BLACKLISTED;
			try {
				final Block block = world.getBlockAt(x, y, z);
				if (!world.isChunkLoaded(block.getChunk()))
					world.loadChunk(block.getChunk());
				if (equalsType(block.getTypeId(), type) || config.replaceAnyway.contains(block.getTypeId()) || type == 0 && replaced == 0) {
					if (block.setTypeIdAndData(replaced, data, false))
						return PerformResult.SUCCESS;
					else
						return PerformResult.ERROR;
				} else
					return PerformResult.NO_ACTION;
			} catch (final Exception ex) {
				log.severe("[LogBlock Rollback] " + ex.toString());
				return PerformResult.ERROR;
			}
		}
	}
}