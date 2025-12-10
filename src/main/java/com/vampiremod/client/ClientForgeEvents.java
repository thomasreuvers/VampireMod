package com.vampiremod.client;

import com.vampiremod.VampireMod;
import com.vampiremod.client.keybind.AbilityKeybinds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side FORGE bus events (for runtime events)
 */
@Mod.EventBusSubscriber(modid = VampireMod.MOD_ID, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Handle keybinding input
        AbilityKeybinds.onClientTick(event);
    }
}