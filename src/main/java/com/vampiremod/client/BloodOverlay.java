package com.vampiremod.client;

import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.capability.PlayerVampireData;
import com.vampiremod.VampireMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = VampireMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BloodOverlay {
    private static final ResourceLocation BLOOD_FULL = new ResourceLocation(VampireMod.MOD_ID, "textures/gui/blood_full.png");
    private static final ResourceLocation BLOOD_HALF = new ResourceLocation(VampireMod.MOD_ID, "textures/gui/blood_half.png");
    private static final ResourceLocation BLOOD_EMPTY = new ResourceLocation(VampireMod.MOD_ID, "textures/gui/blood_empty.png");

    @SubscribeEvent
    public static void hideFood(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        if (isVampire(p) || Minecraft.getInstance().getConnection() == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hideAir(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id())) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        if (isVampire(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderBlood(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Player p = Minecraft.getInstance().player;
        if (p == null || !isVampire(p)) return;

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        GuiGraphics gfx = event.getGuiGraphics();
        float blood = getBlood(p);
        int drops = 10;
        int filled = (int)Math.floor(blood / 2f); // each drop = 2 blood
        boolean half = (blood % 2f) >= 1f;

        int startX = width / 2 + 80; // shift left a bit
        int y = height - 39;

        for (int i = 0; i < drops; i++) {
            int x = startX - i * 8;
            ResourceLocation tex;
            if (i < filled) {
                tex = BLOOD_FULL;
            } else if (i == filled && half) {
                tex = BLOOD_HALF;
            } else {
                tex = BLOOD_EMPTY;
            }
            gfx.blit(tex, x, y, 0, 0, 9, 9, 9, 9);
        }
    }

    private static boolean isVampire(Player p) {
        return p.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false);
    }

    private static float getBlood(Player p) {
        return p.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::getBlood).orElse(0f);
    }
}
