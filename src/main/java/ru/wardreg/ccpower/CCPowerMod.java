package ru.wardreg.ccpower;

import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wardreg.ccpower.debug.CCPowerCommands;
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
 *       tick. Turtles share this code; without RF they stop dead in place.</li>
 *   <li>Peripheral calls and turtle actions add per-use surcharges to the tick cost.</li>
 *   <li>Every cost is defined in the dynamically reloadable common config file.</li>
 * </ul>
 */
@Mod(CCPowerMod.MODID)
public final class CCPowerMod {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LoggerFactory.getLogger("CCPower");

    public CCPowerMod() {
        ModRegistry.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Lua API: one generic method source, then CC:T builds a per-computer
        // `ccpower` API object; the dock and computer peripherals get the same
        // state methods the same way.
        ModRegistry.registerComputerCraft();

        // Common config: watched by Forge and reloaded dynamically on file change.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CCPowerConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CCPowerConfig::onConfigReloading);

        // Per-peripheral-type costs: pre-scanned from every installed mod at
        // server start, auto-grown at runtime, polled while the server runs.
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
            PeripheralCosts.load(event.getServer().getLevel(Level.OVERWORLD)));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) ->
            PeripheralCosts.watchServerTick());

        // Admin-only /ccpower debug toggle.
        MinecraftForge.EVENT_BUS.addListener(CCPowerCommands::register);

        CCPowerMod.LOG.info("CC: Power initialized");
    }
}
