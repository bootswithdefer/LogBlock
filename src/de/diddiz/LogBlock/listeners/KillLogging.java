package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.LogBlock.config.Config.logKillsLevel;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config.LogKillsLevel;

public class KillLogging extends LoggingListener
{
	private final Map<Integer, Integer> lastAttackedEntity = new HashMap<Integer, Integer>();
	private final Map<Integer, Long> lastAttackTime = new HashMap<Integer, Long>();

	public KillLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (isLogging(event.getEntity().getWorld(), Logging.KILL) && event instanceof EntityDamageByEntityEvent && event.getEntity() instanceof LivingEntity) {
			final LivingEntity victim = (LivingEntity)event.getEntity();
			final Entity killer = ((EntityDamageByEntityEvent)event).getDamager();
			if (victim.getHealth() - event.getDamage() > 0 || victim.getHealth() <= 0)
				return;
			if (logKillsLevel == LogKillsLevel.PLAYERS && !(victim instanceof Player && killer instanceof Player))
				return;
			else if (logKillsLevel == LogKillsLevel.MONSTERS && !((victim instanceof Player || victim instanceof Monster) && killer instanceof Player || killer instanceof Monster))
				return;
			if (lastAttackedEntity.containsKey(killer.getEntityId()) && lastAttackedEntity.get(killer.getEntityId()) == victim.getEntityId() && System.currentTimeMillis() - lastAttackTime.get(killer.getEntityId()) < 5000)
				return;
			consumer.queueKill(killer, victim);
			lastAttackedEntity.put(killer.getEntityId(), victim.getEntityId());
			lastAttackTime.put(killer.getEntityId(), System.currentTimeMillis());
		}
	}
}
