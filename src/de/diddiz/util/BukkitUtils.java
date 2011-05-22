package de.diddiz.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BukkitUtils
{
	private static class ItemStackComparator implements Comparator<ItemStack>
	{
		@Override
		public int compare(ItemStack a, ItemStack b) {
			final int aType = a.getTypeId(), bType = b.getTypeId();
			if (aType < bType)
				return -1;
			if (aType > bType)
				return 1;
			final byte aData = rawData(a), bData = rawData(b);
			if (aData < bData)
				return -1;
			if (aData > bData)
				return 1;
			return 0;
		}
	}

	private final static Set<Set<Integer>> blockEquivalents;

	static {
		blockEquivalents = new HashSet<Set<Integer>>(7);
		blockEquivalents.add(new HashSet<Integer>(Arrays.asList(2, 3, 60)));
		blockEquivalents.add(new HashSet<Integer>(Arrays.asList(8, 9, 79)));
		blockEquivalents.add(new HashSet<Integer>(Arrays.asList(10, 11)));
		blockEquivalents.add(new HashSet<Integer>(Arrays.asList(61, 62)));
		blockEquivalents.add(new HashSet<Integer>(Arrays.asList(73, 74)));
		blockEquivalents.add(new HashSet<Integer>(Arrays.asList(75, 76)));
		blockEquivalents.add(new HashSet<Integer>(Arrays.asList(93, 94)));
	}

	public static ItemStack[] compareInventories(ItemStack[] items1, ItemStack[] items2) {
		final ItemStackComparator comperator = new ItemStackComparator();
		final ArrayList<ItemStack> diff = new ArrayList<ItemStack>();
		int c1 = 0, c2 = 0;
		while (c1 < items1.length || c2 < items2.length) {
			if (c1 >= items1.length) {
				diff.add(items2[c2]);
				c2++;
				continue;
			}
			if (c2 >= items2.length) {
				items1[c1].setAmount(items1[c1].getAmount() * -1);
				diff.add(items1[c1]);
				c1++;
				continue;
			}
			final int comp = comperator.compare(items1[c1], items2[c2]);
			System.out.print("Comparing: " + items1[c1] + " & " + items2[c2] + " = " + comp);
			if (comp < 0) {
				items1[c1].setAmount(items1[c1].getAmount() * -1);
				diff.add(items1[c1]);
				c1++;
			} else if (comp > 0) {
				diff.add(items2[c2]);
				c2++;
			} else {
				final int amount = items1[c1].getAmount() - items2[c2].getAmount();
				if (amount != 0) {
					items1[c1].setAmount(amount);
					diff.add(items1[c1]);
				}
				c1++;
				c2++;
			}
		}
		return diff.toArray(new ItemStack[diff.size()]);
	}

	public static ItemStack[] compressInventory(ItemStack[] items) {
		final ArrayList<ItemStack> compressed = new ArrayList<ItemStack>();
		for (final ItemStack item : items)
			if (item != null) {
				final int type = item.getTypeId();
				final byte data = rawData(item);
				boolean found = false;
				for (final ItemStack item2 : compressed)
					if (type == item2.getTypeId() && data == rawData(item2)) {
						item2.setAmount(item2.getAmount() + item.getAmount());
						found = true;
						break;
					}
				if (!found)
					compressed.add(new ItemStack(type, item.getAmount(), (short)0, data));
			}
		Collections.sort(compressed, new ItemStackComparator());
		return compressed.toArray(new ItemStack[compressed.size()]);
	}

	public static boolean equalTypes(int type1, int type2) {
		if (type1 == type2)
			return true;
		for (final Set<Integer> equivalent : blockEquivalents)
			if (equivalent.contains(type1) && equivalent.contains(type2))
				return true;
		return false;
	}

	public static Set<Set<Integer>> getBlockEquivalents() {
		return blockEquivalents;
	}

	public static String getEntityName(Entity entity) {
		if (entity instanceof Player)
			return ((Player)entity).getName();
		if (entity instanceof TNTPrimed)
			return "TNT";
		return entity.getClass().getSimpleName().substring(5);
	}

	public static String getMaterialName(int type) {
		return Material.getMaterial(type).toString().replace('_', ' ').toLowerCase();
	}

	public static String getSenderName(CommandSender sender) {
		if (sender instanceof Player)
			return ((Player)sender).getName();
		if (sender instanceof ConsoleCommandSender)
			return "console";
		return sender.getClass().getSimpleName();
	}

	public static void giveTool(Player player, int type) {
		final Inventory inv = player.getInventory();
		if (inv.contains(type))
			player.sendMessage(ChatColor.RED + "You have alredy a " + getMaterialName(type));
		else {
			final int free = inv.firstEmpty();
			if (free >= 0) {
				inv.setItem(free, player.getItemInHand());
				player.setItemInHand(new ItemStack(type, 1));
				player.sendMessage(ChatColor.GREEN + "Here's your " + getMaterialName(type));
			} else
				player.sendMessage(ChatColor.RED + "You have no empty slot in your inventory");
		}
	}

	public static byte rawData(ItemStack item) {
		if (item.getData() == null)
			return 0;
		return item.getData().getData();
	}
}
