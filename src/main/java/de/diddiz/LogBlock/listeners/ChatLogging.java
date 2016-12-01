package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class ChatLogging extends LoggingListener {
    public ChatLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.CHAT)) {
            consumer.queueChat(Actor.actorFromEntity(event.getPlayer()), event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.CHAT)) {
            consumer.queueChat(Actor.actorFromEntity(event.getPlayer()), event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        consumer.queueChat(new Actor("Console"), "/" + event.getCommand());
    }
}
