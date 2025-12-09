package com.vampiremod.ability;

import com.vampiremod.VampireMod;
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

public class AbilityRegistry {
    public static final DeferredRegister<Ability> ABILITIES =
            DeferredRegister.create(new ResourceLocation(VampireMod.MOD_ID, "abilities"), VampireMod.MOD_ID);

    // Create registry key
    public static final ResourceKey<Registry<Ability>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(VampireMod.MOD_ID, "abilities"));

    private static IForgeRegistry<Ability> registry;

    // Register abilities
    public static final RegistryObject<Ability> INVISIBILITY = ABILITIES.register("invisibility",
            () -> new InvisibilityAbility(new ResourceLocation(VampireMod.MOD_ID, "invisibility"), 200, 3));

    public static void init() {
        // Called during mod initialization
    }

    public static Ability getAbility(ResourceLocation id) {
        if (registry == null) {
            registry = RegistryManager.ACTIVE.getRegistry(REGISTRY_KEY);
        }
        return registry != null ? registry.getValue(id) : null;
    }

    public static Collection<Ability> getAllAbilities() {
        if (registry == null) {
            registry = RegistryManager.ACTIVE.getRegistry(REGISTRY_KEY);
        }
        return registry != null ? registry.getValues() : Collections.emptyList();
    }
}
