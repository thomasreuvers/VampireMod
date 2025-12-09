package com.vampiremod.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.vampiremod.VampireMod;
import com.vampiremod.ability.network.AbilityActivatePacket;
import com.vampiremod.ability.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AbilityKeybinds {
    public static final String CATEGORY = "key.categories.vampiremod.abilities";

    public static final Lazy<KeyMapping> INVISIBILITY = Lazy.of(() ->
            new KeyMapping(
                    "key.vampiremod.invisibility",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_R,
                    CATEGORY
            )
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(INVISIBILITY.get());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            while (INVISIBILITY.get().consumeClick()) {
                NetworkHandler.sendToServer(new AbilityActivatePacket(
                        new ResourceLocation(VampireMod.MOD_ID, "invisibility")
                ));
            }
        }
    }
}
