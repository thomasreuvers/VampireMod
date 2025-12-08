package com.vampiremod.damage;

import com.vampiremod.VampireMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;

public class ModDamageTags {
    // Custom tag used to mark sun-caused fire damage we apply to vampires.
    public static final ResourceLocation SUN_FIRE = new ResourceLocation(VampireMod.MOD_ID, "sun_fire");
}
