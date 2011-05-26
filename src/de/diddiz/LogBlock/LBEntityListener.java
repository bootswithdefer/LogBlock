package de.diddiz.LogBlock;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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

class LBEntityListener extends EntityListener
{
	private final Consumer consumer;
	private final boolean logChestAccess;
	private final boolean logCreeperExplosionsAsPlayer;
	private final Config.LogKillsLevel logKillsLevel;
	private final boolean logSignTexts;

	LBEntityListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
		logSignTexts = logblock.getConfig().logSignTexts;
		logChestAccess = logblock.getConfig().logChestAccess;
		logCreeperExplosionsAsPlayer = logblock.getConfig().logCreeperExplosionsAsPlayerWhoTriggeredThese;
		logKillsLevel = logblock.getConfig().logKillsLevel;
	}

	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity))
			return;
		final LivingEntity victim = (LivingEntity)event.getEntity();
		final Entity killer = ((EntityDamageByEntityEvent)event).getDamager();
		if (victim.getHealth() - event.getDamage() > 0 || victim.getHealth() <= 0)
			return;
		if (logKillsLevel == Config.LogKillsLevel.PLAYERS && !(victim instanceof Player && killer instanceof Player))
			return;
		else if (logKillsLevel == Config.LogKillsLevel.MONSTERS && !((victim instanceof Player || victim instanceof Monster) && killer instanceof Player || killer instanceof Monster))
			return;
		consumer.queueKill(killer, victim);
	}

	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!event.isCancelled()) {
			String name;
			if (event.getEntity() instanceof TNTPrimed)
				name = "TNT";
			else if (event.getEntity() instanceof Creeper) {
				if (logCreeperExplosionsAsPlayer) {
					final Entity target = ((Creeper)event.getEntity()).getTarget();
					if (target instanceof Player)
						name = ((Player)target).getName();
					else
						name = "Creeper";
				} else
					name = "Creeper";
			} else if (event.getEntity() instanceof Fireball)
				name = "Ghast";
			else
				name = "Environment";
			for (final Block block : event.blockList()) {
				final int type = block.getTypeId();
				if (logSignTexts & (type == 63 || type == 68))
					consumer.queueSignBreak(name, (Sign)block.getState());
				else if (logChestAccess && (type == 23 || type == 54 || type == 61))
					consumer.queueContainerBreak(name, block.getState());
				else
					consumer.queueBlockBreak(name, block.getState());
			}
		}
	}
}
