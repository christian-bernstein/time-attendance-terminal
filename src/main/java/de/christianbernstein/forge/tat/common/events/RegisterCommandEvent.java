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

package de.christianbernstein.forge.tat.common.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * TODO: Register to MinecraftForge.EVENT_BUS
 *
 * @author Christian Bernstein
 */
public class RegisterCommandEvent {

    @SubscribeEvent
    public static void onRegisterCommandEvent(@NotNull final RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("tat").executes(context -> {
            final ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(Objects.requireNonNull(context.getSource().getEntity()).getUUID());
            assert player != null;
            player.sendMessage(new TextComponent("Hello world!"), ChatType.SYSTEM, Util.NIL_UUID);
            return 1;
        }));
    }
}
