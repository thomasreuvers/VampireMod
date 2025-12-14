package com.vampiremod.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.vampiremod.ability.network.AbilityActivatePacket;
import com.vampiremod.ability.network.NetworkHandler;
import com.vampiremod.ability.network.TeleportRequestPacket;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.impl.TeleportAbility;
import com.vampiremod.capability.AbilityCapabilityProvider;
import com.vampiremod.client.gui.RadialAbilityScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import com.vampiremod.particle.ModParticles;

public class AbilityKeybinds {
    public static final String CATEGORY = "key.categories.vampiremod.abilities";

    public static final Lazy<KeyMapping> ABILITY_WHEEL = Lazy.of(() ->
            new KeyMapping(
                    "key.vampiremod.ability_whee",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_R,
                    CATEGORY
            )
    );
    public static final Lazy<KeyMapping> ABILITY_ACTIVATE = Lazy.of(() ->
            new KeyMapping(
                    "key.vampiremod.ability_activate",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_V,
                    CATEGORY
            )
    );

    private static boolean teleportPreviewing = false;
    private static BlockPos teleportPreviewPos = null;

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ABILITY_WHEEL.get());
        event.register(ABILITY_ACTIVATE.get());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();

            // Open radial menu while R is held down
            if (ABILITY_WHEEL.get().isDown() && mc.screen == null) {
                mc.setScreen(new RadialAbilityScreen());
            }

            // Activate currently selected ability when V is pressed
            if (mc.player == null) {
                resetTeleportPreview();
                return;
            }

            mc.player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
                ResourceLocation active = cap.getActiveAbility();
                AbilityInstance teleportInstance = cap.getAbility(TeleportAbility.ID);
                boolean teleportActive = active != null
                        && TeleportAbility.ID.equals(active)
                        && teleportInstance != null
                        && teleportInstance.isUnlocked()
                        && mc.screen == null;

                if (teleportActive && teleportInstance.getCooldownRemaining() <= 0) {
                    handleTeleportClient(mc, teleportInstance);
                } else {
                    resetTeleportPreview();
                }

                while (ABILITY_ACTIVATE.get().consumeClick()) {
                    if (teleportActive) {
                        // Teleport is handled on key release after preview
                        continue;
                    }
                    if (active != null) {
                        NetworkHandler.sendToServer(new AbilityActivatePacket(active));
                    }
                }
            });
        }
    }

    private static void handleTeleportClient(Minecraft mc, AbilityInstance instance) {
        boolean keyDown = ABILITY_ACTIVATE.get().isDown();
        if (!keyDown) {
            if (teleportPreviewing && teleportPreviewPos != null) {
                NetworkHandler.sendToServer(new TeleportRequestPacket(teleportPreviewPos));
            }
            resetTeleportPreview();
            return;
        }

        teleportPreviewing = true;

        if (!(instance.getAbility() instanceof TeleportAbility ability)) {
            teleportPreviewPos = null;
            return;
        }

        BlockPos requested = findRequestedTeleportTarget(mc, ability);
        BlockPos safeTarget = requested != null ? ability.findSafeTarget(mc.level, mc.player, requested) : null;
        teleportPreviewPos = safeTarget;
        if (safeTarget != null) {
            spawnPreviewParticles(mc, safeTarget);
        }
    }

    private static BlockPos findRequestedTeleportTarget(Minecraft mc, TeleportAbility ability) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        double maxRange = ability.getMaxRange();
        Vec3 start = mc.player.getEyePosition(0.0F);
        Vec3 look = mc.player.getLookAngle();
        Vec3 end = start.add(look.scale(maxRange));

        BlockPos candidate = null;
        BlockHitResult blockHit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                start,
                end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                mc.player
        ));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            candidate = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else if (blockHit.getType() == HitResult.Type.MISS) {
            candidate = BlockPos.containing(end);
        }

        return candidate != null && isValidClientTarget(mc, candidate, maxRange) ? candidate : null;
    }

    private static boolean isValidClientTarget(Minecraft mc, BlockPos pos, double maxRange) {
        Level level = mc.level;
        if (level == null || mc.player == null) {
            return false;
        }
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        Vec3 destination = Vec3.atBottomCenterOf(pos);
        double max = maxRange + 0.75D; // cushion for block center offsets
        if (mc.player.getEyePosition().distanceToSqr(destination.add(0.0D, 0.6D, 0.0D)) > max * max) {
            return false;
        }

        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }
        AABB movedBox = mc.player.getBoundingBox().move(destination.subtract(mc.player.position()));
        return level.noCollision(mc.player, movedBox);
    }

    private static void spawnPreviewParticles(Minecraft mc, BlockPos pos) {
        if (mc.level == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            double ox = mc.level.random.nextDouble() - 0.5D;
            double oz = mc.level.random.nextDouble() - 0.5D;
            double oy = mc.level.random.nextDouble() * 0.8D;
            double vx = (mc.level.random.nextDouble() - 0.5D) * 0.12D;
            double vz = (mc.level.random.nextDouble() - 0.5D) * 0.12D;
            mc.level.addParticle(
                    ModParticles.BAT_PORTAL.get(),
                    pos.getX() + 0.5D + ox,
                    pos.getY() + 0.8D + oy,
                    pos.getZ() + 0.5D + oz,
                    vx,
                    0.08D,
                    vz
            );
        }
    }

    private static void resetTeleportPreview() {
        teleportPreviewing = false;
        teleportPreviewPos = null;
    }
}
