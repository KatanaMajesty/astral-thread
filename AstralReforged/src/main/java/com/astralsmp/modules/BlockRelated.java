package com.astralsmp.modules;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class BlockRelated {

    public static final List<Material> REP_BLOCKS = new ArrayList<>();
    public static final List<Material> WOOD_BLOCKS = new ArrayList<>();

    private enum WoodenBlocks {
        NOTE_BLOCK,
    }

    private enum ReplaceableBlocks {
        TALL_GRASS,
        GRASS,
        SEAGRASS,
        TALL_SEAGRASS,
        FERN,
        DEAD_BUSH,
        AIR,
        HANGING_ROOTS,
        WARPED_ROOTS,
        CRIMSON_ROOTS,
        GLOW_LICHEN,
        LARGE_FERN,
        VINE,
        SNOW
    }

    public static void initWoodenArray() {
//        for (WoodenBlocks w : WoodenBlocks.values()) {
//            WOOD_BLOCKS.add(Material.valueOf(w.name()));
//        }
        for (Material m : Material.values()) {
            String check = m.toString().toLowerCase();
            if (check.endsWith("planks")) WOOD_BLOCKS.add(m);
            if (check.endsWith("log")) WOOD_BLOCKS.add(m);
            if (check.endsWith("wood")) WOOD_BLOCKS.add(m);
            if (check.contains("bed")) WOOD_BLOCKS.add(m);
            if (check.contains("bee")) WOOD_BLOCKS.add(m);
            if (check.contains("smithing_table")) WOOD_BLOCKS.add(m);
            if (check.contains("crafting_table")) WOOD_BLOCKS.add(m);
            if (check.contains("fletching_table")) WOOD_BLOCKS.add(m);
            if (check.contains("chest")) WOOD_BLOCKS.add(m);
            if (check.contains("campfire")) WOOD_BLOCKS.add(m);
            boolean b = check.contains("potted") || check.contains("leaves") || check.endsWith("sapling");
            if (check.contains("oak") && !b) WOOD_BLOCKS.add(m);
            if (check.contains("spruce") && !b) WOOD_BLOCKS.add(m);
            if (check.contains("birch") && !b) WOOD_BLOCKS.add(m);
            if (check.contains("jungle") && !b) WOOD_BLOCKS.add(m);
            if (check.contains("acacia") && !b) WOOD_BLOCKS.add(m);
            if (check.contains("dark_oak") && !b) WOOD_BLOCKS.add(m);
        }
        System.out.println(WOOD_BLOCKS);
    }

    public static void initReplaceableArray() {
        for (ReplaceableBlocks r : ReplaceableBlocks.values()) {
            REP_BLOCKS.add(Material.valueOf(r.name()));
        }
        System.out.println(REP_BLOCKS);
    }

    public static boolean isReplaceable(Material m) {
        return REP_BLOCKS.contains(m);
    }

    public static boolean isWood(Material m) {
        return WOOD_BLOCKS.contains(m);
    }

}
