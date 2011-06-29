package de.diddiz.LogBlock;

import org.bukkit.Location;

public class BlockChange
{
	public final Location loc;
	public final String playerName;
	public final int replaced, type;
	public final byte data;
	public final String signtext;
	public final ChestAccess ca;

	public BlockChange(Location loc, String playerName, int replaced, int type, byte data, String signtext, ChestAccess ca) {
		this.loc = loc;
		this.playerName = playerName;
		this.replaced = replaced;
		this.type = type;
		this.data = data;
		this.signtext = signtext;
		this.ca = ca;
	}
}
