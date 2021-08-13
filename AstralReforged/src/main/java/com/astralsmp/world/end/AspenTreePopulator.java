package com.astralsmp.world.end;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.generator.BlockPopulator;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class AspenTreePopulator extends BlockPopulator {

    @Override
    public void populate(@NotNull World world, @NotNull Random random, @NotNull Chunk chunk) {
        int x,y,z;
        for (int i = 0; i < 2; i++) {
            if (random.nextInt(100) < 20) {
                x = random.nextInt(15);
                z = random.nextInt(15);
                y = 90;
                while (chunk.getBlock(x,y,z).getType() != Material.END_STONE && y > 60) {
                    if (chunk.getBlock(x,y,z).getBiome() != Biome.SMALL_END_ISLANDS) return;
                    y--;
                }
                Block b = chunk.getBlock(x,y,z).getRelative(BlockFace.UP);
                if (b.getRelative(BlockFace.DOWN).getType() == Material.END_STONE) makeStem(b);
                else return;
                decorateStem(b, random);
                int k = 0;
                do {
                    k++;
                    makeStem(b.getRelative(BlockFace.UP, k));
                } while(k < random.nextInt(3) + 4);
                b = b.getRelative(BlockFace.UP, k);
                switch (random.nextInt(4)) {
                    case 0 -> b = b.getRelative(BlockFace.SOUTH);
                    case 1 -> b = b.getRelative(BlockFace.NORTH);
                    case 2 -> b = b.getRelative(BlockFace.EAST);
                    case 3 -> b = b.getRelative(BlockFace.WEST);
                }
                k = 0;
                do {
                    k++;
                    makeStem(b.getRelative(BlockFace.UP, k));
                    b.getRelative(BlockFace.UP, k);
                } while(k <= random.nextInt(3) + 3);
                b = b.getRelative(BlockFace.UP, k + 1);
                k = 0;
                // генерим ниже блока генерации
                for (int j = 0; j < 6; j++) {
                    if (j < 4) {
                        createSquareCrown(2, b.getRelative(BlockFace.DOWN, j));
                        fillSquareCrown(1, b.getRelative(BlockFace.DOWN, j), random);
                    }
                    else createSquareCrownEmpty(2, b.getRelative(BlockFace.DOWN, j), random);
                }
                // генерим выше блока генерации
                for (int j = 0; j < 4; j++) {
                    fillSquareCrown(1, b.getRelative(BlockFace.UP, j));
                    createSquareCrown(2, b.getRelative(BlockFace.UP, j), random);
                }
                // генерю верхушку
                for (int j = 4; j < 8; j++) {
                    if (j < 6) fillSquareCrown(1, b.getRelative(BlockFace.UP, j));
                    else {
                        makeCrown(b.getRelative(BlockFace.UP, j));
                        createSquareCrown(1, b.getRelative(BlockFace.UP, j), random);
                    }
                }

//                fillSquareCrown(1, b.getRelative(BlockFace.DOWN), random);
//                fillSquareCrown(1, b.getRelative(BlockFace.DOWN, 2), random);
//                createSquareCrown(2, b.getRelative(BlockFace.DOWN));
//                createSquareCrown(2, b.getRelative(BlockFace.DOWN, 2));
//                createSquareCrown(2, b.getRelative(BlockFace.DOWN, 3));
//                createSquareCrownEmpty(2, b.getRelative(BlockFace.DOWN, 4), random);
//                createSquareCrownEmpty(2, b.getRelative(BlockFace.DOWN, 5), random);
            }
        }
    }

    private static void createSquareCrown(int d, Block b) {
        int r = d + d + 1;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                int xm = Math.abs(b.getRelative(d - i, 0, d - j).getX() - b.getX());
                int zm = Math.abs(b.getRelative(d - i, 0, d - j).getZ() - b.getZ());
                if (xm == d || zm == d) {
                    if (xm == d && zm == d) continue;
                    makeCrown(b.getRelative(d - i, 0, d - j));
                }

            }
        }
    }

    // рандом вверх
    private static void createSquareCrown(int d, Block b, Random random) {
        int r = d + d + 1;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                Block rb = b.getRelative(d - i, 0, d - j);
                int xm = Math.abs(rb.getX() - b.getX());
                int zm = Math.abs(rb.getZ() - b.getZ());
                if (xm == d || zm == d) {
                    if (xm == d && zm == d
                            || random.nextInt(100) > 85
                            || rb.getRelative(BlockFace.DOWN).getType() == Material.AIR) continue;
                    makeCrown(b.getRelative(d - i, 0, d - j));
                }

            }
        }
    }

    // рандом вниз
    private static void createSquareCrownEmpty(int d, Block b, Random random) {
        int r = d + d + 1;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                Block rb = b.getRelative(d - i, 0, d - j);
                int xm = Math.abs(rb.getX() - b.getX());
                int zm = Math.abs(rb.getZ() - b.getZ());
                if (xm == d || zm == d) {
                    if ((xm == d && zm == d) ||
                            random.nextInt(100) > 60 ||
                            rb.getRelative(BlockFace.UP).getType() == Material.AIR) continue;
                    makeCrown(rb);
                }

            }
        }
    }

    // Полный топ
    @SuppressWarnings("all")
    private static void fillSquareCrown(int d, Block b) {
        int r = d + d + 1;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                makeCrown(b.getRelative(d - i, 0, d-j));
            }
        }
    }

    // Рандом топ
    @SuppressWarnings("all")
    private static void fillSquareCrown(int d, Block b, Random random) {
        int r = d + d + 1;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                Block rb = b.getRelative(d - i, 0, d-j);
                boolean center = rb.getX() == b.getX() && rb.getZ() == b.getZ();
                if (random.nextInt(100) > 85
                        && rb.getRelative(BlockFace.UP).getType() == Material.AIR
                        && !center || rb.getRelative(BlockFace.DOWN).getType() == Material.AIR)
                    continue;
                makeCrown(rb);
            }
        }
    }

    private static void decorateStem(Block b, Random random) {
        Block r;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                switch (random.nextInt(4)) {
                    case 0 -> r = b.getRelative(BlockFace.SOUTH).getRelative(BlockFace.UP, i);
                    case 1 -> r = b.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP, i);
                    case 2 -> r = b.getRelative(BlockFace.EAST).getRelative(BlockFace.UP, i);
                    case 3 -> r = b.getRelative(BlockFace.WEST).getRelative(BlockFace.UP, i);
                    default -> r = null;
                }
                if (r.getRelative(BlockFace.DOWN).getType() != Material.AIR && random.nextInt(100) < 70)
                    makeStem(r);
            }
        }
    }

    private static void makeStem(Block b) {
        b.setType(Material.NOTE_BLOCK);
        NoteBlock nb = (NoteBlock) b.getBlockData();
        nb.setNote(new Note(3));
        nb.setInstrument(Instrument.BANJO);
        b.setBlockData(nb);
    }

    private static void makeCrown(Block b) {
        b.setType(Material.NOTE_BLOCK);
        NoteBlock nb = (NoteBlock) b.getBlockData();
        nb.setNote(new Note(2));
        nb.setInstrument(Instrument.BANJO);
        b.setBlockData(nb);
    }
}
