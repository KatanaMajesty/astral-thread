package com.astralsmp.custom.items;

import com.astralsmp.custom.AstralItem;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class ChiseledEndStoneBricks extends AstralItem {

    public ChiseledEndStoneBricks(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setInstrument(Instrument.BELL);
        setNote(new Note(3));
        setPlaceable(true);
        setPlaceSound("block.stone.place");
        setItemName("Резные эндерняковые кирпичи");
        setCustomModelDataID(9622);
        setNmsName("chiseled_end_stone_bricks");
        setLore(null);
    }
}
