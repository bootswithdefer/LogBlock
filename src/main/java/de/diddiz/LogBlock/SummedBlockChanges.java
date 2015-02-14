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
	private final Actor actor;

	public SummedBlockChanges(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
		// Actor currently useless here as we don't yet output UUID in results anywhere
		actor = p.sum == SummarizationMode.PLAYERS ? new Actor(rs) : null;
		group = actor == null ? materialName(rs.getInt("type")) : actor.getName();
		created = rs.getInt("created");
		destroyed = rs.getInt("destroyed");
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
