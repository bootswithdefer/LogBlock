package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class SignChangeLogging extends LoggingListener {
    public SignChangeLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (isLogging(event.getBlock().getWorld(), Logging.SIGNTEXT)) {
            BlockState newState = event.getBlock().getState();
            if (newState instanceof Sign sign) {
                SignSide signSide = sign.getSide(event.getSide());
                for (int i = 0; i < 4; i++) {
                    signSide.setLine(i, event.getLine(i));
                }
                consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), event.getBlock().getState(), newState);
            }
        }
    }
}
