package com.vampiremod.event;

import com.vampiremod.Capability.ModCapabilities;
import com.vampiremod.Capability.PlayerVampireData;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MobTargetingHandler {
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Monster monster) || event.getEntity() instanceof IronGolem) {
            return;
        }
        if (event.getNewTarget() instanceof Player player) {
            boolean isVampire = player.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false);
            // Stay neutral unless the vampire attacked this monster.
            if (isVampire && monster.getLastHurtByMob() != player && monster.getLastHurtByMobTimestamp() + 100 < monster.tickCount) {
                event.setNewTarget(null);
            }
        }
    }
}
