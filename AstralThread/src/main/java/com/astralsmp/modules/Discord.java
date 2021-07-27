package com.astralsmp.modules;

import com.astralsmp.commands.LinkingSystem;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;

import javax.security.auth.login.LoginException;

/**
 * Статический класс для работы с Дискорд ботом сервера.
 * 26.07.21 Не стоит создавать объекты от класса.
 */
public class Discord {

    public static JDA jda = null;
    public static Guild GUILD = null;
    public static final char PREFIX = '!';
    private static final String TOKEN = Config.getDiscordConfig().getString("DiscordClass.token");

    /**
     * Инициализация Дискорд бота
     * @throws LoginException не удалось подключиться к боту через токен, так как последний указан неверно
     * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied,
     *                              and the thread is interrupted, either before or during the activity.
     *                              Occasionally a method may wish to test whether the current thread has been interrupted,
     *                              and if so, to immediately throw this exception.
     */
    public static void initialize() throws LoginException, InterruptedException {
        jda = JDABuilder.createDefault(TOKEN).build();
        jda.awaitReady();

        GUILD = jda.getGuildById(Config.getDiscordConfig().getLong("DiscordClass.guild_id"));
        System.out.println(GUILD);

        jda.addEventListener(new LinkingSystem());
    }

    /**
     * Безопасное отключение бота при выключении сервера
     */
    public static void unregister() {
        if (jda != null) {
            jda.shutdown();
        }
    }

}
