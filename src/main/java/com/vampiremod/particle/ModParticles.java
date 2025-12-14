package com.vampiremod.particle;

import com.vampiremod.VampireMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, VampireMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> BAT_PORTAL =
            PARTICLE_TYPES.register("bat_portal", () -> new SimpleParticleType(true));

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
