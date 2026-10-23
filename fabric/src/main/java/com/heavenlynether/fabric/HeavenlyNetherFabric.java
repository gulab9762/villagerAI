package com.heavenlynether.fabric;

import com.heavenlynether.HeavenlyNetherCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;

public class HeavenlyNetherFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HeavenlyNetherCommon.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("heaven")
                            .executes(context -> {
                                try {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ResourceKey<Level> dimKey = ResourceKey.create(
                                            Registries.DIMENSION,
                                            Identifier.fromNamespaceAndPath("heavenlynether", "heavenly_nether")
                                    );
                                    ServerLevel targetLevel = context.getSource().getServer().getLevel(dimKey);
                                    if (targetLevel != null) {
                                        player.teleportTo(targetLevel, 0.5, 70.0, 0.5, Set.of(), 0.0f, 0.0f, true);
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("§a[Heavenly Nether] §fWelcome to the Heavenly Underground!"),
                                                false
                                        );
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("§c[Heavenly Nether] §fDimension 'heavenlynether:heavenly_nether' not found.")
                                        );
                                        return 0;
                                    }
                                } catch (Exception e) {
                                    context.getSource().sendFailure(Component.literal("§cOnly players can run this command."));
                                    return 0;
                                }
                            })
            );
        });
    }
}
