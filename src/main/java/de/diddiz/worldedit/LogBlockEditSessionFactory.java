package de.diddiz.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bags.BlockBag;
import de.diddiz.LogBlock.LogBlock;

public class LogBlockEditSessionFactory extends EditSessionFactory {

	private LogBlock plugin;

	public LogBlockEditSessionFactory(LogBlock lb) {
		this.plugin = lb;
	}

	@Override
	public EditSession getEditSession(LocalWorld world, int maxBlocks, LocalPlayer player) {
		return new LogBlockEditSession(world, maxBlocks, player, plugin);
	}

	@Override
	public EditSession getEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag, LocalPlayer player) {
		return new LogBlockEditSession(world, maxBlocks, blockBag, player, plugin);
	}

	public static void initialize(LogBlock logBlock) {
		try {
			// Check to see if the world edit version is compatible
			Class.forName("com.sk89q.worldedit.EditSessionFactory").getDeclaredMethod("getEditSession", LocalWorld.class, int.class, BlockBag.class, LocalPlayer.class);
			WorldEdit.getInstance().setEditSessionFactory(new LogBlockEditSessionFactory(logBlock));
		} catch (Throwable ignored) {

		}
	}

}
