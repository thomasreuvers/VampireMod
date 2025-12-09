package com.vampiremod.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.vampiremod.capability.ModCapabilities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VampireCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("vampire")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> setVampire(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), true))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> setVampire(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), false))))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> toggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        event.getDispatcher().register(root);
    }

    private static int setVampire(CommandSourceStack source, Player target, boolean value) {
        target.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> cap.setVampire(value));
        ModCapabilities.sync(target);
        source.sendSuccess(() -> Component.literal(
                target.getName().getString() + " vampire state set to " + value), true);
        return 1;
    }

    private static int toggle(CommandSourceStack source, Player target) {
        final boolean[] result = {false};
        target.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> {
            cap.setVampire(!cap.isVampire());
            result[0] = cap.isVampire();
        });
        ModCapabilities.sync(target);
        source.sendSuccess(() -> Component.literal(
                target.getName().getString() + " vampire state set to " + result[0]), true);
        return 1;
    }
}
