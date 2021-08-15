//package com.astralsmp.world.end;
//
//import org.bukkit.*;
//import org.bukkit.block.Biome;
//import org.bukkit.block.Block;
//import org.bukkit.block.BlockFace;
//import org.bukkit.block.data.type.NoteBlock;
//import org.bukkit.generator.BlockPopulator;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Random;
//
//public class EumusPopulator extends BlockPopulator {
//    @Override
//    public void populate(@NotNull World world, @NotNull Random random, @NotNull Chunk chunk) {
////        int x = random.nextInt(16), , z = random.nextInt(16);
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                int y = 90;
//                Block b;
//                while (chunk.getBlock(x,y,z).getType() != Material.END_STONE && y > 55) {
//                    y--;
//                }
//                if (chunk.getBlock(x, y, z).getBiome() != Biome.END_BARRENS) {
//                    int count = 0;
//                    for (Chunk c : getNeighbouringChunks(chunk)) {
//                        if (c.getBlock(8, 0, 8).getBiome() == Biome.END_BARRENS) {
//                            int ly = 90;
//                            while (chunk.getBlock(x,y,z).getType() != Material.END_STONE && ly > 55) {
//                                ly--;
//                            }
//                            Block b2 = c.getBlock(x,y,z);
//                            if (b2.getType() != Material.AIR) b2.setType(Material.END_STONE_BRICKS);
//                            count++;
//                        }
//                        if (count == 0) return;
//                    }
//                }
//                b = chunk.getBlock(x,y,z);
//                if (b.getType() != Material.AIR) placeEumus(b);
//            }
//        }
//
//
////        for (Chunk c : getNeighbouringChunks(chunk)) {
////            if () {
////
////            }
////        }
//    }
//
//    private static void placeEumus(Block b) {
//        b.setType(Material.NOTE_BLOCK);
//        NoteBlock nb = (NoteBlock) b.getBlockData();
//        nb.setNote(new Note(2));
//        nb.setInstrument(Instrument.BANJO);
//        b.setBlockData(nb);
//    }
//
//    private static Chunk[] getNeighbouringChunks(Chunk chunk) {
//        int x = chunk.getX(), z = chunk.getZ();
//        return new Chunk[]
//                {
//                    chunk.getWorld().getChunkAt(x+1, z),
//                    chunk.getWorld().getChunkAt(x-1, z),
//                    chunk.getWorld().getChunkAt(x, z+1),
//                    chunk.getWorld().getChunkAt(x, z-1)
//                };
//    }
//
//}
