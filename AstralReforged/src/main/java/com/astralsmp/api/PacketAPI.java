package com.astralsmp.api;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particle;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PacketAPI {

    public void sendPacket(Packet<PacketListenerPlayOut> packet, Player player) {
        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }

    public void receiveDigPacket(PacketPlayInBlockDig packet, Player player) {
        ((CraftPlayer) player).getHandle().b.a(packet);
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
    public static PacketPlayOutEntityEffect fatigueApplyPacket(Player player) {
        return new PacketPlayOutEntityEffect(
                player.getEntityId(),
                new MobEffect(MobEffectList.fromId(4),
                        32768,
                        -1, false, false));
    }

    /**
     * Этот метод позволяет снять пакет с эффектом усталости с игрока.
     * Негативного эффекта не будет, если снимать нечего
     *
     * @param player Игрок, которому нужно снять эффект усталости
     */
    public static PacketPlayOutRemoveEntityEffect fatigueRemovePacket(Player player) {
        return new PacketPlayOutRemoveEntityEffect(
                player.getEntityId(),
                MobEffectList.fromId(4));
    }

    public static PacketPlayOutBlockBreakAnimation blockBreakAnimationPacket(Block block, byte destroyStage) {
        return new PacketPlayOutBlockBreakAnimation(
                getBlockEntityId(block),
                getBlockPosition(block),
                destroyStage);
    }

    public static PacketPlayOutCustomSoundEffect blockHitSoundPacket(String sound, Block block) {
        return new PacketPlayOutCustomSoundEffect(
                new MinecraftKey(sound),
                SoundCategory.d,
                new Vec3D(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5),
                0.15F, 1F);
    }

    public static PacketPlayOutCustomSoundEffect blockBreakSoundPacket(String sound, Block block) {
        return new PacketPlayOutCustomSoundEffect(
                new MinecraftKey(sound),
                SoundCategory.d,
                new Vec3D(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5),
                1F, 0.75F);
    }

    public static PacketPlayOutCustomSoundEffect blockWalkSoundPacket(String sound, Block block) {
        return new PacketPlayOutCustomSoundEffect(
                new MinecraftKey(sound),
                SoundCategory.d,
                new Vec3D(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5),
                0.15F, 1F);
    }

    public static PacketPlayOutCustomSoundEffect blockPlaceSoundPacket(String sound, Block block) {
        return new PacketPlayOutCustomSoundEffect(
                new MinecraftKey(sound),
                SoundCategory.d,
                new Vec3D(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5),
                1F, 0.75F);
    }

    public static PacketPlayOutCustomSoundEffect blockPlaceSoundPacket(SoundEffect soundEffect, Block block) {
        return new PacketPlayOutCustomSoundEffect(
                soundEffect.a(),
                SoundCategory.d,
                new Vec3D(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5),
                1F, 0.75F);
    }

    // blockFallSoundPacket?

    public static PacketPlayOutAnimation handSwingAnimationPacket(EnumHand enumHand, Player player) {
        int handInd = switch (enumHand) {
            case a -> 0;
            case b -> 3;
        };
        return new PacketPlayOutAnimation(
                ((CraftPlayer) player).getHandle(),
                handInd);
    }

    public static PacketPlayInBlockDig blockDigProcessPacket(PacketPlayInBlockDig.EnumPlayerDigType enumeration, Block b) {
        BlockPosition bp = ((CraftBlock) b).getPosition();
        return new PacketPlayInBlockDig(enumeration, bp, EnumDirection.a);
    }

    public static int getBlockEntityId(Block block) {
        return ((block.getX() & 0xFFF) << 20 | (block.getZ() & 0xFFF) << 8) | (block.getY() & 0xFF);
    }

    public static BlockPosition getBlockPosition(Block block) {
        return new BlockPosition(block.getX(), block.getY(), block.getZ());
    }

}
