package ru.wardreg.ccpower;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
