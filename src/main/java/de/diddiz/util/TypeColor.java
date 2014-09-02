package de.diddiz.util;

import org.bukkit.ChatColor;

public enum TypeColor {
    DEFAULT(ChatColor.YELLOW),
    MATERIAL(ChatColor.BLUE),
    STATE(ChatColor.BLUE),
    DATE(ChatColor.DARK_AQUA),
    BRACKETS(ChatColor.DARK_GRAY),
    COORDINATE(ChatColor.WHITE),
    HEADER(ChatColor.GOLD),
    ERROR(ChatColor.RED);

    private final ChatColor color;

    TypeColor(ChatColor color) {
        this.color = color;
    }

    public ChatColor getColor() {
        return color;
    }

    @Override
    public String toString() {
        return color.toString();
    }
}
