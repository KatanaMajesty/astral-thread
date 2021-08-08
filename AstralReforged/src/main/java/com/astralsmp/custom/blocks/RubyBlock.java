package com.astralsmp.custom.blocks;

import com.astralsmp.custom.AstralBlock;
import com.astralsmp.custom.items.Ruby;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

public class RubyBlock extends AstralBlock {

    public RubyBlock(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setInstrument(Instrument.BANJO);
        /*
        Пересмотреть setNote, так как могу использовать new Note(1), вместо изменённого сеттера
         */
        setNote(1);
        setMaterial(Material.IRON_PICKAXE);
        setBreakTime(22.5);
        /*
        setDropItem(new Ruby()) создаёт каждый сломанный блок новый объект кастомного предмета
        Пересмотреть эту функцию и попробовать оптимизировать, так как это может вызывать нагрузку
         */
        setDropItem(new Ruby());
        setDropCount(1);
        setPlaceSound("block.amethyst_block.place");
        setBreakSound("block.amethyst_block.break");
        setHitSound("block.amethyst_block.hit");
        setWalkSound("block.amethyst_block.walk");
        setFallSound("block.amethyst_block.fall");
        super.init();
    }


}
