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
    protected final int bloodCost;

    protected AbstractAbility(ResourceLocation id, int cooldown, int cost, int bloodCost) {
        this.id = id;
        this.cooldown = cooldown;
        this.cost = cost;
        this.bloodCost = bloodCost;
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
        // Check cooldown
        if (instance.getCooldownRemaining() > 0) {
            return false;
        }

        // Check blood cost - replace with your blood capability check
        return hasEnoughBlood(player, bloodCost);
    }

    protected boolean hasEnoughBlood(Player player, int amount) {
        // Example: Replace with your actual blood capability
        // return player.getCapability(YourBloodCapability.BLOOD).map(cap ->
        //     cap.getBlood() >= amount
        // ).orElse(false);

        // For now, a placeholder
        return true; // TODO: Implement with your blood system
    }

    // Helper method to consume blood - replace with your system
    protected boolean consumeBlood(Player player, int amount) {
        // Example: Replace with your actual blood capability
        // return player.getCapability(YourBloodCapability.BLOOD).map(cap -> {
        //     if (cap.getBlood() >= amount) {
        //         cap.setBlood(cap.getBlood() - amount);
        //         return true;
        //     }
        //     return false;
        // }).orElse(false);

        // For now, a placeholder
        return true; // TODO: Implement with your blood system
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public int getBloodCost() {
        return bloodCost;
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
