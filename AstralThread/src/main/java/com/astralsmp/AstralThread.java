package com.astralsmp;

import com.astralsmp.commands.LinkingSystem;
import com.astralsmp.events.LinkComponentCallback;
import com.astralsmp.modules.Discord;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.util.logging.Logger;

public class AstralThread extends JavaPlugin {

    public static final String LINK_TABLE = "astral_linked_players";

    private final Logger LOG = getLogger();

    @Override
    public void onEnable() {

        LinkingSystem.initTable();
        /*
        discord
         */
        try {
            Discord.initialize("ODYyNzU2MTIxMTc2NDQwODMz.YOc-QA.3wieV86V8kDCGzhzuiuTHTL9GkE");
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(new LinkComponentCallback(), this);

        LOG.info("Включён");
    }

    @Override
    public void onDisable() {
        getLogger().info("Выключен");
    }
}
