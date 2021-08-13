package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import org.bukkit.*;

public class AspenLog extends AstralBlock {
    public AspenLog() {
        super();
    }

    // TODO: 13.08.2021 не выпадает дроп при ломании рукой (скорее всего из-за setDropItem) 
    @Override
    public void init() {
        com.astralsmp.custom.items.AspenLog aspenLog = new com.astralsmp.custom.items.AspenLog(plugin);
        setDefDropItem(aspenLog);
        setDefDropCount(1);
        setDropItem(aspenLog);
        setDropCount(1);
        setMaterial(Material.WOODEN_AXE);
        setHardness(2);
        setInstrument(Instrument.BANJO);
        setNote(new Note(3));
        setBreakSound("custom.block.wood.break");
        setFallSound("custom.block.wood.hit");
        setHitSound("custom.block.wood.hit");
        setWalkSound("custom.block.wood.step");
    }
}
