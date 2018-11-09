package de.diddiz.LogBlock.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.EntityChange;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.worldedit.WorldEditHelper;

public class AdvancedEntityLogging extends LoggingListener {

    private Player lastSpawner;
    private Class<? extends Entity> lastSpawning;
    private boolean lastSpawnerEgg;

    public AdvancedEntityLogging(LogBlock lb) {
        super(lb);
        new BukkitRunnable() {
            @Override
            public void run() {
                resetLastSpawner();
            }
        }.runTaskTimer(lb, 1, 1);
    }

    private void resetLastSpawner() {
        lastSpawner = null;
        lastSpawning = null;
        lastSpawnerEgg = false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Animals) && !(entity instanceof Villager) && !(entity instanceof Golem) && !(entity instanceof ArmorStand)) {
            return;
        }
        Actor actor;
        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent) {
            actor = Actor.actorFromEntity(((EntityDamageByEntityEvent) lastDamage).getDamager());
        } else {
            actor = new Actor(lastDamage.getCause().toString());
        }
        queueEntitySpawnOrKill(entity, actor, EntityChange.EntityChangeType.KILL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack inHand = event.getItem();
            if (inHand != null) {
                Material mat = inHand.getType();
                if (mat == Material.ARMOR_STAND) {
                    lastSpawner = event.getPlayer();
                    lastSpawning = ArmorStand.class;
                    lastSpawnerEgg = false;
                } else if (mat.name().endsWith("_SPAWN_EGG")) {
                    lastSpawner = event.getPlayer();
                    lastSpawning = null;
                    lastSpawnerEgg = true;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack inHand = event.getHand() == EquipmentSlot.HAND ? event.getPlayer().getInventory().getItemInMainHand() : event.getPlayer().getInventory().getItemInOffHand();
        if (inHand != null) {
            Material mat = inHand.getType();
            if (mat.name().endsWith("_SPAWN_EGG")) {
                lastSpawner = event.getPlayer();
                lastSpawning = null;
                lastSpawnerEgg = true;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (!event.isCancelled()) {
            if (event.getSpawnReason() == SpawnReason.CUSTOM) {
                return;
            }
            LivingEntity entity = event.getEntity();
            if (!(entity instanceof Animals) && !(entity instanceof Villager) && !(entity instanceof Golem) && !(entity instanceof ArmorStand)) {
                return;
            }
            Actor actor = null;
            if (lastSpawner != null) {
                if (lastSpawnerEgg && event.getSpawnReason() == SpawnReason.SPAWNER_EGG) {
                    actor = Actor.actorFromEntity(lastSpawner);
                } else if (lastSpawning != null && lastSpawning.isAssignableFrom(entity.getClass())) {
                    actor = Actor.actorFromEntity(lastSpawner);
                }
            }
            if (actor == null) {
                actor = new Actor(event.getSpawnReason().toString());
            }
            queueEntitySpawnOrKill(entity, actor, EntityChange.EntityChangeType.CREATE);
        }
        resetLastSpawner();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Entity entity = event.getEntity();
        Actor actor;
        if (event instanceof HangingBreakByEntityEvent) {
            actor = Actor.actorFromEntity(((HangingBreakByEntityEvent) event).getRemover());
        } else {
            actor = new Actor(event.getCause().toString());
        }
        queueEntitySpawnOrKill(entity, actor, EntityChange.EntityChangeType.KILL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Hanging entity = event.getEntity();
        Actor actor = Actor.actorFromEntity(event.getPlayer());
        queueEntitySpawnOrKill(entity, actor, EntityChange.EntityChangeType.CREATE);
    }

    protected void queueEntitySpawnOrKill(Entity entity, Actor actor, EntityChange.EntityChangeType changeType) {
        Location location = entity.getLocation();
        YamlConfiguration data = new YamlConfiguration();
        data.set("x", location.getX());
        data.set("y", location.getY());
        data.set("z", location.getZ());
        data.set("yaw", location.getYaw());
        data.set("pitch", location.getPitch());
        data.set("worldedit", WorldEditHelper.serializeEntity(entity));
        consumer.queueEntityModification(actor, entity.getUniqueId(), entity.getType(), location, changeType, data);
    }
}
