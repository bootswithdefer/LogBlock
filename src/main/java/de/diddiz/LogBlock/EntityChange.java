package de.diddiz.LogBlock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import de.diddiz.LogBlock.config.Config;

public class EntityChange implements LookupCacheElement {
    public static enum EntityChangeType {
        CREATE,
        KILL,
        MODIFY,
        ADDEQUIP,
        REMOVEEQUIP;

        private static EntityChangeType[] values = values();

        public static EntityChangeType valueOf(int ordinal) {
            return values[ordinal];
        }
    }

    public final long id, date;
    public final Location loc;
    public final Actor actor;
    public final EntityType type;
    public final UUID entityid;
    public final EntityChangeType changeType;
    public final byte[] data;

    public EntityChange(long date, Location loc, Actor actor, EntityType type, UUID entityid, EntityChangeType changeType, byte[] data) {
        id = 0;
        this.date = date;
        this.loc = loc;
        this.actor = actor;
        this.type = type;
        this.entityid = entityid;
        this.changeType = changeType;
        this.data = data;
    }

    public EntityChange(ResultSet rs, QueryParams p) throws SQLException {
        id = p.needId ? rs.getInt("id") : 0;
        date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
        loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
        actor = p.needPlayer ? new Actor(rs) : null;
        type = p.needType ? EntityTypeConverter.getEntityType(rs.getInt("entitytypeid")) : null;
        entityid = p.needData ? UUID.fromString(rs.getString("entityuuid")) : null;
        changeType = p.needType ? EntityChangeType.valueOf(rs.getInt("action")) : null;
        data = p.needData ? rs.getBytes("data") : null;
    }

    @Override
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        if (date > 0) {
            msg.append(Config.formatter.format(date)).append(" ");
        }
        if (actor != null) {
            msg.append(actor.getName()).append(" ");
        }
        if (type != null) {
            boolean living = LivingEntity.class.isAssignableFrom(type.getEntityClass()) && !ArmorStand.class.isAssignableFrom(type.getDeclaringClass());
            if (changeType == EntityChangeType.CREATE) {
                msg.append("created ");
            } else if (changeType == EntityChangeType.KILL) {
                msg.append(living ? "killed " : "destroyed ");
            } else if (changeType == EntityChangeType.ADDEQUIP) {
                msg.append("added an item to ");
            } else if (changeType == EntityChangeType.REMOVEEQUIP) {
                msg.append("removed an item from ");
            } else if (changeType == EntityChangeType.MODIFY) {
                msg.append("modified ");
            }
            msg.append(type.name());
        }
        if (loc != null) {
            msg.append(" at ").append(loc.getBlockX()).append(":").append(loc.getBlockY()).append(":").append(loc.getBlockZ());
        }
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
