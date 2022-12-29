/*
 * Copyright (c) 2022.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package de.christianbernstein.forge.chronos;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import de.christianbernstein.chronos.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("examplemod")
public class ChronosMod {

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ChronosAPI chronosShared;

    public ChronosMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(@NotNull ServerStartingEvent event) {
        // Init chronos :: Supply with locally created Server-Chronos-Bridge
        ChronosMod.chronosShared = new ChronosAPI(new ChronosAPIBridge() {
            @Override
            public void onSessionTimeThresholdReached(@NotNull SessionLeftoverTimeThresholdReachedEvent ev) {
                final ServerPlayer player = event.getServer().getPlayerList().getPlayerByName(ev.getUser().getId());
                if (player == null) return;
                player.sendMessage(new TextComponent(String.format("You have %s %s left", ev.getThreshold().getMeasurand(), ev.getThreshold().getUnit().toString().toLowerCase())), ChatType.CHAT, player.getUUID());
            }

            @Override
            public void onSessionMarkedAsExpired(@NotNull SessionMarkedAsExpiredEvent ev) {
                LOGGER.info("Session of user '{}' was marked as expired", ev.getUser().getId());
                final ServerPlayer player = event.getServer().getPlayerList().getPlayerByName(ev.getUser().getId());
                Objects.requireNonNull(player).connection.disconnect(new TextComponent("You ran out of time!"));
            }

            @NotNull
            @Override
            public List<String> getAllActiveUsers() {
                return Arrays.asList(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerNamesArray());
            }

            @NotNull
            @Override
            public File getWorkingDirectory() {
                return new File("chronos\\");
            }
        });

        ChronosMod.chronosShared.start();

        final Consumer<String> createAdminUser = id -> {
            chronosShared.createUser(id);
            chronosShared.updateUser(id, user -> {
                user.setOperator(true);
                return user;
            });
        };

        createAdminUser.accept("CWies");
        createAdminUser.accept("zZChrisZz");
    }

    @SubscribeEvent
    public void onServerStop(@NotNull ServerStoppingEvent event) {
        chronosShared.shutdown();
    }

    /**
     * Player logged into the game.
     * Check if the player can join:
     *  IF FALSE: Kick the player with an appropriate message
     *  IF TRUE: Let the play join & start a new user session in chronos
     */
    @SubscribeEvent
    public void onPlayerLoggedInEvent(@NotNull final PlayerEvent.PlayerLoggedInEvent event) {
        final String name = event.getPlayer().getName().getString();
        final UUID uuid = event.getPlayer().getUUID();
        chronosShared.requestJoin(name, () -> {
            // User is permitted to join
            chronosShared.executeSessionStart(name);
        }, () -> {
            // user isn't permitted to join
            Objects.requireNonNull(Objects.requireNonNull(event.getEntityLiving().getServer()).getPlayerList().getPlayer(uuid)).connection.disconnect(
                    new TextComponent("Sorry, no time to play!")
            );
        });
    }

    @SubscribeEvent
    public void onPlayerLoggedOutEvent(@NotNull final PlayerEvent.PlayerLoggedOutEvent event) {
        chronosShared.executeSessionStop(event.getPlayer().getName().getString(), ActionMode.ACTION);
    }

    @SubscribeEvent
    public void onRegisterCommandEvent(@NotNull final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("chronos")
                // Time command :: Gets the remaining session time of the calling player
                .then(Commands.literal("time").executes(context -> {
                    try {
                        final Duration timeLeft = chronosShared.getTimeLeft(context.getSource().getDisplayName().getString());
                        final long s = timeLeft.getSeconds();
                        final String formatted = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
                        context.getSource().sendSuccess(new TextComponent(
                                String.format("You have %s left", formatted)
                        ), false);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        return 1;
                    }

                    return 1;
                }))

                .then(Commands.literal("admin")
                        // Replenish command :: Runs replenish routine at execution time
                        .then(Commands.literal("replenish").executes(context -> {
                            chronosShared.replenish();
                            sendBasicChatMessage(context, "Replenish-routing completed");
                            return 1;
                        }))

                        .then(Commands.literal("haltGlobal").executes(context -> {
                            chronosShared.stopGlobalTimer(ChronosAPI.Companion.getConsole());
                            sendBasicChatMessage(context, "Global timer was haltet");
                            return 1;
                        }))

                        .then(Commands.literal("resumeGlobal").executes(context -> {
                            chronosShared.startGlobalTimer(ChronosAPI.Companion.getConsole());
                            sendBasicChatMessage(context, "Global timer was resumed");
                            return 1;
                        }))

                        .then(Commands.literal("halt").executes(context -> {
                            chronosShared.pauseTimerFor(context.getSource().getDisplayName().getString());
                            sendBasicChatMessage(context, "Your timer was haltet");
                            return 1;
                        }))

                        .then(Commands.literal("resume").executes(context -> {
                            chronosShared.resumeTimerFor(context.getSource().getDisplayName().getString());
                            sendBasicChatMessage(context, "Your timer was resumed");
                            return 1;
                        }))
                )
        );
    }

    private void sendBasicChatMessage(@NotNull final CommandContext<CommandSourceStack> context, final String message) {
        context.getSource().sendSuccess(new TextComponent(message), false);
    }
}
