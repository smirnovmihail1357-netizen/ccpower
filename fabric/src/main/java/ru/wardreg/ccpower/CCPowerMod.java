package ru.wardreg.ccpower;

import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wardreg.ccpower.debug.CCPowerCommands;
import ru.wardreg.ccpower.dock.DockPeripheral;
import ru.wardreg.ccpower.lua.ComputerStatePeripheral;
import ru.wardreg.ccpower.lua.PowerAPI;
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

        // Lua API: generic method source plus a per-computer `ccpower` API.
        ComputerCraftAPI.registerGenericSource(new PowerAPI());
        ComputerCraftAPI.registerAPIFactory(system -> new PowerAPI(system.getID()));
        // The dock is a peripheral; its Lua methods need a generic source too.
        ComputerCraftAPI.registerGenericSource(new DockPeripheral());
        // Adds the same state methods to every computer/turtle peripheral.
        ComputerCraftAPI.registerGenericSource(new ComputerStatePeripheral());

        // Admin-only debug toggle for the optional Jade diagnostic line.
        CCPowerCommands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            PeripheralCosts.load(server.overworld()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PeripheralCosts.watchServerTick();
            CCPowerConfig.watchServerTick();
        });

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
