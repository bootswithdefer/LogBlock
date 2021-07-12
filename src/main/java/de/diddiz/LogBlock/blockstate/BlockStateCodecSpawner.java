package de.diddiz.LogBlock.blockstate;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

public class BlockStateCodecSpawner implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.SPAWNER };
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        if (state instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner) state;
            YamlConfiguration conf = new YamlConfiguration();
            conf.set("delay", spawner.getDelay());
            conf.set("maxNearbyEntities", spawner.getMaxNearbyEntities());
            conf.set("maxSpawnDelay", spawner.getMaxSpawnDelay());
            conf.set("minSpawnDelay", spawner.getMinSpawnDelay());
            conf.set("requiredPlayerRange", spawner.getRequiredPlayerRange());
            conf.set("spawnCount", spawner.getSpawnCount());
            conf.set("spawnedType", spawner.getSpawnedType().name());
            conf.set("spawnRange", spawner.getSpawnRange());
            return conf;
        }
        return null;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner) state;
            if (conf != null) {
                spawner.setDelay(conf.getInt("delay"));
                spawner.setMaxNearbyEntities(conf.getInt("maxNearbyEntities"));
                spawner.setMaxSpawnDelay(conf.getInt("maxSpawnDelay"));
                spawner.setMinSpawnDelay(conf.getInt("minSpawnDelay"));
                spawner.setRequiredPlayerRange(conf.getInt("requiredPlayerRange"));
                spawner.setSpawnCount(conf.getInt("spawnCount"));
                spawner.setSpawnedType(EntityType.valueOf(conf.getString("spawnedType")));
                spawner.setSpawnRange(conf.getInt("spawnRange"));
            }
        }
    }

    @Override
    public String toString(YamlConfiguration conf, YamlConfiguration oldState) {
        if (conf != null) {
            EntityType entity = EntityType.valueOf(conf.getString("spawnedType"));
            if (entity != null) {
                return "[" + entity + "]";
            }
        }
        return null;
    }
}
