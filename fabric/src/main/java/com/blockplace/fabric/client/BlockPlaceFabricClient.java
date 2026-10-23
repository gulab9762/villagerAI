package com.blockplace.fabric.client;

import com.blockplace.Constants;
import com.blockplace.client.animation.BlockPlacementManager;
import com.blockplace.client.config.BlockPlaceConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class BlockPlaceFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Initializing {} on Fabric Client", Constants.MOD_NAME);

        // Load persisted config on client startup
        BlockPlaceConfig.load();

        // Tick active block placement animations every client game tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !client.isPaused()) {
                BlockPlacementManager.tick(client.level);
            }
        });

        // Reset flight animations on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BlockPlacementManager.clear();
        });

        // Register client-side /blockplace command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("blockplace")
                            .then(ClientCommands.literal("toggle")
                                    .executes(context -> toggleAnimation(context.getSource()))
                            )
                            .then(ClientCommands.literal("style")
                                    .then(ClientCommands.argument("value", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                for (BlockPlaceConfig.AnimationStyle s : BlockPlaceConfig.AnimationStyle.values()) {
                                                    builder.suggest(s.name().toLowerCase());
                                                }
                                                builder.suggest("vanilla");
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                String val = StringArgumentType.getString(context, "value");
                                                return applyStyleCommand(context.getSource(), val);
                                            })
                                    )
                                    .executes(context -> showCurrentStyle(context.getSource()))
                            )
                            .then(ClientCommands.argument("value", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        for (BlockPlaceConfig.AnimationStyle s : BlockPlaceConfig.AnimationStyle.values()) {
                                            builder.suggest(s.name().toLowerCase());
                                        }
                                        builder.suggest("vanilla");
                                        builder.suggest("toggle");
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String val = StringArgumentType.getString(context, "value");
                                        if (val.equalsIgnoreCase("toggle")) {
                                            return toggleAnimation(context.getSource());
                                        }
                                        return applyStyleCommand(context.getSource(), val);
                                    })
                            )
                            .executes(context -> showCurrentStyle(context.getSource()))
            );
        });
    }

    private static int toggleAnimation(FabricClientCommandSource source) {
        if (BlockPlaceConfig.style == BlockPlaceConfig.AnimationStyle.NONE) {
            BlockPlaceConfig.style = BlockPlaceConfig.AnimationStyle.ROTATING;
            BlockPlaceConfig.save();
            source.sendFeedback(Component.literal("§a[Block Place] §fAnimations §aENABLED §f(style: §erotating§f)"));
        } else {
            BlockPlaceConfig.style = BlockPlaceConfig.AnimationStyle.NONE;
            BlockPlaceConfig.save();
            source.sendFeedback(Component.literal("§a[Block Place] §fAnimations §cDISABLED §7(pure vanilla placement)"));
        }
        return 1;
    }

    private static int applyStyleCommand(FabricClientCommandSource source, String val) {
        if (BlockPlaceConfig.setStyle(val)) {
            String name = (BlockPlaceConfig.style == BlockPlaceConfig.AnimationStyle.NONE) ? "none (vanilla placement)" : BlockPlaceConfig.style.name().toLowerCase();
            source.sendFeedback(Component.literal("§a[Block Place] §fAnimation style set to: §e" + name));
            return 1;
        } else {
            source.sendError(Component.literal("§c[Block Place] §fUnknown style: '§c" + val + "§f'. Available: §esmooth, rotating, pop, drop, spiral, expand, spring, none"));
            return 0;
        }
    }

    private static int showCurrentStyle(FabricClientCommandSource source) {
        String name = (BlockPlaceConfig.style == BlockPlaceConfig.AnimationStyle.NONE) ? "none (vanilla)" : BlockPlaceConfig.style.name().toLowerCase();
        source.sendFeedback(Component.literal("§a[Block Place] §fCurrent style: §e" + name + " §7(Available: §esmooth, rotating, pop, drop, spiral, expand, spring, none§7)"));
        source.sendFeedback(Component.literal("§7Use §f/blockplace style <name> §7or §f/blockplace toggle"));
        return 1;
    }
}
