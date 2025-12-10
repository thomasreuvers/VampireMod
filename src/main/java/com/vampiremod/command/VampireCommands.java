package com.vampiremod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.vampiremod.ability.Ability;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.AbilityRegistry;
import com.vampiremod.ability.network.AbilitySyncPacket;
import com.vampiremod.ability.network.NetworkHandler;
import com.vampiremod.capability.AbilityCapabilityProvider;
import com.vampiremod.capability.ModCapabilities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VampireCommands {
    // Suggestion provider for ability IDs
    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggestResource(
                AbilityRegistry.getAllAbilities().stream()
                        .map(Ability::getId),
                builder
        );
    };

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("vampire")
                .requires(src -> src.hasPermission(2))

                // /vampire set <player> - Make player a vampire
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> setVampire(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), true))))

                // /vampire clear <player> - Remove vampire status
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> setVampire(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), false))))

                // /vampire toggle <player> - Toggle vampire status
                .then(Commands.literal("toggle")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> toggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))

                // /vampire ability unlock <player> <ability_id> - Unlock an ability
                .then(Commands.literal("ability")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("ability", ResourceLocationArgument.id())
                                                .suggests(ABILITY_SUGGESTIONS)
                                                .executes(ctx -> unlockAbility(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        ResourceLocationArgument.getId(ctx, "ability")
                                                )))))

                        // /vampire ability lock <player> <ability_id> - Lock/remove an ability
//                        .then(Commands.literal("lock")
//                                .then(Commands.argument("target", EntityArgument.player())
//                                        .then(Commands.argument("ability", ResourceLocationArgument.id())
//                                                .suggests(ABILITY_SUGGESTIONS)
//                                                .executes(ctx -> lockAbility(
//                                                        ctx.getSource(),
//                                                        EntityArgument.getPlayer(ctx, "target"),
//                                                        ResourceLocationArgument.getId(ctx, "ability")
//                                                )))))

                        // /vampire ability list <player> - List all abilities and their status
                        .then(Commands.literal("list")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> listAbilities(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target")
                                        ))))

                        // /vampire ability unlockall <player> - Unlock all abilities
                        .then(Commands.literal("unlockall")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> unlockAllAbilities(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target")
                                        ))))

                        // /vampire ability lockall <player> - Lock all abilities
//                        .then(Commands.literal("lockall")
//                                .then(Commands.argument("target", EntityArgument.player())
//                                        .executes(ctx -> lockAllAbilities(
//                                                ctx.getSource(),
//                                                EntityArgument.getPlayer(ctx, "target")
//                                        ))))

                        // /vampire ability points set <player> <amount> - Set ability points
                        .then(Commands.literal("points")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setAbilityPoints(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "target"),
                                                                IntegerArgumentType.getInteger(ctx, "amount")
                                                        )))))

                                // /vampire ability points add <player> <amount> - Add ability points
                                .then(Commands.literal("add")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> addAbilityPoints(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "target"),
                                                                IntegerArgumentType.getInteger(ctx, "amount")
                                                        )))))

                                // /vampire ability points get <player> - Check ability points
                                .then(Commands.literal("get")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> getAbilityPoints(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target")
                                                ))))));

        event.getDispatcher().register(root);
    }

    // ===== Vampire Status Commands =====

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

    // ===== Ability Management Commands =====

    /**
     * Unlock a specific ability for a player
     */
    private static int unlockAbility(CommandSourceStack source, Player target, ResourceLocation abilityId) {
        Ability ability = AbilityRegistry.getAbility(abilityId);

        if (ability == null) {
            source.sendFailure(Component.literal("Unknown ability: " + abilityId));
            return 0;
        }

        target.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
            AbilityInstance instance = cap.getAbility(abilityId);

            if (instance == null) {
                // Create new instance if it doesn't exist
                instance = new AbilityInstance(ability);
                cap.addAbility(instance);
            }

            instance.unlock();

            // Sync to client if target is a server player
            if (target instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendToPlayer(
                        new AbilitySyncPacket(cap.serializeNBT()),
                        serverPlayer
                );
            }
        });

        source.sendSuccess(() -> Component.literal(
                "Unlocked ability '" + abilityId.getPath() + "' for " + target.getName().getString()), true);
        return 1;
    }

    /**
     * List all abilities and their status for a player
     */
    private static int listAbilities(CommandSourceStack source, Player target) {
        target.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
            source.sendSuccess(() -> Component.literal(
                    "=== Abilities for " + target.getName().getString() + " ==="), false);

            source.sendSuccess(() -> Component.literal(
                    "Ability Points: " + cap.getAbilityPoints()), false);

            for (Ability ability : AbilityRegistry.getAllAbilities()) {
                AbilityInstance instance = cap.getAbility(ability.getId());
                boolean unlocked = instance != null && instance.isUnlocked();
                int cooldown = instance != null ? instance.getCooldownRemaining() : 0;

                String status = unlocked ? "§a✓ UNLOCKED" : "§c✗ LOCKED";
                String cdInfo = cooldown > 0 ? " §e(Cooldown: " + (cooldown / 20) + "s)" : "";

                source.sendSuccess(() -> Component.literal(
                        "  " + ability.getId().getPath() + ": " + status + cdInfo), false);
            }
        });

        return 1;
    }

    /**
     * Unlock all abilities for a player
     */
    private static int unlockAllAbilities(CommandSourceStack source, Player target) {
        int count = 0;

        for (Ability ability : AbilityRegistry.getAllAbilities()) {
            target.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
                AbilityInstance instance = cap.getAbility(ability.getId());

                if (instance == null) {
                    instance = new AbilityInstance(ability);
                    cap.addAbility(instance);
                }

                instance.unlock();

                // Sync to client
                if (target instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.sendToPlayer(
                            new AbilitySyncPacket(cap.serializeNBT()),
                            serverPlayer
                    );
                }
            });
            count++;
        }

        final int finalCount = count;
        source.sendSuccess(() -> Component.literal(
                "Unlocked " + finalCount + " abilities for " + target.getName().getString()), true);
        return 1;
    }

    /**
     * Set ability points for a player
     */
    private static int setAbilityPoints(CommandSourceStack source, Player target, int amount) {
        target.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
            cap.setAbilityPoints(amount);

            // Sync to client
            if (target instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendToPlayer(
                        new AbilitySyncPacket(cap.serializeNBT()),
                        serverPlayer
                );
            }
        });

        source.sendSuccess(() -> Component.literal(
                "Set ability points to " + amount + " for " + target.getName().getString()), true);
        return 1;
    }

    /**
     * Add ability points to a player
     */
    private static int addAbilityPoints(CommandSourceStack source, Player target, int amount) {
        final int[] newTotal = {0};

        target.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
            int current = cap.getAbilityPoints();
            cap.setAbilityPoints(current + amount);
            newTotal[0] = current + amount;

            // Sync to client
            if (target instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendToPlayer(
                        new AbilitySyncPacket(cap.serializeNBT()),
                        serverPlayer
                );
            }
        });

        source.sendSuccess(() -> Component.literal(
                "Added " + amount + " ability points to " + target.getName().getString() +
                        " (Total: " + newTotal[0] + ")"), true);
        return 1;
    }

    /**
     * Get ability points for a player
     */
    private static int getAbilityPoints(CommandSourceStack source, Player target) {
        target.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
            int points = cap.getAbilityPoints();
            source.sendSuccess(() -> Component.literal(
                    target.getName().getString() + " has " + points + " ability points"), false);
        });

        return 1;
    }
}
