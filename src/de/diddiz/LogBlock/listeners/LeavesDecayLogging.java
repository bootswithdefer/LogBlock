package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.LeavesDecayEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class LeavesDecayLogging extends LoggingListener
{
	public LeavesDecayLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onLeavesDecay(LeavesDecayEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.LEAVESDECAY))
			consumer.queueBlockBreak("LeavesDecay", event.getBlock().getState());
	}
}
