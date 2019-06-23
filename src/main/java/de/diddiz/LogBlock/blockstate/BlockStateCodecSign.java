package de.diddiz.LogBlock.blockstate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;

public class BlockStateCodecSign implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.ACACIA_SIGN, Material.ACACIA_WALL_SIGN, Material.BIRCH_SIGN, Material.BIRCH_WALL_SIGN, Material.DARK_OAK_SIGN, Material.DARK_OAK_WALL_SIGN, Material.JUNGLE_SIGN, Material.JUNGLE_WALL_SIGN, Material.OAK_SIGN, Material.OAK_WALL_SIGN, Material.SPRUCE_SIGN, Material.SPRUCE_WALL_SIGN };
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
            DyeColor signColor = sign.getColor();
            if (signColor == null) {
                signColor = DyeColor.BLACK;
            }
            if (hasText || signColor != DyeColor.BLACK) {
                YamlConfiguration conf = new YamlConfiguration();
                if (hasText) {
                    conf.set("lines", Arrays.asList(lines));
                }
                if (signColor != DyeColor.BLACK) {
                    conf.set("color", signColor.name());
                }
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
            DyeColor signColor = DyeColor.BLACK;
            List<String> lines = Collections.emptyList();
            if (conf != null) {
                if (conf.contains("lines")) {
                    lines = conf.getStringList("lines");
                }
                if (conf.contains("color")) {
                    try {
                        signColor = DyeColor.valueOf(conf.getString("color"));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        // ignored
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                String line = lines.size() > i && lines.get(i) != null ? lines.get(i) : "";
                sign.setLine(i, line);
            }
            sign.setColor(signColor);
        }
    }

    @Override
    public String toString(YamlConfiguration conf) {
        if (conf != null) {
            StringBuilder sb = new StringBuilder();
            for (String line : conf.getStringList("lines")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("[").append(line).append("]");
            }
            return sb.toString();
        }
        return null;
    }
}
