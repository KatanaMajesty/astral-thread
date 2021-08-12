package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class AspenLog extends AstralItem {
    public AspenLog(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setNote(new Note(3));
        setInstrument(Instrument.BANJO);
        setCustomModelDataID(9503);
        setItemName("Осиновое бревно");
        setNmsName("aspen_wood");
        setPlaceable(true);
        setPlaceSound("custom.block.wood.place");
    }
}
