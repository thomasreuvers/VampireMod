package com.vampiremod.ability;

import com.vampiremod.capability.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base implementation with common functionality
 */
public class AbstractAbility implements Ability {
    protected final ResourceLocation id;
    protected final int cooldown;
    protected final int cost;
    protected final int bloodCost;
    @Nullable
    protected final SoundEvent activationSound;

    protected AbstractAbility(ResourceLocation id, int cooldown, int cost, int bloodCost) {
        this(id, cooldown, cost, bloodCost, null);
    }

    protected AbstractAbility(ResourceLocation id, int cooldown, int cost, int bloodCost, @Nullable SoundEvent activationSound) {
        this.id = id;
        this.cooldown = cooldown;
        this.cost = cost;
        this.bloodCost = bloodCost;
        this.activationSound = activationSound;
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

        return hasEnoughBlood(player, bloodCost);
    }

    protected boolean hasEnoughBlood(Player player, int amount) {
        if (amount <= 0 || player.isCreative()) {
            return true;
        }

        return player.getCapability(ModCapabilities.VAMPIRE_CAP)
                .map(cap -> cap.getBlood() >= amount)
                .orElse(false);
    }

    // Helper method to consume blood from the player's vampire pool
    protected boolean consumeBlood(Player player, int amount) {
        if (amount <= 0 || player.isCreative()) {
            return true;
        }

        return player.getCapability(ModCapabilities.VAMPIRE_CAP)
                .map(cap -> {
                    if (cap.getBlood() >= amount) {
                        cap.setBlood(cap.getBlood() - amount);
                        ModCapabilities.sync(player); // keep HUD in sync after spending blood
                        return true;
                    }
                    return false;
                })
                .orElse(false);
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

    public Optional<SoundEvent> getActivationSound() {
        return Optional.ofNullable(activationSound);
    }
}
