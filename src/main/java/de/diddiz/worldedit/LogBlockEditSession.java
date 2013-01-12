package de.diddiz.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bags.BlockBag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LogBlockEditSession extends EditSession {

	private LocalPlayer player;

	/**
	 * {@inheritDoc}
	 */
	public LogBlockEditSession(LocalWorld world, int maxBlocks, LocalPlayer player) {
		super(world, maxBlocks);
		this.player = player;
	}

	/**
	 * {@inheritDoc}
	 */
	public LogBlockEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag, LocalPlayer player) {
		super(world, maxBlocks, blockBag);
		this.player = player;
	}

	@Override
	public boolean rawSetBlock(Vector pt, BaseBlock block) {
		if (!(player.getWorld() instanceof BukkitWorld)) {
			return super.rawSetBlock(pt, block);
		}
		int typeBefore = ((BukkitWorld) player.getWorld()).getWorld().getBlockTypeIdAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
		boolean success = super.rawSetBlock(pt, block);
		if (success && Config.isLogging(player.getWorld().getName(), Logging.WORLDEDIT)) {
			Location location = new Location(Bukkit.getWorld(player.getWorld().getName()), pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
			LogBlock.getInstance().getConsumer().queueBlock(player.getName(), location, typeBefore, location.getBlock().getTypeId(), location.getBlock().getData());
		}
		return success;
	}

}
