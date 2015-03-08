package de.diddiz.LogBlock;

public class ChestAccess
{
	final short itemType, itemAmount, itemData;

	public ChestAccess(short itemType, short itemAmount, short itemData) {
		this.itemType = itemType;
		this.itemAmount = itemAmount;
		this.itemData = itemData >= 0 ? itemData : 0;
	}
}
