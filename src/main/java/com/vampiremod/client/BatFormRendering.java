package com.vampiremod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vampiremod.VampireMod;
import com.vampiremod.ability.impl.BatTransformationAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side rendering swap: when in bat form, cancel normal player render and draw a bat instead.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE, modid = VampireMod.MOD_ID)
public class BatFormRendering {
    private static final Map<UUID, Bat> CLIENT_BATS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!BatTransformationAbility.isActive(player)) {
            return;
        }
        if (!(player.level() instanceof ClientLevel clientLevel)) {
            return;
        }

        Bat bat = CLIENT_BATS.compute(player.getUUID(), (id, existing) -> {
            if (existing != null && existing.level() == clientLevel) {
                return existing;
            }
            Bat b = new Bat(EntityType.BAT, clientLevel);
            b.setNoAi(true);
            b.setSilent(true);
            b.setNoGravity(true);
            b.setResting(false);
            return b;
        });

        bat.setResting(false);
        float partial = event.getPartialTick();

        // Interpolate player position
        double px = player.xOld + (player.getX() - player.xOld) * partial;
        double py = player.yOld + (player.getY() - player.yOld) * partial;
        double pz = player.zOld + (player.getZ() - player.zOld) * partial;

        // Interpolate player rotation
        float yRot = player.yRotO + (player.getYRot() - player.yRotO) * partial;
        float xRot = player.xRotO + (player.getXRot() - player.xRotO) * partial;

        // Update bat's OLD positions first (for next frame's interpolation)
        bat.xo = bat.getX();
        bat.yo = bat.getY();
        bat.zo = bat.getZ();
        bat.yRotO = bat.getYRot();
        bat.xRotO = bat.getXRot();
        bat.yBodyRotO = bat.yBodyRot;
        bat.yHeadRotO = bat.yHeadRot;

        // Calculate bat Y position to center it in the player's hitbox
        // Player Y is at bottom of hitbox, so we offset by half the player height
        // minus half the bat height to center them
        float playerHeight = player.getDimensions(player.getPose()).height;
        float batHeight = bat.getBbHeight();
        double batYOffset = (playerHeight / 2.0) - (batHeight / 2.0);
        double batY = py + batYOffset;

        // Now set current positions
        bat.setPos(px, batY, pz);
        bat.setYRot(yRot);
        bat.setXRot(xRot);
        bat.setYBodyRot(yRot);
        bat.setYHeadRot(yRot);
        bat.tickCount = player.tickCount;

        // Cancel the player render
        event.setCanceled(true);

        // Get the entity render dispatcher
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        // The PoseStack from the event is already camera-relative
        // We just need to render the bat at the correct offset from the player's render position
        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        // Translate to the bat's position relative to the player
        // Since the PoseStack is already positioned at the player's render location,
        // we only need to apply the Y offset
        pose.translate(0, batYOffset, 0);

        // Render the bat at the translated position
        // Pass 0,0,0 for position since we've already translated the PoseStack
        dispatcher.render(bat, 0, 0, 0,
                yRot, partial, pose, event.getMultiBufferSource(), event.getPackedLight());

        pose.popPose();
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        CLIENT_BATS.clear();
    }
}