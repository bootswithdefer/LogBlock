package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.StructureGrowEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;

public class StructureGrowLogging extends LoggingListener
{
	public StructureGrowLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onStructureGrow(StructureGrowEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getWorld());
		if (wcfg != null) {
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
