package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.util.LoggingUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class BlockPlaceLogging extends LoggingListener {
    public BlockPlaceLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Config.isLogging(event.getBlock().getWorld(), Logging.BLOCKPLACE)) {
            final BlockState before = event.getBlockReplacedState();
            final BlockState after = event.getBlockPlaced().getState();
            final Actor actor = Actor.actorFromEntity(event.getPlayer());
            
            LoggingUtil.smartLogBlockPlace(consumer, actor, before, after);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.BLOCKPLACE)) {
            consumer.queueBlockPlace(Actor.actorFromEntity(event.getPlayer()), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), Bukkit.createBlockData(event.getBucket() == Material.LAVA_BUCKET ? Material.LAVA : Material.WATER));
        }
    }
}
