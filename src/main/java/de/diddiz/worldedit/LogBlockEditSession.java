package de.diddiz.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bags.BlockBag;
import com.sk89q.worldedit.blocks.BaseBlock;

public class LogBlockEditSession extends EditSession{

	/**
	 * {@inheritDoc}
	 */
	public LogBlockEditSession(LocalWorld world, int maxBlocks) {
		super(world, maxBlocks);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public LogBlockEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag) {
		super(world, maxBlocks, blockBag);
	}
	
	@Override
	public boolean rawSetBlock(Vector pt, BaseBlock block) {
		//TODO: perform logging actions here
		return super.rawSetBlock(pt, block);
	}

}
