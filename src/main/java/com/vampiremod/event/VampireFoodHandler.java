package com.vampiremod.event;

import com.vampiremod.capability.ModCapabilities;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VampireFoodHandler {

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        boolean isVampire = player.getCapability(ModCapabilities.VAMPIRE_CAP).map(cap -> cap.isVampire()).orElse(false);
        if (!isVampire) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        if (!stack.isEdible()) return; // only block true food

        // Block consumption of regular food; apply nausea.
        event.setCanceled(true);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 10, 0)); // nausea for 10s
    }
}
