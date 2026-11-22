package ru.wardreg.ccpower.forge;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.debug.CCPowerCommands;

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

        // Expose the dock's internal state to adjacent computers as a CC:T peripheral.
        ForgeComputerCraftAPI.registerPeripheralProvider((level, pos, side) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ForgeDockBlockEntity dock) {
                return LazyOptional.of(() -> dock.getPeripheral());
            }
            return LazyOptional.empty();
        });

        // Admin-only debug toggle for the optional Jade diagnostic line.
        MinecraftForge.EVENT_BUS.addListener(
            (RegisterCommandsEvent event) -> CCPowerCommands.register(event.getDispatcher()));

        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
            CCPowerMod.serverStarted(event.getServer().overworld()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) CCPowerMod.serverTick();
        });
    }
}
