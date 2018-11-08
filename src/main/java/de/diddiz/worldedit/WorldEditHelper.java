package de.diddiz.worldedit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import com.sk89q.jnbt.NBTOutputStream;
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

    private static class Internal {
        // private static WorldEditPlugin worldEdit;

        public static void setWorldEdit(Plugin worldEdit) {
            // Internal.worldEdit = (WorldEditPlugin) worldEdit;
        }

        public static byte[] serializeEntity(Entity entity) {
            com.sk89q.worldedit.entity.Entity weEntity = BukkitAdapter.adapt(entity);
            BaseEntity state = weEntity.getState();
            if (state != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    NBTOutputStream nbtos = new NBTOutputStream(baos);
                    nbtos.writeNamedTag("entity", state.getNbtData());
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
