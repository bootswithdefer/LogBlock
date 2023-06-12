package de.diddiz.LogBlock.blockstate;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lectern;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class BlockStateCodecLectern implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.LECTERN };
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        if (state instanceof Lectern) {
            Lectern lectern = (Lectern) state;
            ItemStack book = lectern.getSnapshotInventory().getItem(0);
            if (book != null && book.getType() != Material.AIR) {
                YamlConfiguration conf = new YamlConfiguration();
                conf.set("book", book);
                return conf;
            }
        }
        return null;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof Lectern) {
            Lectern lectern = (Lectern) state;
            ItemStack book = null;
            if (conf != null) {
                book = conf.getItemStack("book");
            }
            try {
                lectern.getSnapshotInventory().setItem(0, book);
            } catch (NullPointerException e) {
                //ignored
            }
        }
    }

    @Override
    public BaseComponent getChangesAsComponent(YamlConfiguration conf, YamlConfiguration oldState) {
        if (conf != null) {
            return new TextComponent("[book]");
        }
        return new TextComponent("empty");
    }
}
