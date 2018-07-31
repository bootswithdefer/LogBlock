package de.diddiz.LogBlock.blockstate;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.YamlConfiguration;

public class BlockStateCodecSkull implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.PLAYER_WALL_HEAD, Material.PLAYER_HEAD };
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        if (state instanceof Skull) {
            Skull skull = (Skull) state;
            OfflinePlayer owner = skull.hasOwner() ? skull.getOwningPlayer() : null;
            if (owner != null) {
                YamlConfiguration conf = new YamlConfiguration();
                conf.set("owner", owner.getUniqueId().toString());
                return conf;
            }
        }
        return null;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof Skull) {
            Skull skull = (Skull) state;
            UUID ownerId = conf == null ? null : UUID.fromString(conf.getString("owner"));
            if (ownerId == null) {
                skull.setOwningPlayer(null);
            } else {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(ownerId));
            }
        }
    }

    @Override
    public String toString(YamlConfiguration conf) {
        UUID ownerId = conf == null ? null : UUID.fromString(conf.getString("owner"));
        if (ownerId != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
            return "[" + (owner.getName() != null ? owner.getName() : owner.getUniqueId().toString()) + "]";
        }
        return null;
    }
}
