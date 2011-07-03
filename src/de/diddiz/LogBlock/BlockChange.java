package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.materialName;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import org.bukkit.Location;

public class BlockChange
{
	private final static SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
	public final long date;
	public final Location loc;
	public final String playerName;
	public final int replaced, type;
	public final byte data;
	public final String signtext;
	public final ChestAccess ca;

	public BlockChange(long date, Location loc, String playerName, int replaced, int type, byte data, String signtext, ChestAccess ca) {
		this.date = date;
		this.loc = loc;
		this.playerName = playerName;
		this.replaced = replaced;
		this.type = type;
		this.data = data;
		this.signtext = signtext;
		this.ca = ca;
	}

	public BlockChange(ResultSet rs, boolean coords) throws SQLException {
		date = rs.getTimestamp("date").getTime();
		loc = coords ? new Location(null, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
		playerName = rs.getString("playername");
		replaced = rs.getInt("replaced");
		type = rs.getInt("type");
		data = (byte)0;
		signtext = type == 63 || type == 68 || replaced == 63 || replaced == 68 ? rs.getString("signtext") : null;
		ca = type == replaced && (type == 23 || type == 54 || type == 61 || type == 62) ? new ChestAccess(rs.getShort("itemtype"), rs.getShort("itemamount"), (byte)0) : null;
	}

	@Override
	public String toString() {
		final StringBuilder msg = new StringBuilder(formatter.format(date) + " " + playerName + " ");
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
					msg.append("took " + ca.itemAmount * -1 + "x " + materialName(ca.itemType));
				else
					msg.append("put in " + ca.itemAmount + "x " + materialName(ca.itemType));
			} else if (type == 69)
				msg.append("swiched " + materialName(type));
			else if (type == 77)
				msg.append("pressed " + materialName(type));
		} else if (type == 0)
			msg.append("destroyed " + materialName(replaced));
		else if (replaced == 0)
			msg.append("created " + materialName(type));
		else
			msg.append("replaced " + materialName(replaced) + " with " + materialName(type));
		if (loc != null)
			msg.append(" at " + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
		return msg.toString();
	}
}
