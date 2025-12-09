package com.vampiremod.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Abstract base implementation with common functionality
 */
public class AbstractAbility implements Ability {
    protected final ResourceLocation id;
    protected final int cooldown;
    protected final int cost;

    protected AbstractAbility(ResourceLocation id, int cooldown, int cost) {
        this.id = id;
        this.cooldown = cooldown;
        this.cost = cost;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean execute(Player player, AbilityInstance instance) {
        return false;
    }

    @Override
    public boolean canUse(Player player, AbilityInstance instance) {
        return instance.getCooldownRemaining() <= 0;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
    }
}
