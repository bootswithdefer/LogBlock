package de.diddiz.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.bags.BlockBag;

public class LogBlockEditSessionFactory extends EditSessionFactory {
	
	@Override
	public EditSession getEditSession(LocalWorld world, int maxBlocks) {
		return new LogBlockEditSession(world, maxBlocks);
	}
	
	@Override
	public EditSession getEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag) {
		return new LogBlockEditSession(world, maxBlocks, blockBag);
	}

}
