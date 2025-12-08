package com.vampiremod.event;

import com.vampiremod.Capability.ModCapabilities;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class ModPlayerEvents {
    private static final String SUN_TICK_KEY = "vampiremod.sunburn";
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
        if (vamp && !has) {
            attr.addTransientModifier(VAMPIRE_SPEED);
        } else if (!vamp && has) {
            attr.removeModifier(VAMPIRE_SPEED);
        }
    }

    private static void applyBuffs(Player player, com.vampiremod.Capability.PlayerVampireData cap) {
        if (cap.getBlood() <= 0f) {
            clearBuffs(player);
            ensureEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
            ensureEffect(player, MobEffects.WEAKNESS, 60, 0);
            return;
        }
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        // Strength I
        ensureEffect(player, MobEffects.DAMAGE_BOOST, 220, 0);
        // Jump boost for higher jumps (roughly ~1.5 blocks)
        ensureEffect(player, MobEffects.JUMP, 220, 1);
    }

    private static void clearBuffs(Player player) {
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.JUMP);
    }

    private static void ensureEffect(Player player, net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        MobEffectInstance inst = player.getEffect(effect);
        if (inst == null || inst.getAmplifier() != amplifier || inst.getDuration() < duration / 2) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, false));
        }
    }

    private static void regenFromBlood(Player player, com.vampiremod.Capability.PlayerVampireData cap) {
        if (player.tickCount % 40 != 0) return; // every 2 seconds
        if (player.getHealth() >= player.getMaxHealth()) return;
        if (cap.getBlood() < 1f) return;

        cap.setBlood(cap.getBlood() - 1f); // consume half drop
        player.heal(1.0F);
        ModCapabilities.sync(player);
    }

    private static void burnInSunlight(Player player) {
        if (player.isSpectator() || player.isCreative()) return;
        var level = player.level();
        if (!level.isDay()) return;

        float brightness = player.getLightLevelDependentMagicValue();
        if (brightness <= 0.5F) return;

        var pos = player.blockPosition();
        if (!level.canSeeSky(pos)) return;

        var helmet = player.getInventory().getArmor(3);
        boolean hasHelmet = !helmet.isEmpty();
        if (!hasHelmet) {
            // Mark sun exposure and let vanilla fire tick handle the damage cadence.
            player.getPersistentData().putLong(SUN_TICK_KEY, player.level().getGameTime());
            player.setSecondsOnFire(8);
        } else if (player.tickCount % 40 == 0) {
            helmet.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.HEAD));
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
