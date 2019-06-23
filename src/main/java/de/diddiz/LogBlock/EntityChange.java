package de.diddiz.LogBlock;

import static de.diddiz.util.ActionColor.CREATE;
import static de.diddiz.util.ActionColor.DESTROY;
import static de.diddiz.util.ActionColor.INTERACT;
import static de.diddiz.util.MessagingUtil.brackets;
import static de.diddiz.util.MessagingUtil.prettyDate;
import static de.diddiz.util.MessagingUtil.prettyEntityType;
import static de.diddiz.util.MessagingUtil.prettyLocation;
import static de.diddiz.util.MessagingUtil.prettyMaterial;

import de.diddiz.util.MessagingUtil.BracketType;
import de.diddiz.util.Utils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

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
    public final int entityId;
    public final UUID entityUUID;
    public final EntityChangeType changeType;
    public final byte[] data;

    public EntityChange(long date, Location loc, Actor actor, EntityType type, UUID entityid, EntityChangeType changeType, byte[] data) {
        id = 0;
        this.date = date;
        this.loc = loc;
        this.actor = actor;
        this.type = type;
        this.entityId = -1;
        this.entityUUID = entityid;
        this.changeType = changeType;
        this.data = data;
    }

    public EntityChange(ResultSet rs, QueryParams p) throws SQLException {
        id = p.needId ? rs.getInt("id") : 0;
        date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
        loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
        actor = p.needPlayer ? new Actor(rs) : null;
        type = p.needType ? EntityTypeConverter.getEntityType(rs.getInt("entitytypeid")) : null;
        entityId = p.needData ? rs.getInt("entityid") : 0;
        entityUUID = p.needData ? UUID.fromString(rs.getString("entityuuid")) : null;
        changeType = p.needType ? EntityChangeType.valueOf(rs.getInt("action")) : null;
        data = p.needData ? rs.getBytes("data") : null;
    }

    @Override
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        if (date > 0) {
            msg.append(brackets(prettyDate(date), BracketType.STANDARD)).append(' ');
        }
        if (actor != null) {
            msg.append(actor.getName()).append(" ");
        }
        if (changeType == EntityChangeType.CREATE) {
            msg.append(CREATE).append("created ");
        } else if (changeType == EntityChangeType.KILL) {
            boolean living = type != null && LivingEntity.class.isAssignableFrom(type.getEntityClass()) && !ArmorStand.class.isAssignableFrom(type.getDeclaringClass());
            msg.append(DESTROY).append(living ? "killed " : "destroyed ");
        } else if (changeType == EntityChangeType.ADDEQUIP) {
            YamlConfiguration conf = Utils.deserializeYamlConfiguration(data);
            ItemStack stack = conf == null ? null : conf.getItemStack("item");
            if (stack == null) {
                msg.append(CREATE).append("added an item to ");
            } else {
                msg.append(CREATE).append("added ").append(prettyMaterial(stack.getType())).append(" to ");
            }
        } else if (changeType == EntityChangeType.REMOVEEQUIP) {
            YamlConfiguration conf = Utils.deserializeYamlConfiguration(data);
            ItemStack stack = conf == null ? null : conf.getItemStack("item");
            if (stack == null) {
                msg.append(DESTROY).append("removed an item from ");
            } else {
                msg.append(DESTROY).append("removed ").append(prettyMaterial(stack.getType())).append(" from ");
            }
        } else if (changeType == EntityChangeType.MODIFY) {
            msg.append(INTERACT).append("modified ");
        } else {
            msg.append(INTERACT).append("did an unknown action to ");
        }
        if (type != null) {
            msg.append(prettyEntityType(type));
        } else {
            msg.append(prettyMaterial("an unknown entity"));
        }
        if (loc != null) {
            msg.append(" at: ").append(prettyLocation(loc));
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
