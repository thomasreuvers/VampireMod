package com.vampiremod.event;

import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.capability.PlayerVampireData;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MobTargetingHandler {
    // Keep monsters neutral to vampires unless they recently hurt the mob; golems stay hostile.
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Monster monster) || event.getEntity() instanceof IronGolem) {
            return;
        }
        if (event.getNewTarget() instanceof Player player) {
            boolean isVampire = player.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false);
            if (isVampire && !recentlyHurtBy(monster, player)) {
                event.setNewTarget(null);
            }
        }
    }

    private static boolean recentlyHurtBy(Monster monster, Player player) {
        if (monster.getLastHurtByMob() != player) return false;
        int elapsed = monster.tickCount - monster.getLastHurtByMobTimestamp();
        return elapsed <= 200; // 10 seconds grace window
    }
}
