package ru.wardreg.ccpower.mixin;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.PeripheralAPI;
import dan200.computercraft.core.computer.ComputerSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wardreg.ccpower.access.PeripheralOwnerAccess;
import ru.wardreg.ccpower.power.PeripheralCosts;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Tracks the peripheral network of every computer (the "network size" component of
 * the tick cost) and exposes the owning computer id to the
 * {@link PeripheralWrapperMixin} call surcharge.
 *
 * <p>Per-call surcharges are no longer charged here: the inner
 * {@code PeripheralWrapper} mixin sees the actual target type and bills from
 * the {@code [rf_per_call]} table instead.
 */
@Mixin(value = PeripheralAPI.class, remap = false)
public abstract class PeripheralAPIMixin implements PeripheralOwnerAccess {
    @Shadow
    @Final
    private IAPIEnvironment environment;

    /** Owning computer id, resolved lazily through the environment. */
    @Unique
    private int ccpower$id = -1;

    @Override
    @Unique
    public int ccpower$computerId() {
        if (ccpower$id < 0) {
            ccpower$id = environment.getComputerID();
        }
        return ccpower$id;
    }

    @Inject(method = "onPeripheralChanged", at = @At("TAIL"), remap = false)
    private void ccpower$onPeripheralChanged(ComputerSide side, IPeripheral peripheral, CallbackInfo ci) {
        String type = null;
        if (peripheral != null) {
            // A broken third-party peripheral must never crash the server thread.
            try {
                type = peripheral.getType();
            } catch (Exception e) {
                type = "unknown";
            }
            if (type == null) type = "unknown";
            // Register the type right at attach time so it lands in the cost
            // file even if the computer never survives to its next billed tick.
            PeripheralCosts.tickCost(type);
            PeripheralCosts.callCost(type);
        }
        PowerManager.onPeripheralChanged(ccpower$computerId(), side, type);
    }
}
