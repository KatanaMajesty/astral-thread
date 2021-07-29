package com.astralsmp.modules;

import java.util.HashMap;
import java.util.Map;

public class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();
    public static final int DEFAULT_COOLDOWN = 120;

    public void setCooldown(String discordID, long cooldown) {
        if (cooldown < 1) cooldowns.remove(discordID);
        else cooldowns.put(discordID, cooldown);
    }

    public long getCooldown(String discordID) {
        return cooldowns.getOrDefault(discordID, 0L);
    }

    public static int[] splitTimeArray(long seconds)
    {
        // Например seconds = 123
        int hours = (int) seconds / 3600; // hours = 0
        int remainder = (int) seconds % 3600; // остаток = 123
        int mins = remainder / 60; // 123 / 60 = 2 минуты
        remainder = remainder % 60; // остаток - 3
        int secs = remainder; // 3
        return new int[]{hours , mins , secs};
    }

}
