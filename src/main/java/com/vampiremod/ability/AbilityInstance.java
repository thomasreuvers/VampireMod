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

        boolean success = ability.execute(player, this);
        if (success) {
            cooldownRemaining = ability.getCooldown();
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
