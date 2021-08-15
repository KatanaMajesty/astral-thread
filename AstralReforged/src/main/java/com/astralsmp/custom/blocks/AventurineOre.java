package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import com.astralsmp.custom.items.Aventurine;
import org.bukkit.*;

public class AventurineOre extends AstralBlock {

    public AventurineOre() {
        super();
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        setNote(new Note(1));
        setMaterial(Material.IRON_PICKAXE);
        setHardness(3);
        setFortunable(true);
        setSilkTouchable(true);
        setSilkDropItem(new com.astralsmp.custom.items.AventurineOre(plugin));
        setSilkDropCount(1);
        setDropItem(new Aventurine(plugin));
        setDropCount(5);
        setDefDropItem(null);
        setDefDropCount(0);
        setBreakSound("block.stone.break");
        setHitSound("block.stone.hit");
        setWalkSound("block.stone.step");
        setFallSound("block.stone.fall");
    }
}
