package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.block.Lectern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

public class LecternLogging extends LoggingListener {
    public LecternLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeLecternBook(PlayerTakeLecternBookEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getPlayer().getWorld());
        if (wcfg != null && wcfg.isLogging(Logging.LECTERNBOOKCHANGE)) {
            Lectern oldState = event.getLectern();
            Lectern newState = (Lectern) oldState.getBlock().getState();
            newState.getSnapshotInventory().setItem(0, null);

            consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), oldState, newState);
        }
    }
}
