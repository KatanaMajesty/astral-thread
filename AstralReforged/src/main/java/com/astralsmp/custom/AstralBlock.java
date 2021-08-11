package com.astralsmp.custom;

import com.astralsmp.AstralReforged;
import com.astralsmp.api.PacketAPI;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.world.EnumHand;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AstralBlock implements Listener {

    private static final ProtocolManager protocol = AstralReforged.protocolManager;
    private static final PacketAPI api = new PacketAPI();
    private static final Map<UUID, BoundingBox> moveBox = new HashMap<>();
    private static final Map<UUID, BlockPosition> breakMap = new HashMap<>();

    static final String CLASS_ID = "astral_block";
    private final Plugin plugin;
    private Instrument instrument;
    private Note note;
    @Nullable
    private Material material;
    private double hardness;
    private AstralItem dropItem;
    private Integer dropCount;
    @Nullable
    private AstralItem defDropItem;
    @Nullable
    private Integer defDropCount;
    private boolean isFortunable = false;
    private boolean isSilkTouchable = false;
    @Nullable
    private AstralItem silkDropItem;
    @Nullable
    private Integer silkDropCount;
    private String breakSound;
    private String hitSound;
    private String walkSound;
    private String fallSound;

    public AstralBlock(Plugin plugin) {
        this.plugin = plugin;
        init();
        if (instrument == null
                | (dropItem == null && defDropItem == null)
                | breakSound == null
                | hitSound == null
                | walkSound == null
                | fallSound == null
                | dropCount == null) closeBlockCreation();
        plugin.getServer().getPluginManager().registerEvents(dropItem, plugin);
        if (silkDropItem != null) plugin.getServer().getPluginManager().registerEvents(silkDropItem, plugin);
        if (defDropItem != null) plugin.getServer().getPluginManager().registerEvents(defDropItem, plugin);
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
                        breakMap.remove(uuid);
                        return;
                    }
                    if (posBlock.getType() != Material.NOTE_BLOCK || isIncorrectBlock(posBlock))
                        return;
                    if (digEnum == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                        event.setCancelled(true);
                        breakMap.put(uuid, blockPosition);
                        api.sendPacket(PacketAPI.fatigueApplyPacket(p), p);
                    }
                    breakSoundThread(blockPosition, posBlock, p, uuid).runTaskAsynchronously(plugin);
                    breakAnimationThread(p, uuid, blockPosition, posBlock).runTaskAsynchronously(plugin);
                }
            }
        });
    }

    private void closeBlockCreation() {
        throw new NullPointerException(String.format("Field of BLOCK is Null at %s", getClass().getName()));
    }

    public abstract void init();

    public Plugin getPlugin() {return this.plugin;}

    public Instrument getInstrument() {
        return this.instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public double getHardness() {
        return hardness;
    }

    public void setHardness(double hardness) {
        this.hardness = hardness;
    }

    public void setDropItem(AstralItem dropItem) {
        this.dropItem = dropItem;
    }

    public void setDropCount(int dropCount) {
        this.dropCount = dropCount;
    }

    public void setBreakSound(String breakSound) {
        this.breakSound = breakSound;
    }

    public void setHitSound(String hitSound) {
        this.hitSound = hitSound;
    }

    public void setWalkSound(String walkSound) {
        this.walkSound = walkSound;
    }

    public void setFallSound(String fallSound) {
        this.fallSound = fallSound;
    }

    public boolean isSilkTouchable() {
        return isSilkTouchable;
    }

    public void setSilkTouchable(boolean silkTouchable) {
        isSilkTouchable = silkTouchable;
    }

    @Nullable
    public AstralItem getDefDropItem() {
        return defDropItem;
    }

    public void setDefDropItem(AstralItem defDropItem) {
        this.defDropItem = defDropItem;
    }

    @Nullable
    public Integer getDefDropCount() {
        return defDropCount;
    }

    public void setDefDropCount(int defDropCount) {
        this.defDropCount = defDropCount;
    }

    @Nullable
    public AstralItem getSilkDropItem() {
        return silkDropItem;
    }

    public void setSilkDropItem(@Nullable AstralItem silkDropItem) {
        this.silkDropItem = silkDropItem;
    }

    @Nullable
    public Integer getSilkDropCount() {
        return silkDropCount;
    }

    public void setSilkDropCount(@Nullable Integer silkDropCount) {
        this.silkDropCount = silkDropCount;
    }

    public boolean isFortunable() {
        return isFortunable;
    }

    public void setFortunable(boolean fortunable) {
        isFortunable = fortunable;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onBlockPhysics(BlockPhysicsEvent event) {
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
        Block b = event.getBlock();
        if (b.getType() == Material.NOTE_BLOCK)
            event.setCancelled(true);
        if (b.getType().toString().toLowerCase().contains("sign"))
            return;
        event.getBlock().getState().update(true, false);
    }

    @EventHandler public void onAstralBlockClick(@NotNull PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getType() == Material.NOTE_BLOCK) {
            e.setCancelled(true);
            if (isIncorrectBlock(e.getClickedBlock())) return;
            Player p = e.getPlayer();
            ItemStack main = p.getInventory().getItemInMainHand();
            if ((main.getType().isBlock() && !main.getType().isAir())) {
                noteBlockPlaceableAgain(p, e, main, EnumHand.a);
            }
        }
    }

    @EventHandler public void onAstralBlockBreak(@NotNull BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.NOTE_BLOCK) {
            Block b = e.getBlock();
            if (isIncorrectBlock(b)) return;
            e.setDropItems(false);
            for (Entity p : b.getWorld().getNearbyEntities(b.getLocation(),
                    16, 16, 16,
                    entity -> entity instanceof Player)) {
                api.sendPacket(PacketAPI.blockBreakSoundPacket(breakSound, b), (Player) p);
            }
            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) getCorrectDrop(e.getPlayer(), b);
        }
    }

    @EventHandler public void onAstralBlockWalk(@NotNull PlayerMoveEvent e) {
        Location l = e.getFrom();
        Block b = l.add(0, -1, 0).getBlock();
        if (b.getType() != Material.NOTE_BLOCK || isIncorrectBlock(b) || e.getPlayer().getPose() == Pose.SNEAKING)
            return;
        UUID uuid = e.getPlayer().getUniqueId();
        Location l2 = e.getTo();
        if ((int) l.getX() - (int) l2.getX() == 0
                && (int) l.getZ() - (int) l2.getZ() == 0)
            return;

        double x = l.getX();
        double z = l.getZ();
        double r = 4;

        if (moveBox.containsKey(uuid) && !moveBox.get(uuid).contains(l.getX(), 0, l.getZ())) {
            moveBox.remove(uuid);
            for (Entity p : b.getWorld().getNearbyEntities(l, 8, 8, 8,
                    entity -> entity instanceof Player)) {
                api.sendPacket(PacketAPI.blockWalkSoundPacket(walkSound, b), (Player) p);
            }
        } else if (!moveBox.containsKey(uuid)) {
            BoundingBox box = new BoundingBox(x + r, 0, z + r, x - r, 0, z - r);;
            moveBox.put(uuid, box);
        }
    }

    @EventHandler public void onAstralBlockPistonExtract(@NotNull BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (b.getType() == Material.NOTE_BLOCK && !isIncorrectBlock(b)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler public void onAstralBlockPistonRetract(@NotNull BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (b.getType() == Material.NOTE_BLOCK && !isIncorrectBlock(b)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private static void noteBlockPlaceableAgain(Player p, PlayerInteractEvent e, ItemStack item, EnumHand hand) {
        Block clicked = e.getClickedBlock();
        BlockFace f = e.getBlockFace();
        EnumDirection d = null;
        switch (f) {
            case DOWN -> d = EnumDirection.a;
            case UP -> d = EnumDirection.b;
            case NORTH -> d = EnumDirection.c;
            case SOUTH -> d = EnumDirection.d;
            case WEST -> d = EnumDirection.e;
            case EAST -> d = EnumDirection.f;
        }
        assert d != null;
        CraftPlayer craftPlayer = (CraftPlayer) p;
        net.minecraft.world.item.ItemStack nmsMain = CraftItemStack.asNMSCopy(item);
        Block rel = clicked.getRelative(f);
        if (isUnPlaceableLoc(rel)) return;
        if (containsEntity(rel)) return;
        nmsMain.placeItem(new ItemActionContext(
                craftPlayer.getHandle(), hand,
                MovingObjectPositionBlock.a(
                        new Vec3D(clicked.getX(), clicked.getY(), clicked.getZ()),
                        d, PacketAPI.getBlockPosition(clicked))
        ), hand);
        if (!isUnPlaceableLoc(rel)) return;
        net.minecraft.world.level.block.Block b = ((CraftBlock) rel).getNMS().getBlock();
        SoundEffectType sType = b.getStepSound(null);
        api.sendPacket(PacketAPI.blockPlaceSoundPacket(sType.getPlaceSound(), clicked.getRelative(f)), p);
    }

    private static boolean isUnPlaceableLoc(Block block) {
        return block.getType() != Material.AIR && block.getType() != Material.WATER && block.getType() != Material.LAVA;
    }

    private static boolean containsEntity(Block block) {
        if (!isUnPlaceableLoc(block))
            if (!block.getLocation().getWorld().getNearbyEntities(
                    block.getLocation().add(0.5, 0.5, 0.5), 0.5, 0, 0.5).isEmpty())
                return true;
        return false;
    }

    // Переделать
    private boolean isIncorrectBlock(@NotNull Block b) {
        if (b.getType() != Material.NOTE_BLOCK)
//                || (!b.getMetadata(AstralItem.CLASS_ID).get(0).asString().equals(dropItem.getNmsName())
//                && !(defDropItem != null && b.getMetadata(AstralItem.CLASS_ID).get(0).asString().equals(defDropItem.getNmsName()))
//                && !(silkDropItem != null && b.getMetadata(AstralItem.CLASS_ID).get(0).asString().equals(silkDropItem.getNmsName()))))
            return true;
        NoteBlock nb = (NoteBlock) b.getBlockData();
        return nb.getInstrument() != instrument || !nb.getNote().equals(note);
    }

    private void getCorrectDrop(Player p, Block b) {
        String[] arr = {"pickaxe", "_axe", "shovel", "hoe"};
        ItemStack item = p.getInventory().getItemInMainHand();
        String pItem = item.toString().toLowerCase();
        String requiredItem = this.material == null ? "null" : this.material.toString().toLowerCase();
        World w = b.getWorld();
        ItemStack drop;
        for (String s : arr) {
            if (pItem.contains(s) && requiredItem.contains(s)) {
                if (getToolHierarchy(item.getType()) >= getToolHierarchy(material)) {
                    if (isSilkTouchable && item.containsEnchantment(Enchantment.SILK_TOUCH) && silkDropItem != null) {
                        drop = silkDropItem.getItem();
                        drop.setAmount(silkDropCount == null ? 1 : silkDropCount);
                    } else {
                        drop = dropItem.getItem();
                        drop.setAmount(dropCount);
                        if (isFortunable && item.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS))
                            drop.setAmount(fortuneMultiplier(item.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS), drop.getAmount()));
                    }
                }
                else if (defDropItem != null) {
                    drop = defDropItem.getItem();
                    drop.setAmount(defDropCount == null ? 1 : defDropCount);
                } else return;
                w.dropItemNaturally(b.getLocation(), drop);
                return;
            }
        }
        if (requiredItem.equals("null")) {
            drop = dropItem.getItem();
            drop.setAmount(dropCount);
            w.dropItemNaturally(b.getLocation(), drop);
        }
    }

    /**
     * Чем больше число иерархии, тем сильнее инструмент
     * @param m
     * @return
     */
    private static int getToolHierarchy(Material m) {
        String s = m.toString().toLowerCase();
        int iHierarchy = 0;
        if (s.contains("pickaxe")) {
            switch (m) {
                case WOODEN_PICKAXE, GOLDEN_PICKAXE -> iHierarchy = 1;
                case STONE_PICKAXE -> iHierarchy = 2;
                case IRON_PICKAXE -> iHierarchy = 3;
                case DIAMOND_PICKAXE -> iHierarchy = 4;
                case NETHERITE_PICKAXE -> iHierarchy = 5;
            }
        } else if (s.contains("axe")) {
            switch (m) {
                case WOODEN_AXE, GOLDEN_AXE -> iHierarchy = 1;
                case STONE_AXE -> iHierarchy = 2;
                case IRON_AXE -> iHierarchy = 3;
                case DIAMOND_AXE -> iHierarchy = 4;
                case NETHERITE_AXE -> iHierarchy = 5;
            }
        } else if (s.contains("shovel")) {
            switch (m) {
                case WOODEN_SHOVEL, GOLDEN_SHOVEL -> iHierarchy = 1;
                case STONE_SHOVEL -> iHierarchy = 2;
                case IRON_SHOVEL -> iHierarchy = 3;
                case DIAMOND_SHOVEL -> iHierarchy = 4;
                case NETHERITE_SHOVEL -> iHierarchy = 5;
            }
        } else if (s.contains("hoe")) {
            switch (m) {
                case WOODEN_HOE, GOLDEN_HOE -> iHierarchy = 1;
                case STONE_HOE  -> iHierarchy = 2;
                case IRON_HOE -> iHierarchy = 3;
                case DIAMOND_HOE -> iHierarchy = 4;
                case NETHERITE_HOE -> iHierarchy = 5;
            }
        }
        return iHierarchy;
    }

    private static int getToolSpeed(Material m) {
        String s = m.toString().toLowerCase();
        int toolSpeed;
        if (!s.contains("axe") && !s.contains("shovel") && !s.contains("hoe")) return 1;
        if (s.contains("wooden")) toolSpeed = 2;
        else if (s.contains("stone")) toolSpeed = 4;
        else if (s.contains("iron")) toolSpeed = 6;
        else if (s.contains("diamond")) toolSpeed = 8;
        else if (s.contains("netherite")) toolSpeed = 9;
        else if (s.contains("gold")) toolSpeed = 12;
        else return 1;
        return toolSpeed;
    }

    private static int fortuneMultiplier(int lvl, int d) {
        final int defWeight = 2;
        Random rd = new Random();
        for (int i = 2; i < defWeight + lvl; i++) {
            if (rd.nextBoolean()) {
                d += i - 1;
                break;
            }
        }
        return d;
    }

    private double realBreakTime(Player p) {
        String[] arr = {"pickaxe", "_axe", "shovel", "hoe"};
        ItemStack it = p.getInventory().getItemInMainHand();
        Material m = it.getType();
        String pItem = it.toString().toLowerCase();
        String requiredItem = this.material == null ? "null" : this.material.toString().toLowerCase();
        double speedMultiplier = 1;
        double damage;
        boolean canHarv = false;
        boolean isBestTool = false;
        for (String s : arr) {
            if (pItem.contains(s) && requiredItem.contains(s)) {
                canHarv = true;
                break;
            }
        }
        if (getToolHierarchy(m) >= (material == null ? 0 : getToolHierarchy(material))) isBestTool = true;

        if (isBestTool) {
            speedMultiplier = getToolSpeed(m);
            if (!canHarv) speedMultiplier = 1;
            if (it.containsEnchantment(Enchantment.DIG_SPEED))
                speedMultiplier += it.getEnchantmentLevel(Enchantment.DIG_SPEED) ^ 2 + 1;
        }
        if (p.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {
            speedMultiplier *= 1 + (0.2 * p.getPotionEffect(PotionEffectType.FAST_DIGGING).getAmplifier());
        }

        if (p.getLocation().getBlock().getType() == Material.WATER && !p.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
            speedMultiplier /= 5;
        }
//        Material onGr = p.getLocation().add(0, -0.5, 0).getBlock().getType();
//        if (onGr == Material.AIR || onGr == Material.LAVA) {
//            speedMultiplier /= 5;
//        }
        damage = speedMultiplier / hardness;
        if (canHarv) damage /= 30;
        else damage /= 100;
        if (damage > 1) return 0;
        double ticks = Math.floor(1 / damage);
        return ticks / 20;
    }

    private BukkitRunnable breakSoundThread(BlockPosition blockPosition, Block posBlock, Player p, UUID uuid) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    while (breakMap.containsKey(uuid)) {
                        if (breakMap.get(uuid) != blockPosition) break;
                        api.sendPacket(PacketAPI.blockHitSoundPacket(breakSound, posBlock), p);
                        Thread.sleep(240);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.cancel();
            }
        };
    }

    private BukkitRunnable breakAnimationThread(Player p, UUID uuid, BlockPosition blockPosition, Block posBlock) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    double millis = realBreakTime(p) * 1000;
                    long b = (long) millis;
                    long cycle = b / 9;
                    byte dS = 0;
                    do {
                        Thread.sleep(cycle);
                        if (breakMap.get(uuid) != blockPosition) break;
                        byte finalDS = dS;
                        Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                            runOnNearbyPlayers(posBlock, 30, 30, 30,
                                    player -> api.sendPacket(PacketAPI.blockBreakAnimationPacket(posBlock, finalDS), player));
                            return null;
                        });
                        if (dS == 9)
                            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                                api.receiveDigPacket(PacketAPI.blockDigProcessPacket(PacketPlayInBlockDig.EnumPlayerDigType.c, posBlock), p);
                                p.spawnParticle(
                                        Particle.BLOCK_CRACK,
                                        posBlock.getLocation().add(0.5, 0.5, 0.5),
                                        30, 0.1, 0.1, 0.1, 0.5,
                                        posBlock.getBlockData());
                                p.breakBlock(posBlock);
                                return null;
                            });
                        dS++;
                    } while (breakMap.containsKey(uuid) && dS < 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.cancel();
            }
        };
    }

    private void runOnNearbyPlayers(Block b, double v1, double v2, double v3, Consumer<Player> consumer) {
        for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().add(0.5, 0.5, 0.5), v1, v2, v3,
                entity -> entity instanceof Player)) {
            consumer.accept((Player) e);
        }
    }
}
