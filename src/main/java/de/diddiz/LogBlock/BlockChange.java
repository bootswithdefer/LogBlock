package de.diddiz.LogBlock;

import de.diddiz.LogBlock.blockstate.BlockStateCodecs;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.util.BukkitUtils;
import de.diddiz.util.Utils;

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

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

    @Override
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        if (date > 0) {
            msg.append(Config.formatter.format(date)).append(" ");
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
        String typeDetails = null;
        if (BlockStateCodecs.hasCodec(type.getMaterial())) {
            try {
                typeDetails = BlockStateCodecs.toString(type.getMaterial(), Utils.deserializeYamlConfiguration(typeState));
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not parse BlockState for " + type.getMaterial(), e);
            }
        }
        if (typeDetails == null) {
            typeDetails = "";
        } else {
            typeDetails = " " + typeDetails;
        }
        String replacedDetails = null;
        if (BlockStateCodecs.hasCodec(replaced.getMaterial())) {
            try {
                replacedDetails = BlockStateCodecs.toString(replaced.getMaterial(), Utils.deserializeYamlConfiguration(replacedState));
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not parse BlockState for " + replaced.getMaterial(), e);
            }
        }
        if (replacedDetails == null) {
            replacedDetails = "";
        } else {
            replacedDetails = " " + replacedDetails;
        }
        if (type.getMaterial().equals(replaced.getMaterial())) {
            if (BukkitUtils.isEmpty(type.getMaterial())) {
                msg.append("did an unspecified action");
            } else if (ca != null) {
                if (ca.itemStack == null) {
                    msg.append("looked inside ").append(type.getMaterial().name());
                } else if (ca.remove) {
                    msg.append("took ").append(BukkitUtils.toString(ca.itemStack)).append(" from ").append(type.getMaterial().name());
                } else {
                    msg.append("put ").append(BukkitUtils.toString(ca.itemStack)).append(" into ").append(type.getMaterial().name());
                }
            } else if (BukkitUtils.getContainerBlocks().contains(type.getMaterial())) {
                msg.append("opened ").append(type.getMaterial().name());
            } else if (type instanceof Openable) {
                // Door, Trapdoor, Fence gate
                msg.append(((Openable) type).isOpen() ? "opened" : "closed").append(" ").append(type.getMaterial().name());
            } else if (type.getMaterial() == Material.LEVER) {
                msg.append("switched ").append(type.getMaterial().name()).append(" ").append(((Switch) type).isPowered() ? "on" : "off");
            } else if (type instanceof Switch) {
                msg.append("pressed ").append(type.getMaterial().name());
            } else if (type.getMaterial() == Material.CAKE) {
                msg.append("ate a piece of ").append(type.getMaterial().name());
            } else if (type.getMaterial() == Material.NOTE_BLOCK) {
                Note note = ((NoteBlock) type).getNote();
                msg.append("set ").append(type.getMaterial().name()).append(" to ").append(note.getTone().name()).append(note.isSharped() ? "#" : "");
            } else if (type.getMaterial() == Material.REPEATER) {
                msg.append("set ").append(type.getMaterial().name()).append(" to ").append(((Repeater) type).getDelay()).append(" ticks delay");
            } else if (type.getMaterial() == Material.COMPARATOR) {
                msg.append("set ").append(type.getMaterial().name()).append(" to ").append(((Comparator) type).getMode());
            } else if (type.getMaterial() == Material.DAYLIGHT_DETECTOR) {
                msg.append("set ").append(type.getMaterial().name()).append(" to ").append(((DaylightDetector) type).isInverted() ? "inverted" : "normal");
            } else if (type instanceof Lectern) {
                msg.append("changed the book on a ").append(type.getMaterial().name()).append(" to").append(typeDetails.length() == 0 ? " empty" : typeDetails);
            } else if (type instanceof Powerable) {
                msg.append("stepped on ").append(type.getMaterial().name());
            } else if (type.getMaterial() == Material.TRIPWIRE) {
                msg.append("ran into ").append(type.getMaterial().name());
            } else if (type instanceof Sign || type instanceof WallSign) {
                msg.append("edited a ").append(type.getMaterial().name()).append(" to ").append(typeDetails);
            } else {
                msg.append("replaced ").append(replaced.getMaterial().name()).append(replacedDetails).append(" with ").append(type.getMaterial().name()).append(typeDetails);
            }
        } else if (BukkitUtils.isEmpty(type.getMaterial())) {
            msg.append("destroyed ").append(replaced.getMaterial().name()).append(replacedDetails);
        } else if (BukkitUtils.isEmpty(replaced.getMaterial())) {
            msg.append("created ").append(type.getMaterial().name()).append(typeDetails);
        } else {
            msg.append("replaced ").append(replaced.getMaterial().name()).append(replacedDetails).append(" with ").append(type.getMaterial().name()).append(typeDetails);
        }
        if (loc != null) {
            msg.append(" at ").append(loc.getBlockX()).append(":").append(loc.getBlockY()).append(":").append(loc.getBlockZ());
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
