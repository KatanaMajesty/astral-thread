package com.astralsmp.world.end;

import org.bukkit.*;
import org.bukkit.generator.BlockPopulator;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class EumusPopulator extends BlockPopulator {
    @Override
    public void populate(@NotNull World world, @NotNull Random random, @NotNull Chunk chunk) {
        if (random.nextInt(100) < 5) {
            // TODO: 12.08.2021 завершить 
        }
    }
}
