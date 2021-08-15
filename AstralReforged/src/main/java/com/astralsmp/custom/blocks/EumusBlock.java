package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import org.bukkit.*;

public class EumusBlock extends AstralBlock {
    public EumusBlock() {
        super();
    }

    @Override
    public void init() {
        setInstrument(Instrument.BELL);
        setNote(new Note(1));
        setFortunable(false);
        setHardness(1.5);
        setDefDropItem(null);
        setDropItem(new com.astralsmp.custom.items.EumusBlock(plugin));
        setDropCount(1);
        setMaterial(Material.STONE_SHOVEL);
        setBreakSound("block.dripstone_block.break");
        setWalkSound("block.dripstone_block.step");
        setFallSound("block.dripstone_block.hit");
        setHitSound("block.dripstone_block.hit");
    }
}
