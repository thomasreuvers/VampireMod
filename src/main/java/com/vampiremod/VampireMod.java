package com.vampiremod;

import com.vampiremod.ability.AbilityRegistry;
import com.vampiremod.ability.network.NetworkHandler;
import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.effect.ModEffects;
import com.vampiremod.entity.ModEntities;
import com.vampiremod.event.ModEvents;
import com.vampiremod.sound.ModSounds;
import com.vampiremod.particle.ModParticles;
import com.vampiremod.network.ModNetworking;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VampireMod.MOD_ID)
public class VampireMod {
    public static final String MOD_ID = "vampiremod";

    public VampireMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(modBus);
        ModEffects.register(modBus);
        ModSounds.register(modBus);
        ModParticles.register(modBus);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModEvents::registerAttributes);

        AbilityRegistry.init();
        NetworkHandler.register();

        ModNetworking.register();

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ModCapabilities::attachCapabilities);
        MinecraftForge.EVENT_BUS.addListener(ModCapabilities::clonePlayerData);
    }
}
