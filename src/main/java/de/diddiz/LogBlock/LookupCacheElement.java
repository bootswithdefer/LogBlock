package de.diddiz.LogBlock;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;

public interface LookupCacheElement {
    public Location getLocation();

    public BaseComponent[] getLogMessage();
}
