package de.diddiz.worldedit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;

public class WorldEditHelper {
    private static boolean checkedForWorldEdit;
    private static boolean hasWorldEdit;

    public static boolean hasWorldEdit() {
        if (!checkedForWorldEdit) {
            checkedForWorldEdit = true;
            Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
            hasWorldEdit = worldEdit != null;
            Internal.setWorldEdit(worldEdit);
        }
        return hasWorldEdit;
    }

    public static byte[] serializeEntity(Entity entity) {
        if (!hasWorldEdit()) {
            return null;
        }
        return Internal.serializeEntity(entity);
    }

    public static Entity restoreEntity(Location location, EntityType type, byte[] serialized) {
        return Internal.restoreEntity(location, type, serialized);
    }

    private static class Internal {
        // private static WorldEditPlugin worldEdit;

        public static void setWorldEdit(Plugin worldEdit) {
            // Internal.worldEdit = (WorldEditPlugin) worldEdit;
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
                    CompoundTag oldNbt = state.getNbtData();
                    UUID oldUUID = new UUID(oldNbt.getLong("UUIDMost"), oldNbt.getLong("UUIDLeast"));
                    com.sk89q.worldedit.entity.Entity weEntity = weLocation.getExtent().createEntity(weLocation, state);
                    if (weEntity != null) {
                        CompoundTag newNbt = weEntity.getState().getNbtData();
                        newUUID = new UUID(newNbt.getLong("UUIDMost"), newNbt.getLong("UUIDLeast"));
                        System.out.println("Old UUID: " + oldUUID);
                        System.out.println("New UUID: " + newUUID);
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
                    value.put("Motion", new ListTag(DoubleTag.class, Arrays.asList(new DoubleTag[] {new DoubleTag(0),new DoubleTag(0),new DoubleTag(0)})));
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

    }
}
