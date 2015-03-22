package de.diddiz.LogBlock;

import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

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
        return kills + spaces((int) ((6 - String.valueOf(kills).length()) / spaceFactor)) + killed + spaces((int) ((7 - String.valueOf(killed).length()) / spaceFactor)) + player.getName();
    }
}
