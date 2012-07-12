package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class SignChangeLogging extends LoggingListener
{
	public SignChangeLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.SIGNTEXT))
			consumer.queueSignPlace(event.getPlayer().getName(), event.getBlock().getLocation(), event.getBlock().getTypeId(), event.getBlock().getData(), event.getLines());
	}
}
