package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import com.astralsmp.custom.items.Ruby;
import com.astralsmp.custom.items.RubyOre;
import org.bukkit.*;

public class RubyBlock extends AstralBlock {

    public RubyBlock() {
        super();
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        setNote(new Note(1));
        setMaterial(Material.IRON_PICKAXE);
        setHardness(3);
        /*
        setDropItem(new Ruby()) создаёт каждый сломанный блок новый объект кастомного предмета
        Пересмотреть эту функцию и попробовать оптимизировать, так как это может вызывать нагрузку
         */
        setFortunable(true);
        setSilkTouchable(true);
        setSilkDropItem(new RubyOre(plugin));
        setSilkDropCount(1);
        setDropItem(new Ruby(plugin));
        setDropCount(5);
        setDefDropItem(null);
        setDefDropCount(0);
        setBreakSound("block.stone.break");
        setHitSound("block.stone.hit");
        setWalkSound("block.stone.step");
        setFallSound("block.stone.fall");
    }
}
