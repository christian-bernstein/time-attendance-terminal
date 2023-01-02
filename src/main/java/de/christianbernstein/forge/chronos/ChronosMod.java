/*
 * Copyright (c) 2022-2023.
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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import de.christianbernstein.chronos.*;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("examplemod")
public class ChronosMod {

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // TODO: ChronosAPI supports internal singleton pattern -> Use it (ChronosAPI.Companion.instance)
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

        // createAdminUser.accept("CWies");
        // createAdminUser.accept("zZChrisZz");
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
        // Create a new user account if the joining user isn't yet registered
        if (!chronosShared.hasUserBeenRegistered(name)) chronosShared.createUser(name);
        // Execute standard join procedure
        chronosShared.requestJoin(name, () -> {
            // User is permitted to join
            final boolean wasSessionStarted = chronosShared.executeSessionStart(name, false);
            // Calculate formatted session matrices
            final Duration timeLeft = chronosShared.getTimeLeft(name);
            final long s = timeLeft.getSeconds();
            final String formatted = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            final String estimatedEndTime = new SimpleDateFormat("HH:mm:ss").format(Date.from(Instant.now().plus(timeLeft)));
            // Send join message

            event.getPlayer().sendMessage(new TextComponent(String.format("You have %s left", formatted)), event.getPlayer().getUUID());
            if (wasSessionStarted) {
                event.getPlayer().sendMessage(new TextComponent(String.format("Your session is estimated to end at %s", estimatedEndTime)), event.getPlayer().getUUID());
            } else {
                event.getPlayer().sendMessage(new TextComponent("Your session hasn't started.").withStyle(ChatFormatting.BOLD), event.getPlayer().getUUID());
            }




        }, () -> {
            // User isn't permitted to join
            Objects.requireNonNull(Objects.requireNonNull(event.getEntityLiving().getServer()).getPlayerList().getPlayer(uuid)).connection.disconnect(
                    new TextComponent("Sorry, no time to play!")
            );
        });
    }

    @SubscribeEvent
    public void onPlayerLoggedOutEvent(@NotNull final PlayerEvent.PlayerLoggedOutEvent event) {
        chronosShared.executeSessionStop(event.getPlayer().getName().getString(), ActionMode.ACTION);
    }

    /**
     * Time command :: Gets the remaining session time of the calling player
     * @param lab Chronos base command node
     */
    private void registerPublicCommands(@NotNull final LiteralArgumentBuilder<CommandSourceStack> lab) {
        lab.then(Commands.literal("time").executes(context -> {
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
        }));
    }

    /**
     * Administrative commands :: Perform administrative tasks, requires permission bypass flag
     * @param lab Chronos base command node
     */
    private void registerAdministrativeCommands(@NotNull final LiteralArgumentBuilder<CommandSourceStack> lab) {
        lab.then(Commands.literal("admin")

                // Admin commands need active 'bypass' flag
                .requires(commandSourceStack -> Objects.requireNonNull(getPlayerContractor(commandSourceStack)).getBypass())

                // Replenish command :: Runs replenish routine at execution time
                .then(Commands.literal("replenish").executes(context -> {
                    final UpdateResult<Unit> result = chronosShared.replenish();
                    this.sendUpdateResponseMessage(context, result, new TextComponent("Replenish-routing completed"));
                    return 1;
                }))

                .then(Commands.literal("haltGlobal").executes(context -> {
                    final UpdateResult<Unit> result = chronosShared.stopGlobalTimer(ChronosAPI.Companion.getConsole());
                    this.sendUpdateResponseMessage(context, result, new TextComponent("Global timer was haltet"));
                    return 1;
                }))

                .then(Commands.literal("resumeGlobal").executes(context -> {
                    final UpdateResult<Unit> result = chronosShared.startGlobalTimer(ChronosAPI.Companion.getConsole());
                    this.sendUpdateResponseMessage(context, result, new TextComponent("Global timer was resumed"));
                    return 1;
                }))

                .then(Commands.literal("halt").executes(context -> {
                    final UpdateResult<Unit> result = chronosShared.pauseTimerFor(getPlayerContractor(context.getSource()), context.getSource().getDisplayName().getString());
                    this.sendUpdateResponseMessage(context, result, new TextComponent("Your timer was haltet"));
                    return 1;
                }))

                .then(Commands.literal("resume").executes(context -> {
                    final UpdateResult<Boolean> result = chronosShared.resumeTimerFor(getPlayerContractor(context.getSource()), context.getSource().getDisplayName().getString());
                    this.sendUpdateResponseMessage(context, result, new TextComponent("Your timer was resumed"));
                    return 1;
                }))

                // TODO: Make aware of present op status -> Different message & no update
                .then(Commands.literal("op")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    final String name = context.getArgument("name", String.class);
                                    chronosShared.updateUser(name, user -> {
                                        user.setOperator(true);
                                        return user;
                                    });
                                    sendBasicChatMessage(context, String.format("%s is now a chronos operator", name), true);
                                    return 1;
                                })
                        )
                )

                // TODO: Make aware of present op status -> Different message & no update
                .then(Commands.literal("deop")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    final String name = context.getArgument("name", String.class);
                                    chronosShared.updateUser(name, user -> {
                                        user.setOperator(false);
                                        return user;
                                    });
                                    sendBasicChatMessage(context, String.format("%s isn't a chronos operator anymore", name), true);
                                    return 1;
                                })
                        )
                )
        );
    }

    @SubscribeEvent
    public void onRegisterCommandEvent(@NotNull final RegisterCommandsEvent event) {
        final LiteralArgumentBuilder<CommandSourceStack> lab = Commands.literal("chronos");
        this.registerPublicCommands(lab);
        this.registerAdministrativeCommands(lab);
        event.getDispatcher().register(lab);
    }

    private void sendUpdateResponseMessage(final CommandContext<CommandSourceStack> context, @NotNull final UpdateResult<?> result, final Component successComponent) {
        if (result.getSuccess()) {
            context.getSource().sendSuccess(successComponent, false);
            return;
        }

        if (result.getCode() == UpdateResultCodes.LACK_OF_PERMISSION.getCode()) {
            context.getSource().sendFailure(new TextComponent("You're not having sufficient permissions to execute this routine")
                    .withStyle(ChatFormatting.RED)
            );
            return;
        }

        if (result.getCode() == UpdateResultCodes.INTERNAL_ERROR.getCode()) {
            context.getSource().sendFailure(new TextComponent("Error occurred while executing internal chronos routine")
                    .withStyle(ChatFormatting.RED)
                    .withStyle(ChatFormatting.BOLD)
            );
            context.getSource().sendFailure(new TextComponent(String.format("Error: %s", result.getError() != null ? result.getError().getClass() : "N/A"))
                    .withStyle(ChatFormatting.RED)
            );
            context.getSource().sendFailure(new TextComponent(String.format("Message: %s", result.getError() != null ? result.getError().getMessage() : "N/A"))
                    .withStyle(ChatFormatting.RED)
            );
        }
    }

    private void sendBasicChatMessage(@NotNull final CommandContext<CommandSourceStack> context, final String message, final boolean success) {
        if (success) {
            context.getSource().sendSuccess(new TextComponent(message), false);
        } else {
            context.getSource().sendFailure(new TextComponent(message));
        }
    }

    @NotNull
    private Contractor getPlayerContractor(@NotNull final CommandSourceStack css) {
        final String id = css.getDisplayName().getString();
        final User user = ChronosAPI.Companion.getInstance().getUserFromID(id);
        if (user == null) {
            LOGGER.warn("Tried to get a player contractor, but related user was null. Falling back to a generic player contractor");
            return new Contractor(id, (o, level) -> {
                if (level == Level.SEVERE) {
                    css.sendFailure(new TextComponent(String.format("GPC log: %s", o.toString())));
                } else {
                    css.sendSuccess(new TextComponent(String.format("GPC log: %s", o.toString())), true);
                }
                return Unit.INSTANCE;
            }, false);
        }
        return new Contractor(id, (o, level) -> {
            if (level == Level.SEVERE) {
                css.sendFailure(new TextComponent(o.toString()));
            } else {
                css.sendSuccess(new TextComponent(o.toString()), false);
            }
            return Unit.INSTANCE;
        }, user.getOperator());
    }
}
