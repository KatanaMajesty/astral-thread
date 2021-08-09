package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.plugin.Plugin;

public class Ruby extends AstralItem {

    public Ruby(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setItemName("Рубин");
        setCustomModelDataID(9500);
        setNmsName("ruby");
        setLore(null);
        super.init();
    }

}
