package com.vampiremod.client;

import com.vampiremod.VampireMod;
import com.vampiremod.client.keybind.AbilityKeybinds;
import com.vampiremod.entity.ModEntities;
import com.vampiremod.entity.client.VampireRenderer;
import com.vampiremod.client.particle.BatParticle;
import com.vampiremod.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side MOD bus events (for registration during mod loading)
 */
@Mod.EventBusSubscriber(modid = VampireMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        // Register all keybindings
        AbilityKeybinds.register(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VAMPIRE.get(), VampireRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BAT_PORTAL.get(), BatParticle.Provider::new);
    }

}
