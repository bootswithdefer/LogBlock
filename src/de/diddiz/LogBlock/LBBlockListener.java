package de.diddiz.LogBlock;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.MaterialData;

class LBBlockListener extends BlockListener
{
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
	private static final Set<Integer> nonFluidProofBlocks = new HashSet<Integer>(Arrays.asList(27, 28, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59, 66, 69, 70, 75, 76, 78, 93, 94));
	private final Consumer consumer;
	private final Map<Integer, WorldConfig> worlds;
	private final List<String> errors = new ArrayList<String>(20);

	LBBlockListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
		worlds = logblock.getConfig().worlds;
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logBlockBreaks) {
			final int type = event.getBlock().getTypeId();
			if (type == 0) {
				final Location loc = event.getBlock().getLocation();
				addError(dateFormat.format(System.currentTimeMillis()) + " Bukkit provided no block type for the block broken by " + event.getPlayer().getName() + " at " + loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() + ".");
			}
			if (wcfg.logSignTexts && (type == 63 || type == 68))
				consumer.queueSignBreak(event.getPlayer().getName(), (Sign)event.getBlock().getState());
			else if (wcfg.logChestAccess && (type == 23 || type == 54 || type == 61))
				consumer.queueContainerBreak(event.getPlayer().getName(), event.getBlock().getState());
			else if (type == 79)
				consumer.queueBlockReplace(event.getPlayer().getName(), event.getBlock().getState(), 11, (byte)0);
			else
				consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlock().getState());
		}
	}

	@Override
	public void onBlockBurn(BlockBurnEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logFire)
			consumer.queueBlockBreak("Fire", event.getBlock().getState());
	}

	@Override
	public void onBlockFromTo(BlockFromToEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null) {
			final int typeFrom = event.getBlock().getTypeId();
			final int typeTo = event.getToBlock().getTypeId();
			if (typeFrom == 10 || typeFrom == 11) {
				if (typeTo == 0) {
					if (wcfg.logLavaFlow)
						consumer.queueBlockPlace("LavaFlow", event.getToBlock().getLocation(), 10, (byte)(event.getBlock().getData() + 1));
				} else if (nonFluidProofBlocks.contains(typeTo))
					consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 10, (byte)(event.getBlock().getData() + 1));
				else if (typeTo == 8 || typeTo == 9)
					if (event.getFace() == BlockFace.DOWN)
						consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 10, (byte)0);
					else
						consumer.queueBlockReplace("LavaFlow", event.getToBlock().getState(), 4, (byte)0);
			} else if (typeFrom == 8 || typeFrom == 9)
				if (typeTo == 0 || nonFluidProofBlocks.contains(typeTo)) {
					if (typeTo == 0) {
						if (wcfg.logWaterFlow)
							consumer.queueBlockPlace("WaterFlow", event.getToBlock().getLocation(), 8, (byte)(event.getBlock().getData() + 1));
					} else
						consumer.queueBlockReplace("WaterFlow", event.getToBlock().getState(), 8, (byte)(event.getBlock().getData() + 1));
					final Block lower = event.getToBlock().getRelative(BlockFace.DOWN);
					if (lower.getTypeId() == 10 || lower.getTypeId() == 11)
						consumer.queueBlockReplace("WaterFlow", lower.getState(), lower.getData() == 0 ? 49 : 4, (byte)0);
				}
		}
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logBlockPlacings) {
			final int type = event.getBlock().getTypeId();
			final BlockState before = event.getBlockReplacedState();
			final BlockState after = event.getBlockPlaced().getState();
			if (type == 0 && event.getItemInHand() != null) {
				if (event.getItemInHand().getTypeId() == 51)
					return;
				final Location loc = event.getBlock().getLocation();
				addError(dateFormat.format(System.currentTimeMillis()) + " Bukkit provided no block type for the block placed by " + event.getPlayer().getName() + " at " + loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() + ". Item in hand was: " + event.getItemInHand().getType() + ".");
				after.setTypeId(event.getItemInHand().getTypeId());
				after.setData(new MaterialData(event.getItemInHand().getTypeId()));
			}
			if (wcfg.logSignTexts && (type == 63 || type == 68))
				return;
			if (before.getTypeId() == 0)
				consumer.queueBlockPlace(event.getPlayer().getName(), after);
			else
				consumer.queueBlockReplace(event.getPlayer().getName(), before, after);
		}
	}

	@Override
	public void onLeavesDecay(LeavesDecayEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logLeavesDecay)
			consumer.queueBlockBreak("LeavesDecay", event.getBlock().getState());
	}

	@Override
	public void onSignChange(SignChangeEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logSignTexts)
			consumer.queueSignPlace(event.getPlayer().getName(), event.getBlock().getLocation(), event.getBlock().getTypeId(), event.getBlock().getData(), event.getLines());
	}

	@Override
	public void onBlockForm(BlockFormEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null) {
			final int type = event.getNewState().getTypeId();
			if (wcfg.logSnowForm && (type == 78 || type == 79))
				consumer.queueBlockReplace("SnowForm", event.getBlock().getState(), event.getNewState());
		}
	}

	@Override
	public void onBlockFade(BlockFadeEvent event) {
		final WorldConfig wcfg = worlds.get(event.getBlock().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null) {
			final int type = event.getBlock().getTypeId();
			if (wcfg.logSnowFade && (type == 78 || type == 79))
				consumer.queueBlockReplace("SnowFade", event.getBlock().getState(), event.getNewState());
		}
	}

	private void addError(String error) {
		errors.add(error);
		if (errors.size() == 20)
			try {
				final File file = new File("plugins/LogBlock/error/BlockListener.log");
				file.getParentFile().mkdirs();
				final PrintWriter writer = new PrintWriter(file);
				for (final String err : errors)
					writer.println(err);
				writer.close();
				errors.clear();
			} catch (final Exception ex) {}
	}
}
