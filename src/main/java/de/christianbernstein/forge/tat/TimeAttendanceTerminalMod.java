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

package de.christianbernstein.forge.tat;

import com.mojang.logging.LogUtils;
import de.christianbernstein.chronos.TimeSpan;
import de.christianbernstein.chronos.TimerAPI;
import de.christianbernstein.chronos.TimerAPIBridge;
import de.christianbernstein.forge.tat.common.events.RegisterCommandEvent;
import de.christianbernstein.forge.tat.common.events.RegisterPlayerJoinEvent;
import de.christianbernstein.forge.tat.common.events.RegisterPlayerQuitEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("examplemod")
public class TimeAttendanceTerminalMod {

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static TimerAPI chronosShared;

    public TimeAttendanceTerminalMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);


        MinecraftForge.EVENT_BUS.register(RegisterCommandEvent.class);
        MinecraftForge.EVENT_BUS.register(RegisterPlayerJoinEvent.class);
        MinecraftForge.EVENT_BUS.register(RegisterPlayerQuitEvent.class);


    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code


        new TimeSpan(1, TimeUnit.SECONDS);

        // TimeAttendanceTerminalMod.chronosShared = new TimerAPI(new TimerAPIBridge() {
        //     @NotNull
        //     @Override
        //     public List<String> getAllActiveUsers() {
        //         return Arrays.asList(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerNamesArray());
        //     }
        //     @NotNull
        //     @Override
        //     public File getWorkingDirectory() {
        //         return new File("chronos\\");
        //     }
        // });
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        // Some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final @NotNull InterModProcessEvent event) {
        // Some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.messageSupplier().get()).
                collect(Collectors.toList()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(@NotNull ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        event.getServer().getServerDirectory();
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {

        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // Register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }
}
