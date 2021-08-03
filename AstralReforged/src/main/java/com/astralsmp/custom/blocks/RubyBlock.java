package com.astralsmp.custom.blocks;

import com.astralsmp.AstralReforged;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.world.EnumHand;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class RubyBlock implements Listener {

    private final Plugin plugin;
    private ItemStack rubyOre;
    private static final List<Material> REP_BLOCKS = new ArrayList<>();
    private static ProtocolManager protocolManager = AstralReforged.protocolManager;

    public RubyBlock(Plugin plugin) {
        this.plugin = plugin;
        setRubyOre();
        initArray();
    }

    enum ReplaceableBlocks {
        TALL_GRASS,
        GRASS,
        SEAGRASS,
        FERN,
        DEAD_BUSH,
        AIR,
        HANGING_ROOTS,
        WARPED_ROOTS,
        CRIMSON_ROOTS,
        GLOW_LICHEN,
        LARGE_FERN,
        VINE
    }

    public static void blockPlacePacket(Player player, EnumHand enumHand) throws InvocationTargetException {
        int handInd = switch (enumHand) {
            case a -> 0;
            case b -> 3;
        };
        PacketContainer anim = protocolManager.createPacket(PacketType.Play.Server.ANIMATION, false);
        anim.getEntityModifier(player.getWorld()).write(0, player);
        anim.getIntegers().write(1, handInd);
        protocolManager.sendServerPacket(player, anim);

//        CraftPlayer craftPlayer = (CraftPlayer) player;
//        Location loc = player.getLocation();
//        String s = "block.amethyst_block.place";
//        MinecraftKey key = new MinecraftKey(s);
//        float vol = 1f;
//        float pitch = 1f;
//        PacketPlayOutNamedSoundEffect sound = new PacketPlayOutNamedSoundEffect(
//                SoundEffectType.a.c(),
//                SoundCategory.a, loc.getX(), loc.getBlockY(), loc.getBlockZ(), vol, pitch);
//        craftPlayer.getHandle().b.sendPacket(sound);

        PacketContainer sound = protocolManager.createPacket(PacketType.Play.Server.NAMED_SOUND_EFFECT, false);
        Location loc = player.getLocation();
//        sound.getModifier().writeDefaults();

        sound.getSoundEffects().write(0, Sound.BLOCK_AMETHYST_BLOCK_PLACE);
        sound.getSoundCategories().write(0, EnumWrappers.SoundCategory.PLAYERS);
        sound.getIntegers()
                .write(0, loc.getBlockX())
                .write(1, loc.getBlockY())
                .write(2, loc.getBlockZ());
        sound.getFloat().write(0, 1F);
        sound.getFloat().write(1, 1F);

        protocolManager.sendServerPacket(player, sound);
    }

    private static void initArray() {
        for (ReplaceableBlocks r : ReplaceableBlocks.values()) {
            REP_BLOCKS.add(Material.valueOf(r.name()));
        }
        System.out.println(REP_BLOCKS);
    }

    private void setRubyOre() {
        rubyOre = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = rubyOre.getItemMeta();
        meta.setCustomModelData(9533);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bРубиновая руда"));
        rubyOre.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        int offset = 1;
        Block aboveBlock = event.getBlock().getLocation().add(0, offset, 0).getBlock();
        if (aboveBlock.getType() == Material.NOTE_BLOCK) {
            while(aboveBlock.getType() == Material.NOTE_BLOCK){
                event.setCancelled(true);
                aboveBlock.getState().update(true, true);
                offset++;
                aboveBlock = event.getBlock().getLocation().add(0, offset, 0).getBlock();
            }
        }
        if (event.getBlock().getType() == Material.NOTE_BLOCK)
            event.setCancelled(true);
        if (event.getBlock().getType().toString().toLowerCase().contains("sign"))
            return;
        event.getBlock().getState().update(true, false);
    }

    /*
    Блок должен соответствовать инструменту BANJO. Нота - 2
     */
    @EventHandler
    public void onRubyOreBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        if (material == Material.NOTE_BLOCK) {
            Block block = event.getBlock();
            NoteBlock noteBlock = (NoteBlock) event.getBlock().getBlockData();
            if (noteBlock.getInstrument() == Instrument.BANJO && noteBlock.getNote().equals(new Note(2))) {
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
                event.setDropItems(false);
                block.getWorld().dropItemNaturally(block.getLocation(), rubyOre);
            }
        }
    }

    // Тык по нотному блоку нельзя!!!
    @EventHandler(priority = EventPriority.HIGH)
    public void onNoteBlockClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock().getType() != Material.NOTE_BLOCK
                && event.getPlayer().getPose() != Pose.SNEAKING
                && event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR)
            event.setCancelled(true);
    }

    @EventHandler
    public void onRubyOrePlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;

        Player player = event.getPlayer();
        ItemStack itemMain = player.getInventory().getItemInMainHand();
        ItemStack itemOff = player.getInventory().getItemInOffHand();
        EnumHand hand;
        ItemStack item;

        if (itemMain.isSimilar(rubyOre)) {
            hand = EnumHand.a;
            item = itemMain;
        } else if (itemOff.isSimilar(rubyOre)) {
            hand = EnumHand.b;
            item = itemOff;
        } else return;
        place(event, item, hand);
    }

    public void place(PlayerInteractEvent event, ItemStack item, EnumHand hand) {
        if (event.getClickedBlock().getType().isInteractable() && event.getPlayer().getPose() != Pose.SNEAKING)
            return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Block relative = block.getRelative(event.getBlockFace());
        for (int i = 0; i < 2; i++) {
            if (!relative.getLocation().getWorld().getNearbyEntities(
                    relative.getRelative(0, i, 0).getLocation(), 0.51, 0, 0.51,
                    entity -> entity instanceof LivingEntity)
                    .isEmpty()) {
                event.setCancelled(true);
                return;
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                Block posToPlace;
                if (REP_BLOCKS.contains(block.getType()))
                    posToPlace = block;
                else posToPlace = relative;

                if (relative.getType() != Material.AIR) return;
                if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                    if (item.isSimilar(rubyOre)) item.setAmount(item.getAmount() - 1);
                }
                try {
                    blockPlacePacket(player, hand);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                player.playSound(posToPlace.getLocation(), "block.amethyst_block.place", 1F, 1F);

                posToPlace.setType(Material.NOTE_BLOCK);
                NoteBlock noteBlock = (NoteBlock) posToPlace.getBlockData();
                noteBlock.setInstrument(Instrument.BANJO);
                noteBlock.setNote(new Note(2));
                posToPlace.setBlockData(noteBlock);
            }
        }.runTaskLater(plugin, 2L);
    }
}
