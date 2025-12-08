package com.vampiremod.event;

import com.vampiremod.Capability.ModCapabilities;
import com.vampiremod.config.VampireConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EntityBloodInitHandler {
    @SubscribeEvent
    public static void onJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (event.getLevel().isClientSide()) return;
        living.getCapability(ModCapabilities.BLOOD_CAP).ifPresent(cap -> {
            if (cap.getMax() > 0) return; // already initialized (e.g., loaded from save)
            EntityType<?> type = living.getType();
            cap.setDrinkable(!VampireConfig.isBlacklisted(type));
            int max = Math.max(1, (int)Math.ceil(living.getMaxHealth()));
            // fallback to config if provided and larger
            max = Math.max(max, VampireConfig.getDefaultBlood(type));
            cap.setMax(max);
            cap.setCurrent(max);
        });
    }
}
