package com.astralsmp.modules;

import com.astralsmp.AstralReforged;
import com.astralsmp.modules.BlockRelated;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.minecraft.network.protocol.game.PacketPlayOutCustomSoundEffect;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class WoodRestore implements Listener {

    private final Map<UUID, Location> B_BROKE = new HashMap<>();
    private final Map<UUID, BoundingBox> _bFrame = new HashMap<>();
    private final ProtocolManager protocolManager = AstralReforged.protocolManager;
    private final Plugin _plugin;

    public WoodRestore(Plugin plugin) {
        this._plugin = plugin;
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
                    Player player = event.getPlayer();
                    if (player.getGameMode() == GameMode.CREATIVE) return;
                    BlockPosition block = event.getPacket().getBlockPositionModifier().read(0);
                    Location loc = block.toLocation(player.getWorld());
                    Block posBlock = loc.getBlock();
                    if (!BlockRelated.isWood(posBlock.getType())) return;

                    UUID uuid = player.getUniqueId();
                    EnumWrappers.PlayerDigType digEnum = event.getPacket().getPlayerDigTypes().read(0);

                    if (digEnum == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) B_BROKE.put(uuid, loc);
                    if (digEnum == EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK
                            || digEnum == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                        B_BROKE.remove(uuid);
                        return;
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            while (B_BROKE.containsKey(uuid)) {
                                try {
                                    if (B_BROKE.get(uuid) != loc) break;
                                    woodBreakProcPacket(loc, player);
                                    Thread.sleep(240);
                                } catch (InvocationTargetException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            this.cancel();
                        }
                    }.runTaskAsynchronously(plugin);
                }
            }
        });
    }

    private static void woodBreakProcPacket(Location loc, Player p) throws InvocationTargetException {
        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("custom.block.wood.hit"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                0.25F, 0.4F);
        ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
    }

    private static void woodBreakPacket(Location loc, Player p) {
        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("custom.block.wood.break"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                1F, 0.75F);
        ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
    }

    private static void woodPlacePacket(Location loc, Player p) {
        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("custom.block.wood.place"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                0.5F, 0.75F);
        ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
    }

    private static void woodWalkPacket(Location loc, Player p) {
        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("custom.block.wood.step"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                0.15F, 1F);
        ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
    }

    @EventHandler
    public void onWoodBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (BlockRelated.isWood(b.getType()))
            for (Entity p : b.getWorld().getNearbyEntities(b.getLocation(), 8, 8, 8,
                    entity -> entity instanceof Player)) {
                woodBreakPacket(b.getLocation(), (Player) p);
            }
    }

    @EventHandler
    public void onWoodPlace(BlockPlaceEvent event) {
        Block b = event.getBlock();
        if (BlockRelated.isWood(b.getType()))
            for (Entity p : b.getWorld().getNearbyEntities(b.getLocation(), 8, 8, 8,
                    entity -> entity instanceof Player)) {
                woodPlacePacket(b.getLocation(), (Player) p);
            }
    }

    @EventHandler
    public void onWoodWalk(PlayerMoveEvent e) {
        Location a = e.getFrom();
        Block block = a.add(0, -1, 0).getBlock();
        if (!BlockRelated.isWood(block.getType())) return;
        if (e.getPlayer().getPose() == Pose.SNEAKING) return;
        UUID uuid = e.getPlayer().getUniqueId();
        if ((int)a.getX() - (int)e.getTo().getX() == 0
                && (int)a.getZ() - (int)e.getTo().getZ() == 0)
            return;

        double x = a.getX();
        double z = a.getZ();
        double r = 8;

        if (_bFrame.containsKey(uuid) && !_bFrame.get(uuid).contains(a.getX(), 0, a.getZ())) {
            _bFrame.remove(uuid);
            for (Entity p : a.getWorld().getNearbyEntities(a, 8, 8, 8,
                    entity -> entity instanceof Player)) {
                woodWalkPacket(a, (Player) p);
            }
        } else if (!_bFrame.containsKey(uuid)) {
            BoundingBox box = new BoundingBox(x + r, 0, z + r, x - r, 0, z - r);;
            _bFrame.put(uuid, box);
        }
    }
}
