package com.vampiremod.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Represents an instance of an ability for a specific player
 * Tracks cooldowns, upgrades, and state
 */
public class AbilityInstance {
    private final Ability ability;
    private int cooldownRemaining;
    private int level;
    private boolean unlocked;

    public AbilityInstance(Ability ability) {
        this.ability = ability;
        this.cooldownRemaining = 0;
        this.level = 0;
        this.unlocked = false;
    }

    public Ability getAbility() {
        return ability;
    }

    public void tick() {
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
        }
    }

    public boolean activate(Player player) {
        if (!ability.canUse(player, this)) {
            return false;
        }

        // Check and consume blood
        if (ability instanceof AbstractAbility abstractAbility) {
            if (!abstractAbility.consumeBlood(player, ability.getBloodCost())) {
                return false; // Not enough blood
            }
        }

        boolean success = ability.execute(player, this);
        if (success) {
            cooldownRemaining = ability.getCooldown();
            if (ability instanceof AbstractAbility abstractAbility) {
                abstractAbility.getActivationSound().ifPresent(sound ->
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                sound, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F));
            }
        }
        return success;
    }

    public int getCooldownRemaining() {
        return cooldownRemaining;
    }

    public void unlock() {
        this.unlocked = true;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ability", ability.getId().toString());
        tag.putInt("cooldown", cooldownRemaining);
        tag.putInt("level", level);
        tag.putBoolean("unlocked", unlocked);
        ability.save(tag);
        return tag;
    }

    public void load(CompoundTag tag) {
        cooldownRemaining = tag.getInt("cooldown");
        level = tag.getInt("level");
        unlocked = tag.getBoolean("unlocked");
        ability.load(tag);
    }
}
