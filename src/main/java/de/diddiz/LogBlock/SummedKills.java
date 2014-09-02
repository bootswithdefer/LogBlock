package de.diddiz.LogBlock;

import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

import static de.diddiz.util.ActionColor.CREATE;
import static de.diddiz.util.ActionColor.DESTROY;
import static de.diddiz.util.TypeColor.DEFAULT;
import static de.diddiz.util.Utils.spaces;

public class SummedKills implements LookupCacheElement {
    private final Actor player;
    private final int kills, killed;
    private final float spaceFactor;

    public SummedKills(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
        player = new Actor(rs);
        kills = rs.getInt("kills");
        killed = rs.getInt("killed");
        this.spaceFactor = spaceFactor;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append(CREATE).append(kills).append(spaces((int)((6 - String.valueOf(kills).length()) / spaceFactor)));
        builder.append(DESTROY).append(killed).append(spaces((int)((7 - String.valueOf(killed).length()) / spaceFactor)));
        builder.append(DEFAULT).append(player.getName());
        return builder.toString();

    }
}
