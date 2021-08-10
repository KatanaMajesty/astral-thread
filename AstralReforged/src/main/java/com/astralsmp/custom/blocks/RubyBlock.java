package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import com.astralsmp.custom.items.Ruby;
import com.astralsmp.custom.items.RubyOre;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.plugin.Plugin;

public class RubyBlock extends AstralBlock {

    public RubyBlock(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        setNote(new Note(1));
        setMaterial(Material.IRON_PICKAXE);
        setBreakTime(22.5);
        /*
        setDropItem(new Ruby()) создаёт каждый сломанный блок новый объект кастомного предмета
        Пересмотреть эту функцию и попробовать оптимизировать, так как это может вызывать нагрузку
         */
        setSilkTouchable(true);
        setSilkDropItem(new RubyOre(getPlugin()));
        setSilkDropCount(1);
        setDropItem(new Ruby(getPlugin()));
        setDropCount(5);
        setDefDropItem(null);
        setDefDropCount(0);
        setPlaceSound("block.amethyst_block.place");
        setBreakSound("block.amethyst_block.break");
        setHitSound("block.amethyst_block.hit");
        setWalkSound("block.amethyst_block.walk");
        setFallSound("block.amethyst_block.fall");
        super.init();
    }
}
