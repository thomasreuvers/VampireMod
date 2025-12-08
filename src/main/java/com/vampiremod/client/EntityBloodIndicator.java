package com.vampiremod.client;

import com.vampiremod.Capability.ModCapabilities;
import com.vampiremod.Capability.PlayerVampireData;
import com.vampiremod.VampireMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VampireMod.MOD_ID, value = Dist.CLIENT)
public class EntityBloodIndicator {
    private static final ResourceLocation BLOOD_FULL = new ResourceLocation(VampireMod.MOD_ID, "textures/gui/blood_full.png");
    private static final ResourceLocation BLOOD_HALF = new ResourceLocation(VampireMod.MOD_ID, "textures/gui/blood_half.png");
    private static final ResourceLocation BLOOD_EMPTY = new ResourceLocation(VampireMod.MOD_ID, "textures/gui/blood_empty.png");

    @SubscribeEvent
    public static void renderBloodText(RenderLivingEvent.Post<LivingEntity, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!isVampire(mc.player)) return;
        if (mc.crosshairPickEntity != event.getEntity()) return;
        if (mc.player.distanceTo(event.getEntity()) > 5.0F) return;

        event.getEntity().getCapability(ModCapabilities.BLOOD_CAP).ifPresent(cap -> {
            if (!cap.isDrinkable()) return;
            drawDrops(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getEntity(), cap.getCurrent(), cap.getMax(), mc.getEntityRenderDispatcher());
        });
    }

    private static void drawDrops(PoseStack poseStack, MultiBufferSource buffers, int packedLight, LivingEntity entity, int current, int max, EntityRenderDispatcher dispatcher) {
        poseStack.pushPose();
        double y = entity.getBbHeight() + 0.6D;
        poseStack.translate(0.0D, y, 0.0D);
        poseStack.mulPose(dispatcher.cameraOrientation());
        float scale = 0.02F;
        poseStack.scale(-scale, -scale, scale);

        int drops = Math.max(1, max / 2);
        int filled = current / 2;
        boolean half = (current % 2) == 1;

        int totalWidth = drops * 9 + (drops - 1) * 2;
        int startX = -totalWidth / 2;
        int yPos = 0;

        for (int i = 0; i < drops; i++) {
            ResourceLocation tex;
            if (i < filled) {
                tex = BLOOD_FULL;
            } else if (i == filled && half) {
                tex = BLOOD_HALF;
            } else {
                tex = BLOOD_EMPTY;
            }
            int x = startX + i * 11;
            var vc = buffers.getBuffer(RenderType.entityCutout(tex));
            float size = 9f;
            float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;
            vc.vertex(poseStack.last().pose(), x, yPos + size, 0).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
            vc.vertex(poseStack.last().pose(), x + size, yPos + size, 0).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
            vc.vertex(poseStack.last().pose(), x + size, yPos, 0).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
            vc.vertex(poseStack.last().pose(), x, yPos, 0).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        }
        poseStack.popPose();
    }

    private static boolean isVampire(Player p) {
        return p.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false);
    }
}