package de.diddiz.LogBlock.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.EntityChange;
import de.diddiz.LogBlock.EntityLogging;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.config.Config;
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

    private void setLastSpawner(Player player, Class<? extends Entity> spawning, boolean spawnEgg) {
        lastSpawner = player;
        lastSpawning = spawning;
        lastSpawnerEgg = spawnEgg;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBlockPlace(BlockPlaceEvent event) {
        Material placed = event.getBlock().getType();
        if (placed == Material.WITHER_SKELETON_SKULL) {
            setLastSpawner(event.getPlayer(), Wither.class, false);
        } else if (placed == Material.CARVED_PUMPKIN) {
            Material below = event.getBlock().getRelative(BlockFace.DOWN).getType();
            if (below == Material.SNOW_BLOCK) {
                setLastSpawner(event.getPlayer(), Snowman.class, false);
            } else if (below == Material.IRON_BLOCK) {
                setLastSpawner(event.getPlayer(), IronGolem.class, false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack inHand = event.getItem();
            if (inHand != null) {
                Material mat = inHand.getType();
                if (mat == Material.ARMOR_STAND) {
                    setLastSpawner(event.getPlayer(), ArmorStand.class, false);
                } else if (mat.name().endsWith("_SPAWN_EGG")) {
                    setLastSpawner(event.getPlayer(), null, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack inHand = event.getHand() == EquipmentSlot.HAND ? event.getPlayer().getInventory().getItemInMainHand() : event.getPlayer().getInventory().getItemInOffHand();
        if (inHand != null && inHand.getType() != Material.AIR) {
            Material mat = inHand.getType();
            if (mat.name().endsWith("_SPAWN_EGG")) {
                setLastSpawner(event.getPlayer(), null, true);
            }

            Entity entity = event.getRightClicked();
            if (entity instanceof ItemFrame) {
                ItemStack oldItem = ((ItemFrame) entity).getItem();
                if (oldItem == null || oldItem.getType() == Material.AIR) {
                    if (Config.isLogging(entity.getWorld(), EntityLogging.MODIFY, entity)) {
                        Actor actor = Actor.actorFromEntity(event.getPlayer());
                        YamlConfiguration data = new YamlConfiguration();
                        inHand = inHand.clone();
                        inHand.setAmount(1);
                        data.set("item", inHand);
                        consumer.queueEntityModification(actor, entity.getUniqueId(), entity.getType(), entity.getLocation(), EntityChange.EntityChangeType.ADDEQUIP, data);
                    }
                }
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
            if (Config.isLogging(entity.getWorld(), EntityLogging.SPAWN, entity)) {
                Actor actor = null;
                if (lastSpawner != null && lastSpawner.getWorld() == entity.getWorld() && lastSpawner.getLocation().distance(entity.getLocation()) < 10) {
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
        }
        resetLastSpawner();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (Config.isLogging(entity.getWorld(), EntityLogging.DESTROY, entity)) {
            Actor actor;
            EntityDamageEvent lastDamage = entity.getLastDamageCause();
            if (lastDamage instanceof EntityDamageByEntityEvent) {
                actor = Actor.actorFromEntity(((EntityDamageByEntityEvent) lastDamage).getDamager());
            } else {
                actor = new Actor(lastDamage.getCause().toString());
            }
            queueEntitySpawnOrKill(entity, actor, EntityChange.EntityChangeType.KILL);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Hanging entity = event.getEntity();
        if (Config.isLogging(entity.getWorld(), EntityLogging.SPAWN, entity)) {
            Actor actor = Actor.actorFromEntity(event.getPlayer());
            queueEntitySpawnOrKill(entity, actor, EntityChange.EntityChangeType.CREATE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Entity entity = event.getEntity();
        if (Config.isLogging(entity.getWorld(), EntityLogging.DESTROY, entity)) {
            Actor actor;
            if (event instanceof HangingBreakByEntityEvent) {
                actor = Actor.actorFromEntity(((HangingBreakByEntityEvent) event).getRemover());
            } else {
                actor = new Actor(event.getCause().toString());
            }
            queueEntitySpawnOrKill(entity, actor, EntityChange.EntityChangeType.KILL);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemFrame) {
            ItemStack oldItem = ((ItemFrame) entity).getItem();
            if (oldItem != null && oldItem.getType() != Material.AIR) {
                if (Config.isLogging(entity.getWorld(), EntityLogging.MODIFY, entity)) {
                    Actor actor;
                    if (event instanceof EntityDamageByEntityEvent) {
                        actor = Actor.actorFromEntity(((EntityDamageByEntityEvent) event).getDamager());
                    } else {
                        actor = new Actor(event.getCause().toString());
                    }
                    YamlConfiguration data = new YamlConfiguration();
                    data.set("item", oldItem);
                    consumer.queueEntityModification(actor, entity.getUniqueId(), entity.getType(), entity.getLocation(), EntityChange.EntityChangeType.REMOVEEQUIP, data);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand entity = event.getRightClicked();
        ItemStack oldItem = event.getArmorStandItem();
        ItemStack newItem = event.getPlayerItem();
        boolean oldEmpty = oldItem == null || oldItem.getType() == Material.AIR;
        boolean newEmpty = newItem == null || newItem.getType() == Material.AIR;
        if ((!oldEmpty || !newEmpty) && Config.isLogging(entity.getWorld(), EntityLogging.MODIFY, entity)) {
            Actor actor = Actor.actorFromEntity(event.getPlayer());
            if (!oldEmpty && !newEmpty && newItem.getAmount() > 1) {
                return;
            }
            if (!oldEmpty) {
                YamlConfiguration data = new YamlConfiguration();
                data.set("item", oldItem);
                data.set("slot", event.getSlot().name());
                consumer.queueEntityModification(actor, entity.getUniqueId(), entity.getType(), entity.getLocation(), EntityChange.EntityChangeType.REMOVEEQUIP, data);
            }
            if (!newEmpty) {
                YamlConfiguration data = new YamlConfiguration();
                data.set("item", newItem);
                data.set("slot", event.getSlot().name());
                consumer.queueEntityModification(actor, entity.getUniqueId(), entity.getType(), entity.getLocation(), EntityChange.EntityChangeType.ADDEQUIP, data);
            }
        }
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
