package com.vampiremod.capability;

import com.vampiremod.ability.AbilityInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Capability interface for entities that can have abilities
 */
public interface IAbilityCapability {
    /** Get all ability instances */
    Collection<AbilityInstance> getAbilities();

    /** Get specific ability instance */
    AbilityInstance getAbility(ResourceLocation id);

    /** Add an ability instance */
    void addAbility(AbilityInstance instance);

    /** Get available ability points */
    int getAbilityPoints();

    /** Set ability points */
    void setAbilityPoints(int points);

    /** Spend ability points */
    boolean spendPoints(int amount);

    /** Serialize to NBT */
    Tag serializeNBT();

    /** Deserialize from NBT */
    void deserializeNBT(Tag nbt);
}
