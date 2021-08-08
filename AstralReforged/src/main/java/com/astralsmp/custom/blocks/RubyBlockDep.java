package com.astralsmp.custom.blocks;

import com.astralsmp.AstralReforged;
import com.astralsmp.modules.BlockRelated;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.protocol.game.PacketPlayOutBlockBreakAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutCustomSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutRemoveEntityEffect;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.world.EnumHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Deprecated
public class RubyBlockDep implements Listener {

    private final Plugin plugin;
    private ItemStack rubyOre;

    private final int breakTime;
    private final Map<UUID, Location> B_BROKE = new HashMap<>();
    private final Map<UUID, BoundingBox> _bFrame = new HashMap<>();
    private static final ProtocolManager protocolManager = AstralReforged.protocolManager;
    private NBTTagCompound blockNbtTagCompound = null;

    private final String _blockName;

    // TODO: 06.08.2021 таймер ломания блока
    // TODO: 06.08.2021 тропинка под блоком
    // TODO: 06.08.2021 нельзя поставить блок, если выше идёт заменяемый блок
    // TODO: 06.08.2021 replaceable не работает
    public RubyBlockDep(Plugin plugin, int breakTime, String blockName) {
        // CUSTOM
        this._blockName = blockName;
        this.breakTime = breakTime;
        // INIT
        this.plugin = plugin;
        setRubyOre();
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,
                PacketType.Play.Server.BLOCK_ACTION,
                PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT
                        && event.getPacket().getSoundEffects().read(0) == Sound.BLOCK_NOTE_BLOCK_BANJO)
                    event.setCancelled(true);
                if (event.getPacketType() == PacketType.Play.Server.BLOCK_ACTION
                        && event.getPacket().getBlocks().read(0) == Material.NOTE_BLOCK) {
                    event.setCancelled(true);
                }
            }
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
                    Player player = event.getPlayer();
                    if (player.getGameMode() == GameMode.CREATIVE) return;
                    BlockPosition block = event.getPacket().getBlockPositionModifier().read(0);
                    Location loc = block.toLocation(player.getWorld());
                    Block posBlock = loc.getBlock();
                    if (posBlock.getType() != Material.NOTE_BLOCK && posBlock.getMetadata("ruby_ore").isEmpty())
                        return;

                    UUID uuid = player.getUniqueId();
                    EnumWrappers.PlayerDigType digEnum = event.getPacket().getPlayerDigTypes().read(0);
                    if (digEnum == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                        fatigueApply(player);
                        B_BROKE.put(uuid, loc);
                    }
                    if (digEnum == EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK
                            || digEnum == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                        blockBreakAnimPacket(posBlock, -1);
                        fatigueRemove(player);
                        B_BROKE.remove(uuid);
                        return;
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                while (B_BROKE.containsKey(uuid)) {
                                    if (B_BROKE.get(uuid) != loc) break;
                                    blockBreakProcPacket(loc, player);
                                    Thread.sleep(240);
                                }
                            } catch (InvocationTargetException | InterruptedException e) {
                                e.printStackTrace();
                            }
                            this.cancel();
                        }
                    }.runTaskAsynchronously(plugin);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            long cycleTime = TimeUnit.SECONDS.toMillis(breakTime / 10);
                            int i = 0;
                            try {
                                while (B_BROKE.containsKey(uuid) || i < 10) {
                                    if (B_BROKE.get(uuid) != loc) break;
                                    blockBreakAnimPacket(posBlock, i);
                                    Thread.sleep(cycleTime);
                                    i++;
                                    if (i == 9) destroyBlock(player, posBlock);
                                }
                                this.cancel();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    }.runTaskAsynchronously(plugin);
                }
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void setRubyOre() {
        rubyOre = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = rubyOre.getItemMeta();
        meta.setCustomModelData(9533);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fРубиновая руда"));
        rubyOre.setItemMeta(meta);
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(rubyOre);
        blockNbtTagCompound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
        blockNbtTagCompound.set("astral_block", NBTTagString.a(_blockName));
        nmsStack.setTag(blockNbtTagCompound);
        rubyOre = CraftItemStack.asBukkitCopy(nmsStack);
    }

    private static int getBlockEntityId(Block block) {
        return ((block.getX() & 0xFFF) << 20 | (block.getZ() & 0xFFF) << 8) | (block.getY() & 0xFF);
    }

    private static net.minecraft.core.BlockPosition getBlockPosition(Block block) {
        return new net.minecraft.core.BlockPosition(block.getX(), block.getY(), block.getZ());
    }

    private static void fatigueApply(Player player) {
        PacketPlayOutEntityEffect packetPlayOutEntityEffect = new PacketPlayOutEntityEffect(
                player.getEntityId(),
                new MobEffect(MobEffectList.fromId(4),
                32768,
                -1, false, false));
        ((CraftPlayer) player).getHandle().b.sendPacket(packetPlayOutEntityEffect);
    }

    private static void fatigueRemove(Player player) {
        PacketPlayOutRemoveEntityEffect packet = new PacketPlayOutRemoveEntityEffect(
                player.getEntityId(),
                MobEffectList.fromId(4));
        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }

    private void destroyBlock(Player player, Block block) throws ExecutionException, InterruptedException {
        Bukkit.getScheduler().callSyncMethod(plugin, () -> {
            player.breakBlock(block);
            return null;
        });
    }

    private void blockBreakAnimPacket(Block block, int i) {
        Bukkit.getScheduler().callSyncMethod(plugin, () -> {
            PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(
                    getBlockEntityId(block),
                    getBlockPosition(block),
                    i);
            Location loc = block.getLocation();
            for (Entity p : loc.getWorld().getNearbyEntities(loc, 12, 12, 12,
                    entity -> entity instanceof Player)) {
                ((CraftPlayer) p).getHandle().b.sendPacket(packet);
            }
            return null;
        });
    }

    private static void blockBreakProcPacket(Location loc, Player p) throws InvocationTargetException {
        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("block.amethyst_block.hit"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                0.25F, 0.4F);
        ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
    }

    private static void blockBreakPacket(Location loc, Player p) {
        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("block.amethyst_block.break"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                1F, 0.75F);
        ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
    }

    private static void blockWalkPacket(Location loc, Player p) {
        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("block.amethyst_block.step"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                0.15F, 1F);
        ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
    }

    private static void blockPlacePacket(Location loc, Player player, EnumHand enumHand) throws InvocationTargetException {
        int handInd = switch (enumHand) {
            case a -> 0;
            case b -> 3;
        };
        PacketContainer anim = protocolManager.createPacket(PacketType.Play.Server.ANIMATION, false);
        anim.getEntityModifier(player.getWorld()).write(0, player);
        anim.getIntegers().write(1, handInd);
        protocolManager.sendServerPacket(player, anim);

        PacketPlayOutCustomSoundEffect cSouEff = new PacketPlayOutCustomSoundEffect(
                new MinecraftKey("block.amethyst_block.place"),
                SoundCategory.d,
                new Vec3D(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                0.5F, 0.75F);
        for (Entity p : loc.getWorld().getNearbyEntities(loc, 16, 16, 16,
                entity -> entity instanceof Player)) {
            ((CraftPlayer) p).getHandle().b.sendPacket(cSouEff);
        }
    }

    private NBTTagCompound retrieveNBTCompound() {
        return blockNbtTagCompound.getCompound("astral_block");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        int offset = 1;
        Block aboveBlock = event.getBlock().getLocation().add(0, offset, 0).getBlock();
        if (aboveBlock.getType() == Material.NOTE_BLOCK) {
            while (aboveBlock.getType() == Material.NOTE_BLOCK) {
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

    @EventHandler
    public void onRubyWalk(PlayerMoveEvent e) {
        Location a = e.getFrom();
        Block block = a.add(0, -1, 0).getBlock();

        if (block.getType() != Material.NOTE_BLOCK && block.getMetadata("ruby_ore").isEmpty())
            return;

        if (e.getPlayer().getPose() == Pose.SNEAKING) return;
        UUID uuid = e.getPlayer().getUniqueId();
        if ((int)a.getX() - (int)e.getTo().getX() == 0
                && (int)a.getZ() - (int)e.getTo().getZ() == 0)
            return;

        double x = a.getX();
        double z = a.getZ();
        double r = 4;

        if (_bFrame.containsKey(uuid) && !_bFrame.get(uuid).contains(a.getX(), 0, a.getZ())) {
            _bFrame.remove(uuid);
            for (Entity p : a.getWorld().getNearbyEntities(a, 8, 8, 8,
                    entity -> entity instanceof Player)) {
                blockWalkPacket(a, (Player) p);
            }
        } else if (!_bFrame.containsKey(uuid)) {
            BoundingBox box = new BoundingBox(x + r, 0, z + r, x - r, 0, z - r);;
            _bFrame.put(uuid, box);
        }
    }

    @EventHandler
    public void onRubyOreBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        if (material == Material.NOTE_BLOCK) {
            Block block = event.getBlock();
            NoteBlock noteBlock = (NoteBlock) event.getBlock().getBlockData();
            if (noteBlock.getInstrument() == Instrument.BANJO
                    && noteBlock.getNote().equals(new Note(2))) {
                Location loc = block.getLocation();
                for (Entity p : loc.getWorld().getNearbyEntities(loc, 16, 16, 16,
                        entity -> entity instanceof Player)) {
                    blockBreakPacket(block.getLocation(), (Player) p);
                }

                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
                event.setDropItems(false);
                block.getWorld().dropItemNaturally(block.getLocation(), rubyOre);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNoteBlockClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            if (event.getPlayer().getPose() == Pose.SNEAKING
                    && event.getPlayer().getInventory().getItemInMainHand().getType().isBlock()
                    && !event.getPlayer().getInventory().getItemInMainHand().getType().isAir()) {
                event.setCancelled(false);
            }
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK
            && event.getClickedBlock().getType() == Material.NOTE_BLOCK) event.getPlayer().playSound(
                event.getClickedBlock().getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_HIT,
                0.5F, 0.2F);
    }

    @EventHandler
    public void onRubyInterrupt(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack itemMain = player.getInventory().getItemInMainHand();
        ItemStack itemOff = player.getInventory().getItemInOffHand();
        if (itemOff.getType().isBlock() && !itemOff.getType().isAir()
                && itemMain.isSimilar(rubyOre)) {
            event.setCancelled(true);
        }
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
        net.minecraft.world.item.ItemStack cmpMain = CraftItemStack.asNMSCopy(itemMain);
        net.minecraft.world.item.ItemStack cmpOff = CraftItemStack.asNMSCopy(itemOff);

        if (cmpMain.getTag() != null && cmpMain.getTag().getCompound("astral_block").equals(retrieveNBTCompound())) {
            hand = EnumHand.a;
            item = itemMain;
        } else if (cmpOff.getTag() != null
                && cmpOff.getTag().getCompound("astral_block").equals(retrieveNBTCompound())) {
            if (itemMain.getType().isBlock() && !itemMain.getType().isAir()) return;
            hand = EnumHand.b;
            item = itemOff;
        } else return;
        place(event, item, hand);
    }

    @EventHandler
    public void onBlockExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks())
            if (b.getType() == Material.NOTE_BLOCK) {
                NoteBlock noteBlock = (NoteBlock) b.getBlockData();
                Note n = noteBlock.getNote();
                Instrument i = noteBlock.getInstrument();
                if (n.equals(new Note(2)) && i == Instrument.BANJO
                        && !b.getMetadata("ruby_ore").isEmpty())
                    event.setCancelled(true);
            }
    }

    @EventHandler
    public void onBlockRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks())
            if (b.getType() == Material.NOTE_BLOCK) {
                NoteBlock noteBlock = (NoteBlock) b.getBlockData();
                Note n = noteBlock.getNote();
                Instrument i = noteBlock.getInstrument();
                if (n.equals(new Note(2)) && i == Instrument.BANJO
                        && !b.getMetadata("ruby_ore").isEmpty())
                    event.setCancelled(true);
            }
    }

    @SuppressWarnings("ConstantConditions")
    private void place(PlayerInteractEvent event, ItemStack item, EnumHand hand) {
        if (event.getClickedBlock().getType().isInteractable() && event.getPlayer().getPose() != Pose.SNEAKING)
            return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Block relative = block.getRelative(event.getBlockFace());
        for (int i = 0; i < 2; i++) {
            if (BlockRelated.isReplaceable(block.getType())) continue;
            if (!relative.getLocation().getWorld().getNearbyEntities(
                    relative.getRelative(0, i, 0).getLocation().add(0.5, 0, 0.5), 0.50, 0, 0.50,
                    entity -> entity instanceof LivingEntity)
                    .isEmpty()) {
                event.setCancelled(true);
                return;
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                net.minecraft.world.item.ItemStack cmpItem = CraftItemStack.asNMSCopy(item);
                Block posToPlace;
                if (BlockRelated.isReplaceable(block.getType()))
                    posToPlace = block;
                else posToPlace = relative;

                if (relative.getType() != Material.AIR && relative.getType() != Material.WATER  ) return;
                if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                    if (cmpItem.getTag() != null
                            && cmpItem.getTag().getCompound("astral_block").equals(retrieveNBTCompound()))
                        item.setAmount(item.getAmount() - 1);
                }
                try {
                    blockPlacePacket(posToPlace.getLocation(), player, hand);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                posToPlace.setType(Material.NOTE_BLOCK);
                posToPlace.setMetadata("ruby_ore", new FixedMetadataValue(plugin, "astral_block"));
                NoteBlock noteBlock = (NoteBlock) posToPlace.getBlockData();
                noteBlock.setInstrument(Instrument.BANJO);
                noteBlock.setNote(new Note(2));
                posToPlace.setBlockData(noteBlock);
            }
        }.runTask(plugin);
    }

}
