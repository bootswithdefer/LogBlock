package de.diddiz.worldedit;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bags.BlockBag;

public class LogBlockEditSessionFactory extends EditSessionFactory {

	@Override
	public EditSession getEditSession(LocalWorld world, int maxBlocks, LocalPlayer player) {
		return new LogBlockEditSession(world, maxBlocks, player);
	}

	@Override
	public EditSession getEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag, LocalPlayer player) {
		return new LogBlockEditSession(world, maxBlocks, blockBag, player);
	}

	public static void initialize() {
		try {
			// Check to see if the world edit version is compatible
			Class.forName("com.sk89q.worldedit.EditSessionFactory").getDeclaredMethod("getEditSession", LocalWorld.class, int.class, BlockBag.class, LocalPlayer.class);
			WorldEdit.getInstance().setEditSessionFactory(new LogBlockEditSessionFactory());
		} catch (Throwable t) {

		}
	}

}
