package com.blockplace.neoforge;

import com.blockplace.BlockPlaceCommon;
import com.blockplace.Constants;
import com.blockplace.client.animation.BlockPlacementManager;
import com.blockplace.client.config.BlockPlaceConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@Mod(Constants.MOD_ID)
public class BlockPlaceNeoForge {

    public BlockPlaceNeoForge() {
        BlockPlaceCommon.init();
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && !client.isPaused()) {
                BlockPlacementManager.tick(client.level);
            }
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            BlockPlaceConfig.load();

            event.getDispatcher().register(
                    Commands.literal("blockplace")
                            .then(Commands.literal("toggle")
                                    .executes(context -> toggleAnimation(context.getSource()))
                            )
                            .then(Commands.literal("style")
                                    .then(Commands.argument("value", StringArgumentType.word())
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
                            .then(Commands.argument("value", StringArgumentType.word())
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
        }

        private static int toggleAnimation(CommandSourceStack source) {
            if (BlockPlaceConfig.style == BlockPlaceConfig.AnimationStyle.NONE) {
                BlockPlaceConfig.style = BlockPlaceConfig.AnimationStyle.ROTATING;
                BlockPlaceConfig.save();
                source.sendSuccess(() -> Component.literal("§a[Block Place] §fAnimations §aENABLED §f(style: §erotating§f)"), false);
            } else {
                BlockPlaceConfig.style = BlockPlaceConfig.AnimationStyle.NONE;
                BlockPlaceConfig.save();
                source.sendSuccess(() -> Component.literal("§a[Block Place] §fAnimations §cDISABLED §7(pure vanilla placement)"), false);
            }
            return 1;
        }

        private static int applyStyleCommand(CommandSourceStack source, String val) {
            if (BlockPlaceConfig.setStyle(val)) {
                String name = (BlockPlaceConfig.style == BlockPlaceConfig.AnimationStyle.NONE) ? "none (vanilla placement)" : BlockPlaceConfig.style.name().toLowerCase();
                source.sendSuccess(() -> Component.literal("§a[Block Place] §fAnimation style set to: §e" + name), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[Block Place] §fUnknown style: '§c" + val + "§f'. Available: §esmooth, rotating, pop, drop, spiral, expand, spring, none"));
                return 0;
            }
        }

        private static int showCurrentStyle(CommandSourceStack source) {
            String name = (BlockPlaceConfig.style == BlockPlaceConfig.AnimationStyle.NONE) ? "none (vanilla)" : BlockPlaceConfig.style.name().toLowerCase();
            source.sendSuccess(() -> Component.literal("§a[Block Place] §fCurrent style: §e" + name + " §7(Available: §esmooth, rotating, pop, drop, spiral, expand, spring, none§7)"), false);
            source.sendSuccess(() -> Component.literal("§7Use §f/blockplace style <name> §7or §f/blockplace toggle"), false);
            return 1;
        }
    }
}
