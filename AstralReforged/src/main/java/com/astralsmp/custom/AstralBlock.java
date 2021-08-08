package com.astralsmp.custom;

import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutRemoveEntityEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public abstract class AstralBlock implements Listener {

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
        throw new NullPointerException(String.format("Field is Null at %s", getClass().getName()));
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

    /*
    Возможно, более целесообразно выдавать эффект усталости при входе на сервер. Таким образом, я могу избавиться от лишних
    передач пакетов между сервером и клиентом, в дальнейшем оптимизируя нагрузку плагина на последние.
     */

    /**
     * Данный метод позволяет отправлять пакет с эффектом усталости на сервер.
     * Я использую это в новой системе ломания блоков. Есть некоторые минусы данной системы.
     * К примеру, древние стражи более не могут накладывать немоту на игрока. Может быть, в будущем я попытаюсь это решить.
     * Усталость длится **.** и выдаётся -1 уровня - таким образом я не меняю визуальный эффект ломания блока, тем не менее
     * попытки сломать его четны.
     *
     * @param player Игрок, которому будет отправлен эффект усталости
     */
    public void fatiguePacketSend(Player player) {
        PacketPlayOutEntityEffect packetPlayOutEntityEffect = new PacketPlayOutEntityEffect(
                player.getEntityId(),
                new MobEffect(MobEffectList.fromId(4),
                        32768,
                        -1, false, false));
        ((CraftPlayer) player).getHandle().b.sendPacket(packetPlayOutEntityEffect);
    }

    /**
     * Этот метод позволяет снять пакет с эффектом усталости с игрока.
     * Негативного эффекта не будет, если снимать нечего
     *
     * @param player Игрок, которому нужно снять эффект усталости
     */
    private void fatiguePacketRemove(Player player) {
        PacketPlayOutRemoveEntityEffect packet = new PacketPlayOutRemoveEntityEffect(
                player.getEntityId(),
                MobEffectList.fromId(4));
        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }

    @EventHandler public void onAstralBlockPlace(PlayerInteractEvent e) {

    }

}
