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
