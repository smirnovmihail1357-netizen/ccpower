package ru.wardreg.ccpower;

import dan200.computercraft.api.ComputerCraftAPI;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
 * <p>Design summary:
 * <ul>
 *   <li>RF energy is consumed only while a computer is turned on. A turned-off
 *       computer consumes nothing (it still recharges its buffer).</li>
 *   <li>Every computer has an internal RF buffer. Stationary computers pull RF
 *       from adjacent FE-providing blocks; turtles carry a small battery
 *       refilled at the Recharge Dock.</li>
 *   <li>A running computer that cannot pay its tick cost is fully discharged:
 *       it is shut down (the running program is lost, as with a normal CC
 *       shutdown) and cannot boot again until its buffer can cover a running
 *       tick. Turtles share this code; without RF (or vanilla fuel) they stop
 *       dead in place.</li>
 *   <li>Peripheral calls and turtle actions add per-use surcharges to the tick cost.</li>
 *   <li>Every cost is defined in the dynamically reloadable common config file.</li>
 * </ul>
 */
@Mod(CCPowerMod.MODID)
public final class CCPowerMod {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LoggerFactory.getLogger("CCPower");

    public CCPowerMod(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);

        // Lua API: register the generic method sources once, then let CC:T build
        // a per-computer `ccpower` API object through its factory.
        ComputerCraftAPI.registerGenericSource(new PowerAPI());
        ComputerCraftAPI.registerAPIFactory(system -> new PowerAPI(system.getID()));
        // The dock is a peripheral; its Lua methods need a generic source too.
        ComputerCraftAPI.registerGenericSource(new DockPeripheral());
        // Adds the same state methods to every computer/turtle peripheral.
        ComputerCraftAPI.registerGenericSource(new ComputerStatePeripheral());

        // Common config: watched by NeoForge and reloaded dynamically on file change.
        container.registerConfig(ModConfig.Type.COMMON, CCPowerConfig.SPEC);
        modBus.addListener(CCPowerConfig::onConfigReloading);

        // Expose the dock's internal buffer as a block FE capability.
        modBus.addListener((RegisterCapabilitiesEvent event) ->
            ModRegistry.registerCapabilities(event));

        // Per-peripheral-type costs: pre-scanned from every installed mod at
        // server start, auto-grown at runtime, polled while the server runs.
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
            PeripheralCosts.load(event.getServer().overworld()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> PeripheralCosts.watchServerTick());

        // Admin-only debug toggle for the optional Jade diagnostic line.
        NeoForge.EVENT_BUS.addListener(CCPowerCommands::register);

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
