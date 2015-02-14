package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.LoggingUtil.smartLogBlockBreak;
import static de.diddiz.util.LoggingUtil.smartLogFallables;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;

public class BlockBurnLogging extends LoggingListener
{
	public BlockBurnLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.FIRE)) {
			smartLogBlockBreak(consumer, new Actor("Fire"), event.getBlock());
			smartLogFallables(consumer, new Actor("Fire"), event.getBlock());
		}
	}
}
