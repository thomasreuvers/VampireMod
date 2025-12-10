package com.vampiremod.capability;

import com.vampiremod.ability.AbilityInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Capability interface for entities that can have abilities
 */
public interface IAbilityCapability {
    /** Get all ability instances */
    Collection<AbilityInstance> getAbilities();

    /** Get specific ability instance */
    AbilityInstance getAbility(ResourceLocation id);

    /** Whether the player is currently transformed into a bat */
    boolean isBatForm();

    /** Set bat transformation flag */
    void setBatForm(@Nullable Player player, boolean batForm);

    /** Convenience overload for contexts without an available player reference */
    default void setBatForm(boolean batForm) {
        setBatForm(null, batForm);
    }

    /** Add an ability instance */
    void addAbility(AbilityInstance instance);

    /** Ensure all registered abilities have an instance (locked by default) */
    void ensureAbilitiesRegistered();

    /** Get available ability points */
    int getAbilityPoints();

    /** Set ability points */
    void setAbilityPoints(int points);

    /** Spend ability points */
    boolean spendPoints(int amount);

    /** Serialize to NBT */
    CompoundTag  serializeNBT();

    void deserializeNBT(CompoundTag nbt);
}
