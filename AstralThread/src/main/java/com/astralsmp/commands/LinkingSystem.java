package com.astralsmp.commands;

import com.astralsmp.AstralThread;
import com.astralsmp.annotations.AstralCommand;
import com.astralsmp.events.LinkComponentCallback;
import com.astralsmp.exceptions.InitTableException;
import com.astralsmp.modules.Config;
import com.astralsmp.modules.Database;
import com.astralsmp.modules.Discord;
import com.astralsmp.modules.Formatter;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

// TODO: 26.07.2021 Спам проверка
// TODO: 26.07.2021 Проверка на нажатие кнопок
// TODO: 26.07.2021 Команда для отвязки для модерации
/**
 * Система привязки аккаунтов между Дискордом и Майнкрафтом.
 * Инстанс данного метода должен быть создан до применения его статического метода
 * для инициализации таблицы
 * @see LinkingSystem#initTable()
 */
public class LinkingSystem extends ListenerAdapter {

    private final String LINKED_ROLE_ID = Config.getDiscordConfig().getString("LinkingSystemClass.linked_role");
    private final String GUILD_ID = Discord.GUILD_ID;

    private static final char PREFIX = Discord.PREFIX;
    private static final String LINK_TABLE = AstralThread.LINK_TABLE;

    // TODO: 26.07.2021 Добавить возможность чистить коллекцию командой в игре
    public static final List<Map.Entry<String, UUID>> SPAM_MAP = new ArrayList<>();

    /**
     * Данный метод инициализирует таблицу для хранения привязанных игроков.
     * Следуя из этого он спокойно может быть статическим.
     * Крайне важно вызывать его перед инициализацией Дискорд бота,
     * поскольку бот не сможет вносить данные в бд, пока таблица для хранения не была инициализирована
     */
    public static void initTable() throws InitTableException {
        // тут нечего описать, просто получаю соединение с бд и вношу в неё таблицу, если та ещё не создана.
        try (Connection connection = Database.getConnection()) {
            final String QUERY = String.format("CREATE TABLE IF NOT EXISTS %s (uuid UUID, display_name VARCHAR(16), discord_id DECIMAL(18,0));", LINK_TABLE);
            Statement statement = connection.createStatement();
            statement.execute(QUERY);
        } catch (SQLException e) {
            throw new InitTableException("Не удалось создать таблицу " + LINK_TABLE);
        }
    }

    /**
     * Данный метод позволяет нам реализовать систему привязки/отвязки через Дискорд
     * с помощью команд.
     * Я решил не реализовывать отдельные каналы на сервере, поэтому эта система будет
     * работать только через личные сообщения бота.
     * Чтобы привязать аккаунт участник сервера обязан отключить запрет на сообщения в лс
     * от участников сервера, что значительно упрощает код и делает его более понятным
     *
     * @param event поскольку мы перезаписываем метод, этот параметр нам нужен
     *              для получения всех объектов, которые связанны с получением
     *              сообщения в ЛС
     */
    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().startsWith(String.valueOf(PREFIX))) {
            String[] args = event.getMessage().getContentRaw().split(" ");
            switch (args[0]) {
                case PREFIX + "привязать" ->
                        // привязка
                        onLinkCommand(event, args);
                case PREFIX + "отвязать" -> {
                    // отвязка
                    try {
                        onUnlinkCommand(event, args);
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }

    // TODO: 26.07.2021 закончить оформление метода
    /**
     * Метод, который вызывается командой !привязать.
     * Данный метод возможен только при взаимодействии с ботом через личные сообщения пользователя
     * СИНТАКСИС: !привязать [Ник игрока в Майнкрафт]
     * @see LinkingSystem#onPrivateMessageReceived(PrivateMessageReceivedEvent)
     *
     * @param event ивент, который вызывает метод
     * @param args аргументы сообщения, которые были отправлены в ивент
     */
    @AstralCommand(cmdName = "Link")
    public void onLinkCommand(final PrivateMessageReceivedEvent event, String[] args) {
        MessageChannel channel = event.getChannel();
        User sender = event.getAuthor();
        /*
        Если в сообщении нет 2 аргументов - мы жалуемся на это и отказываемся выполнять команду
        */
        if (args.length != 2) {
            channel.sendMessage("Недостаточно аргументов").queue();
            return;
        }
        /*
        Создаём объект игрока через ник, который берём со второго аргумента.
        Если объект = null, отказываемся выполнять его и жалуемся.
        Если не null, отправляем игроку на сервер сообщение о подтверждении привязки
         */
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            channel.sendMessage("Указанный игрок не в сети").queue();
            return;
        }
        /*
        Перед привязкой проверяю базу данных на наличие Discord ID пользователя.
        Если Discord ID имеется в бд - запрещаю привязывать новый аккаунт
         */
        String username;
        if ((username = getLinkedPlayerName(sender)) != null) {
            channel.sendMessage("К вашему Дискорд аккаунту уже привязан Майнкрафт аккаунт " + username).queue();
            return;
        }
        /*
        Проверяю коллекцию спама на наличие в ней игрока.
        Если игрок находится в коллекции - делаю вид, что на него пожаловались и запрещаю ему отправлять запросы.
         */
        for (Map.Entry<String, UUID> entry : SPAM_MAP) {
            if (entry.getKey().equals(sender.getId())) {
                if (entry.getValue().equals(target.getUniqueId())) {
                    channel.sendMessage("Вы не можете отправлять запросы о привязке этому аккаунту, так как его владелец обозначил Ваши попытки привязки спамом").queue();
                    return;
                }
            }
        }
        // отправляем заявку на привязку
        channel.sendMessage("Запрос на привязку Вашего Дискорд аккаунта был отправлен " + target.getDisplayName()).queue();
        target.spigot().sendMessage(linkComponents(target, event));
    }

    // TODO: 26.07.2021 закончить оформление метода
    /**
     * Метод, позволяющий отправлять подтверждение о привязке на сервер.
     * Сделал метод приватным, так как мы не будем вызывать его из вне.
     *
     * @param target цель, которой будет отправлен текст
     * @param event ивент, который передаётся в метод для получения дополнительных объектов в случае надобности
     * @return возвращаем объект интерфейса BaseComponent, который реализован через TextComponent. Этот блок будет отправлен игроку на сервере
     * и даст ему возможность привязать или не привязывать аккаунт
     */
    private BaseComponent linkComponents(Player target, PrivateMessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        User sender = event.getAuthor();
        String targetName = target.getDisplayName();
        target.sendMessage("Получен запрос на привязку аккаунта к Дискорду " + sender.getAsTag());

        /*
        Создаю все компоненты, инициализирую.
         */
        TextComponent accept = new TextComponent("✔ Привязать");
        TextComponent cancel = new TextComponent("⌀ Отмена");
        TextComponent spam = new TextComponent("✎ Спам");
        TextComponent slash = new TextComponent(" / ");

        /*
        Даю им цвета, создаю ивент при наведении на них в чате
         */
        accept.setColor(ChatColor.of("#37d90f")); // green
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Привязать аккаунт"))));
        cancel.setColor(ChatColor.of("#f21d1d")); // red
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Отменить привязку"))));
        spam.setColor(ChatColor.of("#ffd30f")); // yellow
        spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Обозначить спамом"))));
        slash.setColor(ChatColor.of("#dbdbdb")); // gray

        /*
        Данный блок кода позволяет мне, помимо обычного выполнения майнкрафт команды,
        так же выполнять блоки кода, которые реализованы через калбэк
        TODO 26.07.21 Закончить калбэки, добавить проверку на нажатую кнопку
        TODO 26.07.21 Настроить эмбеды
        TODO 26.07.21 Настроить HEX
         */
        LinkComponentCallback.execute(accept, player -> {
            insertPlayer(target, sender);
            channel.sendMessage("Ваш Дискорд аккаунт был успешно привязан к " + targetName).queue();
        });
        LinkComponentCallback.execute(cancel, player -> {
            channel.sendMessage(String.format("Владелец аккаунта %s отклонил запрос о привязке аккаунтов", targetName)).queue();
        });
        LinkComponentCallback.execute(spam, player -> {
            SPAM_MAP.add(new AbstractMap.SimpleEntry<>(sender.getId(), player.getUniqueId()));
            channel.sendMessage("Ваши попытки привязки были обозначены спамом. Более Вы не сможете отправлять запрос о привязке на данный аккаунт").queue();
        });

        // отдаём BaseComponent другому методу, который обработает его и отправит пользователю на сервер
        return new TextComponent(accept, slash, cancel, slash, spam);
    }

    // TODO: 26.07.2021 Закончить оформление метода
    /**
     * Метод, который вызывается командой !отвязать.
     * Данный метод возможен только при взаимодействии с ботом через личные сообщения пользователя
     * СИНТАКСИС: !отвязать
     * @see LinkingSystem#onPrivateMessageReceived(PrivateMessageReceivedEvent)
     *
     * @param event ивент, который вызывает метод
     * @param args аргументы сообщения, которые были отправлены в ивент
     */
    @AstralCommand(cmdName = "Unlink")
    public void onUnlinkCommand(final PrivateMessageReceivedEvent event, String[] args) throws SQLException {
        MessageChannel channel = event.getChannel();
        /*
        Нам не нужен второй аргумент, поэтому мы выполняем проверку на наличие только 1 аргумента и не более.
        Второй аргумент нам не нужен, так как в этом методе мы взаимодействуем с базами данных и проверяем наличие пользователя
        отдельными методами, которые прописаны в коде ниже.
         */
        if (args.length != 1) {
            channel.sendMessage("Недостаточно аргументов").queue();
            return;
        }
        String playerName = getLinkedPlayerName(event.getAuthor());
        if (playerName == null) {
            channel.sendMessage("Мне не удалось найти аккаунт, привязанный к Вашему Дискорду").queue();
            return;
        }
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            target.spigot().sendMessage(unlinkComponents(target, event));
        } else channel.sendMessage("Вы должны быть на сервере во время отвязки аккаунта").queue();
    }

    // TODO: 26.07.2021 закончить оформление метода
    /**
     * Метод, позволяющий отправлять подтверждение об отвязке на сервер.
     * Сделал метод приватным, так как мы не будем вызывать его из вне.
     *
     * @param target цель, которой будет отправлен текст
     * @param event ивент, который передаётся в метод для получения дополнительных объектов в случае надобности
     * @return возвращаем объект интерфейса BaseComponent, который реализован через TextComponent. Этот блок будет отправлен игроку на сервере
     * и даст ему возможность отвязать или не отвязывать свой аккаунт
     */
    private BaseComponent unlinkComponents(Player target, PrivateMessageReceivedEvent event) {
        /*
        В этом методе всё абсолютно так же как и в методе linkComponents().
        Если забыл что-то - смотри в последний.
         */
        User sender = event.getAuthor();
        MessageChannel channel = event.getChannel();

        TextComponent accept = new TextComponent("✔ Отвязать");
        TextComponent cancel = new TextComponent("⌀ Отмена");
        TextComponent slash = new TextComponent(" / ");

        accept.setColor(ChatColor.of("#37d90f")); // green
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Отвязать аккаунт"))));
        cancel.setColor(ChatColor.of("#f21d1d")); // red
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Отменить отвязку"))));
        slash.setColor(ChatColor.of("#dbdbdb")); // gray

        LinkComponentCallback.execute(accept, player -> {
            removePlayer(sender);
            channel.sendMessage("Ваш аккаунт был успешно отвязан. Жду не дождусь вновь его привязать!").queue();
            player.sendMessage(Formatter.colorize(String.format("#dbdbdbДискорд аккаунт %s более не привязан к Вашему Майнкрафт аккаунту", sender.getAsTag())));
        });
        LinkComponentCallback.execute(cancel, player -> {
            channel.sendMessage("Отвязка аккаунтов была отклонена. Можно ведь и не отвязывать вовсе :)").queue();
            player.sendMessage(Formatter.colorize("#dbdbdbОтвязка аккаунтов была успешно отменена"));
        });

        return new TextComponent(accept, slash, cancel);
    }

    // TODO: 26.07.2021 Убрать printStackTrace()
    /**
     * Метод для добавления данных об игроке в базу данных. Вызывается только когда пользователь Дискорд пытается привязать свой аккаунт
     * к игроку в Майнкрафт.
     *
     * @param target объект игрока, к которому пытаются привязать Дискорд аккаунт
     * @param sender Дискорд пользователь, который пытается привязать свой аккаунт к Майнкрафту
     */
    private void insertPlayer(Player target, User sender) {
        try (Connection connection = Database.getConnection()) {
            final String QUERY = String.format("INSERT INTO %s (uuid, display_name, discord_id) VALUES (?, ?, ?);", LINK_TABLE);
            try (PreparedStatement statement = connection.prepareStatement(QUERY)) {
                statement.setObject(1, target.getUniqueId());
                statement.setObject(2, target.getDisplayName());
                statement.setObject(3, sender.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TODO: 26.07.2021 Убрать printStackTrace()
    /**
     * Метод для удаления данных об игроке из базы данных. Метод должен вызываться только в время
     * отвязки аккаунтов по команде. Нет смысла делать его public
     *
     * @param sender получает пользователя класса User, который пытается отвязать свой Дискорд аккаунт
     *               от Майнкрафт аккаунта.
     */
    private void removePlayer(User sender) {
        try (Connection connection = Database.getConnection()) {
            final String QUERY = String.format("DELETE FROM %s WHERE discord_id = ?;", LINK_TABLE);
            try (PreparedStatement statement = connection.prepareStatement(QUERY)) {
                statement.setObject(1, sender.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
    You should never pass a ResultSet around through public methods.
    This is prone to resource leaking because you're forced to keep the statement and the connection open.
    Closing them would implicitly close the result set. But keeping them open would cause them
    to dangle around and cause the DB to run out of resources when there are too many of them open.
     */

    // TODO: 26.07.2021 Убрать printStackTrace()
    /**
     * Метод обрабатывает Discord ID пользователя, который пытается отвязать свой аккаунт.
     * Возвращает имя игрока в Майнкрафт, к которому привязан аккаунт Дискорд.
     *
     * @param sender получаем объект класса User для работы с его данными.
     * @return возвращаем имя игрока типа String, которое взято из базы данных.
     *         Если Дискорд ID пользователя не находится в базе данных, то метод вернёт NULL
     */
    private String getLinkedPlayerName(User sender) {
        // нечего объяснять, я просто подключаюсь к бд, создаю утверждение и выполняю запрос, получая имя пользователя
        try (Connection connection = Database.getConnection()) {
            final String QUERY = String.format("SELECT * FROM %s WHERE discord_id = %s", LINK_TABLE, sender.getId());
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(QUERY);
            if (rs.next()) {
                return rs.getString(2);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}
