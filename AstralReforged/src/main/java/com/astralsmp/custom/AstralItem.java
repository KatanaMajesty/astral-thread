package com.astralsmp.custom;

import com.astralsmp.api.PacketAPI;
import com.astralsmp.modules.BlockRelated;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.EnumHand;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

// TODO: 11.08.2021 Во время использования предмета из правой руки и попытки установки блока из левой - блок не должен устанавливаться (пофиксить)
public abstract class AstralItem implements Listener {

    private static PacketAPI api = new PacketAPI();

    static final String CLASS_ID = "astral_item";
    static final String CLASS_ID_B = "astral_placeable_item";
    private final Plugin plugin;
    private String itemName;
    private String nmsName;
    private boolean placeable = false;
    @Nullable private List<String> lore;
    private Integer customModelDataID;
    private String placeSound;
    private ItemStack item;
    private NBTTagCompound nbtTagCompound;
    private Instrument instrument;
    private Note note;

    public AstralItem(Plugin plugin) {
        this.plugin = plugin;
        init();
        createItem();
        if (itemName == null
                | nmsName == null
                | customModelDataID == null
                | (placeable && placeSound == null)) closeItemCreation();
    }

    private void closeItemCreation() {
        throw new NullPointerException(String.format("Field of ITEM is Null at %s", getClass().getName()));
    }

    public abstract void init();

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

    public String getPlaceSound() {
        return placeSound;
    }

    public void setPlaceSound(String placeSound) {
        this.placeSound = placeSound;
    }

    public Instrument getInstrument() {
        return instrument;
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
        nbtTagCompound = nmsItem.getTag() != null ? nmsItem.getTag() : new NBTTagCompound();
        nbtTagCompound.set(CLASS_ID, NBTTagString.a(nmsName));
        if (placeable) nbtTagCompound.set(CLASS_ID_B, NBTTagString.a("placeable"));
        nmsItem.setTag(nbtTagCompound);
        item = CraftItemStack.asBukkitCopy(nmsItem);
    }

    @EventHandler public void onAstralBlockPlace(@NotNull PlayerInteractEvent e) {
        if (!placeable || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        EnumHand hand;
        ItemStack item;
        if (isCorrectItem(main)) {
            hand = EnumHand.a;
            item = main;
        } else if (isCorrectItem(off)
                && e.getClickedBlock().getType() != Material.NOTE_BLOCK
                    && !isPlaceableCustom(main)) {
            hand = EnumHand.b;
            item = off;
        } else return;
        place(e, item, hand);
    }

    private void place(PlayerInteractEvent event, ItemStack item, EnumHand hand) {
        //
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Block relative = block.getRelative(event.getBlockFace());
        for (int i = 0; i < 2; i++) {
            if (BlockRelated.isReplaceable(block.getType())) continue;
            if (!checkForNearbyEntities(relative, 0.5, 0.5, 0.5)) {
                event.setCancelled(true);
                return;
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                Block posToPlace;
                if (BlockRelated.isReplaceable(block.getType()))
                    posToPlace = block;
                else posToPlace = relative;

                if (relative.getType() != Material.AIR && relative.getType() != Material.WATER) return;
                if (event.getPlayer().getGameMode() == GameMode.SURVIVAL && isCorrectItem(item))
                    item.setAmount(item.getAmount() - 1);
                runOnNearbyPlayers(posToPlace, 8, 8, 8,
                        p -> api.sendPacket(PacketAPI.blockPlaceSoundPacket(placeSound, posToPlace), p));
                api.sendPacket(PacketAPI.handSwingAnimationPacket(hand, player), player);

                posToPlace.setType(Material.NOTE_BLOCK);
                posToPlace.setMetadata(CLASS_ID, new FixedMetadataValue(plugin, nmsName));
                NoteBlock noteBlock = (NoteBlock) posToPlace.getBlockData();
                noteBlock.setInstrument(instrument);
                noteBlock.setNote(note);
                posToPlace.setBlockData(noteBlock);
            }
        }.runTask(plugin);
    }

    /**
     *
     * @param b блок
     * @param v1 дистанция по x
     * @param v2 дистанция по y
     * @param v3 дистанция по z
     * @return TRUE если никого нет рядом, FALSE если рядом есть хотя-бы 1 LivingEntity
     */
    private static boolean checkForNearbyEntities(Block b, double v1, double v2, double v3) {
        return b.getWorld().getNearbyEntities(b.getLocation().add(0.5, 0.5, 0.5), v1, v2, v3,
                entity -> entity instanceof LivingEntity).isEmpty();
    }

    private boolean isCorrectItem(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if (nmsItem.getTag() == null || !nmsItem.hasTag()) return false;
        NBTTagCompound nmsTag = nmsItem.getTag();
        if (nmsTag.get(CLASS_ID) == null) return false;
        return nmsTag.get(CLASS_ID).asString().equals(nmsName);
    }

    private static boolean isPlaceableCustom(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound t = nmsItem.getTag();
        return t != null
                && t.hasKey(CLASS_ID_B)
                && t.get(CLASS_ID_B).asString().equals("placeable");
    }

    private static void runOnNearbyPlayers(Block b, double v1, double v2, double v3, Consumer<Player> consumer) {
        for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().add(0.5, 0.5, 0.5), v1, v2, v3,
                entity -> entity instanceof Player)) {
            consumer.accept((Player) e);
        }
    }

}
