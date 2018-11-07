package de.diddiz.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;

public class CuboidRegion implements Cloneable {

    private World world;
    private BlockVector min = new BlockVector();
    private BlockVector max = new BlockVector();

    public CuboidRegion(World world, BlockVector first, BlockVector second) {
        this.world = world;
        this.min.setX(Math.min(first.getBlockX(),second.getBlockX()));
        this.min.setY(Math.min(first.getBlockY(),second.getBlockY()));
        this.min.setZ(Math.min(first.getBlockZ(),second.getBlockZ()));
        this.max.setX(Math.max(first.getBlockX(),second.getBlockX()));
        this.max.setY(Math.max(first.getBlockY(),second.getBlockY()));
        this.max.setZ(Math.max(first.getBlockZ(),second.getBlockZ()));
    }

    public static CuboidRegion fromPlayerSelection(Player player, Plugin worldEditPlugin) {
        LocalSession session = ((WorldEditPlugin) worldEditPlugin).getSession(player);
        World world = player.getWorld();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        if (!weWorld.equals(session.getSelectionWorld())) {
            throw new IllegalArgumentException("No selection defined");
        }
        Region selection;
        try {
            selection = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            throw new IllegalArgumentException("No selection defined");
        }
        if (selection == null) {
            throw new IllegalArgumentException("No selection defined");
        }
        if (!(selection instanceof com.sk89q.worldedit.regions.CuboidRegion)) {
            throw new IllegalArgumentException("You have to define a cuboid selection");
        }
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();
        return new CuboidRegion(world, new BlockVector(min.getBlockX(), min.getBlockY(), min.getBlockZ()), new BlockVector(max.getBlockX(), max.getBlockY(), max.getBlockZ()));
    }

    public static CuboidRegion fromCorners(World world, Location first, Location second) {
        return new CuboidRegion(world, new BlockVector(first.getBlockX(), first.getBlockY(), first.getBlockZ()), new BlockVector(second.getBlockX(), second.getBlockY(), second.getBlockZ()));
    }

    public World getWorld() {
        return world;
    }

    public BlockVector getMinimumPoint() {
        return min;
    }

    public BlockVector getMaximumPoint() {
        return max;
    }

    public int getSizeX() {
        return max.getBlockX() - min.getBlockX() + 1;
    }

    public int getSizeZ() {
        return max.getBlockZ() - min.getBlockZ() + 1;
    }

    @Override
    public CuboidRegion clone() {
        try {
            CuboidRegion clone = (CuboidRegion) super.clone();
            clone.min = min.clone();
            clone.max = max.clone();
            return clone;
        } catch (final CloneNotSupportedException ex) {
            throw new Error("CuboidRegion should be cloneable", ex);
        }
    }
}
