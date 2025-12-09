package com.vampiremod.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Base interface for abilities.
 */
public interface Ability {
    /** Unique identifier for this ability */
    ResourceLocation getId();

    /** Execute the ability */
    boolean execute(Player player, AbilityInstance instance);

    /** Check if ability can be used */
    boolean canUse(Player player, AbilityInstance instance);

    /** Get cooldown in ticks */
    int getCooldown();

    int getBloodCost();

    /** Cost in ability points to unlock */
    int getCost();

    /** Save ability-specific data */
    CompoundTag save(CompoundTag tag);

    /** Load ability-specific data */
    void load(CompoundTag tag);
}
