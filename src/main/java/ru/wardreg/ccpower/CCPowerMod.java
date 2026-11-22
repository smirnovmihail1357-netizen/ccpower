package ru.wardreg.ccpower;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wardreg.ccpower.debug.CCPowerCommands;
import ru.wardreg.ccpower.dock.DockBlockEntity;
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

        // Lua API: CC:T builds a per-computer `ccpower` API through our factory.
        ComputerCraftAPI.registerAPIFactory(system -> new PowerAPI(system.getID()));
        // Adds the same state methods to every computer/turtle peripheral.
        ComputerCraftAPI.registerGenericSource(new ComputerStatePeripheral());
        // Expose the dock's internal state to adjacent computers as a CC:T peripheral.
        // CC:T 1.99.x (Forge 1.17.1) uses peripheral providers, not a capability.
        ComputerCraftAPI.registerPeripheralProvider(CCPowerMod::getDockPeripheral);

        // Common config: watched by Forge and reloaded dynamically on file change.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CCPowerConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CCPowerConfig::onConfigReloading);

        // Per-peripheral-type costs: pre-scanned from every installed mod at
        // server start, auto-grown at runtime, polled while the server runs.
        MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) ->
            PeripheralCosts.load(event.getServer().getLevel(Level.OVERWORLD)));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) ->
            PeripheralCosts.watchServerTick());

        // Admin-only debug toggle for the optional Jade diagnostic line.
        MinecraftForge.EVENT_BUS.addListener(CCPowerCommands::register);

        CCPowerMod.LOG.info("CC: Power initialized");
    }

    private static LazyOptional<dan200.computercraft.api.peripheral.IPeripheral> getDockPeripheral(
        Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DockBlockEntity dock) {
            return LazyOptional.of(() -> dock.getPeripheral());
        }
        return LazyOptional.empty();
    }
}
