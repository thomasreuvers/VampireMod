package com.vampiremod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.network.AbilitySelectPacket;
import com.vampiremod.ability.network.NetworkHandler;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class RadialAbilityScreen extends Screen {
    private static final int INNER_RADIUS = 40;
    private static final int OUTER_RADIUS = 100;
    private static final int SEGMENTS = 60;

    private final List<AbilityInstance> abilities = new ArrayList<>();
    private int selectedIndex = -1;
    private int activeIndex = -1;
    private int mouseX;
    private int mouseY;

    public RadialAbilityScreen() {
        super(Component.translatable("screen.vampiremod.ability_selection.title"));

        // Load player's unlocked abilities
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                    .ifPresent(cap -> cap.getAbilities().stream()
                            .filter(AbilityInstance::isUnlocked)
                            .forEach(abilities::add));
            mc.player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                    .ifPresent(cap -> {
                        if (cap.getActiveAbility() != null) {
                            for (int i = 0; i < abilities.size(); i++) {
                                if (abilities.get(i).getAbility().getId().equals(cap.getActiveAbility())) {
                                    activeIndex = i;
                                    break;
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        // Darken background slightly
        graphics.fillGradient(0, 0, this.width, this.height,
                0x40000000, 0x40000000);

        if (abilities.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.vampiremod.ability_selection.empty").getString(),
                    this.width / 2, this.height / 2, 0xFFFFFF);
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Calculate selected slice based on mouse position
        updateSelectedSlice(centerX, centerY);

        // Render radial menu
        renderRadialMenu(graphics, centerX, centerY);

        // Render ability info
        if (selectedIndex >= 0 && selectedIndex < abilities.size()) {
            renderAbilityInfo(graphics, abilities.get(selectedIndex));
        }
    }

    private void updateSelectedSlice(int centerX, int centerY) {
        int dx = mouseX - centerX;
        int dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < INNER_RADIUS || distance > OUTER_RADIUS + 20) {
            selectedIndex = -1;
            return;
        }

        // Calculate angle (-PI to PI)
        double angle = Math.atan2(dy, dx);
        // Convert to 0 to 2PI
        if (angle < 0) angle += 2 * Math.PI;

        // Calculate which slice
        double sliceAngle = (2 * Math.PI) / abilities.size();
        // Offset by half a slice so first ability is at top
        angle = (angle + sliceAngle / 2 + Math.PI / 2) % (2 * Math.PI);

        selectedIndex = (int) (angle / sliceAngle);
    }

    private void renderRadialMenu(GuiGraphics graphics, int centerX, int centerY) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        double sliceAngle = (2 * Math.PI) / abilities.size();

        for (int i = 0; i < abilities.size(); i++) {
            double startAngle = i * sliceAngle - Math.PI / 2;
            double endAngle = (i + 1) * sliceAngle - Math.PI / 2;

            boolean selected = i == selectedIndex;
            boolean active = i == activeIndex;
            int color = selected ? 0xFFFFAA44 : active ? 0xFF55AAFF : 0xFF444444;
            int alpha = selected || active ? 220 : 150;

            renderSlice(poseStack, centerX, centerY, INNER_RADIUS, OUTER_RADIUS,
                    startAngle, endAngle, color, alpha);

            // Render ability icon/text in the middle of the slice
            double midAngle = (startAngle + endAngle) / 2;
            int textX = centerX + (int) ((double) (INNER_RADIUS + OUTER_RADIUS) / 2 * Math.cos(midAngle));
            int textY = centerY + (int) ((double) (INNER_RADIUS + OUTER_RADIUS) / 2 * Math.sin(midAngle));

            AbilityInstance ability = abilities.get(i);
            String abilityName = getAbilityName(ability);

            graphics.drawCenteredString(this.font, abilityName.substring(0, Math.min(3, abilityName.length())).toUpperCase(),
                    textX, textY - 4, selected ? 0xFFFFFF : (active ? 0x99CCFF : 0xAAAAAA));
        }

        // Draw center circle
        renderCircle(poseStack, centerX, centerY, INNER_RADIUS, 0xFF222222, 200);

        poseStack.popPose();
    }

    private void renderSlice(PoseStack poseStack, int centerX, int centerY,
                             float innerRadius, float outerRadius,
                             double startAngle, double endAngle,
                             int color, int alpha) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        double angleStep = (endAngle - startAngle) / SEGMENTS;

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = startAngle + i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // Inner vertex
            buffer.vertex(matrix, centerX + innerRadius * cos, centerY + innerRadius * sin, 0)
                    .color(r, g, b, alpha).endVertex();

            // Outer vertex
            buffer.vertex(matrix, centerX + outerRadius * cos, centerY + outerRadius * sin, 0)
                    .color(r, g, b, alpha).endVertex();
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void renderCircle(PoseStack poseStack, int centerX, int centerY,
                              float radius, int color, int alpha) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Center point
        buffer.vertex(matrix, centerX, centerY, 0)
                .color(r, g, b, alpha).endVertex();

        // Circle edge
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = (2 * Math.PI * i) / SEGMENTS;
            float x = centerX + radius * (float) Math.cos(angle);
            float y = centerY + radius * (float) Math.sin(angle);
            buffer.vertex(matrix, x, y, 0)
                    .color(r, g, b, alpha).endVertex();
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void renderAbilityInfo(GuiGraphics graphics, AbilityInstance ability) {
        int infoX = this.width / 2;
        int infoY = 20;

        String name = getAbilityName(ability);
        graphics.drawCenteredString(this.font, name, infoX, infoY, 0xFFFFFF);

        String cooldown = Component.translatable("screen.vampiremod.ability_selection.cooldown",
                ability.getCooldownRemaining() / 20).getString();
        graphics.drawCenteredString(this.font, cooldown, infoX, infoY + 12, 0xAAAAAA);

        String bloodCost = Component.translatable("screen.vampiremod.ability_selection.blood_cost",
                ability.getAbility().getBloodCost()).getString();
        graphics.drawCenteredString(this.font, bloodCost, infoX, infoY + 24, 0xFF4444);

        String hint = Component.translatable("screen.vampiremod.ability_selection.hint").getString();
        graphics.drawCenteredString(this.font, hint, infoX, infoY + 40, 0xCCCCCC);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && selectedIndex >= 0 && selectedIndex < abilities.size()) {
            // Select active ability
            AbilityInstance selected = abilities.get(selectedIndex);
            NetworkHandler.sendToServer(new AbilitySelectPacket(selected.getAbility().getId()));
            activeIndex = selectedIndex;
            this.onClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Close when R is released
        this.onClose();
        return true;
    }

    private String getAbilityName(AbilityInstance ability) {
        String key = "ability." + ability.getAbility().getId().getNamespace() + "." + ability.getAbility().getId().getPath();
        String translated = Component.translatable(key).getString();
        if (!translated.equals(key)) {
            return translated;
        }
        return ability.getAbility().getId().getPath();
    }
}
