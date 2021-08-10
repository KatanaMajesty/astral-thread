package com.astralsmp.custom;

import net.md_5.bungee.api.ChatColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.EnumHand;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public abstract class AstralItem implements Listener {

    static final String CLASS_ID = "astral_item";
    private Plugin plugin;
    private String itemName;
    private String nmsName;
    private boolean placeable = false;
    @Nullable private List<String> lore;
    private Integer customModelDataID;

    private ItemStack item;
    private NBTTagCompound nbtTagCompound;

    public AstralItem(Plugin plugin) {
        this.plugin = plugin;
        init();
        createItem();
    }

    private void closeItemCreation() {
        throw new NullPointerException(String.format("Field of ITEM is Null at %s", getClass().getName()));
    }

    public void init() {
        if (itemName == null
                | nmsName == null
                | customModelDataID == null) closeItemCreation();
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getNmsName() {
        return nmsName;
    }

    public void setNmsName(String nmsName) {
        this.nmsName = nmsName;
    }

    @Nullable
    public List<String> getLore() {
        return lore;
    }

    public void setLore(@Nullable List<String> lore) {
        this.lore = lore;
    }

    public Integer getCustomModelDataID() {
        return customModelDataID;
    }

    public void setCustomModelDataID(Integer customModelDataID) {
        this.customModelDataID = customModelDataID;
    }

    public ItemStack getItem() {
        return item;
    }

    public NBTTagCompound getNbtTagCompound() {
        return nbtTagCompound.getCompound(CLASS_ID);
    }

    public boolean isPlaceable() {
        return placeable;
    }

    public void setPlaceable(boolean placeable) {
        this.placeable = placeable;
    }

    private void createItem() {
        item = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f" + itemName));
        meta.setCustomModelData(customModelDataID);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        NBTTagProcessing();
    }

    private void NBTTagProcessing() {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        nbtTagCompound = nmsItem.hasTag() ? nmsItem.getTag() : new NBTTagCompound();
        nbtTagCompound.set(CLASS_ID, NBTTagString.a(nmsName));
        nmsItem.setTag(nbtTagCompound);
        item = CraftItemStack.asBukkitCopy(nmsItem);
    }

    @EventHandler public void onAstralBlockPlace(@NotNull PlayerInteractEvent e) {
        if (!placeable || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        System.out.println(2);
        Player p = e.getPlayer();
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        NBTTagCompound nmsMain = CraftItemStack.asNMSCopy(main).getTag();
        NBTTagCompound nmsOff = CraftItemStack.asNMSCopy(off).getTag();
        EnumHand hand;
        ItemStack item;
        if (nmsMain != null && nmsMain.getCompound(AstralItem.CLASS_ID).equals(getNbtTagCompound())) {
            hand = EnumHand.a;
            item = main;
        } else if (nmsOff != null && nmsOff.getCompound(AstralItem.CLASS_ID).equals(getNbtTagCompound())
                && !(main.getType().isBlock() && !main.getType().isAir())) {
            hand = EnumHand.b;
            item = off;
        } else return;
    }

}
