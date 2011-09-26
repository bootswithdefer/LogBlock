package de.diddiz.LogBlock;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.EntityEnderman;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.entity.CraftEnderman;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EndermanPickupEvent;
import org.bukkit.event.entity.EndermanPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;

class LBEntityListener extends EntityListener
{
	private final Consumer consumer;
	private final boolean logCreeperExplosionsAsPlayer;
	private final Config.LogKillsLevel logKillsLevel;
	private final Map<Integer, WorldConfig> worlds;
	private final Map<Integer, Integer> lastAttackedEntity = new HashMap<Integer, Integer>();
	private final Map<Integer, Long> lastAttackTime = new HashMap<Integer, Long>();

	LBEntityListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
		worlds = logblock.getConfig().worlds;
		logCreeperExplosionsAsPlayer = logblock.getConfig().logCreeperExplosionsAsPlayerWhoTriggeredThese;
		logKillsLevel = logblock.getConfig().logKillsLevel;
	}

	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		final WorldConfig wcfg = worlds.get(event.getEntity().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logKills && event instanceof EntityDamageByEntityEvent && event.getEntity() instanceof LivingEntity) {
			final LivingEntity victim = (LivingEntity)event.getEntity();
			final Entity killer = ((EntityDamageByEntityEvent)event).getDamager();
			if (victim.getHealth() - event.getDamage() > 0 || victim.getHealth() <= 0)
				return;
			if (logKillsLevel == Config.LogKillsLevel.PLAYERS && !(victim instanceof Player && killer instanceof Player))
				return;
			else if (logKillsLevel == Config.LogKillsLevel.MONSTERS && !((victim instanceof Player || victim instanceof Monster) && killer instanceof Player || killer instanceof Monster))
				return;
			if (lastAttackedEntity.containsKey(killer.getEntityId()) && lastAttackedEntity.get(killer.getEntityId()) == victim.getEntityId() && System.currentTimeMillis() - lastAttackTime.get(killer.getEntityId()) < 5000)
				return;
			consumer.queueKill(killer, victim);
			lastAttackedEntity.put(killer.getEntityId(), victim.getEntityId());
			lastAttackTime.put(killer.getEntityId(), System.currentTimeMillis());
		}
	}

	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		final WorldConfig wcfg = worlds.get(event.getLocation().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logExplosions) {
			final String name;
			if (event.getEntity() == null)
				name = "Explosion";
			else if (event.getEntity() instanceof TNTPrimed)
				name = "TNT";
			else if (event.getEntity() instanceof Creeper) {
				if (logCreeperExplosionsAsPlayer) {
					final Entity target = ((Creeper)event.getEntity()).getTarget();
					name = target instanceof Player ? ((Player)target).getName() : "Creeper";
				} else
					name = "Creeper";
			} else if (event.getEntity() instanceof Fireball)
				name = "Ghast";
			else
				name = "Explosion";
			for (final Block block : event.blockList()) {
				final int type = block.getTypeId();
				if (wcfg.logSignTexts & (type == 63 || type == 68))
					consumer.queueSignBreak(name, (Sign)block.getState());
				else if (wcfg.logChestAccess && (type == 23 || type == 54 || type == 61))
					consumer.queueContainerBreak(name, block.getState());
				else
					consumer.queueBlockBreak(name, block.getState());
			}
		}
	}

	@Override
	public void onEndermanPickup(EndermanPickupEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logEndermen)
			consumer.queueBlockBreak("Enderman", event.getBlock().getState());
	}

	@Override
	public void onEndermanPlace(EndermanPlaceEvent event) {
		final WorldConfig wcfg = worlds.get(event.getLocation().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logEndermen && event.getEntity() instanceof Enderman) {
			final EntityEnderman enderman = ((CraftEnderman)event.getEntity()).getHandle();
			consumer.queueBlockPlace("Enderman", event.getLocation(), enderman.getCarriedId(), (byte)enderman.getCarriedData());
		}
	}
}
