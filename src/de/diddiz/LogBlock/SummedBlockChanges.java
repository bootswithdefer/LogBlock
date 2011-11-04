package de.diddiz.LogBlock;

import static de.diddiz.util.MaterialName.materialName;
import static de.diddiz.util.Utils.spaces;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class SummedBlockChanges implements LookupCacheElement
{
	private final String group;
	private final int created, destroyed;
	private final float spaceFactor;

	public SummedBlockChanges(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
		group = p.sum == SummarizationMode.PLAYERS ? rs.getString(1) : materialName(rs.getInt(1));
		created = rs.getInt(2);
		destroyed = rs.getInt(3);
		this.spaceFactor = spaceFactor;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getMessage() {
		return created + spaces((int)((10 - String.valueOf(created).length()) / spaceFactor)) + destroyed + spaces((int)((10 - String.valueOf(destroyed).length()) / spaceFactor)) + group;
	}
}
