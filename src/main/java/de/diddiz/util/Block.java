package de.diddiz.util;

import java.util.List;

public class Block
{
	private int block;
	private int data;

	/**
	 * @param block The id of the block
	 * @param data The data for the block, -1 for any data
	 *
	 */
	public Block(int block, int data) {
		this.block = block;
		this.data = data;
	}

	public int getBlock() {
		return this.block;
	}

	public int getData() {
		return this.data;
	}

	public static boolean inList(List<Block> types, int blockID) {
		for (Block block : types) {
			if (block.getBlock() == blockID) {
				return true;
			}
		}
		return false;
	}
}
