package ru.wardreg.ccpower.integration.jade;

import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import ru.wardreg.ccpower.debug.DebugState;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

/**
 * Jade integration. Discovered through the {@link WailaPlugin} annotation on
 * Forge and through the {@code jade} entry point on Fabric, so this class is
 * only ever loaded when Jade is present; the rest of the mod never references
 * Jade directly.
 *
 * <p>Adapted to Jade's 1.20.1 API (11.x): {@link IBlockComponentProvider}
 * replaces the generic component provider, resource locations use the old
 * two-argument constructor, and {@code getGroups} receives player/level/target
 * separately. The RF state lives on the server only. An energy extension
 * provider registered for both computer block entities and the Recharge Dock
 * feeds Jade's universal energy component, which renders the progress bar
 * with its standard numeric "current / capacity FE" label. The only custom
 * text line is the optional diagnostic shown after {@code /ccpower debug}.
 */
@WailaPlugin
public class CCPowerJadePlugin implements IWailaPlugin {
    /** Own UID so ours never collides with Jade's built-in energy provider. */
    private static final ResourceLocation ENERGY_UID =
        new ResourceLocation("ccpower", "energy");
    private static final ResourceLocation POWER_UID =
        new ResourceLocation("ccpower", "power");

    /** Server -> client keys for the opt-in diagnostic line. */
    private static final String KEY_DEBUG = "ccpower_debug";
    private static final String KEY_CHARGE = "ccpower_charge";
    private static final String KEY_DISCHARGE = "ccpower_discharge";
    private static final String KEY_ENERGY = "ccpower_energy";
    private static final String KEY_CAPACITY = "ccpower_capacity";

    /** Energy unit used by Jade for FE storages. */
    private static final String UNIT = "FE";

    /** Server side: only the opt-in diagnostic data for computers. */
    private final IServerDataProvider<BlockAccessor> server = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof AbstractComputerBlockEntity computer)) return;
            if (accessor.getPlayer() == null
                || !DebugState.isEnabled(accessor.getPlayer().getUUID())) return;

            int id = computer.getComputerID();
            if (id < 0) return;
            ComputerPowerState state = PowerManager.stateOrNull(id);
            if (state == null) return;

            data.putBoolean(KEY_DEBUG, true);
            data.putLong(KEY_CHARGE, state.chargeRate());
            data.putLong(KEY_DISCHARGE, state.dischargeRate());
            data.putLong(KEY_ENERGY, state.energyStored());
            data.putLong(KEY_CAPACITY, state.energyCapacity());
        }

        @Override
        public ResourceLocation getUid() {
            return POWER_UID;
        }
    };

    /** Client side: the bar comes from Jade; only diagnostics are text. */
    private final IBlockComponentProvider client = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.getBoolean(KEY_DEBUG)) return;
            tooltip.add(Component.translatable("tooltip.ccpower.debug",
                data.getLong(KEY_CHARGE),
                data.getLong(KEY_DISCHARGE),
                data.getLong(KEY_ENERGY),
                data.getLong(KEY_CAPACITY)));
        }

        @Override
        public ResourceLocation getUid() {
            return POWER_UID;
        }
    };

    /**
     * Feeds the internal RF store of computers (and the battery of turtles)
     * into Jade's generic energy bar. The Recharge Dock is included so both
     * blocks are described through the same extension provider and therefore
     * get the identical Jade bar with the same numeric label.
     */
    private enum EnergyProvider
        implements IServerExtensionProvider<Object, CompoundTag>,
                   IClientExtensionProvider<CompoundTag, EnergyView> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ENERGY_UID;
        }

        @Override
        public int getDefaultPriority() {
            return 10_001;
        }

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(
            ServerPlayer player, ServerLevel level, Object target, boolean fullData) {
            long current;
            long max;

            if (target instanceof AbstractComputerBlockEntity computer) {
                int id = computer.getComputerID();
                if (id < 0) return null;
                ComputerPowerState state = PowerManager.stateOrNull(id);
                if (state == null) return null;
                current = state.energyStored();
                max = state.energyCapacity();
            } else if (target instanceof DockBlockEntity dock) {
                current = dock.getBuffer();
                max = dock.getBufferCapacity();
            } else {
                return null;
            }

            ViewGroup<CompoundTag> group = new ViewGroup<>(List.of(EnergyView.of(current, max)));
            group.getExtraData().putString("Unit", UNIT);
            return List.of(group);
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(
            snownee.jade.api.Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
            return groups.stream()
                .map(group -> new ClientViewGroup<>(group.views.stream()
                    .map(tag -> EnergyView.read(tag, group.getExtraData().getString("Unit")))
                    .toList()))
                .toList();
        }
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(server, AbstractComputerBlockEntity.class);
        registration.registerEnergyStorage(EnergyProvider.INSTANCE, AbstractComputerBlockEntity.class);
        registration.registerEnergyStorage(EnergyProvider.INSTANCE, DockBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(client, ComputerBlock.class);
        registration.registerBlockComponent(client, TurtleBlock.class);
        registration.registerEnergyStorageClient(EnergyProvider.INSTANCE);

        // The dock has no custom text any more, but keep a no-op component so
        // Jade still requests our server data for it when needed.
        registration.registerBlockComponent(client, DockBlock.class);
    }
}
