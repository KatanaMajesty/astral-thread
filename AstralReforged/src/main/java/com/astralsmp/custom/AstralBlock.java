package com.astralsmp.custom;

import com.astralsmp.api.PacketAPI;
import net.minecraft.core.EnumDirection;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;

public abstract class AstralBlock implements Listener {

    private static final PacketAPI api = new PacketAPI();

    private static final String CLASS_ID = "astral_block";
    private final Plugin plugin;
    private Instrument instrument;
    private Note note;
    @Nullable
    private Material material;
    private double breakTime;
    private AstralItem dropItem;
    private Integer dropCount;
    @Nullable
    private AstralItem defDropItem;
    @Nullable
    private Integer defDropCount;
    private boolean isSilkTouchable = false;
    @Nullable
    private AstralItem silkDropItem;
    @Nullable
    private Integer silkDropCount;
    private String placeSound;
    private String breakSound;
    private String hitSound;
    private String walkSound;
    private String fallSound;

    public AstralBlock(Plugin plugin) {
        this.plugin = plugin;
        init();
        plugin.getServer().getPluginManager().registerEvents(dropItem, plugin);
    }

    private void closeBlockCreation() {
        throw new NullPointerException(String.format("Field of BLOCK is Null at %s", getClass().getName()));
    }

    public void init() {
        if (instrument == null
                | (dropItem == null && defDropItem == null)
                | placeSound == null
                | breakSound == null
                | hitSound == null
                | walkSound == null
                | fallSound == null
                | dropCount == null) closeBlockCreation();
    }

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

    public double getBreakTime() {
        return breakTime;
    }

    public void setBreakTime(double breakTime) {
        this.breakTime = breakTime;
    }

    public void setDropItem(AstralItem dropItem) {
        this.dropItem = dropItem;
    }

    public void setDropCount(int dropCount) {
        this.dropCount = dropCount;
    }

    public void setPlaceSound(String placeSound) {
        this.placeSound = placeSound;
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
            if (main.getType().isBlock() && !main.getType().isAir()) {
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

    private boolean isIncorrectBlock(@NotNull Block b) {
//        if (b.getType() != Material.NOTE_BLOCK || b.getMetadata(CLASS_ID).isEmpty()) return true;
        if (b.getType() != Material.NOTE_BLOCK) return true;
        NoteBlock nb = (NoteBlock) b.getBlockData();
        return nb.getInstrument() != instrument || !nb.getNote().equals(note);
    }

    // ДЛЯ ТЕСТА! ПОТОМ ПЕРЕПИСАТЬ!
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
                        if (item.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS))
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

}
