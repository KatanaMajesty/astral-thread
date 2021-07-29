package com.astralsmp.commands;

import com.astralsmp.AstralThread;
import com.astralsmp.annotations.AstralCommand;
import com.astralsmp.events.LinkComponentCallback;
import com.astralsmp.exceptions.InitTableException;
import com.astralsmp.modules.*;
import com.astralsmp.modules.Formatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO: 29.07.2021 По приезду можно сделать настройку через конфиг 
// TODO: 26.07.2021 Команда для отвязки для модерации
/**
 * Система привязки аккаунтов между Дискордом и Майнкрафтом.
 * Инстанс данного метода должен быть создан до применения его статического метода
 * для инициализации таблицы
 * @see LinkingSystem#initTable()
 */
public class LinkingSystem extends ListenerAdapter implements Listener {
    /**
     * Маленький набор для передачи state...
     */
    enum CommandState {
        SUCCESS, ERROR, SPAM
    }
    
    // Нужная палитра цветов
    private static final String GREEN = Config.getConfig().getString("ColorPalette.green");
    private static final String RED = Config.getConfig().getString("ColorPalette.red");
    private static final String YELLOW = Config.getConfig().getString("ColorPalette.yellow");
    private static final String GRAY = Config.getConfig().getString("ColorPalette.gray");

    private final Guild GUILD = Discord.GUILD;
    private final CooldownManager COOLDOWN_MANAGER = new CooldownManager();
    private final String LINKED_ROLE_ID = Config.getDiscordConfig().getString("LinkingSystemClass.linked_role");
    private final String COOLDOWN_ROLE_ID = Config.getDiscordConfig().getString("LinkingSystemClass.cooldown_bypass_role");
    private final long EXPIRE_AFTER = Config.getConfig().getLong("LinkingSystem.expire_after_secs");
    private final Role LINKED_ROLE;

    {
        if (GUILD == null) {
            LINKED_ROLE = null;
        } else {
            assert LINKED_ROLE_ID != null;
            LINKED_ROLE = GUILD.getRoleById(LINKED_ROLE_ID);
        }
    }

    private static final Map<Player, String> UNFINISHED = new HashMap<>();
    private static final char PREFIX = Discord.PREFIX;
    private static final String LINK_TABLE = AstralThread.LINK_TABLE;

    // TODO: 26.07.2021 Добавить возможность чистить коллекцию командой в игре
    public static final List<Map.Entry<String, UUID>> SPAM_MAP = new ArrayList<>();





    // МЕТОДЫ ДЛЯ ДИСКОРДА И МНОГОПОТОЧНОСТИ





    /**
     * Данный метод нужен для определения наличия роли у конкретного участника.
     * Если участник выйдет с сервера во время проверки, появится будет ошибка, поэтому, желательно,
     * не использовать данный метод в цикличных процессах/процессах, требующих ответа от пользователя.
     *
     * @param member объект участника сервера
     * @param roleId id роли, которую нужно найти у участника
     * @return возвращает логический оператор в зависимости от наличия роли у участника сервера
     */
    private boolean hasRole(Member member, String roleId) {
        List<Role> roles = member.getRoles();
        Role neededRole = roles.stream().filter(role -> role.getId().equals(roleId)).findFirst().orElse(null);
        return neededRole != null;
    }

    /**
     * Данный метод устанавливает обратный отчёт после использования команды !привязать или !отвязать.
     * КД должен устанавливаться даже если команда была набрана неправильно!
     *
     * @param event передаю ивент PrivateMessageReceivedEvent для получения данных о пользователе
     * @return логический оператор в зависимости от того, имеется ли кд у игрока
     */
    private boolean isOnCooldown(PrivateMessageReceivedEvent event) {
        User sender = event.getAuthor();
        Member member = GUILD.retrieveMember(sender).complete();
        MessageChannel channel = event.getChannel();
        long timeLeft = System.currentTimeMillis() - COOLDOWN_MANAGER.getCooldown(sender.getId());
        long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeLeft);
        if (member != null) {
            if (!hasRole(member, COOLDOWN_ROLE_ID)) {
                if (secondsLeft < CooldownManager.DEFAULT_COOLDOWN) {
                    int[] formattedLeft = CooldownManager.splitTimeArray(CooldownManager.DEFAULT_COOLDOWN - secondsLeft);
                    channel.sendMessageEmbeds(
                            embedBuilder(sender, String.format("Подождите ещё %d:%d перед отправкой следующей заявки", formattedLeft[1], formattedLeft[2]), CommandState.ERROR))
                            .queue();
                    return true;
                }
                // Место для добавления кд!
                COOLDOWN_MANAGER.setCooldown(sender.getId(), System.currentTimeMillis());
                return false;
            }
        } else {
            System.out.println("Не удалось установить КД для пользователя " + sender.getAsTag());
        }
        return false;
    }

    /**
     * Метод для запуска кд на принятие запроса. Я сделал это, чтобы коллекции постоянно чистились и не забивались. Мало-ли, кто-то захочет забить, хз
     * Данный метод имеет много параметров, поэтому практически нигде не будет иметь применения, поэтому private
     * Контент метода меняется в зависимости от последнего параметра - логического оператора
     *
     * @param sender отправитель запроса
     * @param channel канал, в котором был запрос. В основном это будет ЛС с ботом, но в случае с модерацией, может быть, имеют место быть исключения.
     * @param target цель, которой отправлен запрос
     * @param EXPIRE_AFTER секунды, после которых запрос будет отменён
     * @param link контент должен соответствовать привязке или отвязке?
     */
    private void requestExpire(User sender, MessageChannel channel, Player target, long EXPIRE_AFTER, boolean link) {
        long expireAfterSecs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + EXPIRE_AFTER;
        while(UNFINISHED.containsKey(target)) {
            if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) > expireAfterSecs) {
                String formMsg = link
                        ? RED + String.format("Вы не реагировали на запрос о привязке в течении %d секунд, поэтому он был отклонён автоматически", EXPIRE_AFTER)
                        : RED + String.format("Вы не реагировали на запрос об отвязке в течении %d секунд, поэтому он был отклонён автоматически", EXPIRE_AFTER);
                String desc = link
                        ? "Аккаунт, к которому Вы попытались привязать аккаунт, не отреагировал на привязку. Запрос был отклонён"
                        : "Аккаунт, к которому Вы попытались привязать аккаунт, не отреагировал на отвязку. Запрос был отклонён";
                Formatter form = new Formatter(target);
                form.sendMessage(formMsg);
                channel.sendMessageEmbeds(embedBuilder(sender, desc, CommandState.ERROR)).queue();
                UNFINISHED.remove(target);
            }
        }
    }

    /**
     * Данный метод предназначен для создания однотипных, шаблонных эмбедов
     *
     * @param sender отправитель
     * @param description описание эмбеда (основной текст)
     * @param state ENUM значение для определённой команды
     * @return эмбед
     */
    private MessageEmbed embedBuilder(User sender, String description, CommandState state) {
        String title;
        Color embedColor;
        switch (state) {
            case SUCCESS -> {
                title = "Успех!";
                embedColor = Formatter.hexColorToRGB(GREEN);
            }
            case SPAM -> {
                title = "Чел, ты в спаме!";
                embedColor = Formatter.hexColorToRGB(YELLOW);
            }
            case ERROR -> {
                title = "Ошибка!";
                embedColor = Formatter.hexColorToRGB(RED);
            }
            default -> throw new IllegalStateException("Unexpected value: " + state);
        }
        
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(description);
        embedBuilder.setColor(embedColor);
        embedBuilder.setFooter(sender.getAsTag(), sender.getAvatarUrl());
        embedBuilder.setTimestamp(Instant.now());
        return embedBuilder.build();
    }





    // МЕТОДЫ ДЛЯ ОБРАБОТКИ КОМАНД





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
                case PREFIX + "привязать" -> // привязка
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

    /**
     * Метод для удаления игрока из базы данных привязанных игроков перед его выходом с сервера.
     * @param event срабатывает при выходе с сервера
     */
    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        removePlayer(event.getUser());
    }

    /**
     * Когда игрок выходит с сервера в Майнкрафте, я удаляю его из коллекции незавершённых привязок/отвязок,
     * чтобы при входе он снова смог привязать/отвязать аккаунт
     * @param event срабатывает при выходе игрока с сервера
     */
    @EventHandler
    public void onLinkingLeave(PlayerQuitEvent event) {
        UNFINISHED.remove(event.getPlayer());
    }

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
            channel.sendMessageEmbeds(embedBuilder(sender, "Неверное количество аргументов.\nСинтаксис: `!привязать [ник игрока в Майнкрафт]`", CommandState.ERROR)).queue();
            return;
        }
        /*
        Создаём объект игрока через ник, который берём со второго аргумента.
        Если объект = null, отказываемся выполнять его и жалуемся.
        Если не null, отправляем игроку на сервер сообщение о подтверждении привязки
         */
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Не удалось отправить запрос о привязке. Мне не удалось найти указанного игрока на сервере", CommandState.ERROR))
                    .queue();
            return;
        }
        /*
        Перед привязкой проверяю базу данных на наличие Discord ID пользователя.
        Если Discord ID имеется в бд - запрещаю привязывать новый аккаунт
         */
        String username;
        if ((username = getLinkedPlayerName(sender)) != null) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Не удалось отправить запрос о привязке. К Вашему Дискорд аккаунту уже привязан аккаунт " + username, CommandState.ERROR)).queue();
            return;
        }

        /*
        Проверяю наличие игрока, к которому пытаются привязаться, в базе данных
         */
        if (isLinked(target.getDisplayName())) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Не удалось отправить запрос о привязке. Данный аккаунт уже имеет стороннюю привязку", CommandState.ERROR))
                    .queue();
            return;
        }
        /*
        Проверяю коллекцию спама на наличие в ней игрока.
        Если игрок находится в коллекции - делаю вид, что на него пожаловались и запрещаю ему отправлять запросы.
         */
        for (Map.Entry<String, UUID> entry : SPAM_MAP) {
            if (entry.getKey().equals(sender.getId())) {
                if (entry.getValue().equals(target.getUniqueId())) {
                    channel.sendMessageEmbeds(embedBuilder(sender,
                            "Вы не можете отправлять запросы о привязке этому аккаунту, так как его владелец обозначил Ваши попытки привязки спамом. Не делайте так",
                            CommandState.SPAM)).queue();
                    return;
                }
            }
        }
        /*
        Здесь я проверяю, не отправил ли отправитель сообщения запрос об отвязке/привязке аккаунта.
        Если его Discord ID находится в коллекции - отменяю запрос о привязке
         */
        if (UNFINISHED.containsValue(sender.getId())) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Не удалось отправить запрос о привязке. Вы не завершили предыдущую привязку или отвязку", CommandState.ERROR))
                    .queue();
            return;
        }

        /*
        Здесь я выполняю проверку на наличии объекта цели, к которой пытаются привязать аккаунт.
        Если данной цели уже отправили запрос о привязке/отвязке - отменяю новый запрос во избежание спама
         */
        if (UNFINISHED.containsKey(target)) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Не удалось отправить запрос о привязке. Кто-то уже отправил запрос о привязке/отвязке этому аккаунту",
                    CommandState.ERROR)).queue();
            return;
        }

        // ВАЖНО! ЭТА ПРОВЕРКА ДОЛЖНА ИДТИ В ПОСЛЕДНЮЮ ОЧЕРЕДЬ ПЕРЕД ОТПРАВКОЙ!
        /*
        Если у участника уже есть кд - игнорируем.
        Если кд не найден - присваиваем участнику кд.
         */
        if(isOnCooldown(event)) return;

        // отправляем заявку на привязку
        channel.sendMessageEmbeds(embedBuilder(sender, "Запрос на привязку Вашего Дискорд аккаунта был успешно отправлен " + target.getDisplayName(), CommandState.SUCCESS))
                .queue();
        target.spigot().sendMessage(linkComponents(target, event));

        /*
        Запускаю цикл, в котором жду действия от пользователя в течении определённого количества секунд
        Если в течении этого времени он не реагирует на запрос - отклоняю его
         */
        requestExpire(sender, channel, target, EXPIRE_AFTER, true);
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
    @SuppressWarnings("ConstantConditions")
    private BaseComponent linkComponents(Player target, PrivateMessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        User sender = event.getAuthor();
        String targetName = target.getDisplayName();
        UNFINISHED.put(target, sender.getId());

        /* Если target не null - создаю объект форматтера
        Крайне важная хуйня. Позволяет избавиться от лишнего текста и прочего.
        Очень рекомендую использовать, если отправляется сообщение только одному игроку или если ты не долбоёб
         */
        Formatter form = new Formatter(target);
        form.sendMessage(GRAY + "Получен запрос на привязку аккаунта к " + sender.getAsTag());

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
        accept.setColor(ChatColor.of(Formatter.tagRemove(GREEN))); // green
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Привязать аккаунт"))));
        cancel.setColor(ChatColor.of(Formatter.tagRemove(RED))); // red
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Отменить привязку"))));
        spam.setColor(ChatColor.of(Formatter.tagRemove(YELLOW))); // yellow
        spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Обозначить спамом"))));
        slash.setColor(ChatColor.of(Formatter.tagRemove(GRAY))); // gray

        /*
        Данный блок кода позволяет мне, помимо обычного выполнения майнкрафт команды,
        так же выполнять блоки кода, которые реализованы через калбэк
        TODO 26.07.21 Настроить эмбеды
        TODO 26.07.21 Настроить HEX
         */
        LinkComponentCallback.execute(accept, player -> {
                /*
            В строке ниже я проверяю, завершил ли пользователь привязку.
            Если пользователь находится в коллекции UNFINISHED - делаю вид, что он ещё не нажал на кнопку
            и позволяю ему сделать это.
            По нажатии на кнопку все остальные кнопки станут недоступны, так как тоже содержат в себе эту строку.
             */
            if (!UNFINISHED.containsKey(player)) return;
            /*
            Выполняю проверку на правильность созданных объектов сервера и роли.
            Если всё в порядке - игнорирую if
             */
            if (GUILD == null | LINKED_ROLE == null) {
                UNFINISHED.remove(player);
                form.sendMessage(RED + "Не удалось завершить привязку. Подробности были отправлены в Дискорд");
                channel.sendMessageEmbeds(embedBuilder(sender,
                        "Мне не удалось выдать Вам роль на нашем Дискорд сервере. Обратитесь к модерации.\n" +
                                "Привязка аккаунтов была отменена", CommandState.ERROR)).queue(null, ignored -> {});
                return;
            }
            /*
            Пробую выдать роль отправителю в случае успешной привязки.
            Тем не менее, если бот не может выдать роль отправителю - отменяю привязку и прошу обратиться к модерации
            Бот не может выдавать другим роль, которая равна его наивысшей роли или роли выше
             */
            try {
                Member member = GUILD.retrieveMember(sender).complete();
                GUILD.addRoleToMember(member.getId(), LINKED_ROLE).queue();
            } catch (HierarchyException e) {
                UNFINISHED.remove(player);
                form.sendMessage(RED + "Не удалось завершить привязку. Подробности были отправлены в Дискорд");
                channel.sendMessageEmbeds(embedBuilder(sender,
                        "У меня недостаточно прав для выдачи роли привязанного игрока.\nОбратитесь к модерации: " + e.getStackTrace()[0], CommandState.ERROR))
                        .queue(null, ignored -> {});
                return;
            } catch (ErrorResponseException e) {
                UNFINISHED.remove(player);
                form.sendMessage(RED + "Не удалось привязать аккаунт, так как мне не удалось найти Вас на нашем Дискорд сервере");
                return;
            }
            /*
            Добавляю игрока в базу данных, сообщаю ему об успешной привязке, удаляю из списка незавершённых привязок
             */
            insertPlayer(target, sender);
            form.sendMessage(String.format(GRAY + "Ваш аккаунт успешно привязан к Дискорду %s! Приятной игры", sender.getAsTag()));
            channel.sendMessageEmbeds(embedBuilder(sender, "Ваш Дискорд аккаунт был успешно привязан к " + targetName, CommandState.SUCCESS)).queue(null, ignored -> {});
            UNFINISHED.remove(player);
        });
        LinkComponentCallback.execute(cancel, player -> {
            /*
            В строке ниже я проверяю, завершил ли пользователь привязку.
            Если пользователь находится в коллекции UNFINISHED - делаю вид, что он ещё не нажал на кнопку
            и позволяю ему сделать это.
            По нажатии на кнопку все остальные кнопки станут недоступны, так как тоже содержат в себе эту строку.
             */
            if (!UNFINISHED.containsKey(player)) return;
            form.sendMessage(GRAY + "Привязка аккаунта успешно отменена.");
            channel.sendMessageEmbeds(embedBuilder(sender, String.format("Запрос о привязке был успешно отклонён со стороны аккаунта %s", targetName), CommandState.SUCCESS))
                    .queue(null, ignored -> {});
            UNFINISHED.remove(player);
        });
        LinkComponentCallback.execute(spam, player -> {
            /*
            В строке ниже я проверяю, завершил ли пользователь привязку.
            Если пользователь находится в коллекции UNFINISHED - делаю вид, что он ещё не нажал на кнопку
            и позволяю ему сделать это.
            По нажатии на кнопку все остальные кнопки станут недоступны, так как тоже содержат в себе эту строку.
             */
            if (!UNFINISHED.containsKey(player)) return;
            SPAM_MAP.add(new AbstractMap.SimpleEntry<>(sender.getId(), player.getUniqueId()));
            form.sendMessage(GRAY + "Данный пользователь был добавлен в игнор. Он больше не сможет отправлять Вам запрос о привязке/отвязке");
            channel.sendMessageEmbeds(embedBuilder(sender,
                    "Ваши попытки привязки были обозначены спамом. Более Вы не сможете отправлять запрос о привязке на данный аккаунт", CommandState.SPAM))
                    .queue(null, ignored -> {});
            UNFINISHED.remove(player);
        });

        // отдаём BaseComponent другому методу, который обработает его и отправит пользователю на сервер
        return new TextComponent(accept, slash, cancel, slash, spam);
    }

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
        User sender = event.getAuthor();
        /*
        Нам не нужен второй аргумент, поэтому мы выполняем проверку на наличие только 1 аргумента и не более.
        Второй аргумент нам не нужен, так как в этом методе мы взаимодействуем с базами данных и проверяем наличие пользователя
        отдельными методами, которые прописаны в коде ниже.
         */
        if (args.length != 1) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Неверное количество аргументов.\nСинтаксис: `!отвязать`", CommandState.ERROR)).queue();
            return;
        }
        String playerName = getLinkedPlayerName(event.getAuthor());
        if (playerName == null) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Мне не удалось найти аккаунт, привязанный к Вашему Дискорду", CommandState.ERROR)).queue();
            return;
        }
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Вы должны быть на сервере во время отвязки аккаунта", CommandState.ERROR)).queue();
            return;
        }

        /*
        Здесь я проверяю, не отправил ли отправитель сообщения запрос об отвязке/привязке аккаунта.
        Если его Discord ID находится в коллекции - отменяю запрос о привязке
         */
        if (UNFINISHED.containsValue(sender.getId())) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Вы не завершили предыдущую привязку или отвязку", CommandState.ERROR)).queue();
            return;
        }

        /*
        Здесь я выполняю проверку на наличии объекта цели, к которой пытаются привязать аккаунт.
        Если данной цели уже отправили запрос о привязке/отвязке - отменяю новый запрос во избежание спама
         */
        if (UNFINISHED.containsKey(target)) {
            channel.sendMessageEmbeds(embedBuilder(sender, "Кто-то уже отправил запрос о привязке/отвязке этому аккаунту", CommandState.ERROR)).queue();
            return;
        }

        // ВАЖНО! ЭТА ПРОВЕРКА ДОЛЖНА ИДТИ В ПОСЛЕДНЮЮ ОЧЕРЕДЬ ПЕРЕД ОТПРАВКОЙ!
        /*
        Если у участника уже есть кд - игнорируем.
        Если кд не найден - присваиваем участнику кд.
         */
        if(isOnCooldown(event)) return;

        channel.sendMessageEmbeds(embedBuilder(sender,
                "Запрос об отвязке был успешно отправлен владельцу аккаунта " + target.getDisplayName(), CommandState.SUCCESS))
                .queue();
        target.spigot().sendMessage(unlinkComponents(target, event));

        /*
        Запускаю цикл, в котором жду действия от пользователя в течении определённого количества секунд
        Если в течении этого времени он не реагирует на запрос - отклоняю его
         */
        requestExpire(sender, channel, target, EXPIRE_AFTER, false);
    }

    /**
     * Метод, позволяющий отправлять подтверждение об отвязке на сервер.
     * Сделал метод приватным, так как мы не будем вызывать его из вне.
     *
     * @param target цель, которой будет отправлен текст
     * @param event ивент, который передаётся в метод для получения дополнительных объектов в случае надобности
     * @return возвращаем объект интерфейса BaseComponent, который реализован через TextComponent. Этот блок будет отправлен игроку на сервере
     * и даст ему возможность отвязать или не отвязывать свой аккаунт
     */
    @SuppressWarnings("ConstantConditions")
    private BaseComponent unlinkComponents(Player target, PrivateMessageReceivedEvent event) {
        /*
        В этом методе всё абсолютно так же как и в методе linkComponents().
        Если забыл что-то - смотри в последний.
         */
        User sender = event.getAuthor();
        MessageChannel channel = event.getChannel();
        UNFINISHED.put(target, sender.getId());
        /* Если target не null - создаю объект форматтера
        Крайне важная хуйня. Позволяет избавиться от лишнего текста и прочего.
        Очень рекомендую использовать, если отправляется сообщение только одному игроку
         */
        Formatter form = new Formatter(target);
        form.sendMessage(GRAY + "Вы действительно хотите отвязать свой аккаунт?" +
                "\nВаши нынешние репутация и достижения останутся, тем не менее Вы больше не сможете пользоваться этим функционалом");

        TextComponent accept = new TextComponent("✔ Отвязать");
        TextComponent cancel = new TextComponent("⌀ Отмена");
        TextComponent slash = new TextComponent(" / ");

        accept.setColor(ChatColor.of(Formatter.tagRemove(GREEN))); // green
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Отвязать аккаунт"))));
        cancel.setColor(ChatColor.of(Formatter.tagRemove(RED))); // red
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Отменить отвязку"))));
        slash.setColor(ChatColor.of(Formatter.tagRemove(GRAY))); // gray

        LinkComponentCallback.execute(accept, player -> {
            /*
            Создаю объект member, и проверяю есть ли этот пользователь на нашем сервере
            Если его нет - возвращаю ошибку
             */
            if (GUILD == null) {
                UNFINISHED.remove(player);
                form.sendMessage(RED + "Не удалось получить ID гильдии. Сообщите модерации об этой проблеме");
                return;
            }

            /*
            Выполняю проверку на правильность созданных объектов сервера и роли.
            Если всё в порядке - игнорирую if
             */
            if (LINKED_ROLE == null) {
                UNFINISHED.remove(player);
                form.sendMessage(RED + "Не удалось завершить привязку. Подробности были отправлены в Дискорд");
                channel.sendMessageEmbeds(embedBuilder(sender,
                        "Мне не удалось выдать Вам роль на нашем Дискорд сервере. Обратитесь к модерации.\n" +
                                "Отвязка аккаунтов была отменена", CommandState.ERROR)).queue(null, ignored -> {});
                return;
            }

            /*
            В строке ниже я проверяю, завершил ли пользователь привязку.
            Если пользователь находится в коллекции UNFINISHED - делаю вид, что он ещё не нажал на кнопку
            и позволяю ему сделать это.
            По нажатии на кнопку все остальные кнопки станут недоступны, так как тоже содержат в себе эту строку.
             */
            if (!UNFINISHED.containsKey(player)) return;

            // Убираю роль при отвязке
            try {
                Member member = GUILD.retrieveMember(sender).complete();
                if (member == null) {
                    UNFINISHED.remove(player);
                    form.sendMessage(RED + "Мне не удалось найти Вас на нашем сервере");
                    return;
                }
                GUILD.removeRoleFromMember(member, LINKED_ROLE).queue();
            } catch (HierarchyException e) {
                UNFINISHED.remove(player);
                form.sendMessage(RED + "Не удалось завершить привязку. Подробности были отправлены в Дискорд");
                channel.sendMessageEmbeds(embedBuilder(sender,
                        "У меня недостаточно прав для выдачи роли привязанного игрока.\nОбратитесь к модерации: " + e.getStackTrace()[0], CommandState.ERROR))
                        .queue(null, ignored -> {});
                return;
            } catch (ErrorResponseException e) {
                UNFINISHED.remove(player);
                form.sendMessage(RED + "Не удалось отвязать аккаунт, так как мне не удалось найти Вас на нашем Дискорд сервере");
                return;
            }

            removePlayer(sender);
            channel.sendMessageEmbeds(embedBuilder(sender, "Ваш аккаунт был успешно отвязан. Жду не дождусь вновь его привязать!", CommandState.SUCCESS)).queue(null, ignored -> {});
            form.sendMessage(String.format(GRAY + "Дискорд %s был успешно отвязан от Вашего аккаунта", sender.getAsTag()));
            UNFINISHED.remove(player);
        });
        LinkComponentCallback.execute(cancel, player -> {
            /*
            В строке ниже я проверяю, завершил ли пользователь привязку.
            Если пользователь находится в коллекции UNFINISHED - делаю вид, что он ещё не нажал на кнопку
            и позволяю ему сделать это.
            По нажатии на кнопку все остальные кнопки станут недоступны, так как тоже содержат в себе эту строку.
             */
            if (!UNFINISHED.containsKey(player)) return;
            channel.sendMessageEmbeds(embedBuilder(sender,
                    "Отвязка аккаунтов была успешно отклонена. Можно ведь и не отвязывать вовсе :slight_smile:", CommandState.SUCCESS))
                    .queue(null, ignored -> {});
            form.sendMessage(GRAY + "Отвязка аккаунтов была успешно отменена");
            UNFINISHED.remove(player);
        });

        return new TextComponent(accept, slash, cancel);
    }





    // МЕТОДЫ ДЛЯ БАЗЫ ДАННЫХ, ДЯ!





    /**
     * Данный метод инициализирует таблицу для хранения привязанных игроков.
     * Следуя из этого он спокойно может быть статическим.
     * Крайне важно вызывать его перед инициализацией Дискорд бота,
     * поскольку бот не сможет вносить данные в бд, пока таблица для хранения не была инициализирована
     */
    public static void initTable() throws InitTableException {
        // тут нечего описать, просто получаю соединение с бд и вношу в неё таблицу, если та ещё не создана.
        try (Connection connection = Database.getConnection()) {
            final String QUERY = String.format("CREATE TABLE IF NOT EXISTS %s (uuid VARCHAR(36), display_name VARCHAR(16), discord_id DECIMAL(18,0));", LINK_TABLE);
            Statement statement = connection.createStatement();
            statement.execute(QUERY);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new InitTableException("Не удалось создать таблицу " + LINK_TABLE);
        }
    }

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
                statement.setString(1, target.getUniqueId().toString());
                statement.setObject(2, target.getDisplayName());
                statement.setObject(3, sender.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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

    /**
     * Данный метод позволяет мне проверить, находится ли игрок с указанным в параметре ником
     * в базе данных привязанных игроков. Если он находится в ней - true, иначе - false
     * @param displayName ник игрока, который должен находиться в бд
     * @return true или false. При SQLException возвращает false
     */
    private boolean isLinked(String displayName) {
        try (Connection connection = Database.getConnection()) {
            final String QUERY = String.format("SELECT * FROM %s WHERE display_name = '%s'", LINK_TABLE, displayName);
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(QUERY);
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
