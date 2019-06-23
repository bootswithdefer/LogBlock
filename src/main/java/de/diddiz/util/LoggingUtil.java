package de.diddiz.util;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bell;
import org.bukkit.block.data.type.Bell.Attachment;
import org.bukkit.block.data.type.Lantern;
import java.util.List;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.mb4;

public class LoggingUtil {

    public static void smartLogBlockPlace(Consumer consumer, Actor actor, BlockState replaced, BlockState placed) {
        Location loc = replaced.getLocation();
        if (!placed.getType().hasGravity() || !BukkitUtils.canDirectlyFallIn(replaced.getBlock().getRelative(BlockFace.DOWN).getType())) {
            if (BukkitUtils.isEmpty(replaced.getType())) {
                consumer.queueBlockPlace(actor, placed);
            } else {
                consumer.queueBlockReplace(actor, replaced, placed);
            }
            return;
        }
        int x = loc.getBlockX();
        int initialy = loc.getBlockY();
        int y = initialy;
        int z = loc.getBlockZ();
        while (y > 0 && BukkitUtils.canFallIn(loc.getWorld(), x, (y - 1), z)) {
            y--;
        }
        if (initialy != y && !BukkitUtils.isEmpty(replaced.getType())) {
            // this is not the final location but the block got removed (vines etc)
            consumer.queueBlockBreak(actor, replaced);
        }
        // If y is 0 then the block fell out of the world :(
        if (y != 0) {
            // Run this check to avoid false positives
            Location finalLoc = new Location(loc.getWorld(), x, y, z);
            if (y == initialy || !BukkitUtils.getFallingEntityKillers().contains(finalLoc.getBlock().getType())) {
                if (BukkitUtils.isEmpty(finalLoc.getBlock().getType())) {
                    consumer.queueBlockPlace(actor, finalLoc, placed.getBlockData());
                } else {
                    consumer.queueBlockReplace(actor, finalLoc.getBlock().getState(), placed.getBlockData());
                }
            }
        }
    }

    public static void smartLogFallables(Consumer consumer, Actor actor, Block origin) {

        WorldConfig wcfg = getWorldConfig(origin.getWorld());
        if (wcfg == null) {
            return;
        }

        //Handle falling blocks
        Block checkBlock = origin.getRelative(BlockFace.UP);
        int up = 0;
        final int highestBlock = checkBlock.getWorld().getHighestBlockYAt(checkBlock.getLocation());
        while (checkBlock.getType().hasGravity()) {

            // Record this block as falling
            consumer.queueBlockBreak(actor, checkBlock.getState());

            // Guess where the block is going (This could be thrown of by explosions, but it is better than nothing)
            Location loc = origin.getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            while (y > 0 && BukkitUtils.canFallIn(loc.getWorld(), x, (y - 1), z)) {
                y--;
            }
            // If y is 0 then the sand block fell out of the world :(
            if (y != 0) {
                Location finalLoc = new Location(loc.getWorld(), x, y, z);
                // Run this check to avoid false positives
                if (!BukkitUtils.getFallingEntityKillers().contains(finalLoc.getBlock().getType())) {
                    finalLoc.add(0, up, 0); // Add this here after checking for block breakers
                    if (BukkitUtils.isEmpty(finalLoc.getBlock().getType())) {
                        consumer.queueBlockPlace(actor, finalLoc, checkBlock.getBlockData());
                    } else {
                        consumer.queueBlockReplace(actor, finalLoc, finalLoc.getBlock().getBlockData(), checkBlock.getBlockData());
                    }
                    up++;
                }
            }
            if (checkBlock.getY() >= highestBlock) {
                break;
            }
            checkBlock = checkBlock.getRelative(BlockFace.UP);
        }
    }

    public static void smartLogBlockBreak(Consumer consumer, Actor actor, Block origin) {
        smartLogBlockReplace(consumer, actor, origin, null);
    }

    public static void smartLogBlockReplace(Consumer consumer, Actor actor, Block origin, BlockData replacedWith) {

        WorldConfig wcfg = getWorldConfig(origin.getWorld());
        if (wcfg == null) {
            return;
        }

        Block checkBlock = origin.getRelative(BlockFace.UP);
        Material typeAbove = checkBlock.getType();
        if (BukkitUtils.getRelativeTopBreakabls().contains(typeAbove)) {
            if (typeAbove == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(typeAbove)) {
                Block doorBlock = checkBlock;
                // If the doorBlock is the top half a door the player simply punched a door
                // this will be handled later.
                if (!BukkitUtils.isTop(doorBlock.getBlockData())) {
                    doorBlock = doorBlock.getRelative(BlockFace.UP);
                    // Fall back check just in case the top half wasn't a door
                    if (doorBlock.getType() == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(doorBlock.getType())) {
                        consumer.queueBlockBreak(actor, doorBlock.getState());
                    }
                    consumer.queueBlockBreak(actor, checkBlock.getState());
                }
            } else if (BukkitUtils.isDoublePlant(typeAbove)) {
                Block plantBlock = checkBlock;
                // If the plantBlock is the top half of a double plant the player simply
                // punched the plant this will be handled later.
                if (!BukkitUtils.isTop(plantBlock.getBlockData())) {
                    plantBlock = plantBlock.getRelative(BlockFace.UP);
                    // Fall back check just in case the top half wasn't a plant
                    if (BukkitUtils.isDoublePlant(plantBlock.getType())) {
                        consumer.queueBlockBreak(actor, plantBlock.getState());
                    }
                    consumer.queueBlockBreak(actor, checkBlock.getState());
                }
            } else {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        } else if (typeAbove == Material.LANTERN) {
            Lantern lantern = (Lantern) checkBlock.getBlockData();
            if (!lantern.isHanging()) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        } else if (typeAbove == Material.BELL) {
            Bell bell = (Bell) checkBlock.getBlockData();
            if (bell.getAttachment() == Attachment.FLOOR) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        }

        checkBlock = origin.getRelative(BlockFace.DOWN);
        Material typeBelow = checkBlock.getType();
        if (typeBelow == Material.LANTERN) {
            Lantern lantern = (Lantern) checkBlock.getBlockData();
            if (lantern.isHanging()) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        } else if (typeBelow == Material.BELL) {
            Bell bell = (Bell) checkBlock.getBlockData();
            if (bell.getAttachment() == Attachment.CEILING) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        }

        List<Location> relativeBreakables = BukkitUtils.getBlocksNearby(origin, BukkitUtils.getRelativeBreakables());
        if (relativeBreakables.size() != 0) {
            for (Location location : relativeBreakables) {
                Block block = location.getBlock();
                BlockData blockData = block.getBlockData();
                if (blockData instanceof Directional) {
                    if (blockData.getMaterial() == Material.BELL) {
                        if (((Bell) blockData).getAttachment() == Attachment.SINGLE_WALL) {
                            if (block.getRelative(((Bell) blockData).getFacing()).equals(origin)) {
                                consumer.queueBlockBreak(actor, block.getState());
                            }
                        }
                    } else {
                        if (block.getRelative(((Directional) blockData).getFacing().getOppositeFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, block.getState());
                        }
                    }
                }
            }
        }

        // Special door check
        if (origin.getType() == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(origin.getType())) {
            Block doorBlock = origin;

            // Up or down?
            if (!BukkitUtils.isTop(doorBlock.getBlockData())) {
                doorBlock = doorBlock.getRelative(BlockFace.UP);
            } else {
                doorBlock = doorBlock.getRelative(BlockFace.DOWN);
            }

            if (doorBlock.getType() == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(doorBlock.getType())) {
                consumer.queueBlockBreak(actor, doorBlock.getState());
            }
        } else if (BukkitUtils.isDoublePlant(origin.getType())) { // Special double plant check
            Block plantBlock = origin;

            // Up or down?
            if (!BukkitUtils.isTop(origin.getBlockData())) {
                plantBlock = plantBlock.getRelative(BlockFace.UP);
            } else {
                plantBlock = plantBlock.getRelative(BlockFace.DOWN);
            }

            if (BukkitUtils.isDoublePlant(plantBlock.getType())) {
                consumer.queueBlockBreak(actor, plantBlock.getState());
            }
        }

        // Do this down here so that the block is added after blocks sitting on it
        if (replacedWith == null) {
            consumer.queueBlockBreak(actor, origin.getState());
        } else {
            consumer.queueBlockReplace(actor, origin.getState(), replacedWith);
        }
    }

    public static String checkText(String text) {
        if (text == null) {
            return text;
        }
        if (mb4) {
            return text;
        }
        return text.replaceAll("[^\\u0000-\\uFFFF]", "?");
    }
}
