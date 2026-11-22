package ru.wardreg.ccpower;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.wardreg.ccpower.debug.CCPowerCommands;
import ru.wardreg.ccpower.dock.DockPeripheral;
import ru.wardreg.ccpower.lua.ComputerStatePeripheral;
import ru.wardreg.ccpower.lua.PowerAPI;
import ru.wardreg.ccpower.power.PeripheralCosts;
import ru.wardreg.ccpower.registry.ModRegistry;

/** CC: Power - RF (Forge Energy) consumption for CC:Tweaked computers and turtles. */
@Mod(CCPowerMod.MODID)
public final class CCPowerMod {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LogManager.getLogger("CCPower");

    public CCPowerMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);

        // Lua API: register the generic method source once, then let CC:T build
        // a per-computer `ccpower` API object through its factory. The dock and
        // the computer peripheral get their own generic method sources.
        ComputerCraftAPI.registerGenericSource(new PowerAPI());
        ComputerCraftAPI.registerAPIFactory(system -> new PowerAPI(system.getID()));
        ComputerCraftAPI.registerGenericSource(new DockPeripheral());
        ComputerCraftAPI.registerGenericSource(new ComputerStatePeripheral());

        // Common config: watched by Forge and reloaded dynamically on file change.
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.COMMON, CCPowerConfig.SPEC, "ccpower-common.toml");
        modBus.addListener(CCPowerConfig::onConfigReloading);

        // Per-peripheral-type costs: pre-scan at server start, poll while running.
        MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) ->
            PeripheralCosts.load(event.getServer().overworld()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) PeripheralCosts.watchServerTick();
        });

        // Admin-only debug toggle for the optional Jade diagnostic line.
        MinecraftForge.EVENT_BUS.addListener(CCPowerCommands::register);

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
