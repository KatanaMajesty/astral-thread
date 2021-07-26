package com.astralsmp.events;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class LinkComponentCallback implements Listener {

    private static final Map<UUID, Consumer<Player>> EVENT_MAP = new HashMap<>();
    private static final String CMD = "/astralthread:link";

    @EventHandler
    public void linkCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().startsWith(CMD)) return;
        String[] args = event.getMessage().split(" ");
        if (args.length == 2 && args[1].split("-").length == 5) {
            UUID uuid = UUID.fromString(args[1]);
            Consumer<Player> consumer = EVENT_MAP.remove(uuid);
            if (consumer != null) consumer.accept(event.getPlayer());
            event.setCancelled(true);
        }
    }

    public static void execute(TextComponent component, Consumer<Player> consumer) {
        UUID uuid = UUID.randomUUID();
        EVENT_MAP.put(uuid, consumer);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, CMD + " " + uuid));
    }

}
