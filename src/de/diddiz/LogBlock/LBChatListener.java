package de.diddiz.LogBlock;

import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;

class LBChatListener extends PlayerListener
{
	private final Consumer consumer;

	LBChatListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
	}

	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!event.isCancelled())
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if (!event.isCancelled())
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}
}
