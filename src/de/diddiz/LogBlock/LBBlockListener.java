package de.diddiz.LogBlock;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;

public class LBBlockListener extends BlockListener
{
	private final Config config;
	private final Consumer consumer;

	LBBlockListener(LogBlock logblock) {
		config = logblock.getConfig();
		consumer = logblock.getConsumer();
	}

	@Override
	public void onBlockFromTo(BlockFromToEvent event) {
		if (!event.isCancelled()) {
			final int typeFrom = event.getBlock().getTypeId();
			final int typeTo = event.getToBlock().getTypeId();
			if (typeFrom == 10 || typeFrom == 11) {
				if (typeTo == 0 || typeTo == 78)
					consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 10, (byte)(event.getBlock().getData() + 1));
				else if (typeTo == 8 || typeTo == 9) {
					if (event.getFace() == BlockFace.DOWN)
						consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 10, (byte)0);
					else
						consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 4, (byte)0);
				}
			}
		}
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!event.isCancelled() && !(config.logSignTexts && (event.getBlock().getType() == Material.WALL_SIGN || event.getBlock().getType() == Material.SIGN_POST))) {
			final BlockState before = event.getBlockReplacedState();
			final BlockState after = event.getBlockPlaced().getState();
			if (before.getTypeId() == 0)
				consumer.queueBlockPlace(event.getPlayer().getName(), after);
			else
				consumer.queueBlockReplace(event.getPlayer().getName(), before, after);
		}
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled())
			consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlock().getState());
	}

	@Override
	public void onSignChange(SignChangeEvent event) {
		if (!event.isCancelled())
			consumer.queueSign(event.getPlayer().getName(), event.getBlock().getLocation(), event.getBlock().getTypeId(), event.getBlock().getData(), "sign [" + event.getLine(0) + "] [" + event.getLine(1) + "] [" + event.getLine(2) + "] [" + event.getLine(3) + "]");
	}

	@Override
	public void onBlockBurn(BlockBurnEvent event) {
		if (!event.isCancelled())
			consumer.queueBlockBreak("Fire", event.getBlock().getState());
	}

	@Override
	public void onLeavesDecay(LeavesDecayEvent event) {
		if (!event.isCancelled())
			consumer.queueBlockBreak("LeavesDecay", event.getBlock().getState());
	}
}
