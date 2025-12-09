package com.vampiremod.effect;

import com.vampiremod.event.ModPlayerEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SunburnEffect extends MobEffect {
    private static final int FIRE_REFRESH_RATE = 20;
    private static final int DEBUFF_DURATION = 60;

    public SunburnEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF7A2D);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % FIRE_REFRESH_RATE == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }

        player.getPersistentData().putLong(ModPlayerEvents.SUN_TICK_KEY, player.level().getGameTime());

        player.setSecondsOnFire(8);
        applyDebuff(player, MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, 0);
        applyDebuff(player, MobEffects.WEAKNESS, DEBUFF_DURATION, 0);
    }

    private void applyDebuff(Player player, net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        MobEffectInstance inst = player.getEffect(effect);
        if (inst == null || inst.getAmplifier() != amplifier || inst.getDuration() < duration / 2) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, false));
        }
    }
}
