package com.astralsmp.world.overworld;

import com.astralsmp.AstralReforged;
import com.astralsmp.world.OrePopulator;
import org.bukkit.Material;

public class AventurineOrePopulator extends OrePopulator {

    public AventurineOrePopulator(AstralReforged plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        setChance(60);
        setMaxLodeC(4);
        setContChance(80);
        setGenB(getPlugin().aventurineOre);
        setLocMat(Material.STONE);
        setY1(3);
        setY2(20);
    }
}
