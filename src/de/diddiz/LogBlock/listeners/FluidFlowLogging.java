package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFromToEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;

public class FluidFlowLogging extends LoggingListener
{
	private static final Set<Integer> nonFluidProofBlocks = new HashSet<Integer>(Arrays.asList(27, 28, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59, 66, 69, 70, 75, 76, 78, 93, 94, 104, 105, 106));

	public FluidFlowLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockFromTo(BlockFromToEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
		if (!event.isCancelled() && wcfg != null) {
			final int typeFrom = event.getBlock().getTypeId();
			final int typeTo = event.getToBlock().getTypeId();
			if (typeFrom == 10 || typeFrom == 11) {
				if (typeTo == 0) {
					if (wcfg.isLogging(Logging.LAVAFLOW))
						consumer.queueBlockPlace("LavaFlow", event.getToBlock().getLocation(), 10, (byte)(event.getBlock().getData() + 1));
				} else if (nonFluidProofBlocks.contains(typeTo))
					consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 10, (byte)(event.getBlock().getData() + 1));
				else if (typeTo == 8 || typeTo == 9)
					if (event.getFace() == BlockFace.DOWN)
						consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 10, (byte)0);
					else
						consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 4, (byte)0);
			} else if (typeFrom == 8 || typeFrom == 9)
				if (typeTo == 0 || nonFluidProofBlocks.contains(typeTo)) {
					if (typeTo == 0) {
						if (wcfg.isLogging(Logging.WATERFLOW))
							consumer.queueBlockPlace("WaterFlow", event.getToBlock().getLocation(), 8, (byte)(event.getBlock().getData() + 1));
					} else
						consumer.queueBlockReplace("WaterFlow", event.getToBlock().getState(), 8, (byte)(event.getBlock().getData() + 1));
					final Block lower = event.getToBlock().getRelative(BlockFace.DOWN);
					if (lower.getTypeId() == 10 || lower.getTypeId() == 11)
						consumer.queueBlockReplace("WaterFlow", lower.getState(), lower.getData() == 0 ? 49 : 4, (byte)0);
				}
		}
	}
}
