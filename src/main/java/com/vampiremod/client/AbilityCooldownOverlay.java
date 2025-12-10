package com.vampiremod.client;

import com.vampiremod.VampireMod;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders a small cooldown indicator for the active ability.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = VampireMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AbilityCooldownOverlay {
    @SubscribeEvent
    public static void renderCooldown(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
            ResourceLocation activeId = cap.getActiveAbility();
            if (activeId == null) {
                return;
            }
            AbilityInstance instance = cap.getAbility(activeId);
            if (instance == null) {
                return;
            }

            int cooldownTicks = instance.getCooldownRemaining();
            if (cooldownTicks <= 0) {
                return;
            }

            GuiGraphics gfx = event.getGuiGraphics();
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();

            int boxWidth = 80;
            int boxHeight = 14;
            int x = width / 2 - boxWidth / 2;
            int y = height - 58;

            String nameKey = "ability." + activeId.getNamespace() + "." + activeId.getPath();
            String name = net.minecraft.network.chat.Component.translatable(nameKey).getString();
            if (name.equals(nameKey)) {
                name = activeId.getPath();
            }

            String time = (cooldownTicks / 20) + "s";
            int timeWidth = mc.font.width(time);

            int bg = 0xAA000000;
            gfx.fill(x, y, x + boxWidth, y + boxHeight, bg);
            int nameMaxWidth = boxWidth - timeWidth - 10;
            String displayName = mc.font.plainSubstrByWidth(name, nameMaxWidth);
            gfx.drawString(mc.font, displayName, x + 4, y + 3, 0xFFFFFF, false);
            gfx.drawString(mc.font, time, x + boxWidth - timeWidth - 4, y + 3, 0xFF8888, false);
        });
    }
}
