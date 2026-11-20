package ru.wardreg.ccpower.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import ru.wardreg.ccpower.CCPowerMod;

/** Fabric entry point (both client and server use this initializer). */
public final class FabricEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        ModRegistry.register();
        CCPowerMod.init(new FabricPlatform(), new FabricConfigProvider(FabricLoader.getInstance().getConfigDir()));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> CCPowerMod.serverStarted(server.overworld()));
        ServerTickEvents.END_SERVER_TICK.register(server -> CCPowerMod.serverTick());
    }
}
