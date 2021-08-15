package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class AventurineOre extends AstralItem {

    public AventurineOre(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        setNote(new Note(1));
        setPlaceable(true);
        setPlaceSound("block.stone.place");
        setItemName("Авантюриновая руда");
        setCustomModelDataID(9600);
        setNmsName("aventurine_pre");
        setLore(null);
    }
}
