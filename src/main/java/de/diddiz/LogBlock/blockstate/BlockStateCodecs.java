package de.diddiz.LogBlock.blockstate;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.util.Utils;

public class BlockStateCodecs {
    private static Map<Material, BlockStateCodec> codecs = new EnumMap<>(Material.class);

    public static void registerCodec(BlockStateCodec codec) {
        Material[] materials = codec.getApplicableMaterials();
        for (Material material : materials) {
            if (codecs.containsKey(material)) {
                throw new IllegalArgumentException("BlockStateCodec for " + material + " already registered!");
            }
            codecs.put(material, codec);
        }
    }

    static {
        registerCodec(new BlockStateCodecSign());
        registerCodec(new BlockStateCodecSkull());
        registerCodec(new BlockStateCodecBanner());
        registerCodec(new BlockStateCodecSpawner());
    }

    public static boolean hasCodec(Material material) {
        return codecs.containsKey(material);
    }

    public static byte[] serialize(BlockState state) {
        BlockStateCodec codec = codecs.get(state.getType());
        if (codec != null) {
            YamlConfiguration serialized = codec.serialize(state);
            if (serialized != null && !serialized.getKeys(false).isEmpty()) {
                return Utils.serializeYamlConfiguration(serialized);
            }
        }
        return null;
    }

    public static void deserialize(BlockState block, byte[] state) {
        BlockStateCodec codec = codecs.get(block.getType());
        if (codec != null) {
            YamlConfiguration conf = null;
            try {
                if (state != null) {
                    conf = Utils.deserializeYamlConfiguration(state);
                }
            } catch (InvalidConfigurationException e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Exception while deserializing BlockState", e);
            }
            codec.deserialize(block, conf);
        }
    }

    public static String toString(Material material, byte[] state) {
        BlockStateCodec codec = codecs.get(material);
        if (codec != null) {
            YamlConfiguration conf = null;
            try {
                if (state != null) {
                    conf = Utils.deserializeYamlConfiguration(state);
                }
            } catch (InvalidConfigurationException e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Exception while deserializing BlockState", e);
            }
            return codec.toString(conf);
        }
        return null;
    }
}
