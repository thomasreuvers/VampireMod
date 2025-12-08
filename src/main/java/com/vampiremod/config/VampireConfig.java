package com.vampiremod.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VampireConfig {
    // Simple hardcoded defaults; can be replaced with a Forge config later.
    private static final Map<ResourceLocation, Integer> BLOOD_YIELD = new HashMap<>();
    private static final Set<ResourceLocation> BLACKLIST = new HashSet<>();

    static {
        // Defaults
        BLOOD_YIELD.put(new ResourceLocation("minecraft", "villager"), 20);
        BLOOD_YIELD.put(new ResourceLocation("minecraft", "cow"), 10);
        BLOOD_YIELD.put(new ResourceLocation("minecraft", "pig"), 8);
        BLOOD_YIELD.put(new ResourceLocation("minecraft", "sheep"), 8);
        BLOOD_YIELD.put(new ResourceLocation("minecraft", "horse"), 12);
        BLOOD_YIELD.put(new ResourceLocation("minecraft", "player"), 15);

        // Undrinkable baseline set
        String[] blacklist = new String[]{
                "minecraft:skeleton", "minecraft:stray", "minecraft:wither_skeleton",
                "minecraft:zombie", "minecraft:husk", "minecraft:drowned", "minecraft:zombie_villager",
                "minecraft:creeper",
                "minecraft:enderman", "minecraft:endermite",
                "minecraft:warden",
                "minecraft:ghast",
                "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin",
                "minecraft:blaze",
                "minecraft:iron_golem", "minecraft:snow_golem",
                "minecraft:shulker",
                "minecraft:silverfish",
                "minecraft:slime", "minecraft:magma_cube",
                "minecraft:phantom",
                "minecraft:ender_dragon", "minecraft:wither", "minecraft:elder_guardian",
                "minecraft:skeleton_horse", "minecraft:zombie_horse"
        };
        for (String id : blacklist) {
            BLACKLIST.add(new ResourceLocation(id));
        }
    }

    public static int getDefaultBlood(EntityType<?> type) {
        ResourceLocation id = EntityType.getKey(type);
        return BLOOD_YIELD.getOrDefault(id, 8);
    }

    public static boolean isBlacklisted(EntityType<?> type) {
        ResourceLocation id = EntityType.getKey(type);
        return BLACKLIST.contains(id);
    }
}
