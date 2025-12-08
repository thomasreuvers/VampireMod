package com.vampiremod.Entity.client;

import com.vampiremod.Entity.VampireEntity;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.resources.ResourceLocation;

public class VampireRenderer extends IllagerRenderer<VampireEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/illager/vindicator.png");

    public VampireRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.VINDICATOR)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(VampireEntity entity) {
        return TEXTURE;
    }
}
