package com.astralsmp;

import com.astralsmp.custom.blocks.RubyBlock;
import com.astralsmp.custom.blocks.RubyBlockDep;
import com.astralsmp.modules.BlockRelated;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


public class AstralReforged extends JavaPlugin implements Listener {

    public static ProtocolManager protocolManager = null;

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        BlockRelated.initReplaceableArray();
        BlockRelated.initWoodenArray();

        getServer().getPluginManager().registerEvents(new RubyBlock(this), this);
        getServer().getPluginManager().registerEvents(new WoodRestore(this), this);

        getLogger().info("Плагин включён");
    }

    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
    }
}
