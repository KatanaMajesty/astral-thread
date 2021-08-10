package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.plugin.Plugin;

public class RubyOre extends AstralItem {

    public RubyOre(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setItemName("Рубиновая руда");
        setCustomModelDataID(9501);
        setNmsName("ruby_ore");
        setPlaceable(true);
        setLore(null);
        super.init();
    }
}
