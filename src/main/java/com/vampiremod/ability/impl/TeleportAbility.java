package com.vampiremod.ability.impl;

import com.vampiremod.VampireMod;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.AbstractAbility;
import com.vampiremod.sound.ModSounds;
import com.vampiremod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Active teleport ability with server-side validation.
 * The client sets a pending target position; the server validates and executes.
 */
public class TeleportAbility extends AbstractAbility {
    public static final ResourceLocation ID = new ResourceLocation(VampireMod.MOD_ID, "teleport");

    private static final int COOLDOWN_TICKS = 200;
    private static final double MAX_RANGE = 20.0D;
    private static final double MAX_RANGE_SQR = MAX_RANGE * MAX_RANGE;
    private static final String KEY_TARGET = "teleport_target";

    public TeleportAbility() {
        // Sound handled manually at origin/destination
        super(ID, COOLDOWN_TICKS, 0, 2, null);
    }

    /**
     * Store the requested destination in the instance data so it can be validated
     * inside {@link #canUse} and used during {@link #execute}.
     */
    public void setPendingTarget(AbilityInstance instance, BlockPos pos) {
        instance.getData().put(KEY_TARGET, NbtUtils.writeBlockPos(pos));
    }

    /**
     * Clear any queued teleport target to prevent stale reuse.
     */
    public void clearPendingTarget(AbilityInstance instance) {
        instance.getData().remove(KEY_TARGET);
    }

    @Nullable
    private BlockPos getPendingTarget(AbilityInstance instance) {
        CompoundTag data = instance.getData();
        if (!data.contains(KEY_TARGET)) {
            return null;
        }
        return NbtUtils.readBlockPos(data.getCompound(KEY_TARGET));
    }

    @Override
    public boolean canUse(Player player, AbilityInstance instance) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos requested = getPendingTarget(instance);
        if (requested == null) {
            return false;
        }

        BlockPos safeTarget = findSafeTarget(serverLevel, serverPlayer, requested);
        if (safeTarget == null) {
            return false;
        }

        return super.canUse(player, instance);
    }

    @Override
    public boolean execute(Player player, AbilityInstance instance) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos requested = getPendingTarget(instance);
        clearPendingTarget(instance);
        if (requested == null) {
            return false;
        }

        BlockPos safeTarget = findSafeTarget(serverLevel, serverPlayer, requested);
        if (safeTarget == null) {
            return false;
        }

        Vec3 destination = Vec3.atBottomCenterOf(safeTarget);
        Vec3 origin = serverPlayer.position();

        spawnTeleportEffects(serverLevel, serverPlayer, origin);
        serverPlayer.teleportTo(destination.x, destination.y, destination.z);
        spawnTeleportEffects(serverLevel, serverPlayer, destination);
        return true;
    }

    @Nullable
    public BlockPos findSafeTarget(Level level, Player player, BlockPos requested) {
        if (!level.isInWorldBounds(requested) || !level.hasChunkAt(requested)) {
            return null;
        }
        WorldBorder border = level.getWorldBorder();
        if (!border.isWithinBounds(requested)) {
            return null;
        }

        Vec3 playerPos = player.position();
        int[] offsets = {0, -1, -2, 1, 2, 3};
        for (int dy : offsets) {
            BlockPos candidate = requested.offset(0, dy, 0);
            if (!level.isInWorldBounds(candidate) || !level.hasChunkAt(candidate) || !border.isWithinBounds(candidate)) {
                continue;
            }
            Vec3 dest = Vec3.atBottomCenterOf(candidate);
            if (!withinRange(player, dest)) {
                continue;
            }
            if (!isSpaceSafe(level, candidate)) {
                continue;
            }

            AABB movedBox = player.getBoundingBox().move(dest.subtract(playerPos));
            if (level.noCollision(player, movedBox)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isSpaceSafe(Level level, BlockPos pos) {
        // Require clear 2-block column with no fluids; allow hovering without ground support.
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
            return false;
        }
        return level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty();
    }

    private boolean withinRange(Player player, Vec3 dest) {
        double max = MAX_RANGE + 0.75D; // small cushion to avoid failing due to block center offsets
        double maxSq = max * max;
        return player.getEyePosition().distanceToSqr(dest.add(0.0D, 0.6D, 0.0D)) <= maxSq;
    }

    private void spawnTeleportEffects(ServerLevel level, ServerPlayer player, Vec3 position) {
        double centerY = position.y + player.getBbHeight() * 0.5D;
        double horizontalSpread = Math.max(0.35D, player.getBbWidth() * 0.6D);
        double verticalSpread = Math.max(0.35D, player.getBbHeight() * 0.35D);

        level.sendParticles(
                ModParticles.BAT_PORTAL.get(),
                position.x,
                centerY,
                position.z,
                8,
                horizontalSpread,
                verticalSpread,
                horizontalSpread,
                0.05
        );
        level.playSound(
                null,
                position.x,
                position.y,
                position.z,
                ModSounds.BAT_CHIRPING.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    public double getMaxRange() {
        return MAX_RANGE;
    }
}
