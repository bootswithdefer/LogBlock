package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.getMaterialName;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class HistoryFormatter
{
	private final SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
	private final SummarizationMode sum;

	HistoryFormatter(SummarizationMode sum) {
		this.sum = sum;
	}

	String format(ResultSet rs, boolean coords) throws SQLException {
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
				else if (type == 23 || type == 54 || type == 61) {
					final int itemType = rs.getInt("itemtype");
					final int itemAmount = rs.getInt("itemamount");
					if (itemType == 0 || itemAmount == 0)
						msg.append("looked inside " + getMaterialName(type));
					else if (itemAmount < 0)
						msg.append("took " + itemAmount * -1 + "x " + getMaterialName(itemType));
					else
						msg.append("put in " + itemAmount + "x " + getMaterialName(itemType));
				}
			} else if (type == 0)
				msg.append("destroyed " + getMaterialName(replaced));
			else if (replaced == 0)
				msg.append("created " + getMaterialName(type));
			else
				msg.append("replaced " + getMaterialName(replaced) + " with " + getMaterialName(type));
			if (coords)
				msg.append(" at " + rs.getInt("x") + ":" + rs.getInt("y") + ":" + rs.getInt("z"));
			return msg.toString();
		} else if (sum == SummarizationMode.TYPES)
			return fillWithSpaces(rs.getInt("created")) + fillWithSpaces(rs.getInt("destroyed")) + getMaterialName(rs.getInt("type"));
		else
			return fillWithSpaces(rs.getInt("created")) + fillWithSpaces(rs.getInt("destroyed")) + rs.getString("playername");
	}

	private static String fillWithSpaces(Integer number) {
		final StringBuilder filled = new StringBuilder(number.toString());
		final int neededSpaces = (36 - filled.length() * 6) / 4;
		for (int i = 0; i < neededSpaces; i++)
			filled.append(' ');
		return filled.toString();
	}
}
