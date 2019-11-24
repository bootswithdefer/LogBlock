package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class ScaffoldingLogging extends LoggingListener {
    private final static long MAX_SCAFFOLDING_LOG_TIME_MS = 2000;
    private final static EnumSet<BlockFace> NEIGHBOURS_SIDES_AND_UP = EnumSet.of(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private final static EnumSet<BlockFace> NEIGHBOURS_SIDES_AND_BELOW = EnumSet.of(BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    private final ArrayDeque<ScaffoldingBreaker> scaffoldingBreakersList = new ArrayDeque<>();
    private final HashMap<Location, ScaffoldingBreaker> scaffoldingBreakersByLocation = new HashMap<>();
    private final HashMap<Location, Player> scaffoldingPlacersByLocation = new HashMap<>();

    public ScaffoldingLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        if (isLogging(block.getWorld(), Logging.SCAFFOLDING)) {
            final Material type = block.getType();
            if (type == Material.SCAFFOLDING) {
                Player placer = scaffoldingPlacersByLocation.get(block.getLocation());
                cleanupScaffoldingBreakers();
                if (placer != null) {
                    consumer.queueBlockReplace(Actor.actorFromEntity(placer), block.getState(), event.getNewState());
                    return;
                }
                Player breaker = getScaffoldingBreaker(block);
                if (breaker != null) {
                    for (BlockFace dir : NEIGHBOURS_SIDES_AND_UP) {
                        Block otherBlock = block.getRelative(dir);
                        if (otherBlock.getType() == Material.SCAFFOLDING) {
                            addScaffoldingBreaker(breaker, otherBlock);
                        }
                    }
                }
                consumer.queueBlockReplace(breaker == null ? new Actor("ScaffoldingFall") : Actor.actorFromEntity(breaker), block.getState(), event.getNewState());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isLogging(block.getWorld(), Logging.SCAFFOLDING)) {
            cleanupScaffoldingBreakers();
            Block otherBlock;
            if (block.getType() == Material.SCAFFOLDING) {
                for (BlockFace dir : NEIGHBOURS_SIDES_AND_UP) {
                    otherBlock = block.getRelative(dir);
                    if (otherBlock.getType() == Material.SCAFFOLDING) {
                        addScaffoldingBreaker(event.getPlayer(), otherBlock);
                    }
                }
            } else if ((otherBlock = block.getRelative(BlockFace.UP)).getType() == Material.SCAFFOLDING) {
                addScaffoldingBreaker(event.getPlayer(), otherBlock);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (isLogging(block.getWorld(), Logging.SCAFFOLDING)) {
            cleanupScaffoldingBreakers();
            if (block.getType() == Material.SCAFFOLDING) {
                scaffoldingPlacersByLocation.put(block.getLocation(), event.getPlayer());
            }
        }
    }

    private void addScaffoldingBreaker(Player player, Block block) {
        ScaffoldingBreaker breaker = new ScaffoldingBreaker(player, block.getLocation());
        scaffoldingBreakersList.addLast(breaker);
        scaffoldingBreakersByLocation.put(breaker.getLocation(), breaker);

    }

    private void cleanupScaffoldingBreakers() {
        if (!scaffoldingPlacersByLocation.isEmpty()) {
            scaffoldingPlacersByLocation.clear();
        }
        if (!scaffoldingBreakersList.isEmpty()) {
            long time = System.currentTimeMillis() - MAX_SCAFFOLDING_LOG_TIME_MS;
            while (!scaffoldingBreakersList.isEmpty() && scaffoldingBreakersList.getFirst().getTime() < time) {
                ScaffoldingBreaker breaker = scaffoldingBreakersList.removeFirst();
                scaffoldingBreakersByLocation.remove(breaker.getLocation(), breaker);
            }
        }
    }

    private Player getScaffoldingBreaker(Block block) {
        if (scaffoldingBreakersList.isEmpty()) {
            return null;
        }

        ScaffoldingBreaker breaker = scaffoldingBreakersByLocation.get(block.getLocation());
        if (breaker != null) {
            return breaker.getBreaker();
        }

        // Search all connected scaffoldings
        ArrayDeque<Block> front = new ArrayDeque<>();
        HashSet<Block> frontAndDone = new HashSet<>();
        front.addLast(block);
        frontAndDone.add(block);
        while (!front.isEmpty()) {
            Block current = front.removeFirst();
            Location loc = current.getLocation();

            breaker = scaffoldingBreakersByLocation.get(loc);
            if (breaker != null) {
                return breaker.getBreaker();
            }

            for (BlockFace dir : NEIGHBOURS_SIDES_AND_BELOW) {
                Block otherBlock = current.getRelative(dir);
                if (!frontAndDone.contains(otherBlock) && otherBlock.getType() == Material.SCAFFOLDING) {
                    front.addLast(otherBlock);
                    frontAndDone.add(otherBlock);
                }
            }
        }
        return null;
    }

    class ScaffoldingBreaker {
        protected final Player breaker;
        protected final long time;
        protected final Location location;

        public ScaffoldingBreaker(Player breaker, Location location) {
            this.breaker = breaker;
            this.location = location;
            this.time = System.currentTimeMillis();
        }

        public Player getBreaker() {
            return breaker;
        }

        public Location getLocation() {
            return location;
        }

        public long getTime() {
            return time;
        }
    }
}
