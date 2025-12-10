package com.vampiremod.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.vampiremod.VampireMod;
import com.vampiremod.ability.network.AbilityActivatePacket;
import com.vampiremod.ability.network.AbilitySelectPacket;
import com.vampiremod.ability.network.NetworkHandler;
import com.vampiremod.capability.AbilityCapabilityProvider;
import com.vampiremod.client.gui.RadialAbilityScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AbilityKeybinds {
    public static final String CATEGORY = "key.categories.vampiremod.abilities";

    public static final Lazy<KeyMapping> ABILITY_WHEEL = Lazy.of(() ->
            new KeyMapping(
                    "key.vampiremod.ability_whee",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_R,
                    CATEGORY
            )
    );
    public static final Lazy<KeyMapping> ABILITY_ACTIVATE = Lazy.of(() ->
            new KeyMapping(
                    "key.vampiremod.ability_activate",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_V,
                    CATEGORY
            )
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ABILITY_WHEEL.get());
        event.register(ABILITY_ACTIVATE.get());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();

            // Open radial menu while R is held down
            if (ABILITY_WHEEL.get().isDown() && mc.screen == null) {
                mc.setScreen(new RadialAbilityScreen());
            }

            // Activate currently selected ability when V is pressed
            while (ABILITY_ACTIVATE.get().consumeClick()) {
                if (mc.player == null) {
                    continue;
                }
                mc.player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
                    ResourceLocation active = cap.getActiveAbility();
                    if (active != null) {
                        NetworkHandler.sendToServer(new AbilityActivatePacket(active));
                    }
                });
            }
        }
    }
}
