package com.vampiremod.event;

import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.capability.PlayerVampireData;
import com.vampiremod.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class ModPlayerEvents {
    public static final String SUN_TICK_KEY = "vampiremod.sunburn";
    public static final String SUN_EXPOSURE_KEY = "vampiremod.sun_exposure";
    private static final String BLOOD_HEAL_KEY = "vampiremod.blood_heal";
    private static final int SUN_EXPOSURE_THRESHOLD = 200;
    private static final int SUN_EXPOSURE_CAP = 400;
    private static final int HELMET_DAMAGE_INTERVAL = 40;
    private static final UUID VAMPIRE_SPEED_ID = UUID.fromString("6a6b75b0-6f9b-4f74-9e5e-5fb1648c3e32");
    private static final AttributeModifier VAMPIRE_SPEED =
            new AttributeModifier(VAMPIRE_SPEED_ID, "Vampire speed bonus", 0.05D, AttributeModifier.Operation.ADDITION);
    private static final UUID VAMPIRE_HEALTH_ID = UUID.fromString("d4b76c5e-5603-4b2e-8b48-5c6a5ae4e1a7");
    private static final AttributeModifier VAMPIRE_HEALTH =
            new AttributeModifier(VAMPIRE_HEALTH_ID, "Vampire health bonus", 4.0D, AttributeModifier.Operation.ADDITION);

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;
        player.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> {
            boolean vamp = cap.isVampire();
            handleAttributes(player, vamp);
            if (vamp) {
                keepBreathing(player);
                keepHungerSatisfied(player);
                burnInSunlight(player);
                applyBuffs(player, cap);
                regenFromBlood(player, cap);
            } else {
                clearBuffs(player);
            }
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        ModCapabilities.sync(player);

        // Reapply attributes immediately on login
        player.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> {
            handleAttributes(player, cap.isVampire());
        });
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        var player = event.getEntity();
        ModCapabilities.sync(player);

        player.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> {
            cap.setBlood(20);

            // Reapply attributes immediately on respawn
            handleAttributes(player, cap.isVampire());
        });
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        boolean vamp = player.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false);
        if (!vamp) return;

        // Allow our own blood-based healing through
        if (player.getPersistentData().getBoolean(BLOOD_HEAL_KEY)) {
            return;
        }

        // If this heal looks like vanilla food-based regen, cancel it.
        if (player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
            var food = player.getFoodData();
            boolean isLikelyFoodRegen = food.getFoodLevel() >= 18 && !player.hasEffect(MobEffects.REGENERATION) && event.getAmount() <= 1.0F;
            if (isLikelyFoodRegen) {
                event.setCanceled(true);
            }
        }
    }

    private static void handleAttributes(Player player, boolean vamp) {
        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (speedAttr == null || healthAttr == null) return;

        boolean hasSpeed = speedAttr.hasModifier(VAMPIRE_SPEED);
        boolean hasHealth = healthAttr.hasModifier(VAMPIRE_HEALTH);
        if (vamp) {
            if (!hasSpeed) {
                speedAttr.addPermanentModifier(VAMPIRE_SPEED);
            }
            if (!hasHealth) {
                healthAttr.addPermanentModifier(VAMPIRE_HEALTH);
            }
        } else {
            if (hasSpeed) {
                speedAttr.removeModifier(VAMPIRE_SPEED_ID);
            }
            if (hasHealth) {
                healthAttr.removeModifier(VAMPIRE_HEALTH_ID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
            clearSunburn(player);
        }
    }

    private static void applyBuffs(Player player, PlayerVampireData cap) {
        if (cap.getBlood() <= 0f) {
            clearBuffs(player);
            ensureEffect(player, ModEffects.VAMPIRIC_WEAKNESS.get(), 60, 0, false, false, true);
            return;
        }
        player.removeEffect(ModEffects.VAMPIRIC_WEAKNESS.get());

        // Strength I
        ensureEffect(player, MobEffects.DAMAGE_BOOST, 220, 0);
        // Jump boost for higher jumps (roughly ~1.5 blocks)
        ensureEffect(player, MobEffects.JUMP, 220, 1);
    }

    private static void clearBuffs(Player player) {
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.JUMP);
    }

    private static void keepHungerSatisfied(Player player) {
        var food = player.getFoodData();
        if (food.getFoodLevel() < 20) {
            food.setFoodLevel(20);
        }
        if (food.getSaturationLevel() < 20.0F) {
            food.setSaturation(20.0F);
        }
        food.setExhaustion(0.0F);
    }

    private static void keepBreathing(Player player) {
        player.setAirSupply(player.getMaxAirSupply());
    }

    private static void clearSunburn(Player player) {
        player.getPersistentData().putInt(SUN_EXPOSURE_KEY, 0);
        player.removeEffect(ModEffects.SUNBURN.get());
    }

    private static void ensureEffect(Player player, MobEffect effect, int duration, int amplifier) {
        ensureEffect(player, effect, duration, amplifier, true, false, false);
    }

    private static void ensureEffect(Player player, MobEffect effect, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
        MobEffectInstance inst = player.getEffect(effect);
        if (inst == null || inst.getAmplifier() != amplifier || inst.getDuration() < duration / 2) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, showParticles, showIcon));
        }
    }

    private static void regenFromBlood(Player player, PlayerVampireData cap) {
        if (player.tickCount % 20 != 0) return; // every second
        if (player.getHealth() >= player.getMaxHealth()) return;
        if (cap.getBlood() < 0.5f) return;

        cap.setBlood(cap.getBlood() - 0.5f); // consume half a drop
        // Mark this heal so we don't cancel it in the global heal handler
        player.getPersistentData().putBoolean(BLOOD_HEAL_KEY, true);
        player.heal(1.0F);
        player.getPersistentData().remove(BLOOD_HEAL_KEY);
        ModCapabilities.sync(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        boolean vamp = player.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false);
        if (!vamp) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_DROWNING)) {
            event.setCanceled(true);
        }
    }

    private static void burnInSunlight(Player player) {
        if (player.isSpectator() || player.isCreative()) {
            clearSunburn(player);
            return;
        }
        var level = player.level();
        if (!level.isDay()) {
            clearSunburn(player);
            return;
        }

        float brightness = player.getLightLevelDependentMagicValue();
        if (brightness <= 0.5F) {
            clearSunburn(player);
            return;
        }

        var pos = player.blockPosition();
        if (!level.canSeeSky(pos)) {
            clearSunburn(player);
            return;
        }

        spawnSunSizzle(player);

        var helmet = player.getInventory().getArmor(3);
        if (!helmet.isEmpty()) {
            if (player.tickCount % HELMET_DAMAGE_INTERVAL == 0) {
                helmet.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.HEAD));
            }
            clearSunburn(player);
            return;
        }

        int exposure = Math.min(SUN_EXPOSURE_CAP, player.getPersistentData().getInt(SUN_EXPOSURE_KEY) + 1);
        player.getPersistentData().putInt(SUN_EXPOSURE_KEY, exposure);

        if (exposure >= SUN_EXPOSURE_THRESHOLD) { // 10 seconds grace, then periodic burn/debuff maintenance
            player.getPersistentData().putLong(SUN_TICK_KEY, player.level().getGameTime());
            ensureEffect(player, ModEffects.SUNBURN.get(), 60, 0, false, false, true);
        } else {
            player.removeEffect(ModEffects.SUNBURN.get());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        boolean vamp = player.getCapability(ModCapabilities.VAMPIRE_CAP).map(c -> c.isVampire()).orElse(false);
        if (!vamp) return;

        DamageSource source = event.getSource();
        long lastSun = player.getPersistentData().getLong(SUN_TICK_KEY);
        boolean recentSun = player.level().getGameTime() - lastSun <= 60;
        boolean sun = recentSun && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && player.level().isDay() && player.isOnFire();
        boolean woodenSword = source.getDirectEntity() instanceof Player attacker &&
                attacker.getMainHandItem().is(Items.WOODEN_SWORD);

        if (!sun && !woodenSword) {
            event.setCanceled(true);
            player.setHealth(1.0F);
        }
    }

    public static void spawnSunSizzle(Player player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        if (!player.level().isDay()) {
            return;
        }
        if (!player.level().canSeeSky(player.blockPosition())) {
            return;
        }
        if (!player.getInventory().getArmor(3).isEmpty()) {
            return;
        }
        if (player.getLightLevelDependentMagicValue() <= 0.5F) {
            return;
        }
        server.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + player.getBbHeight() * 0.6D, player.getZ(),
                2, 0.2D, 0.2D, 0.2D, 0.0D);
    }
}
