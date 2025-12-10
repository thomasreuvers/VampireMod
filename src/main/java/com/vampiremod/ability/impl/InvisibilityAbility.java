package com.vampiremod.ability.impl;

import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.AbstractAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class InvisibilityAbility extends AbstractAbility {
    private static final int DURATION = 200; // 10 seconds

    public InvisibilityAbility(ResourceLocation id) {
        super(id, DURATION, 0, 2, SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public boolean execute(Player player, AbilityInstance instance) {
        player.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY,
                DURATION,
                0,
                false,
                false
        ));

        return true;
    }
}
