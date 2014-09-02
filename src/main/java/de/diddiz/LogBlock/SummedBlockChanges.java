package de.diddiz.LogBlock;

import de.diddiz.LogBlock.QueryParams.SummarizationMode;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static de.diddiz.util.ActionColor.CREATE;
import static de.diddiz.util.ActionColor.DESTROY;
import static de.diddiz.util.MessagingUtil.prettyMaterial;
import static de.diddiz.util.TypeColor.DEFAULT;
import static de.diddiz.util.Utils.spaces;

public class SummedBlockChanges implements LookupCacheElement {
    private final int type;
    private final int created, destroyed;
    private final float spaceFactor;
    private final Actor actor;

    public SummedBlockChanges(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
        // Actor currently useless here as we don't yet output UUID in results anywhere
        actor = p.sum == SummarizationMode.PLAYERS ? new Actor(rs) : null;
        type = p.sum == SummarizationMode.TYPES ? rs.getInt("type") : 0;
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
        StringBuilder builder = new StringBuilder();
        builder.append(CREATE).append(created).append(spaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)));
        builder.append(DESTROY).append(destroyed).append(spaces((int)((10 - String.valueOf(destroyed).length()) / spaceFactor)));
        builder.append(actor != null ? DEFAULT + actor.getName() : prettyMaterial(Objects.toString(MaterialConverter.getMaterial(type))));
        return builder.toString();
    }
}
