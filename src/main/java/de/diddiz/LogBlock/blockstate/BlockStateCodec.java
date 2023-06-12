package de.diddiz.LogBlock.blockstate;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;

public interface BlockStateCodec {
    Material[] getApplicableMaterials();

    YamlConfiguration serialize(BlockState state);

    void deserialize(BlockState state, YamlConfiguration conf);

    BaseComponent getChangesAsComponent(YamlConfiguration conf, YamlConfiguration oldState);
}
