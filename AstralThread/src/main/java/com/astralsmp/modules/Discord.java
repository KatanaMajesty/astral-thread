package com.astralsmp.modules;

import com.astralsmp.commands.LinkingSystem;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class Discord {

    public static JDA jda;
    public static final String GUILD_ID = "723812053550104597"; // TODO: 26.07.2021 конфиг
    public static final char PREFIX = '!';

    public static void initialize(String token) throws LoginException, InterruptedException {
        jda = JDABuilder.createDefault(token).build();
        jda.addEventListener(new LinkingSystem());
        jda.awaitReady();
    }

}
