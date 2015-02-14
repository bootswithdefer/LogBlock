package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;

public class SignChangeLogging extends LoggingListener
{
	public SignChangeLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.SIGNTEXT))
			consumer.queueSignPlace(Actor.actorFromEntity(event.getPlayer()), event.getBlock().getLocation(), event.getBlock().getTypeId(), event.getBlock().getData(), event.getLines());
	}
}
