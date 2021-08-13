package com.astralsmp;

import com.astralsmp.custom.blocks.AspenLog;
import com.astralsmp.custom.blocks.EumusBlock;
import com.astralsmp.custom.blocks.RubyBlock;
import com.astralsmp.modules.BlockRelated;
import com.astralsmp.modules.WoodRestore;
import com.astralsmp.world.end.AspenTreePopulator;
import com.astralsmp.world.end.EumusPopulator;
import com.astralsmp.world.overworld.RubyOrePopulator;
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
    public RubyBlock rubyBlock;
    public EumusBlock eumusBlock;
    public AspenLog aspenLog;

    @EventHandler
    public void onWInit(WorldInitEvent e) {
        World w = e.getWorld();
        if (w.getEnvironment() == World.Environment.THE_END) {
            w.getPopulators().add(new EumusPopulator());
            w.getPopulators().add(new AspenTreePopulator());
        }
        if (w.getEnvironment() == World.Environment.NORMAL) {
            w.getPopulators().add(new RubyOrePopulator(this));
        }
    }

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        eumusBlock = new EumusBlock();
        rubyBlock = new RubyBlock();
        aspenLog = new AspenLog();

        BlockRelated.initReplaceableArray();
        BlockRelated.initWoodenArray();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(eumusBlock, this);
        getServer().getPluginManager().registerEvents(aspenLog, this);
        getServer().getPluginManager().registerEvents(rubyBlock, this);

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
