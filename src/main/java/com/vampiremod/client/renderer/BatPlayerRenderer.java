package com.vampiremod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vampiremod.VampireMod;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders players as bats when they are in bat form and hides first-person arms.
 */
@Mod.EventBusSubscriber(modid = VampireMod.MOD_ID, value = Dist.CLIENT)
public class BatPlayerRenderer {
    private static final ResourceLocation BAT_TEXTURE = new ResourceLocation("minecraft", "textures/entity/bat.png");
    private static BatModel batModel;
    private static Bat cachedBat;

    private BatPlayerRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!shouldRenderAsBat(player)) {
            return;
        }

        event.setCanceled(true);
        renderBat(player, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && shouldRenderAsBat(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && shouldRenderAsBat(player)) {
            event.setCanceled(true);
        }
    }

    private static void renderBat(Player player, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.2D, 0.0D);

        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(2.0F, 2.0F, 2.0F);

        if (player.isCrouching()) {
            poseStack.translate(0.0D, -0.15D, 0.0D);
        }

        Bat bat = getOrCreateBat(player);
        BatModel model = getBatModel();
        float limbSwing = player.walkAnimation.position(partialTick);
        float limbSwingAmount = player.walkAnimation.speed(partialTick);
        float ageInTicks = player.tickCount + partialTick;
        float netHeadYaw = 0.0F; // keep head aligned to body to avoid jitter
        float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());

        bat.xo = player.xo;
        bat.yo = player.yo;
        bat.zo = player.zo;
        bat.setPos(player.getX(), player.getY(), player.getZ());

        bat.yBodyRotO = player.yBodyRotO;
        bat.yBodyRot = player.yBodyRot;
        bat.yRotO = player.yRotO;
        bat.setYRot(player.getYRot());
        bat.yHeadRotO = player.yHeadRotO;
        bat.setYHeadRot(player.getYHeadRot());
        bat.xRotO = player.xRotO;
        bat.setXRot(player.getXRot());
        bat.tickCount = player.tickCount;
        bat.walkAnimation.update(limbSwing, limbSwingAmount);

        model.prepareMobModel(bat, limbSwing, limbSwingAmount, partialTick);
        model.setupAnim(bat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        VertexConsumer consumer = buffer.getBuffer(model.renderType(BAT_TEXTURE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private static BatModel getBatModel() {
        if (batModel == null) {
            EntityModelSet models = Minecraft.getInstance().getEntityModels();
            batModel = new BatModel(models.bakeLayer(ModelLayers.BAT));
        }
        return batModel;
    }

    private static Bat getOrCreateBat(Player player) {
        if (cachedBat == null || cachedBat.level() != player.level()) {
            cachedBat = new Bat(EntityType.BAT, player.level());
            cachedBat.setInvisible(true);
        }
        return cachedBat;
    }

    private static boolean shouldRenderAsBat(Player player) {
        return player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .map(cap -> cap.isBatForm())
                .orElse(false);
    }
}
