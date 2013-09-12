package de.diddiz.worldedit;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RegionContainer {

	private Selection selection;

	public RegionContainer(Selection sel) {
		this.selection = sel;
	}

	public static RegionContainer fromPlayerSelection(Player player, Plugin plugin) {
		final Selection selection = ((WorldEditPlugin) plugin).getSelection(player);
		if (selection == null) {
			throw new IllegalArgumentException("No selection defined");
		}
		if (!(selection instanceof CuboidSelection)) {
			throw new IllegalArgumentException("You have to define a cuboid selection");
		}
		return new RegionContainer(selection);
	}

	public static RegionContainer fromCorners(World world, Location first, Location second) {
		return new RegionContainer(new CuboidSelection(world, first, second));
	}

	public Selection getSelection() {
		return selection;
	}

	public void setSelection(Selection selection) {
		this.selection = selection;
	}
}
