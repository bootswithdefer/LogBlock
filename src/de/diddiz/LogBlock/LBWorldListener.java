package de.diddiz.LogBlock;

import java.util.Map;
import org.bukkit.block.BlockState;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldListener;

public class LBWorldListener extends WorldListener
{
	private final Consumer consumer;
	private final Map<Integer, WorldConfig> worlds;

	LBWorldListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
		worlds = logblock.getLBConfig().worlds;
	}

	@Override
	public void onStructureGrow(StructureGrowEvent event) {
		final WorldConfig wcfg = worlds.get(event.getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null) {
			final String playerName;
			if (event.getPlayer() != null) {
				if (!wcfg.isLogging(Logging.BONEMEALSTRUCTUREGROW))
					return;
				playerName = event.getPlayer().getName();
			} else {
				if (!wcfg.isLogging(Logging.NATURALSTRUCTUREGROW))
					return;
				playerName = "NaturalGrow";
			}
			for (final BlockState state : event.getBlocks())
				consumer.queueBlockReplace(playerName, state.getBlock().getState(), state);
		}
	}
}
