package com.astralsmp.commands;

import com.astralsmp.AstralThread;
import com.astralsmp.annotations.AstralCommand;
import com.astralsmp.events.LinkComponentCallback;
import com.astralsmp.modules.Database;
import com.astralsmp.modules.Discord;
import net.dv8tion.jda.api.JDA;
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

public class LinkingSystem extends ListenerAdapter {

    private static final JDA jda = Discord.jda;
    private static final char PREFIX = Discord.PREFIX;
    private static final String LINK_TABLE = AstralThread.LINK_TABLE;

    public static void initTable() {
        try (Connection connection = Database.getConnection()) {
            final String QUERY = String.format("CREATE TABLE IF NOT EXISTS %s (uuid UUID, display_name VARCHAR(16), discord_id DECIMAL(18,0));", LINK_TABLE);
            Statement statement = connection.createStatement();
            statement.execute(QUERY);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split(" ");
        switch (args[0]) {
            case PREFIX + "привязать" -> {
                // привязка
                onLinkCommand(event, args);
            }
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

    // привязка
    @AstralCommand
    public void onLinkCommand(final PrivateMessageReceivedEvent event, String[] args) {
        if (args.length != 2) {
            System.out.println("мало аргументов");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            target.spigot().sendMessage(linkComponents(target, event));
        } else System.out.println("игрок не в сети");
    }

    private BaseComponent linkComponents(Player target, PrivateMessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        User author = event.getAuthor();
        target.sendMessage(String.format("Отправлен запрос на привязку аккаунта к Дискорду %s", author.getAsTag()));

        TextComponent accept = new TextComponent("да");
        TextComponent cancel = new TextComponent("нет");
        TextComponent spam = new TextComponent("спам");
        TextComponent slash = new TextComponent(" / ");

        accept.setColor(ChatColor.GREEN);
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Привязать аккаунт")));
        cancel.setColor(ChatColor.RED);
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Отменить привязку")));
        spam.setColor(ChatColor.YELLOW);
        spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Обозначить спамом")));
        slash.setColor(ChatColor.GRAY);

        LinkComponentCallback.execute(accept, player -> {
            insertPlayer(target, author);
            channel.sendMessage("привязка завершена. Всё заебись").queue();
        });
        LinkComponentCallback.execute(cancel, player -> {
            channel.sendMessage("привязка отклонена").queue();
        });
        LinkComponentCallback.execute(spam, player -> {
            channel.sendMessage("привязка обозначена спамом").queue();
        });

        return new TextComponent(accept, slash, cancel, slash, spam);
    }

    // отвязка
    @AstralCommand
    public void onUnlinkCommand(final PrivateMessageReceivedEvent event, String[] args) throws SQLException {
        if (args.length != 1) {
            System.out.println("мало аргументов");
            return;
        }
        String playerName = getLinkedPlayerName(event.getAuthor());
        if (playerName == null) {
            System.out.println("Не в бд");
            return;
        }
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            target.spigot().sendMessage(unlinkComponents(target, event));
        } else System.out.println("Игрок не в сети");
    }

    private BaseComponent unlinkComponents(Player target, PrivateMessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();

        TextComponent accept = new TextComponent("отвязать");
        TextComponent cancel = new TextComponent("отмена");
        TextComponent slash = new TextComponent(" / ");

        accept.setColor(ChatColor.GREEN);
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Отвязать аккаунт")));
        cancel.setColor(ChatColor.RED);
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Отменить отвязку")));
        slash.setColor(ChatColor.GRAY);

        LinkComponentCallback.execute(accept, player -> {
            channel.sendMessage("отвязан").queue();
            removePlayer(event.getAuthor());
        });
        LinkComponentCallback.execute(cancel, player -> {
            channel.sendMessage("Отменено").queue();
        });

        return new TextComponent(accept, slash, cancel);
    }

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
    private String getLinkedPlayerName(User sender) {
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
