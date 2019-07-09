package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lectern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.ItemStack;

public class LecternLogging extends LoggingListener {
    public LecternLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getPlayer().getWorld());
        if (wcfg != null && wcfg.isLogging(Logging.LECTERNBOOKCHANGE)) {
            final BlockState before = event.getBlockReplacedState();
            final BlockState after = event.getBlockPlaced().getState();
            if (before.getType() == Material.LECTERN && after.getType() == Material.LECTERN) {
                Lectern lecternBefore = (Lectern) before.getBlock().getState();
                ItemStack book = lecternBefore.getSnapshotInventory().getItem(0);
                try {
                    lecternBefore.getSnapshotInventory().setItem(0, null);
                } catch (NullPointerException e) {
                    //ignored
                }
                lecternBefore.setBlockData(before.getBlockData());
                consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), lecternBefore, after);
                try {
                    lecternBefore.getSnapshotInventory().setItem(0, book);
                } catch (NullPointerException e) {
                    //ignored
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeLecternBook(PlayerTakeLecternBookEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getPlayer().getWorld());
        if (wcfg != null && wcfg.isLogging(Logging.LECTERNBOOKCHANGE)) {
            Lectern oldState = event.getLectern();
            Lectern newState = (Lectern) oldState.getBlock().getState();
            try {
                newState.getSnapshotInventory().setItem(0, null);
            } catch (NullPointerException e) {
                //ignored
            }
            org.bukkit.block.data.type.Lectern oldBlockData = (org.bukkit.block.data.type.Lectern) oldState.getBlockData();
            org.bukkit.block.data.type.Lectern blockData = (org.bukkit.block.data.type.Lectern) Material.LECTERN.createBlockData();
            blockData.setFacing(oldBlockData.getFacing());
            blockData.setPowered(oldBlockData.isPowered());
            newState.setBlockData(blockData);
            consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), oldState, newState);
        }
    }
}
