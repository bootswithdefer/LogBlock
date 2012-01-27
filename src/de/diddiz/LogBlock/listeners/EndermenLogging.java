package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import net.minecraft.server.EntityEnderman;
import org.bukkit.craftbukkit.entity.CraftEnderman;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EndermanPickupEvent;
import org.bukkit.event.entity.EndermanPlaceEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class EndermenLogging extends LoggingListener
{
	public EndermenLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEndermanPickup(EndermanPickupEvent event) {
		if (!event.isCancelled() && isLogging(event.getBlock().getWorld(), Logging.ENDERMEN))
			consumer.queueBlockBreak("Enderman", event.getBlock().getState());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEndermanPlace(EndermanPlaceEvent event) {
		if (!event.isCancelled() && isLogging(event.getLocation().getWorld(), Logging.ENDERMEN) && event.getEntity() instanceof Enderman) {
			final EntityEnderman enderman = ((CraftEnderman)event.getEntity()).getHandle();
			consumer.queueBlockPlace("Enderman", event.getLocation(), enderman.getCarriedId(), (byte)enderman.getCarriedData());
		}
	}
}
