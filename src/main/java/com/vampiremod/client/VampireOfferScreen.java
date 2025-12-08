package com.vampiremod.client;

import com.vampiremod.network.ModNetworking;
import com.vampiremod.network.RespondVampireBitePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VampireOfferScreen extends Screen {
    private final int vampireEntityId;

    public VampireOfferScreen(int vampireEntityId) {
        super(Component.literal("Vampire's Offer"));
        this.vampireEntityId = vampireEntityId;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        addRenderableWidget(Button.builder(Component.literal("Accept"), btn -> {
            ModNetworking.CHANNEL.sendToServer(new RespondVampireBitePacket(vampireEntityId, true));
            onClose();
        }).bounds(centerX - 80, centerY, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Decline"), btn -> {
            ModNetworking.CHANNEL.sendToServer(new RespondVampireBitePacket(vampireEntityId, false));
            onClose();
        }).bounds(centerX + 10, centerY, 70, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        int centerX = this.width / 2;
        int titleY = this.height / 2 - 30;
        gfx.drawCenteredString(this.font, "The vampire weakens...", centerX, titleY, 0xFFFFFF);
        gfx.drawCenteredString(this.font, "Do you accept the curse?", centerX, titleY + 12, 0xFF0000);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
