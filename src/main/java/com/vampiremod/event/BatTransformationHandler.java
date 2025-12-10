package com.vampiremod.event;

import com.vampiremod.VampireMod;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;

/**
 * Handles bat form physics, dimensions, and ambient sounds.
 */
@Mod.EventBusSubscriber(modid = VampireMod.MOD_ID)
public class BatTransformationHandler {
    private static final EntityDimensions BAT_DIMENSIONS = EntityDimensions.scalable(0.5F, 0.9F);
    private static final float BAT_STEP_HEIGHT = 0.25F; // lower step reduces visual clipping into blocks
    private static final float DEFAULT_STEP_HEIGHT = 0.6F;

    private BatTransformationHandler() {
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isBat(player)) {
            return;
        }

        event.setNewSize(BAT_DIMENSIONS);
        // Camera roughly 65% of bat height to keep head above ground and reduce clipping visuals
        event.setNewEyeHeight(BAT_DIMENSIONS.height * 0.65F);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isBat(player)) {
            if (player.maxUpStep() != BAT_STEP_HEIGHT) {
                player.setMaxUpStep(BAT_STEP_HEIGHT);
            }
        } else if (player.maxUpStep() != DEFAULT_STEP_HEIGHT) {
            player.setMaxUpStep(DEFAULT_STEP_HEIGHT);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!isBat(serverPlayer)) {
            return;
        }

        playAmbientSounds(serverPlayer);
    }

    private static void playAmbientSounds(ServerPlayer player) {
        if (player.tickCount % 80 == 0 && player.getRandom().nextFloat() < 0.3F) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.0F);
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        double vertical = player.getDeltaMovement().y;
        double horizontal = player.getDeltaMovement().horizontalDistance();

        if (!player.onGround() && vertical > 0.01D) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 0.3F, 1.0F);
            return;
        }

        if (horizontal > 0.05D) {
            float volume = (float) Math.min(0.8D, 0.3D + horizontal);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, volume, 1.0F);
        }
    }

    private static boolean isBat(Player player) {
        return player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .map(cap -> cap.isBatForm())
                .orElse(false);
    }
}
