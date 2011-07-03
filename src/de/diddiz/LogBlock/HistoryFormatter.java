package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.materialName;
import static de.diddiz.util.Utils.spaces;
import java.sql.ResultSet;
import java.sql.SQLException;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class HistoryFormatter
{
	private final SummarizationMode sum;
	private final boolean coords;
	private final float factor;

	HistoryFormatter(SummarizationMode sum, boolean coords, float factor) {
		this.sum = sum;
		this.coords = coords;
		this.factor = factor;
	}

	String format(ResultSet rs) throws SQLException {
		if (sum == SummarizationMode.NONE)
			return new BlockChange(rs, coords).toString();
		String c1 = String.valueOf(rs.getInt(2)), c2 = String.valueOf(rs.getInt(3));
		c1 += spaces((int)((10 - c1.length()) / factor));
		c2 += spaces((int)((10 - c2.length()) / factor));
		if (sum == SummarizationMode.TYPES)
			return c1 + c2 + materialName(rs.getInt(1));
		return c1 + c2 + rs.getString(1);
	}
}
