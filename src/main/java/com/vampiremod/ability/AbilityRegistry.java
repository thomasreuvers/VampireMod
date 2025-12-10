package com.vampiremod.ability;

import com.vampiremod.VampireMod;
import com.vampiremod.ability.impl.BatTransformationAbility;
import com.vampiremod.ability.impl.InvisibilityAbility;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple registry for abilities without using Forge's registry system
 * This is easier and works perfectly fine for abilities
 */
public class AbilityRegistry {
    private static final Map<ResourceLocation, Ability> ABILITIES = new HashMap<>();

    // Register all abilities here
    public static final Ability BAT_TRANSFORMATION = register(new BatTransformationAbility());
    public static final Ability INVISIBILITY = register(new InvisibilityAbility(
            new ResourceLocation("vampiremod", "invisibility")
    ));

    /**
     * Register an ability
     */
    private static Ability register(Ability ability) {
        ABILITIES.put(ability.getId(), ability);
        return ability;
    }

    /**
     * Get an ability by ID
     */
    public static Ability getAbility(ResourceLocation id) {
        return ABILITIES.get(id);
    }

    /**
     * Get all registered abilities
     */
    public static Collection<Ability> getAllAbilities() {
        return ABILITIES.values();
    }

    /**
     * Initialize the registry (called from main mod class)
     */
    public static void init() {
        // This just ensures the class is loaded and static initializers run
        // All abilities are registered in the static block above
    }
}
