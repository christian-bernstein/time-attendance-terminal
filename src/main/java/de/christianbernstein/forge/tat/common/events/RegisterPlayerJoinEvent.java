package de.christianbernstein.forge.tat.common.events;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.logging.Logger;

/**
 * @author Christian Bernstein
 */
public class RegisterPlayerJoinEvent {

    @SubscribeEvent
    public static void onPlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        LogUtils.getLogger().error("onPlayerLoggedInEvent");
    }
}
