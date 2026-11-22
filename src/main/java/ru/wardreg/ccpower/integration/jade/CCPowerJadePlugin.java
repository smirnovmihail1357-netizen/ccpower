package ru.wardreg.ccpower.integration.jade;

import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import java.util.List;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.debug.DebugState;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Jade integration for Forge 1.16.5. Discovered through the {@link WailaPlugin}
 * annotation, so this class is only loaded when Jade is present.
 *
 * <p>Era note: 1.16.5 Jade 2.x uses the {@code mcp.mobius.waila.api} package and
 * has no energy-extension API ({@code registerEnergyStorage} / {@code EnergyView}
 * from 0.8.0's NeoForge Jade). Only the opt-in diagnostic line is portable;
 * the standard Jade RF bar is skipped on this branch.
 */
@WailaPlugin(CCPowerMod.MODID)
public class CCPowerJadePlugin implements IWailaPlugin {
    /** Server -> client keys for the opt-in diagnostic line. */
    private static final String KEY_DEBUG = "ccpower_debug";
    private static final String KEY_CHARGE = "ccpower_charge";
    private static final String KEY_DISCHARGE = "ccpower_discharge";
    private static final String KEY_ENERGY = "ccpower_energy";
    private static final String KEY_CAPACITY = "ccpower_capacity";

    /** Server side: only send diagnostic data when the looking player opted in. */
    private final IServerDataProvider<TileEntity> server = new IServerDataProvider<TileEntity>() {
        @Override
        public void appendServerData(CompoundNBT data, ServerPlayerEntity player,
                                     World world, TileEntity tile) {
            if (!(tile instanceof TileComputerBase)) return;
            if (player == null || !DebugState.isEnabled(player.getGameProfile().getId())) return;

            int id = ((TileComputerBase) tile).getComputerID();
            if (id < 0) return;
            ComputerPowerState state = PowerManager.stateOrNull(id);
            if (state == null) return;

            data.putBoolean(KEY_DEBUG, true);
            data.putLong(KEY_CHARGE, state.chargeRate());
            data.putLong(KEY_DISCHARGE, state.dischargeRate());
            data.putLong(KEY_ENERGY, state.energyStored());
            data.putLong(KEY_CAPACITY, state.energyCapacity());
        }
    };

    /** Client side: render only the optional diagnostic line. */
    private final IComponentProvider client = new IComponentProvider() {
        @Override
        public void appendBody(List<ITextComponent> tooltip, IDataAccessor accessor,
                               IPluginConfig config) {
            CompoundNBT data = accessor.getServerData();
            if (data == null || !data.getBoolean(KEY_DEBUG)) return;
            tooltip.add(new TranslationTextComponent("tooltip.ccpower.debug",
                data.getLong(KEY_CHARGE),
                data.getLong(KEY_DISCHARGE),
                data.getLong(KEY_ENERGY),
                data.getLong(KEY_CAPACITY)));
        }
    };

    @Override
    public void register(IRegistrar registration) {
        registration.registerBlockDataProvider(server, TileComputerBase.class);
        registration.registerComponentProvider(client, TooltipPosition.BODY, BlockComputer.class);
        registration.registerComponentProvider(client, TooltipPosition.BODY, BlockTurtle.class);
    }
}
