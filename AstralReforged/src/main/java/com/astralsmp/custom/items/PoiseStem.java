package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class PoiseStem extends AstralItem {
    public PoiseStem(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setNote(new Note(2));
        setInstrument(Instrument.BELL);
        setCustomModelDataID(9621);
        setItemName("Корень Края");
        setNmsName("poise_stem");
        setPlaceable(true);
        setPlaceSound("custom.block.wood.place");
    }
}
