package de.diddiz.LogBlock.blockstate;

import de.diddiz.LogBlock.util.BukkitUtils;
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
        return BukkitUtils.getAllSignsArray();
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
                if (sign.isGlowingText()) {
                    conf.set("glowing", true);
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
            boolean glowing = false;
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
                glowing = conf.getBoolean("glowing", false);
            }
            for (int i = 0; i < 4; i++) {
                String line = lines.size() > i && lines.get(i) != null ? lines.get(i) : "";
                sign.setLine(i, line);
            }
            sign.setColor(signColor);
            sign.setGlowingText(glowing);
        }
    }

    @Override
    public String toString(YamlConfiguration state, YamlConfiguration oldState) {
        if (state != null) {
            List<String> lines = state.getStringList("lines");
            List<String> oldLines = Collections.emptyList();
            DyeColor signColor = DyeColor.BLACK;
            if (state.contains("color")) {
                try {
                    signColor = DyeColor.valueOf(state.getString("color"));
                } catch (IllegalArgumentException | NullPointerException e) {
                    // ignored
                }
            }
            DyeColor oldSignColor = DyeColor.BLACK;
            boolean glowing = state.getBoolean("glowing", false);
            boolean oldGlowing = false;
            if (oldState != null) {
                oldLines = oldState.getStringList("lines");
                if (oldState.contains("color")) {
                    try {
                        oldSignColor = DyeColor.valueOf(oldState.getString("color"));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        // ignored
                    }
                }
                oldGlowing = oldState.getBoolean("glowing", false);
            }

            StringBuilder sb = new StringBuilder();
            if (!lines.equals(oldLines)) {
                for (String line : lines) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append("[").append(line).append("]");
                }
            }
            if (signColor != oldSignColor) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("(color: " + signColor.name().toLowerCase() + ")");
            }
            if (glowing != oldGlowing) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                if (glowing) {
                    sb.append("(glowing)");
                } else {
                    sb.append("(not glowing)");
                }
            }
            return sb.toString();
        }
        return null;
    }
}
