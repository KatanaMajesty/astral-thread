package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class RubyOre extends AstralItem {

    public RubyOre(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        setNote(new Note(1));
        setPlaceable(true);
        setPlaceSound("block.stone.place");
        setItemName("Рубиновая руда");
        setCustomModelDataID(9501);
        setNmsName("ruby_ore");
        setLore(null);
    }
}