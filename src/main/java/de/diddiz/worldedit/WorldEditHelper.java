package de.diddiz.worldedit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.util.CuboidRegion;

public class WorldEditHelper {
    private static boolean checkedForWorldEdit;
    private static boolean hasWorldEdit;

    public static boolean hasWorldEdit() {
        if (!checkedForWorldEdit) {
            checkedForWorldEdit = true;
            Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
            hasWorldEdit = worldEdit != null;
            if (worldEdit != null) {
                Internal.setWorldEdit(worldEdit);
            }
        }
        return hasWorldEdit;
    }

    public static boolean hasFullWorldEdit() {
        return hasWorldEdit && Internal.hasBukkitImplAdapter();
    }

    public static byte[] serializeEntity(Entity entity) {
        if (!hasWorldEdit()) {
            return null;
        }
        return Internal.serializeEntity(entity);
    }

    public static Entity restoreEntity(Location location, EntityType type, byte[] serialized) {
        if (!hasWorldEdit()) {
            return null;
        }
        return Internal.restoreEntity(location, type, serialized);
    }

    public static CuboidRegion getSelectedRegion(Player player) throws IllegalArgumentException {
        if (!hasWorldEdit()) {
            throw new IllegalArgumentException("WorldEdit not found!");
        }
        return Internal.getSelectedRegion(player);
    }

    private static class Internal {
        private static WorldEditPlugin worldEdit;
        private static Method getBukkitImplAdapter;

        public static void setWorldEdit(Plugin worldEdit) {
            Internal.worldEdit = (WorldEditPlugin) worldEdit;
        }

        public static boolean hasBukkitImplAdapter() {
            if (getBukkitImplAdapter == null) {
                try {
                    getBukkitImplAdapter = WorldEditPlugin.class.getDeclaredMethod("getBukkitImplAdapter");
                    getBukkitImplAdapter.setAccessible(true);
                } catch (Exception e) {
                    LogBlock.getInstance().getLogger().log(Level.SEVERE, "Exception while checking for BukkitImplAdapter", e);
                    return false;
                }
            }
            try {
                return getBukkitImplAdapter.invoke(worldEdit) != null;
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Exception while checking for BukkitImplAdapter", e);
                return false;
            }
        }

        public static Entity restoreEntity(Location location, EntityType type, byte[] serialized) {
            com.sk89q.worldedit.world.entity.EntityType weType = BukkitAdapter.adapt(type);
            com.sk89q.worldedit.util.Location weLocation = BukkitAdapter.adapt(location);
            try {
                NBTInputStream nbtis = new NBTInputStream(new ByteArrayInputStream(serialized));
                NamedTag namedTag = nbtis.readNamedTag();
                nbtis.close();
                UUID newUUID = null;
                if (namedTag.getName().equals("entity") && namedTag.getTag() instanceof CompoundTag) {
                    CompoundTag serializedState = (CompoundTag) namedTag.getTag();
                    BaseEntity state = new BaseEntity(weType, serializedState);
                    com.sk89q.worldedit.entity.Entity weEntity = weLocation.getExtent().createEntity(weLocation, state);
                    if (weEntity != null) {
                        CompoundTag newNbt = weEntity.getState().getNbtData();
                        newUUID = new UUID(newNbt.getLong("UUIDMost"), newNbt.getLong("UUIDLeast"));
                    }
                }
                return newUUID == null ? null : Bukkit.getEntity(newUUID);
            } catch (IOException e) {
                throw new RuntimeException("This IOException should be impossible", e);
            }
        }

        public static byte[] serializeEntity(Entity entity) {
            com.sk89q.worldedit.entity.Entity weEntity = BukkitAdapter.adapt(entity);
            BaseEntity state = weEntity.getState();
            if (state != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    NBTOutputStream nbtos = new NBTOutputStream(baos);
                    CompoundTag nbt = state.getNbtData();
                    LinkedHashMap<String, Tag> value = new LinkedHashMap<>(nbt.getValue());
                    value.put("Health", new FloatTag(20.0f));
                    value.put("Motion", new ListTag(DoubleTag.class, Arrays.asList(new DoubleTag[] { new DoubleTag(0), new DoubleTag(0), new DoubleTag(0) })));
                    value.put("Fire", new ShortTag((short) -20));
                    value.put("HurtTime", new ShortTag((short) 0));
                    nbtos.writeNamedTag("entity", new CompoundTag(value));
                    nbtos.close();
                    return baos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("This IOException should be impossible", e);
                }
            }
            return null;
        }

        public static CuboidRegion getSelectedRegion(Player player) throws IllegalArgumentException {
            LocalSession session = worldEdit.getSession(player);
            World world = player.getWorld();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            if (!weWorld.equals(session.getSelectionWorld())) {
                throw new IllegalArgumentException("No selection defined");
            }
            Region selection;
            try {
                selection = session.getSelection(weWorld);
            } catch (IncompleteRegionException e) {
                throw new IllegalArgumentException("No selection defined");
            }
            if (selection == null) {
                throw new IllegalArgumentException("No selection defined");
            }
            if (!(selection instanceof com.sk89q.worldedit.regions.CuboidRegion)) {
                throw new IllegalArgumentException("You have to define a cuboid selection");
            }
            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();
            return new CuboidRegion(world, new BlockVector(min.getBlockX(), min.getBlockY(), min.getBlockZ()), new BlockVector(max.getBlockX(), max.getBlockY(), max.getBlockZ()));
        }
    }
}
