package com.astralsmp.custom.blocks;

import com.astralsmp.api.PacketAPI;
import com.astralsmp.custom.AstralBlock;
import com.astralsmp.custom.items.Ruby;
import com.astralsmp.custom.items.RubyOre;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class RubyBlock extends AstralBlock {

    public RubyBlock(Plugin plugin) {
        super(plugin);
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
        setSilkDropItem(new RubyOre(getPlugin()));
        setSilkDropCount(1);
        setDropItem(new Ruby(getPlugin()));
        setDropCount(5);
        setDefDropItem(null);
        setDefDropCount(0);
        setBreakSound("block.stone.break");
        setHitSound("block.stone.hit");
        setWalkSound("block.stone.step");
        setFallSound("block.stone.fall");
    }
}
