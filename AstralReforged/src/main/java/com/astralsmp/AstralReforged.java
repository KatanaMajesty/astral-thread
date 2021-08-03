package com.astralsmp;

import com.astralsmp.custom.blocks.RubyBlock;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.minecraft.network.protocol.game.PacketPlayInBlockPlace;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumHand;
import net.minecraft.world.level.block.SoundEffectType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class AstralReforged extends JavaPlugin implements Listener {

    public static ProtocolManager protocolManager = null;

    @Override
    public void onEnable() {

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {

                if (event.getPacketType() == PacketType.Play.Server.ANIMATION
                  && (event.getPacket().getIntegers().read(1) == 1
                        | event.getPacket().getIntegers().read(1) == 3)) {
                    event.setCancelled(true);
                }
            }
        });
        for (Field f : PacketType.Play.Server.NAMED_SOUND_EFFECT.getPacketClass().getDeclaredFields()) {
            System.out.println(f);
        }
        getServer().getPluginManager().registerEvents(new RubyBlock(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Плагин включён");
    }


}
