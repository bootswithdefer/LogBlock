package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.block.data.type.PointedDripstone.Thickness;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockSpreadEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class BlockSpreadLogging extends LoggingListener {

    public BlockSpreadLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {

        String name;

        World world = event.getNewState().getWorld();
        Material type = event.getNewState().getType();

        switch (type) {
            case GRASS:
                if (!isLogging(world, Logging.GRASSGROWTH)) {
                    return;
                }
                name = "GrassGrowth";
                break;
            case MYCELIUM:
                if (!isLogging(world, Logging.MYCELIUMSPREAD)) {
                    return;
                }
                name = "MyceliumSpread";
                break;
            case VINE:
            case CAVE_VINES:
            case CAVE_VINES_PLANT:
            case WEEPING_VINES:
            case WEEPING_VINES_PLANT:
            case TWISTING_VINES:
            case TWISTING_VINES_PLANT:
                if (!isLogging(world, Logging.VINEGROWTH)) {
                    return;
                }
                name = "VineGrowth";
                break;
            case RED_MUSHROOM:
            case BROWN_MUSHROOM:
                if (!isLogging(world, Logging.MUSHROOMSPREAD)) {
                    return;
                }
                name = "MushroomSpread";
                break;
            case BAMBOO:
            case BAMBOO_SAPLING:
                if (!isLogging(world, Logging.BAMBOOGROWTH)) {
                    return;
                }
                name = "BambooGrowth";
                if (type == Material.BAMBOO_SAPLING) {
                    // bamboo sapling gets replaced by bamboo
                    consumer.queueBlockReplace(new Actor(name), event.getSource().getState(), Material.BAMBOO.createBlockData());
                }
                break;
            case POINTED_DRIPSTONE:
                if (!isLogging(world, Logging.DRIPSTONEGROWTH)) {
                    return;
                }
                name = "DripstoneGrowth";
                PointedDripstone pointed = (PointedDripstone) event.getNewState().getBlockData();
                if (pointed.getThickness() != Thickness.TIP_MERGE) {
                    BlockFace direction = pointed.getVerticalDirection();
                    Block previousPart = event.getBlock().getRelative(direction.getOppositeFace());
                    if (previousPart.getType() == Material.POINTED_DRIPSTONE) {
                        PointedDripstone newBelow = (PointedDripstone) previousPart.getBlockData();
                        newBelow.setThickness(Thickness.FRUSTUM);
                        consumer.queueBlockReplace(new Actor(name), previousPart.getState(), newBelow);

                        previousPart = previousPart.getRelative(direction.getOppositeFace());
                        if (previousPart.getType() == Material.POINTED_DRIPSTONE) {
                            Block evenMorePrevious = previousPart.getRelative(direction.getOppositeFace());
                            newBelow = (PointedDripstone) previousPart.getBlockData();
                            newBelow.setThickness(evenMorePrevious.getType() == Material.POINTED_DRIPSTONE ? Thickness.MIDDLE : Thickness.BASE);
                            consumer.queueBlockReplace(new Actor(name), previousPart.getState(), newBelow);
                        }
                    }
                } else {
                    // special case because the old state is already changed (for one half)
                    PointedDripstone oldState = (PointedDripstone) event.getNewState().getBlockData();
                    oldState.setThickness(Thickness.TIP);
                    consumer.queueBlockReplace(new Actor(name), oldState, event.getNewState());
                    return;
                }
                break;
            default:
                return;
        }

        consumer.queueBlockReplace(new Actor(name), event.getBlock().getState(), event.getNewState());
    }
}
