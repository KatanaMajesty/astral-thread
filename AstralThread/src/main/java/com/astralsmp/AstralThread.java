package com.astralsmp;

import com.astralsmp.commands.LinkingSystem;
import com.astralsmp.events.LinkComponentCallback;
import com.astralsmp.exceptions.InitTableException;
import com.astralsmp.modules.Config;
import com.astralsmp.modules.Discord;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.logging.Logger;

// TODO: 26.07.2021 закончить оформление класса
/**
 * Главный класс, который вызывается при запуске сервера.
 * Не создавать никакие объекты от класса. Только использовать статические поля и методы
 */
public class AstralThread extends JavaPlugin {

    public static final String LINK_TABLE = "astral_linked_players";

    private final Logger LOG = getLogger();

    @Override
    public void onEnable() {
        /*
        Блок кода для инициализации конфига и всей подобной дряни, которая с ним связанна
        Данный блок должен иметь первый приоритет при запуске плагина!!!
         */
        Config config = new Config(this);
        try {
            config.initialize();
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        /*
        Блок кода ниже должен использоваться только для инициализации всякой дряни,
        которая связанна с Дискордом
         */
        try {
            Discord.initialize();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }

        /*
        Блок кода ниже для инициализации необходимой дресни, которая связанна
        с инициализацией таблиц в базе данных
         */
        try {
            LinkingSystem.initTable();
        } catch (InitTableException e) {
            e.printStackTrace();
        }

        /*
        Блок кода ниже для объявления новых ивентов и команд
         */
        getServer().getPluginManager().registerEvents(new LinkComponentCallback(), this);

        // Логируем всё что нужно
        LOG.info("Включён");
    }

    @Override
    public void onDisable() {
        /*
        Блок кода ниже для инициализации необходимой дресни, которая связанна
        с инициализацией таблиц в базе данных
         */
        // some code

        /*
        Блок кода ниже должен использоваться только для инициализации всякой дряни,
        которая связанна с Дискордом
         */
        Discord.unregister();

        // Логируем всё что нужно
        getLogger().info("Выключен");
    }
}
