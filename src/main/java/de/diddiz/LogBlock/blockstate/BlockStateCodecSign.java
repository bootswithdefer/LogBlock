package de.diddiz.LogBlock.blockstate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;

public class BlockStateCodecSign implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.WALL_SIGN, Material.SIGN };
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        if (state instanceof Sign) {
            Sign sign = (Sign) state;
            String[] lines = sign.getLines();
            boolean hasText = false;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null && lines[i].length() > 0) {
                    hasText = true;
                    break;
                }
            }
            if (hasText) {
                YamlConfiguration conf = new YamlConfiguration();
                conf.set("lines", Arrays.asList(lines));
                return conf;
            }
        }
        return null;
    }

    /**
     * This is required for the SignChangeEvent, because we have no BlockState there.
     */
    public static YamlConfiguration serialize(String[] lines) {
        YamlConfiguration conf = new YamlConfiguration();
        conf.set("lines", Arrays.asList(lines));
        return conf;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof Sign) {
            Sign sign = (Sign) state;
            List<String> lines = Collections.emptyList();
            if (conf != null) {
                lines = conf.getStringList("lines");
            }
            for (int i = 0; i < 4; i++) {
                String line = lines.size() > i && lines.get(i) != null ? lines.get(i) : "";
                sign.setLine(i, line);
            }
        }
    }

    @Override
    public String toString(YamlConfiguration conf) {
        if (conf != null) {
            StringBuilder sb = new StringBuilder();
            for (String line : conf.getStringList("lines")) {
                if (sb.length() > 0)
                    sb.append(" ");
                sb.append("[").append(line).append("]");
            }
            return sb.toString();
        }
        return null;
    }
}
