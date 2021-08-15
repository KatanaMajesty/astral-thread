package com.astralsmp.world;

import com.astralsmp.AstralReforged;
import com.astralsmp.custom.AstralBlock;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.generator.BlockPopulator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public abstract class OrePopulator extends BlockPopulator {

    private final AstralReforged plugin;
    // c - максимальное количество залежей в одном чанке
    private int c;
    // Шанс заспавнить залежь
    private int chance;
    // Шанс продолжить залежь руды
    private int contChance;
    // максимальное количество блоков в залежи
    private int maxBlockCount;
    // руда генерируется от y1 до y2
    private int y1;
    private int y2;
    // Материал, в котором руда может спавнится
    /**
     * Если null - Material.STONE
     */
    @Nullable
    private Material locMat;
    // Блок, который будет генерироваться
    private AstralBlock genB;

    public AstralReforged getPlugin() {
        return plugin;
    }

    public int getC() {
        return c;
    }

    public void setMaxLodeC(int c) {
        this.c = c;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public int getContChance() {
        return contChance;
    }

    public void setContChance(int contChance) {
        this.contChance = contChance;
    }

    public int getMaxBlockCount() {
        return maxBlockCount;
    }

    public void setMaxBlockCount(int maxBlockCount) {
        this.maxBlockCount = maxBlockCount;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public @Nullable Material getLocMat() {
        return locMat;
    }

    public void setLocMat(@Nullable Material locMat) {
        this.locMat = locMat;
    }

    public @NotNull AstralBlock getGenB() {
        return genB;
    }

    public void setGenB(@NotNull AstralBlock genB) {
        this.genB = genB;
    }

    public OrePopulator(AstralReforged plugin) {
        this.plugin = plugin;
        init();
        if (genB == null) cancelPopulator("genB");
    }

    private static void cancelPopulator(String s) {
        throw new NullPointerException("Populator field is NULL: " + s);
    }

    public abstract void init();

    @Override
    public void populate(@NotNull World world, @NotNull Random random, @NotNull Chunk source) {
        int X, Y, Z;
        boolean con;
        for (int i = 0; i < c; i++) {  // Number of tries
            if (random.nextInt(100) < chance - 1) {  // The chance of spawning
                X = random.nextInt(15);
                Z = random.nextInt(15);
                Y = random.nextInt(30);  // Get randomized coordinates
                int c = 1;
                if (source.getBlock(X, Y, Z).getType() == locMat) {
                    con = true;
                    while (con) {
                        if (c > 7) return;
                        Block b = source.getBlock(X, Y, Z);
                        b.setType(Material.NOTE_BLOCK);
                        NoteBlock nb = (NoteBlock) b.getBlockData();
                        nb.setInstrument(genB.getInstrument());
                        nb.setNote(genB.getNote());
                        b.setBlockData(nb);
                        if (random.nextInt(100) < contChance - 1) {   // The chance of continuing the vein
                            switch (random.nextInt(6)) {  // The direction chooser
                                case 0 -> X++;
                                case 1 -> Y++;
                                case 2 -> Z++;
                                case 3 -> X--;
                                case 4 -> Y--;
                                case 5 -> Z--;
                            }
                            if (X < 0 || Z < 0 || X > 15 || Z > 15) return;
                            if (source.getBlock(X, Y, Z).getType() != locMat) return;
                            c++;
                        } else con = false;
                    }
                }
            }
        }
    }
}
