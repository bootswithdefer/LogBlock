package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.BukkitUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.logCreeperExplosionsAsPlayerWhoTriggeredThese;
import static de.diddiz.util.BukkitUtils.getContainerBlocks;

import java.util.UUID;

public class ExplosionLogging extends LoggingListener {

    private UUID lastBedInteractionPlayer;
    private Location lastBedInteractionLocation;

    public ExplosionLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getLocation().getWorld());
        if (wcfg != null) {
            Actor actor = new Actor("Explosion");
            Entity source = event.getEntity();
            if (source == null) {
                if (!wcfg.isLogging(Logging.MISCEXPLOSION)) {
                    return;
                }
            } else if (source instanceof TNTPrimed) {
                if (!wcfg.isLogging(Logging.TNTEXPLOSION)) {
                    return;
                }
                actor = new Actor("TNT");
            } else if (source instanceof ExplosiveMinecart) {
                if (!wcfg.isLogging(Logging.TNTMINECARTEXPLOSION)) {
                    return;
                }
                actor = new Actor("TNTMinecart");
            } else if (source instanceof Creeper) {
                if (!wcfg.isLogging(Logging.CREEPEREXPLOSION)) {
                    return;
                }
                if (logCreeperExplosionsAsPlayerWhoTriggeredThese) {
                    final Entity target = ((Creeper) source).getTarget();
                    actor = target instanceof Player ? Actor.actorFromEntity(target) : new Actor("Creeper");
                } else {
                    new Actor("Creeper");
                }
            } else if (source instanceof Fireball) {
                Fireball fireball = (Fireball) source;
                ProjectileSource shooter = fireball.getShooter();
                if (shooter == null) {
                    return;
                }
                if (shooter instanceof Ghast) {
                    if (!wcfg.isLogging(Logging.GHASTFIREBALLEXPLOSION)) {
                        return;
                    }
                    actor = Actor.actorFromProjectileSource(shooter);
                } else if (shooter instanceof Wither) {
                    if (!wcfg.isLogging(Logging.WITHER)) {
                        return;
                    }
                    actor = Actor.actorFromProjectileSource(shooter);
                }
            } else if (source instanceof EnderDragon) {
                if (!wcfg.isLogging(Logging.ENDERDRAGON)) {
                    return;
                }
                actor = Actor.actorFromEntity(source);
            } else if (source instanceof Wither) {
                if (!wcfg.isLogging(Logging.WITHER)) {
                    return;
                }
                actor = Actor.actorFromEntity(source);
            } else if (source instanceof WitherSkull) {
                if (!wcfg.isLogging(Logging.WITHER_SKULL)) {
                    return;
                }
                actor = Actor.actorFromEntity(source);

            } else if (source instanceof EnderCrystal) {
                if (!wcfg.isLogging(Logging.ENDERCRYSTALEXPLOSION)) {
                    return;
                }
                actor = Actor.actorFromEntity(source);

            } else {
                if (!wcfg.isLogging(Logging.MISCEXPLOSION)) {
                    return;
                }
            }
            for (final Block block : event.blockList()) {
                final Material type = block.getType();
                if (wcfg.isLogging(Logging.CHESTACCESS) && (getContainerBlocks().contains(type))) {
                    consumer.queueContainerBreak(actor, block.getState());
                } else {
                    consumer.queueBlockBreak(actor, block.getState());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && BukkitUtils.isBed(event.getClickedBlock().getType())) {
            Block block = event.getClickedBlock();
            if (!Config.isLogging(block.getWorld(), Logging.BEDEXPLOSION)) {
                return;
            }
            lastBedInteractionPlayer = event.getPlayer().getUniqueId();
            lastBedInteractionLocation = block.getLocation();
            new BukkitRunnable() {
                @Override
                public void run() {
                    lastBedInteractionPlayer = null;
                    lastBedInteractionLocation = null;
                }
            }.runTask(LogBlock.getInstance());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Player bedCause = null;
        if (lastBedInteractionPlayer != null && lastBedInteractionLocation != null) {
            Location block = event.getBlock().getLocation();
            if (lastBedInteractionLocation.getWorld() == block.getWorld() && block.distanceSquared(lastBedInteractionLocation) <= 1) {
                bedCause = Bukkit.getPlayer(lastBedInteractionPlayer);
            }
        }

        for (final Block block : event.blockList()) {
            final WorldConfig wcfg = getWorldConfig(block.getLocation().getWorld());

            if (wcfg != null) {
                Actor actor = new Actor("Explosion");
                if (bedCause != null) {
                    if (!wcfg.isLogging(Logging.BEDEXPLOSION)) {
                        return;
                    }
                    if (Config.logBedExplosionsAsPlayerWhoTriggeredThese) {
                        actor = Actor.actorFromEntity(bedCause);
                    }
                } else if (!wcfg.isLogging(Logging.MISCEXPLOSION)) {
                    return;
                }

                final Material type = block.getType();
                if (wcfg.isLogging(Logging.CHESTACCESS) && (getContainerBlocks().contains(type))) {
                    consumer.queueContainerBreak(actor, block.getState());
                } else {
                    consumer.queueBlockBreak(actor, block.getState());
                }
            }
        }
    }
}
