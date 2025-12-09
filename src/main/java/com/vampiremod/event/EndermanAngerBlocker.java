package com.vampiremod.event;

import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.capability.PlayerVampireData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.EnderManAngerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EndermanAngerBlocker {
    // Prevent endermen from getting angry at vampire players staring at them.
    @SubscribeEvent
    public static void onEndermanAnger(EnderManAngerEvent event) {
        var player = event.getPlayer();
        if (!(player instanceof Player)) {
            return;
        }
        boolean isVampire = player.getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false);
        if (isVampire) {
            event.setCanceled(true);
        }
    }
}
