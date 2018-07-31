package de.diddiz.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public class RegionContainer implements Cloneable {

    private World world;
    private BlockVector min = new BlockVector();
    private BlockVector max = new BlockVector();

    public RegionContainer(World world, Vector first, Vector second) {
        this.world = world;
        this.min.setX(Math.min(first.getBlockX(),second.getBlockX()));
        this.min.setY(Math.min(first.getBlockY(),second.getBlockY()));
        this.min.setZ(Math.min(first.getBlockZ(),second.getBlockZ()));
        this.max.setX(Math.max(first.getBlockX(),second.getBlockX()));
        this.max.setY(Math.max(first.getBlockY(),second.getBlockY()));
        this.max.setZ(Math.max(first.getBlockZ(),second.getBlockZ()));
    }

    public static RegionContainer fromPlayerSelection(Player player, Plugin worldEditPlugin) {
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
        if (!(selection instanceof CuboidRegion)) {
            throw new IllegalArgumentException("You have to define a cuboid selection");
        }
        com.sk89q.worldedit.Vector weMin = selection.getMinimumPoint();
        com.sk89q.worldedit.Vector weMax = selection.getMaximumPoint();
        Vector min = new Vector(weMin.getBlockX(), weMin.getBlockY(), weMin.getBlockZ());
        Vector max = new Vector(weMax.getBlockX(), weMax.getBlockY(), weMax.getBlockZ());
        return new RegionContainer(world, min, max);
    }

    public static RegionContainer fromCorners(World world, Location first, Location second) {
        return new RegionContainer(world, first.toVector(), second.toVector());
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
    public RegionContainer clone() {
        try {
            RegionContainer clone = (RegionContainer) super.clone();
            clone.min = min.clone();
            clone.max = max.clone();
            return clone;
        } catch (final CloneNotSupportedException ex) {
            throw new Error("RegionContainer should be cloneable", ex);
        }
    }
}
