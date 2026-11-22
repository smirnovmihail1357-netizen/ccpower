package ru.wardreg.ccpower;

import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
 * <p>Fabric entry point. All gameplay logic is loader-agnostic; this class only
 * wires the Fabric lifecycle events and registers the dock content.
 */
public final class CCPowerMod implements ModInitializer {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LoggerFactory.getLogger("CCPower");

    @Override
    public void onInitialize() {
        ModRegistry.register();

        // Lua API: register the generic method source once, then let CC:T build
        // a per-computer `ccpower` API object through its factory.
        ComputerCraftAPI.registerGenericSource(new PowerAPI());
        ComputerCraftAPI.registerAPIFactory(system -> new PowerAPI(system.getID()));
        // The dock is a peripheral; its Lua methods need a generic source too.
        ComputerCraftAPI.registerGenericSource(new DockPeripheral());
        // Adds the same state methods to every computer/turtle peripheral.
        ComputerCraftAPI.registerGenericSource(new ComputerStatePeripheral());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            CCPowerCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            PeripheralCosts.load(server.getLevel(net.minecraft.world.level.Level.OVERWORLD)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PeripheralCosts.watchServerTick();
            CCPowerConfig.watchServerTick();
        });

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
