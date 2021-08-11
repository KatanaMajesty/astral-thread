package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class EumusBlock extends AstralBlock {
    public EumusBlock(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        setNote(new Note(2));
        setFortunable(false);
        setHardness(1.5);
        setDefDropItem(null);
        setDropItem(new com.astralsmp.custom.items.EumusBlock(getPlugin()));
        setDropCount(1);
        setMaterial(Material.STONE_SHOVEL);
        setBreakSound("block.dripstone_block.break");
        setWalkSound("block.dripstone_block.step");
        setFallSound("block.dripstone_block.hit");
        setHitSound("block.dripstone_block.hit");
    }
}