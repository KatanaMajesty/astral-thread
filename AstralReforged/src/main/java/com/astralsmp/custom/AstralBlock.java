package com.astralsmp.custom;

import com.astralsmp.api.PacketAPI;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumHand;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class AstralBlock implements Listener {

    private final PacketAPI api = new PacketAPI();

    private static final String CLASS_ID = "astral_block";
    private Plugin plugin;
    private Instrument instrument;
    private Note note;
    private Material material;
    private double breakTime;
    private AstralItem dropItem;
    private int dropCount;
    private String placeSound;
    private String breakSound;
    private String hitSound;
    private String walkSound;
    private String fallSound;

    public AstralBlock(Plugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void closeBlockCreation() {
        throw new NullPointerException(String.format("Field of BLOCK is Null at %s", getClass().getName()));
    }

    public void init() {
        if (instrument == null
                | material == null
                | dropItem == null
                | placeSound == null
                | breakSound == null
                | hitSound == null
                | walkSound == null
                | fallSound == null) closeBlockCreation();
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

    public void setNote(int note) {
        this.note = Note.natural(note, Note.Tone.A);
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

    @EventHandler public void onAstralBlockBreak(@NotNull BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.NOTE_BLOCK) {
            Block b = e.getBlock();
            NoteBlock nb = (NoteBlock) e.getBlock().getBlockData();
            if (nb.getInstrument() != instrument && nb.getNote() != note) return;
            e.setDropItems(false);
            World w = b.getLocation().getWorld();
            assert w != null;
            w.dropItemNaturally(b.getLocation(), dropItem.getItem());
        }
    }

    @EventHandler public void onAstralBlockPlace(@NotNull PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        NBTTagCompound nmsMain = CraftItemStack.asNMSCopy(main).getTag();
        NBTTagCompound nmsOff = CraftItemStack.asNMSCopy(off).getTag();
        EnumHand hand;
        ItemStack item;
        if (nmsMain != null && nmsMain.getCompound(AstralItem.CLASS_ID).equals(dropItem.getNbtTagCompound())) {
            hand = EnumHand.a;
            item = main;
        } else if (nmsOff != null && nmsOff.getCompound(AstralItem.CLASS_ID).equals(dropItem.getNbtTagCompound())
                && !(main.getType().isBlock() && !main.getType().isAir())) {
            hand = EnumHand.b;
            item = off;
        } else return;
    }

}
