package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFadeEvent;

public class SnowFadeLogging extends LoggingListener
{
	public SnowFadeLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.SNOWFADE)) {
			final int type = event.getBlock().getTypeId();
			if (type == 78 || type == 79)
				consumer.queueBlockReplace(new Actor("SnowFade"), event.getBlock().getState(), event.getNewState());
		}
	}
}
