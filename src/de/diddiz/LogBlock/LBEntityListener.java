package de.diddiz.LogBlock;

import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;

public class LBEntityListener extends EntityListener
{
	private final Config config;
	private final Consumer consumer;

	LBEntityListener(LogBlock logblock) {
		config= logblock.getConfig();
		consumer = logblock.getConsumer();
	}

	public void onEntityExplode(EntityExplodeEvent event) {
	if (!event.isCancelled()) {	
		String name;
		if (event.getEntity() instanceof TNTPrimed)
			name = "TNT";
		else if (event.getEntity() instanceof Creeper)
			name = "Creeper";
		else if (event.getEntity() instanceof Fireball)
			name = "Ghast";
		else
			name = "Environment";
		for (Block block : event.blockList())
			consumer.queueBlock(name, block, block.getTypeId(), 0, block.getData());
		}
	}

	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity))
			return;
		LivingEntity victim = (LivingEntity)event.getEntity();
		Entity killer = ((EntityDamageByEntityEvent)event).getDamager();
		if (victim.getHealth() - event.getDamage() > 0 || victim.getHealth() <= 0 )
			return;
		if (config.logKillsLevel == Config.LogKillsLevel.PLAYERS && !(victim instanceof Player && killer instanceof Player))
			return;
		else if (config.logKillsLevel == Config.LogKillsLevel.MONSTERS && !((victim instanceof Player || victim instanceof Monster) && killer instanceof Player || killer instanceof Monster))
			return;
		consumer.queueKill(killer, victim);
	}
}
