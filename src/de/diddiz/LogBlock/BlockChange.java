package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.materialName;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import org.bukkit.Location;

public class BlockChange implements LookupCacheElement
{
	private final static SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
	public final long id, date;
	public final Location loc;
	public final String playerName;
	public final int replaced, type;
	public final byte data;
	public final String signtext;
	public final ChestAccess ca;

	public BlockChange(long date, Location loc, String playerName, int replaced, int type, byte data, String signtext, ChestAccess ca) {
		id = 0;
		this.date = date;
		this.loc = loc;
		this.playerName = playerName;
		this.replaced = replaced;
		this.type = type;
		this.data = data;
		this.signtext = signtext;
		this.ca = ca;
	}

	public BlockChange(ResultSet rs, QueryParams p) throws SQLException {
		id = p.needId ? rs.getInt("id") : 0;
		date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
		loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
		playerName = p.needPlayer ? rs.getString("playername") : null;
		replaced = p.needType ? rs.getInt("replaced") : 0;
		type = p.needType ? rs.getInt("type") : 0;
		data = p.needData ? rs.getByte("data") : (byte)0;
		signtext = p.needSignText ? rs.getString("signtext") : null;
		ca = p.needChestAccess && rs.getShort("itemtype") != 0 && rs.getShort("itemamount") != 0 ? new ChestAccess(rs.getShort("itemtype"), rs.getShort("itemamount"), rs.getByte("itemdata")) : null;
	}

	@Override
	public String toString() {
		final StringBuilder msg = new StringBuilder();
		if (date > 0)
			msg.append(formatter.format(date) + " ");
		if (playerName != null)
			msg.append(playerName + " ");
		if (signtext != null) {
			final String action = type == 0 ? "destroyed " : "created ";
			if (!signtext.contains("\0"))
				msg.append(action + signtext);
			else
				msg.append(action + materialName(type != 0 ? type : replaced) + " [" + signtext.replace("\0", "] [") + "]");
		} else if (type == replaced) {
			if (type == 0)
				msg.append("did a unspecified action");
			else if (ca != null) {
				if (ca.itemType == 0 || ca.itemAmount == 0)
					msg.append("looked inside " + materialName(type));
				else if (ca.itemAmount < 0)
					msg.append("took " + ca.itemAmount * -1 + "x " + materialName(ca.itemType, ca.itemData));
				else
					msg.append("put in " + ca.itemAmount + "x " + materialName(ca.itemType, ca.itemData));
			} else if (type == 23 || type == 54 || type == 61 || type == 62)
				msg.append("opened " + materialName(type));
			else if (type == 69)
				msg.append("swiched " + materialName(type));
			else if (type == 77)
				msg.append("pressed " + materialName(type));
		} else if (type == 0)
			msg.append("destroyed " + materialName(replaced, data));
		else if (replaced == 0)
			msg.append("created " + materialName(type, data));
		else
			msg.append("replaced " + materialName(replaced, (byte)0) + " with " + materialName(type, data));
		if (loc != null)
			msg.append(" at " + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
		return msg.toString();
	}

	@Override
	public Location getLocation() {
		return loc;
	}

	@Override
	public String getMessage() {
		return toString();
	}
}
