package de.diddiz.LogBlock;

import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListener;

public class LBServerListener extends ServerListener
{
	private final Consumer consumer;

	LBServerListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
	}

	@Override
	public void onServerCommand(ServerCommandEvent event) {
		consumer.queueChat("Console", "/" + event.getCommand());
	}
}
