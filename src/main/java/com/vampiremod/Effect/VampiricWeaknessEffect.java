package com.vampiremod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class VampiricWeaknessEffect extends MobEffect {
    private static final int DEBUFF_REFRESH_RATE = 20; // Apply debuffs every second
    private static final int DEBUFF_DURATION = 60; // 3 seconds duration for each debuff

    public VampiricWeaknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000); // Dark red color
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % DEBUFF_REFRESH_RATE == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }

        // Apply Slowness II (amplifier 1)
        applyDebuff(player, MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, 1);

        // Apply Weakness I (amplifier 0)
        applyDebuff(player, MobEffects.WEAKNESS, DEBUFF_DURATION, 0);
    }

    private void applyDebuff(Player player, MobEffect effect, int duration, int amplifier) {
        MobEffectInstance inst = player.getEffect(effect);
        if (inst == null || inst.getAmplifier() != amplifier || inst.getDuration() < duration / 2) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, false));
        }
    }
}
