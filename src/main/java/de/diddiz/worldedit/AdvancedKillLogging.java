package de.diddiz.worldedit;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.EntityChange;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.listeners.LoggingListener;

public class AdvancedKillLogging extends LoggingListener {

    public AdvancedKillLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Animals) && !(entity instanceof Villager)) {
            return;
        }
        Actor killer;
        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent) {
            killer = Actor.actorFromEntity(((EntityDamageByEntityEvent) lastDamage).getDamager());
        } else {
            killer = new Actor(lastDamage.getCause().toString());
        }
        Location location = entity.getLocation();
        YamlConfiguration data = new YamlConfiguration();
        data.set("x", location.getX());
        data.set("y", location.getY());
        data.set("z", location.getZ());
        data.set("yaw", location.getYaw());
        data.set("pitch", location.getPitch());

        data.set("worldedit", WorldEditHelper.serializeEntity(entity));
        
        consumer.queueEntityModification(killer, entity.getUniqueId(), entity.getType(), location, EntityChange.EntityChangeType.KILL, data);
    }
}
