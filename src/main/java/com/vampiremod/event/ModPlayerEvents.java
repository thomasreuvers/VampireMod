package com.vampiremod.event;

import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.capability.PlayerVampireData;
import com.vampiremod.effect.ModEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class ModPlayerEvents {
    public static final String SUN_TICK_KEY = "vampiremod.sunburn";
    public static final String SUN_EXPOSURE_KEY = "vampiremod.sun_exposure";
    private static final int SUN_EXPOSURE_THRESHOLD = 200;
    private static final int SUN_EXPOSURE_CAP = 400;
    private static final int HELMET_DAMAGE_INTERVAL = 40;
    private static final UUID VAMPIRE_SPEED_ID = UUID.fromString("6a6b75b0-6f9b-4f74-9e5e-5fb1648c3e32");
    private static final AttributeModifier VAMPIRE_SPEED =
            new AttributeModifier(VAMPIRE_SPEED_ID, "Vampire speed bonus", 0.08D, AttributeModifier.Operation.ADDITION);

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;
        player.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> {
            boolean vamp = cap.isVampire();
            handleSpeed(player, vamp);
            if (vamp) {
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
        ModCapabilities.sync(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ModCapabilities.sync(event.getEntity());
        event.getEntity().getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> cap.setBlood(20));
    }

    private static void handleSpeed(Player player, boolean vamp) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        boolean has = attr.hasModifier(VAMPIRE_SPEED);
        if (vamp) {
            if (!has) {
                attr.addTransientModifier(VAMPIRE_SPEED);
            }
        } else {
            if (has) {
                attr.removeModifier(VAMPIRE_SPEED);
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
        if (player.tickCount % 40 != 0) return; // every 2 seconds
        if (player.getHealth() >= player.getMaxHealth()) return;
        if (cap.getBlood() < 1f) return;

        cap.setBlood(cap.getBlood() - 1f); // consume half drop
        player.heal(1.0F);
        ModCapabilities.sync(player);
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
}
