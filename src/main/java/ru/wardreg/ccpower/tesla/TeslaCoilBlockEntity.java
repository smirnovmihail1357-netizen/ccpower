package ru.wardreg.ccpower.tesla;

import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.util.NonNegativeId;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.DeviceKind;
import ru.wardreg.ccpower.power.PowerManager;
import ru.wardreg.ccpower.registry.ModRegistry;

/**
 * Block entity of a Tesla Coil. Every configured interval it pulses and
 * pushes RF into portable CC computers (pocket computers and Smart Glasses)
 * found in players' inventories or as dropped items inside its radius.
 */
public class TeslaCoilBlockEntity extends BlockEntity {
    private final boolean tall;
    private int ticks;

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state, boolean tall) {
        super(tall ? ModRegistry.TESLA_BE_TALL.get() : ModRegistry.TESLA_BE_SMALL.get(), pos, state);
        this.tall = tall;
    }

    public static void serverTick(Level level, BlockPos pos, TeslaCoilBlockEntity coil) {
        if (!(level instanceof ServerLevel server)) return;
        if (++coil.ticks < CCPowerConfig.teslaIntervalSeconds() * 20) return;
        coil.ticks = 0;

        int radius = coil.tall
            ? CCPowerConfig.teslaLargeRadius()
            : CCPowerConfig.teslaSmallRadius();
        long energy = CCPowerConfig.teslaEnergyPerTransfer();
        AABB area = new AABB(pos).inflate(radius);

        for (Player player : server.getPlayers(p -> p.isAlive() && area.contains(p.getX(), p.getY(), p.getZ()))) {
            chargeInventory(player, energy);
        }
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, area)) {
            chargeStack(entity.getItem(), energy);
        }
    }

    private static void chargeInventory(Player player, long energy) {
        var inventory = player.getInventory();
        for (ItemStack stack : inventory.items) chargeStack(stack, energy);
        for (ItemStack stack : inventory.armor) chargeStack(stack, energy);
        chargeStack(inventory.offhand.get(0), energy);
    }

    /** Charge only CC items that own one of our portable RF buffers. */
    static void chargeStack(ItemStack stack, long energy) {
        if (stack.isEmpty() || energy <= 0) return;

        DeviceKind kind = null;
        if (stack.getItem() instanceof PocketComputerItem) {
            kind = DeviceKind.POCKET;
        } else if (DeviceKind.isSmartGlassesItem(stack.getItem())) {
            kind = DeviceKind.SMART_GLASSES;
        }
        if (kind == null) return;

        NonNegativeId idValue = stack.get(
            dan200.computercraft.shared.ModRegistry.DataComponents.COMPUTER_ID.get());
        if (idValue == null) return;
        int id = idValue.id();
        ComputerPowerState state = PowerManager.state(id);
        if (!state.kind().isPortable()) state.setKind(kind);
        state.receivePortable(energy);
    }
}
