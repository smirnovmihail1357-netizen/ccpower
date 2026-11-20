package ru.wardreg.ccpower;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wardreg.ccpower.power.PeripheralCosts;
import ru.wardreg.ccpower.registry.ModRegistry;

/** Fabric entry point. All game logic lives in the loader-agnostic packages. */
public final class CCPowerMod implements ModInitializer {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LoggerFactory.getLogger("CCPower");

    @Override
    public void onInitialize() {
        ModRegistry.register();
        CCPowerConfig.load();
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            PeripheralCosts.load(server.getLevel(Level.OVERWORLD)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PeripheralCosts.watchServerTick();
            CCPowerConfig.watch();
        });
        LOG.info("CC: Power initialized");
    }
}
