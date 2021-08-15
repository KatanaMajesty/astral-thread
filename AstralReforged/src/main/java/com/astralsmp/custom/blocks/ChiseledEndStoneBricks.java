package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;

public class ChiseledEndStoneBricks extends AstralBlock {

//    public ChiseledEndStoneBricks(Plugin plugin) {
//        super();
//    }

    @Override
    public void init() {
        setInstrument(Instrument.BELL);
        setNote(new Note(3));
        setMaterial(Material.WOODEN_PICKAXE);
        setHardness(3);
        setDropItem(new com.astralsmp.custom.items.ChiseledEndStoneBricks(plugin));
        setDropCount(1);
        setDefDropItem(null);
        setBreakSound("block.stone.break");
        setHitSound("block.stone.hit");
        setWalkSound("block.stone.step");
        setFallSound("block.stone.fall");
    }
}
