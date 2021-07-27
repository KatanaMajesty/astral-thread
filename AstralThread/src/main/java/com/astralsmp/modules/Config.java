package com.astralsmp.modules;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Класс для инициализации и получения всех конфигов
 * которые используются в проекте
 */
public class Config {

    private final Plugin PLUGIN;
    private static FileConfiguration discordConfig = null;
    private static FileConfiguration config = null;
    private static File discordConfigFile;

    /**
     * Конструктор принимает Plugin и использует его для инициализации конфига
     * @param plugin объект плагина
     */
    public Config(Plugin plugin) {
        PLUGIN = plugin;
    }

    /**
     * Метод для получения дискорд конфига.
     * Я специально сделал его статическим, чтобы не создавать кучу ненужных объектов.
     * Если вызов этого статического метода будет до инициализации конфига - дропнет NPE
     * @see Config#initialize()
     *
     * @return возвращает Дискорд конфиг объекта FileConfiguration
     * @throws NullPointerException если не удалось загрузить конфиг
     */
    public static FileConfiguration getDiscordConfig() {
        if (discordConfig == null) throw new NullPointerException("discord.yml == null");
        return discordConfig;
    }

    /**
     * Метод для получения дискорд конфига.
     * Я специально сделал его статическим, чтобы не создавать кучу ненужных конструкторов, которые
     * я бы создавал для получения инстанса Plugin
     * Если вызов этого статического метода будет до инициализации конфига - дропнет NPE
     * @see Config#initialize()
     *
     * @return возвращает дефолтный конфиг объект FileConfiguration
     * @throws NullPointerException если не удалось загрузить конфиг
     */
    public static FileConfiguration getConfig() {
        if (config == null) throw new NullPointerException("config.yml == null");
        return config;
    }

    /**
     * Данный метод позволяет сохранить Дискорд конфиг
     * @throws IOException при неудавшемся сохранении файла
     */
    private static void saveDiscordConfig() throws IOException {
        discordConfig.save(discordConfigFile);
    }

    /**
     * Данный метод позволяет перезагрузить Дискорд конфиг, одновременно с этим выключив Дискорд бота.
     * ВАЖНО! Все коллекции, сохранённые во время работы бота данные, не будут обнулены!
     * @throws LoginException при неудачной попытке применения токена
     * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied,
     * and the thread is interrupted, either before or during the activity.
     * Occasionally a method may wish to test whether the current thread has been interrupted,
     * and if so, to immediately throw this exception
     */
    public static void reloadDiscordConfig() throws LoginException, InterruptedException {
        Discord.jda.shutdown();
        discordConfig = YamlConfiguration.loadConfiguration(discordConfigFile);
        Discord.initialize();
    }

    /**
     * Этот метод создан для иниаицализации всех нужных конфигов в плагине.
     * На данный момент это config.yml и discord.yml. Все эти файлы должны находится
     * в папке resources, иначе IOException дропнется
     * @throws IOException при неудачном создании файла
     * @throws InvalidConfigurationException при неверной конфигурации
     */
    @SuppressWarnings("all")
    public void initialize() throws IOException, InvalidConfigurationException {
        /*
        Данный блок кода инициализирует кастомный конфиг discord.yml
        Этот конфиг используется для получения значений, связанных с Дискорд ботом
         */

        try (Reader reader = new InputStreamReader(PLUGIN.getResource("discord.yml"), StandardCharsets.UTF_8)) {
            discordConfigFile = new File(PLUGIN.getDataFolder(), "discord.yml");
            YamlConfiguration defaultDiscordConfig = YamlConfiguration.loadConfiguration(reader);
            discordConfig = YamlConfiguration.loadConfiguration(discordConfigFile);
            discordConfig.setDefaults(defaultDiscordConfig);
            discordConfig.options().copyDefaults(true);
            saveDiscordConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        Блок кода, который инициализирует дефолтный config.yml.
        Этот конфиг должен использоваться для получения информации на Майнкрафт сервере
         */
        config = PLUGIN.getConfig();
        config.options().copyDefaults(true);
        PLUGIN.saveConfig();
    }

}
