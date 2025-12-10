package com.vampiremod.event;

import com.vampiremod.ability.impl.BatTransformationAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BatFormEvents {
    private static final String WARN_KEY = "vampiremod.bat_warn";

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker && BatTransformationAbility.isActive(attacker)) {
            event.setAmount(event.getAmount() * 0.6F);
        }

        if (event.getEntity() instanceof Player player && BatTransformationAbility.isActive(player)) {
            event.setAmount(event.getAmount() * 1.25F);
            float projectedHealth = player.getHealth() - event.getAmount();
            if (projectedHealth <= BatTransformationAbility.REVERT_HEALTH_THRESHOLD) {
                BatTransformationAbility.forceDeactivate(player);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (shouldBlock(player)) {
            event.setCanceled(true);
            warn(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (shouldBlock(player)) {
            event.setCanceled(true);
            warn(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (shouldBlock(player)) {
            event.setCanceled(true);
            warn(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (shouldBlock(player)) {
            event.setCanceled(true);
            warn(player);
        }
    }

    private static boolean shouldBlock(Player player) {
        return BatTransformationAbility.isActive(player);
    }

    private static void warn(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(WARN_KEY);
        if (now - last > 20) {
            player.getPersistentData().putLong(WARN_KEY, now);
            player.displayClientMessage(Component.literal("Cannot use items or attack while in bat form."), true);
        }
    }
}
