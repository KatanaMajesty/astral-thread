package com.astralsmp.modules;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Все методы класса должны быть статическими. Класс не должен иметь конструктор
 * Класс используется исключительно для форматтирования текста на сервере майнкрафта
 * Все методы должны возвращать строку
 */
public class Formatter {

    private Player player = null;

    public Formatter(Player player) {
        this.player = player;
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F_0-9]{6}"); // паттерн {#??????}

    /**
     * @see Formatter#HEX_PATTERN
     * Заменяет паттерн цвета на цвет
     *
     * @param message необработанная строка с паттернами
     * @return возвращает обработанную строку без паттернов
     */
    public static String colorize(String message) {
        String result = message;

        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        while (hexMatcher.find()) {
            String matchedStr = hexMatcher.group();
            String formattedStr = matchedStr.replaceAll("&", "");
            result = result.replace(matchedStr, ChatColor.of(formattedStr) + "");
        }

        result = ChatColor.translateAlternateColorCodes('&', result);
        return result;
    }

    /**
     *
     * @param hexStr должен соответствовать формату "#ffffff"
     * @return возвращает new Color формата int, int, int
     */
    public static Color hexColorToRGB(String hexStr) {
        hexStr = hexStr.replace("&", "");
        return new Color(
                Integer.valueOf(hexStr.substring( 1, 3 ), 16),
                Integer.valueOf(hexStr.substring( 3, 5 ), 16),
                Integer.valueOf(hexStr.substring( 5, 7 ), 16)
        );
    }

    public static String tagRemove(String hexStr) {
        return hexStr.replace("&", "");
    }

    /**
     * Применяет функцию к строке, позволяя заменить плейсхолдер
     *
     * @param message необработанная строка
     * @param fun функция для обработки строки
     * @return обработанная строка
     */
    @Deprecated
    public static String placeholderFunction(String message, Function<String, String> fun) {
        return fun.apply(message);
    }

    public void sendMessage(@NotNull String message) {
        player.sendMessage(colorize(message));
    }

}
