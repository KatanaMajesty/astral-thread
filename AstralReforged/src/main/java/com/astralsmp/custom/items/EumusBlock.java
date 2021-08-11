package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class EumusBlock extends AstralItem {
    public EumusBlock(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        setNote(new Note(2));
        setPlaceable(true);
        setNmsName("eumus");
        setItemName("Почва Края");
        setPlaceSound("block.dripstone_block.place");
        setLore(null);
        setCustomModelDataID(9502);
    }
}
