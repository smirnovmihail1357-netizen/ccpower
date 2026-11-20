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
 * <p>Fabric entry point. The loader-specific pieces are:
 * <ul>
 *   <li>content registration via the vanilla registries (see {@link ModRegistry});</li>
 *   <li>the common config read by our own minimal TOML reader (see {@link CCPowerConfig});</li>
 *   <li>Team Reborn Energy as the FE storage API.</li>
 * </ul>
 * The CC:T mixins and the accounting logic are loader-agnostic and identical to
 * the NeoForge build.
 */
public final class CCPowerMod implements ModInitializer {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LoggerFactory.getLogger("CCPower");

    @Override
    public void onInitialize() {
        ModRegistry.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            PeripheralCosts.load(server.overworld()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PeripheralCosts.watchServerTick();
            CCPowerConfig.watchServerTick();
        });

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
