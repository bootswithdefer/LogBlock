package de.diddiz.LogBlock;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;

public class LBBlockListener extends BlockListener
{
	private final LogBlock logblock;

	LBBlockListener(LogBlock logblock) {
		this.logblock = logblock;
	}

	public void onBlockPlace(BlockPlaceEvent event) {
		if (!event.isCancelled() && !(logblock.config.logSignTexts && (event.getBlock().getType() == Material.WALL_SIGN || event.getBlock().getType() == Material.SIGN_POST))) {
			logblock.consumer.queueBlock(event.getPlayer().getName(), event.getBlockPlaced(), event.getBlockReplacedState().getTypeId(), event.getBlockPlaced().getTypeId(), event.getBlockPlaced().getData());
		}
	}

	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled())
			logblock.consumer.queueBlock(event.getPlayer().getName(), event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData());
	}

	public void onSignChange(SignChangeEvent event) {
		if (!event.isCancelled())
			logblock.consumer.queueBlock(event.getPlayer().getName(), event.getBlock(), 0, event.getBlock().getTypeId(), event.getBlock().getData(), "sign [" + event.getLine(0) + "] [" + event.getLine(1) + "] [" + event.getLine(2) + "] [" + event.getLine(3) + "]", null);
	}

	public void onBlockBurn(BlockBurnEvent event) {
		if (!event.isCancelled())
			logblock.consumer.queueBlock("Fire", event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData());
	}

	public void onLeavesDecay(LeavesDecayEvent event) {
		if (!event.isCancelled())
			logblock.consumer.queueBlock("LeavesDecay", event.getBlock(), event.getBlock().getTypeId(), 0, event.getBlock().getData());
	}
}
