package com.vampiremod.event;

import com.vampiremod.VampireMod;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AbilityEvents {
    /**
     * Tick abilities to handle cooldowns
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        event.player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .ifPresent(cap -> {
                    for (AbilityInstance instance : cap.getAbilities()) {
                        instance.tick();
                        instance.getAbility().tick(event.player, instance);
                    }
                });
    }

    /**
     * Handle passive abilities (e.g., vampiric strength)
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                    .ifPresent(cap -> {
                        AbilityInstance strength = cap.getAbility(
                                new ResourceLocation(VampireMod.MOD_ID, "vampiric_strength")
                        );

                        if (strength != null && strength.isUnlocked()) {
                            // TODO: Increase damage by 50%
                        }
                    });
        }
    }
}
