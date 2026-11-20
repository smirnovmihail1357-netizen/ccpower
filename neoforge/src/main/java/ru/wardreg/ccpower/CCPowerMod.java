package ru.wardreg.ccpower;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public CCPowerMod(IEventBus modBus) {
        ModRegistry.register(modBus);

        // Common config: watched by NeoForge and reloaded dynamically on file change.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CCPowerConfig.SPEC);
        modBus.addListener(CCPowerConfig::onConfigReloading);

        // Expose the dock's internal buffer as a block FE capability.
        modBus.addListener((RegisterCapabilitiesEvent event) ->
            ModRegistry.registerCapabilities(event));

        // Per-peripheral-type costs: pre-scanned from every installed mod at
        // server start, auto-grown at runtime, polled while the server runs.
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
            PeripheralCosts.load(event.getServer().overworld()));
        NeoForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) PeripheralCosts.watchServerTick();
        });

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
