package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import org.bukkit.*;

public class PoiseStem extends AstralBlock {
    public PoiseStem() {
        super();
    }

    // TODO: 13.08.2021 не выпадает дроп при ломании рукой (скорее всего из-за setDropItem)
    @Override
    public void init() {
        com.astralsmp.custom.items.PoiseStem poiseStem = new com.astralsmp.custom.items.PoiseStem(plugin);
        setDefDropItem(poiseStem);
        setDefDropCount(1);
        setDropItem(poiseStem);
        setDropCount(1);
        setMaterial(Material.WOODEN_AXE);
        setHardness(2);
        setInstrument(Instrument.BELL);
        setNote(new Note(2));
        setBreakSound("custom.block.wood.break");
        setFallSound("custom.block.wood.hit");
        setHitSound("custom.block.wood.hit");
        setWalkSound("custom.block.wood.step");
    }
}
