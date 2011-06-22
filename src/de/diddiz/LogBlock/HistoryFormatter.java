package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.materialName;
import static de.diddiz.util.Utils.spaces;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class HistoryFormatter
{
	private final SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
	private final SummarizationMode sum;
	private final boolean coords;
	private final float factor;

	HistoryFormatter(SummarizationMode sum, boolean coords, float factor) {
		this.sum = sum;
		this.coords = coords;
		this.factor = factor;
	}

	String format(ResultSet rs) throws SQLException {
		if (sum == SummarizationMode.NONE) {
			final StringBuilder msg = new StringBuilder(formatter.format(rs.getTimestamp("date")) + " " + rs.getString("playername") + " ");
			final int type = rs.getInt("type");
			final int replaced = rs.getInt("replaced");
			final String signtext;
			if ((type == 63 || type == 68 || replaced == 63 || replaced == 68) && (signtext = rs.getString("signtext")) != null) {
				final String action = type == 0 ? "destroyed " : "created ";
				if (!signtext.contains("\0"))
					msg.append(action + signtext);
				else
					msg.append(action + "sign [" + signtext.replace("\0", "] [") + "]");
			} else if (type == replaced) {
				if (type == 0)
					msg.append("did a unspecified action");
				else if (type == 23 || type == 54 || type == 61 || type == 62) {
					final int itemType = rs.getInt("itemtype");
					final int itemAmount = rs.getInt("itemamount");
					if (itemType == 0 || itemAmount == 0)
						msg.append("looked inside " + materialName(type));
					else if (itemAmount < 0)
						msg.append("took " + itemAmount * -1 + "x " + materialName(itemType));
					else
						msg.append("put in " + itemAmount + "x " + materialName(itemType));
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
			if (coords)
				msg.append(" at " + rs.getInt("x") + ":" + rs.getInt("y") + ":" + rs.getInt("z"));
			return msg.toString();
		}
		String c1 = String.valueOf(rs.getInt(2)), c2 = String.valueOf(rs.getInt(3));
		c1 += spaces((int)((10 - c1.length()) / factor));
		c2 += spaces((int)((10 - c2.length()) / factor));
		if (sum == SummarizationMode.TYPES)
			return c1 + c2 + materialName(rs.getInt(1));
		return c1 + c2 + rs.getString(1);
	}
}
