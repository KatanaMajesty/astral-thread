package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.plugin.Plugin;

public class Aventurine extends AstralItem {

    public Aventurine(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setItemName("Авантюриновый кристалл");
        setCustomModelDataID(9500);
        setNmsName("aventurine");
        setPlaceable(false);
        setLore(null);
    }

}
