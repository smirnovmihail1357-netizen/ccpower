package ru.wardreg.ccpower.integration.jade;

import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Jade integration. Discovered through the {@link WailaPlugin} annotation, so
 * this class is only ever loaded when Jade is present; the rest of the mod
 * never references Jade directly.
 *
 * <p>The internal RF state lives on the server only, so the data is shipped to
 * the client through Jade's server-data channel rather than the block entity's
 * own sync packet: a dock reports its buffer, computers and turtles report
 * their internal energy and the "off due to no RF" state.</p>
 */
@WailaPlugin
public class CCPowerJadePlugin implements IWailaPlugin {
    /** NBT keys used on the server -> client data channel. */
    private static final String KEY_BUFFER = "ccpower_buffer";
    private static final String KEY_CAPACITY = "ccpower_capacity";
    private static final String KEY_OFF = "ccpower_off";
    private static final String KEY_TURTLE = "ccpower_turtle";

    /** Server side: collect the power state into the Jade data tag. */
    private final IServerDataProvider<BlockAccessor> server = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof DockBlockEntity dock) {
                data.putLong(KEY_BUFFER, dock.getBuffer());
                data.putLong(KEY_CAPACITY, dock.getBufferCapacity());
                return;
            }
            if (accessor.getBlockEntity() instanceof AbstractComputerBlockEntity computer) {
                int id = computer.getComputerID();
                if (id >= 0) {
                    ComputerPowerState state = PowerManager.stateOrNull(id);
                    if (state != null) {
                        if (state.hasTurtle()) {
                            data.putLong(KEY_TURTLE, state.turtleEnergy());
                            data.putLong(KEY_CAPACITY, CCPowerConfig.turtleBufferRF());
                        } else {
                            data.putLong(KEY_BUFFER, state.bufferStored());
                            data.putLong(KEY_CAPACITY, CCPowerConfig.computerBufferRF());
                        }
                        var serverComputer = computer.getServerComputer();
                        boolean on = serverComputer != null && serverComputer.isOn();
                        // Turned off and unable to boot back up: out of RF.
                        if (!on && !PowerManager.canPowerOn(id)) {
                            data.putBoolean(KEY_OFF, true);
                        }
                    }
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath("ccpower", "power");
        }
    };

    /** Client side: render the received power state as tooltip lines. */
    private final IComponentProvider<BlockAccessor> client = new IComponentProvider<>() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains(KEY_TURTLE)) {
                tooltip.add(Component.translatable("tooltip.ccpower.turtle",
                    fmt(data.getLong(KEY_TURTLE).orElse(0L)), fmt(data.getLong(KEY_CAPACITY).orElse(0L))));
            } else if (data.contains(KEY_BUFFER)) {
                tooltip.add(Component.translatable("tooltip.ccpower.buffer",
                    fmt(data.getLong(KEY_BUFFER).orElse(0L)), fmt(data.getLong(KEY_CAPACITY).orElse(0L))));
            }
            if (data.getBoolean(KEY_OFF).orElse(false)) {
                tooltip.add(Component.translatable("tooltip.ccpower.denied"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath("ccpower", "power");
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Server-data channel feeding the client lines.
        registration.registerBlockDataProvider(server, DockBlockEntity.class);
        registration.registerBlockDataProvider(server, AbstractComputerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(client, DockBlock.class);
        registration.registerBlockComponent(client, ComputerBlock.class);
        registration.registerBlockComponent(client, TurtleBlock.class);
    }

    /** Plain integer formatting; avoids locale-specific digit grouping. */
    private static String fmt(long value) {
        return String.valueOf(value);
    }
}
