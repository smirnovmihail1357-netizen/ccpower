package ru.wardreg.ccpower;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wardreg.ccpower.power.PeripheralCosts;
import ru.wardreg.ccpower.registry.ModRegistry;

/**
 * CC: Power - RF (Forge Energy) consumption for CC:Tweaked computers and turtles.
 *
 * <p>Fabric entry point. All gameplay logic is loader-agnostic; this class only
 * wires the Fabric lifecycle events and registers the dock content.
 */
public final class CCPowerMod implements ModInitializer {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LoggerFactory.getLogger("CCPower");

    @Override
    public void onInitialize() {
        ModRegistry.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            PeripheralCosts.load(server.getLevel(net.minecraft.world.level.Level.OVERWORLD)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PeripheralCosts.watchServerTick();
            CCPowerConfig.watchServerTick();
        });

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
