package ru.wardreg.ccpower.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.wardreg.ccpower.CCPowerMod;

/** Forge entry point. Forge 47 requires a zero-arg constructor. */
@Mod(CCPowerMod.MODID)
public final class ForgeEntry {
    public ForgeEntry() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);

        ForgeConfigProvider config = new ForgeConfigProvider();
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.COMMON, ForgeConfigProvider.SPEC, "ccpower-common.toml");
        modBus.addListener((ModConfigEvent.Reloading event) -> config.onReload(event));

        CCPowerMod.init(new ForgePlatform(), config);

        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
            CCPowerMod.serverStarted(event.getServer().overworld()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) CCPowerMod.serverTick();
        });
    }
}
