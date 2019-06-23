package de.diddiz.LogBlock;

import static de.diddiz.util.ActionColor.CREATE;
import static de.diddiz.util.ActionColor.DESTROY;
import static de.diddiz.util.ActionColor.INTERACT;
import static de.diddiz.util.MessagingUtil.brackets;
import static de.diddiz.util.MessagingUtil.prettyDate;
import static de.diddiz.util.MessagingUtil.prettyLocation;
import static de.diddiz.util.MessagingUtil.prettyMaterial;
import static de.diddiz.util.MessagingUtil.prettyState;

import de.diddiz.LogBlock.blockstate.BlockStateCodecs;
import de.diddiz.util.BukkitUtils;
import de.diddiz.util.MessagingUtil.BracketType;
import de.diddiz.util.Utils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.DaylightDetector;
import org.bukkit.block.data.type.Lectern;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.ItemStack;

public class BlockChange implements LookupCacheElement {
    public final long id, date;
    public final Location loc;
    public final Actor actor;
    public final String playerName;
    public final int replacedMaterial, replacedData, typeMaterial, typeData;
    public final byte[] replacedState, typeState;
    public final ChestAccess ca;

    public BlockChange(long date, Location loc, Actor actor, int replaced, int replacedData, byte[] replacedState, int type, int typeData, byte[] typeState, ChestAccess ca) {
        id = 0;
        this.date = date;
        this.loc = loc;
        this.actor = actor;
        this.replacedMaterial = replaced;
        this.replacedData = replacedData;
        this.replacedState = replacedState;
        this.typeMaterial = type;
        this.typeData = typeData;
        this.typeState = typeState;
        this.ca = ca;
        this.playerName = actor == null ? null : actor.getName();
    }

    public BlockChange(ResultSet rs, QueryParams p) throws SQLException {
        id = p.needId ? rs.getInt("id") : 0;
        date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
        loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
        actor = p.needPlayer ? new Actor(rs) : null;
        playerName = p.needPlayer ? rs.getString("playername") : null;
        replacedMaterial = p.needType ? rs.getInt("replaced") : 0;
        replacedData = p.needType ? rs.getInt("replacedData") : -1;
        typeMaterial = p.needType ? rs.getInt("type") : 0;
        typeData = p.needType ? rs.getInt("typeData") : -1;
        replacedState = p.needType ? rs.getBytes("replacedState") : null;
        typeState = p.needType ? rs.getBytes("typeState") : null;
        ChestAccess catemp = null;
        if (p.needChestAccess) {
            ItemStack stack = Utils.loadItemStack(rs.getBytes("item"));
            if (stack != null) {
                catemp = new ChestAccess(stack, rs.getBoolean("itemremove"), rs.getInt("itemtype"));
            }
        }
        ca = catemp;
    }

    private String getTypeDetails(BlockData type, byte[] typeState) {
        String typeDetails = null;

        if (BlockStateCodecs.hasCodec(type.getMaterial())) {
            try {
                typeDetails = BlockStateCodecs.toString(type.getMaterial(), Utils.deserializeYamlConfiguration(typeState));
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not parse BlockState for " + type.getMaterial(), e);
            }
        }

        if (typeDetails == null) {
            return "";
        } else {
            return " " + typeDetails;
        }
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
        BlockData type = getBlockSet();
        BlockData replaced = getBlockReplaced();
        if (type == null || replaced == null) {
            msg.append("did an unknown block modification");
            return msg.toString();
        }

        // Process type details once for later use.
        String typeDetails = getTypeDetails(type, typeState);
        String replacedDetails = getTypeDetails(replaced, replacedState);

        if (type.getMaterial().equals(replaced.getMaterial())) {
            if (BukkitUtils.isEmpty(type.getMaterial())) {
                msg.append(INTERACT).append("did an unspecified action");
            } else if (ca != null) {
                if (ca.itemStack == null) {
                    msg.append(INTERACT).append("looked inside ").append(prettyMaterial(type.getMaterial()));
                } else if (ca.remove) {
                    msg.append(DESTROY).append("took ").append(BukkitUtils.toString(ca.itemStack)).append(" from ").append(prettyMaterial(type.getMaterial()));
                } else {
                    msg.append(CREATE).append("put ").append(BukkitUtils.toString(ca.itemStack)).append(" into ").append(prettyMaterial(type.getMaterial()));
                }
            } else if (BukkitUtils.getContainerBlocks().contains(type.getMaterial())) {
                msg.append(INTERACT).append("opened ").append(prettyMaterial(type.getMaterial()));
            } else if (type instanceof Openable) {
                // Door, Trapdoor, Fence gate
                msg.append(INTERACT).append(((Openable) type).isOpen() ? "opened" : "closed").append(" ").append(prettyMaterial(type.getMaterial()));
            } else if (type.getMaterial() == Material.LEVER) {
                msg.append(INTERACT).append("switched ").append(prettyMaterial(type.getMaterial())).append(" ").append(prettyState(((Switch) type).isPowered() ? "on" : "off"));
            } else if (type instanceof Switch) {
                msg.append(INTERACT).append("pressed ").append(prettyMaterial(type.getMaterial()));
            } else if (type.getMaterial() == Material.CAKE) {
                msg.append(DESTROY).append("ate a piece of ").append(prettyMaterial(type.getMaterial()));
            } else if (type.getMaterial() == Material.NOTE_BLOCK) {
                Note note = ((NoteBlock) type).getNote();
                msg.append(INTERACT).append("set ").append(prettyMaterial(type.getMaterial())).append(" to ").append(prettyState(note.getTone().name() + (note.isSharped() ? "#" : "")));
            } else if (type.getMaterial() == Material.REPEATER) {
                msg.append(INTERACT).append("set ").append(prettyMaterial(type.getMaterial())).append(" to ").append(prettyState(((Repeater) type).getDelay())).append(" ticks delay");
            } else if (type.getMaterial() == Material.COMPARATOR) {
                msg.append(INTERACT).append("set ").append(prettyMaterial(type.getMaterial())).append(" to ").append(prettyState(((Comparator) type).getMode()));
            } else if (type.getMaterial() == Material.DAYLIGHT_DETECTOR) {
                msg.append(INTERACT).append("set ").append(prettyMaterial(type.getMaterial())).append(" to ").append(prettyState(((DaylightDetector) type).isInverted() ? "inverted" : "normal"));
            } else if (type instanceof Lectern) {
                msg.append(INTERACT).append("changed the book on a ").append(prettyMaterial(type.getMaterial())).append(" to").append(prettyState(typeDetails.length() == 0 ? " empty" : typeDetails));
            } else if (type instanceof Powerable) {
                msg.append(INTERACT).append("stepped on ").append(prettyMaterial(type.getMaterial()));
            } else if (type.getMaterial() == Material.TRIPWIRE) {
                msg.append(INTERACT).append("ran into ").append(prettyMaterial(type.getMaterial()));
            } else if (type instanceof Sign || type instanceof WallSign) {
                msg.append(CREATE).append("edited a ").append(prettyMaterial(type.getMaterial())).append(CREATE).append(" to ").append(prettyState(typeDetails));
            } else {
                msg.append(CREATE).append("replaced ").append(prettyMaterial(replaced.getMaterial())).append(prettyState(replacedDetails)).append(CREATE).append(" with ").append(prettyMaterial(type.getMaterial())).append(prettyState(typeDetails));
            }
        } else if (BukkitUtils.isEmpty(type.getMaterial())) {
            msg.append(DESTROY).append("destroyed ").append(prettyMaterial(replaced.getMaterial())).append(prettyState(replacedDetails));
        } else if (BukkitUtils.isEmpty(replaced.getMaterial())) {
            msg.append(CREATE).append("created ").append(prettyMaterial(type.getMaterial())).append(prettyState(typeDetails));
        } else {
            msg.append(CREATE).append("replaced ").append(prettyMaterial(replaced.getMaterial())).append(prettyState(replacedDetails)).append(CREATE).append(" with ").append(type.getMaterial().name()).append(typeDetails);
        }
        if (loc != null) {
            msg.append(" at: ").append(prettyLocation(loc));
        }
        return msg.toString();
    }

    public BlockData getBlockReplaced() {
        return MaterialConverter.getBlockData(replacedMaterial, replacedData);
    }

    public BlockData getBlockSet() {
        return MaterialConverter.getBlockData(typeMaterial, typeData);
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
