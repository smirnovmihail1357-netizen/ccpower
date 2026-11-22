package ru.wardreg.ccpower;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wardreg.ccpower.dock.DockPeripheral;
import ru.wardreg.ccpower.lua.ComputerStatePeripheral;
import ru.wardreg.ccpower.lua.PowerAPI;
import ru.wardreg.ccpower.platform.CcpPlatform;
import ru.wardreg.ccpower.platform.CcpPlatformHolder;
import ru.wardreg.ccpower.platform.ConfigProvider;
import ru.wardreg.ccpower.power.PeripheralCosts;

/**
 * CC: Power - RF (Forge Energy) consumption for CC:Tweaked computers and turtles.
 *
 * <p>Loader-independent startup and server-hook plumbing. The actual entry
 * points ({@code @Mod} on Forge, {@code ModInitializer} on Fabric) install
 * the platform/config providers and forward the server lifecycle events here.
 */
public final class CCPowerMod {
    public static final String MODID = "ccpower";
    public static final Logger LOG = LoggerFactory.getLogger("CCPower");

    public static void init(CcpPlatform platform, ConfigProvider config) {
        CcpPlatformHolder.init(platform);
        CCPowerConfig.init(config);
        PeripheralCosts.loadConfig();

        // Lua API: register the generic method source once, then let CC:T build
        // a per-computer `ccpower` API object through its factory.
        ComputerCraftAPI.registerGenericSource(new PowerAPI());
        ComputerCraftAPI.registerAPIFactory(system -> new PowerAPI(system.getID()));
        // The dock is a peripheral; its Lua methods need a generic source too.
        ComputerCraftAPI.registerGenericSource(new DockPeripheral());
        // Adds the same state methods to every computer/turtle peripheral.
        ComputerCraftAPI.registerGenericSource(new ComputerStatePeripheral());

        LOG.info("CC: Power initialized");
    }

    public static void serverStarted(ServerLevel level) {
        PeripheralCosts.load(level);
    }

    public static void serverTick() {
        CCPowerConfig.tick();
        PeripheralCosts.watchServerTick();
    }

    private CCPowerMod() {}
}
