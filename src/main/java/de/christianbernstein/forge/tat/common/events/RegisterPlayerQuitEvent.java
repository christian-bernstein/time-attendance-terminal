package de.christianbernstein.forge.tat.common.events;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @author Christian Bernstein
 */
public class RegisterPlayerQuitEvent {

    @SubscribeEvent
    public static void onPlayerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event) {
        LogUtils.getLogger().error("onPlayerLoggedOutEvent");
    }
}
