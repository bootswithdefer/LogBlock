package de.diddiz.util;

import static de.diddiz.util.ActionColor.CREATE;
import static de.diddiz.util.ActionColor.DESTROY;
import static de.diddiz.util.TypeColor.DEFAULT;
import static de.diddiz.util.Utils.spaces;

import de.diddiz.LogBlock.config.Config;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;

public class MessagingUtil {
    public static BaseComponent[] formatSummarizedChanges(int created, int destroyed, BaseComponent actor, int createdWidth, int destroyedWidth, float spaceFactor) {
        TextComponent textCreated = createTextComponentWithColor(created + spaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)), CREATE.getColor());
        TextComponent textDestroyed = createTextComponentWithColor(destroyed + spaces((int) ((10 - String.valueOf(destroyed).length()) / spaceFactor)), DESTROY.getColor());
        return new BaseComponent[] { textCreated, textDestroyed, actor };
    }

    public static TextComponent createTextComponentWithColor(String text, ChatColor color) {
        TextComponent tc = new TextComponent(text);
        tc.setColor(color);
        return tc;
    }

    public static TextComponent brackets(BracketType type, BaseComponent... content) {
        TextComponent tc = createTextComponentWithColor(type.getStarting(), TypeColor.BRACKETS.getColor());
        for (BaseComponent c : content) {
            tc.addExtra(c);
        }
        tc.addExtra(new TextComponent(type.getEnding()));
        return tc;
    }

    public static TextComponent prettyDate(long date) {
        TextComponent tc = brackets(BracketType.STANDARD, createTextComponentWithColor(Config.formatterShort.format(date), TypeColor.DATE.getColor()));
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Config.formatter.format(date)).create()));
        return tc;
    }

    public static TextComponent prettyState(String stateName) {
        return createTextComponentWithColor(stateName, TypeColor.STATE.getColor());
    }

    public static TextComponent prettyState(int stateValue) {
        return prettyState(Integer.toString(stateValue));
    }

    public static <E extends Enum<E>> TextComponent prettyState(E enumerator) {
        return prettyState(enumerator.toString());
    }

    public static TextComponent prettyMaterial(String materialName) {
        return createTextComponentWithColor(materialName.toUpperCase(), TypeColor.MATERIAL.getColor());
    }

    public static TextComponent prettyMaterial(Material material) {
        return prettyMaterial(material.name());
    }

    public static TextComponent prettyMaterial(BlockData material) {
        TextComponent tc = prettyMaterial(material.getMaterial());
        String bdString = material.getAsString();
        int bracket = bdString.indexOf("[");
        if (bracket >= 0) {
            int bracket2 = bdString.indexOf("]", bracket);
            if (bracket2 >= 0) {
                String state = bdString.substring(bracket + 1, bracket2).replace(',', '\n');
                tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(state).create()));
            }
        }
        return tc;
    }

    public static TextComponent prettyEntityType(EntityType type) {
        return prettyMaterial(type.name());
    }

    public static TextComponent prettyLocation(Location loc, int entryId) {
        return prettyLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), entryId);
    }

    public static TextComponent prettyLocation(int x, int y, int z, int entryId) {
        TextComponent tc = createTextComponentWithColor("", DEFAULT.getColor());
        tc.addExtra(createTextComponentWithColor(Integer.toString(x), TypeColor.COORDINATE.getColor()));
        tc.addExtra(createTextComponentWithColor(", ", DEFAULT.getColor()));
        tc.addExtra(createTextComponentWithColor(Integer.toString(y), TypeColor.COORDINATE.getColor()));
        tc.addExtra(createTextComponentWithColor(", ", DEFAULT.getColor()));
        tc.addExtra(createTextComponentWithColor(Integer.toString(z), TypeColor.COORDINATE.getColor()));
        if (entryId > 0) {
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lb tp " + entryId));
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Teleport here").create()));
        }
        return tc;
    }

    public enum BracketType {
        STANDARD("[", "]"),
        ANGLE("<", ">");

        private String starting, ending;

        BracketType(String starting, String ending) {
            this.starting = starting;
            this.ending = ending;
        }

        public String getStarting() {
            return starting;
        }

        public String getEnding() {
            return ending;
        }
    }
}
