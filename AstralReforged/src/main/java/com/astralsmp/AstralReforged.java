package com.astralsmp;

import com.astralsmp.custom.blocks.PoiseStem;
import com.astralsmp.custom.blocks.ChiseledEndStoneBricks;
import com.astralsmp.custom.blocks.EumusBlock;
import com.astralsmp.custom.blocks.AventurineOre;
import com.astralsmp.modules.BlockRelated;
import com.astralsmp.modules.WoodRestore;
import com.astralsmp.world.end.AspenTreePopulator;
//import com.astralsmp.world.end.EumusPopulator;
import com.astralsmp.world.overworld.AventurineOrePopulator;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AstralReforged extends JavaPlugin implements Listener {

    public static ProtocolManager protocolManager = null;
    public AventurineOre aventurineOre;
    public EumusBlock eumusBlock;
    public PoiseStem poiseStem;
    public ChiseledEndStoneBricks chiseledEndStoneBricks;

    @EventHandler
    public void onWInit(WorldInitEvent e) {
        World w = e.getWorld();
        if (w.getEnvironment() == World.Environment.THE_END) {
            w.getPopulators().add(new AspenTreePopulator());
        }
        if (w.getEnvironment() == World.Environment.NORMAL) {
            w.getPopulators().add(new AventurineOrePopulator(this));
        }
    }

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        chiseledEndStoneBricks = new ChiseledEndStoneBricks();
        eumusBlock = new EumusBlock();
        aventurineOre = new AventurineOre();
        poiseStem = new PoiseStem();

        BlockRelated.initReplaceableArray();
        BlockRelated.initWoodenArray();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(eumusBlock, this);
        getServer().getPluginManager().registerEvents(poiseStem, this);
        getServer().getPluginManager().registerEvents(aventurineOre, this);
        getServer().getPluginManager().registerEvents(chiseledEndStoneBricks, this);

        getServer().getPluginManager().registerEvents(new WoodRestore(this), this);

        getLogger().info("Плагин включён");
    }

    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
    }

    public static AstralReforged getInstance() {
        return (AstralReforged) Bukkit.getPluginManager().getPlugin("AstralReforged");
    }
}
