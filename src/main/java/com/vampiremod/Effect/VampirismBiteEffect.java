package com.vampiremod.effect;

import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.capability.PlayerVampireData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class VampirismBiteEffect extends MobEffect {
    public VampirismBiteEffect() {
        super(MobEffectCategory.HARMFUL, 0x880000);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) { return true; }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && !player.level().isClientSide) {
            var inst = player.getEffect(ModEffects.VAMPIRISM_BITE.get());
            if (inst != null && inst.getDuration() <= 1) {
                PlayerVampireData.get(player).setVampire(true);
                ModCapabilities.sync(player);
            }
        }
    }
}
