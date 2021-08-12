package com.astralsmp.custom.blocks;

import com.astralsmp.api.PacketAPI;
import com.astralsmp.custom.AstralBlock;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class AspenLog extends AstralBlock {
    public AspenLog(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        com.astralsmp.custom.items.AspenLog aspenLog = new com.astralsmp.custom.items.AspenLog(getPlugin());
        setDefDropItem(aspenLog);
        setDefDropCount(1);
        setDropItem(aspenLog);
        setDropCount(1);
        setMaterial(Material.WOODEN_AXE);
        setHardness(2);
        setInstrument(Instrument.BANJO);
        setNote(new Note(3));
        setBreakSound("custom.block.wood.break");
        setFallSound("custom.block.wood.hit");
        setHitSound("custom.block.wood.hit");
        setWalkSound("custom.block.wood.step");
    }

    @Override
    public void packetListener() {
        protocol.addPacketListener(new PacketAdapter(getPlugin(), ListenerPriority.NORMAL,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,
                PacketType.Play.Server.BLOCK_ACTION,
                PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT
                        && event.getPacket().getSoundEffects().read(0) == Sound.BLOCK_NOTE_BLOCK_BANJO)
                    event.setCancelled(true);
                if (event.getPacketType() == PacketType.Play.Server.BLOCK_ACTION
                        && event.getPacket().getBlocks().read(0) == Material.NOTE_BLOCK)
                    event.setCancelled(true);
            }
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
                    System.out.println(1);
                    Player p = event.getPlayer();
                    if (p.getGameMode() == GameMode.CREATIVE) return;
                    BlockPosition blockPosition = event.getPacket().getBlockPositionModifier().read(0);
                    Block posBlock = blockPosition.toLocation(p.getWorld()).getBlock();
                    EnumWrappers.PlayerDigType digEnum = event.getPacket().getPlayerDigTypes().read(0);
                    UUID uuid = p.getUniqueId();
                    if (digEnum == EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK
                            || digEnum == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                        // мб это можно лучше реализовать, хз
                        Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                            for (Entity e : posBlock.getWorld().getNearbyEntities(posBlock.getLocation(), 30, 30, 30,
                                    entity -> entity instanceof Player)) {
                                api.sendPacket(PacketAPI.blockBreakAnimationPacket(posBlock, (byte) -1), (Player) e);
                            }
                            return null;
                        });
                        api.sendPacket(PacketAPI.fatigueRemovePacket(p), p);
                        AstralBlock.breakMap.remove(uuid);
                        return;
                    }
                    if (posBlock.getType() != Material.NOTE_BLOCK || isIncorrectBlock(posBlock))
                        return;
                    if (digEnum == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                        event.setCancelled(true);
                        AstralBlock.breakMap.put(uuid, blockPosition);
                        api.sendPacket(PacketAPI.fatigueApplyPacket(p), p);
                    }
                    breakSoundThread(blockPosition, posBlock, p, uuid).runTaskAsynchronously(plugin);
                    breakAnimationThread(p, uuid, blockPosition, posBlock).runTaskAsynchronously(plugin);
                }
            }
        });
    }
}
