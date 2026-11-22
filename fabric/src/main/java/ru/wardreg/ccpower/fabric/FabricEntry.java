package ru.wardreg.ccpower.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.debug.CCPowerCommands;

/** Fabric entry point (both client and server use this initializer). */
public final class FabricEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        ModRegistry.register();
        CCPowerMod.init(new FabricPlatform(), new FabricConfigProvider(FabricLoader.getInstance().getConfigDir()));

        // Admin-only debug toggle for the optional Jade diagnostic line.
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registry, environment) -> CCPowerCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> CCPowerMod.serverStarted(server.overworld()));
        ServerTickEvents.END_SERVER_TICK.register(server -> CCPowerMod.serverTick());
    }
}
