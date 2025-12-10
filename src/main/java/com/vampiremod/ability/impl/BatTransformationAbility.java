package com.vampiremod.ability.impl;

import com.vampiremod.VampireMod;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.AbstractAbility;
import com.vampiremod.ability.network.AbilitySyncPacket;
import com.vampiremod.ability.network.NetworkHandler;
import com.vampiremod.capability.AbilityCapabilityProvider;
import com.vampiremod.event.ModPlayerEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

public class BatTransformationAbility extends AbstractAbility {
    public static final ResourceLocation ID = new ResourceLocation(VampireMod.MOD_ID, "bat_transformation");
    public static final float REVERT_HEALTH_THRESHOLD = 4.0F; // 2 hearts

    private static final int COOLDOWN_TICKS = 200; // 10 seconds
    private static final int ECHO_INTERVAL = 20;
    private static final int ECHO_RADIUS = 16;
    private static final float FLY_SPEED_MULTIPLIER = 0.5F;

    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("c4f3c7be-b4a9-4c43-8ab6-5f1c97b9fbb7");
    private static final UUID WET_SLOW_ID = UUID.fromString("f3f28a61-4b2c-4b6c-8bc8-56b8e8bafbb0");
    private static final AttributeModifier HEALTH_MODIFIER =
            new AttributeModifier(HEALTH_MODIFIER_ID, "Bat form health penalty", -8.0D, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier WET_SLOW =
            new AttributeModifier(WET_SLOW_ID, "Bat form water slowdown", -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private static final String KEY_ACTIVE = "active";
    private static final String KEY_PREV_MAYFLY = "prevMayfly";
    private static final String KEY_PREV_FLYING = "prevFlying";
    private static final String KEY_PREV_FLY_SPEED = "prevFlySpeed";

    public BatTransformationAbility() {
        super(ID, COOLDOWN_TICKS, 0, 0, SoundEvents.BAT_TAKEOFF);
    }

    public BatTransformationAbility(ResourceLocation id) {
        super(id, COOLDOWN_TICKS, 0, 0, SoundEvents.BAT_TAKEOFF);
    }

    @Override
    public boolean execute(Player player, AbilityInstance instance) {
        CompoundTag data = instance.getData();
        boolean active = data.getBoolean(KEY_ACTIVE);
        if (active) {
            deactivate(player, data);
            data.putBoolean(KEY_ACTIVE, false);
            setBatForm(player, false);
        } else {
            activate(player, data);
            data.putBoolean(KEY_ACTIVE, true);
            setBatForm(player, true);
        }
        return true;
    }

    @Override
    public void tick(Player player, AbilityInstance instance) {
        if (player.level().isClientSide) {
            return;
        }

        CompoundTag data = instance.getData();
        boolean active = data.getBoolean(KEY_ACTIVE);
        if (!active) {
            return;
        }

        if (!instance.isUnlocked()) {
            forceDeactivate(player, data);
            return;
        }

        if (player.getHealth() <= REVERT_HEALTH_THRESHOLD) {
            forceDeactivate(player, data);
            return;
        }

        enforceFlight(player, data);
        applyHealthModifier(player);
        applyNightVision(player);
        runEcholocation(player);
        ModPlayerEvents.spawnSunSizzle(player);

        // Force swimming pose for bat form to match hitbox
        if (player.getPose() != Pose.SWIMMING) {
            player.setForcedPose(Pose.SWIMMING);
        }
    }

    public static boolean isActive(Player player) {
        return player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .map(cap -> {
                    AbilityInstance inst = cap.getAbility(ID);
                    return cap.isBatForm() && inst != null && inst.isUnlocked();
                })
                .orElse(false);
    }

    public static void forceDeactivate(Player player) {
        player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .ifPresent(cap -> {
                    AbilityInstance inst = cap.getAbility(ID);
                    if (inst != null && inst.getData().getBoolean(KEY_ACTIVE)
                            && inst.getAbility() instanceof BatTransformationAbility ability) {
                        ability.forceDeactivate(player, inst.getData());
                        cap.setBatForm(player, false);
                        ability.sync(player);
                    }
                });
    }

    private void forceDeactivate(Player player, CompoundTag data) {
        deactivate(player, data);
        data.putBoolean(KEY_ACTIVE, false);
        setBatForm(player, false);
        sync(player);
    }

    private void activate(Player player, CompoundTag data) {
        var abilities = player.getAbilities();
        data.putBoolean(KEY_PREV_MAYFLY, abilities.mayfly);
        data.putBoolean(KEY_PREV_FLYING, abilities.flying);
        data.putFloat(KEY_PREV_FLY_SPEED, abilities.getFlyingSpeed());

        abilities.mayfly = true;
        abilities.flying = true;
        abilities.setFlyingSpeed(abilities.getFlyingSpeed() * FLY_SPEED_MULTIPLIER);
        player.onUpdateAbilities();

        // Set swimming pose for bat form
        player.setForcedPose(Pose.SWIMMING);

        applyHealthModifier(player);
        spawnTransformEffects(player, true);
    }

    private void deactivate(Player player, CompoundTag data) {
        var abilities = player.getAbilities();
        abilities.mayfly = data.getBoolean(KEY_PREV_MAYFLY);
        abilities.flying = data.getBoolean(KEY_PREV_FLYING) && abilities.mayfly;
        if (data.contains(KEY_PREV_FLY_SPEED)) {
            abilities.setFlyingSpeed(data.getFloat(KEY_PREV_FLY_SPEED));
        }
        player.onUpdateAbilities();

        // Clear forced pose to return to normal
        player.setForcedPose(null);

        clearHealthModifier(player);
        clearWetSlow(player);
        player.removeEffect(MobEffects.NIGHT_VISION);
        spawnTransformEffects(player, false);
    }

    private void enforceFlight(Player player, CompoundTag data) {
        var abilities = player.getAbilities();
        boolean wet = player.isInWaterRainOrBubble();
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (wet) {
            if (abilities.flying || abilities.mayfly) {
                abilities.flying = false;
                abilities.mayfly = false;
                player.onUpdateAbilities();
            }
            if (movement != null && !movement.hasModifier(WET_SLOW)) {
                movement.addTransientModifier(WET_SLOW);
            }
            return;
        }

        boolean changed = false;
        if (!abilities.mayfly) {
            abilities.mayfly = true;
            changed = true;
        }

        float baseFly = data.contains(KEY_PREV_FLY_SPEED) ? data.getFloat(KEY_PREV_FLY_SPEED) : abilities.getFlyingSpeed();
        float batFly = baseFly * FLY_SPEED_MULTIPLIER;
        if (abilities.getFlyingSpeed() != batFly) {
            abilities.setFlyingSpeed(batFly);
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }

        if (movement != null && movement.hasModifier(WET_SLOW)) {
            movement.removeModifier(WET_SLOW);
        }
    }

    private void applyHealthModifier(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && !maxHealth.hasModifier(HEALTH_MODIFIER)) {
            maxHealth.addTransientModifier(HEALTH_MODIFIER);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private void clearHealthModifier(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.hasModifier(HEALTH_MODIFIER)) {
            maxHealth.removeModifier(HEALTH_MODIFIER);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private void clearWetSlow(Player player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && movement.hasModifier(WET_SLOW)) {
            movement.removeModifier(WET_SLOW);
        }
    }

    private void applyNightVision(Player player) {
        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null || effect.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
        }
    }

    private void runEcholocation(Player player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        if (player.tickCount % ECHO_INTERVAL != 0) {
            return;
        }

        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(ECHO_RADIUS),
                entity -> entity != player && entity.isAlive());

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, true, false));
        }
    }

    private void spawnTransformEffects(Player player, boolean entering) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5D, player.getZ(),
                entering ? 30 : 15, 0.5D, 0.5D, 0.5D, 0.2D);
        server.playSound(null, player.blockPosition(),
                entering ? SoundEvents.BAT_TAKEOFF : SoundEvents.PHANTOM_BITE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                    .ifPresent(cap -> NetworkHandler.sendToTrackingAndSelf(serverPlayer,
                            new AbilitySyncPacket(serverPlayer.getId(), cap.serializeNBT())));
        }
    }

    private void setBatForm(Player player, boolean value) {
        player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .ifPresent(cap -> cap.setBatForm(player, value));
    }
}
