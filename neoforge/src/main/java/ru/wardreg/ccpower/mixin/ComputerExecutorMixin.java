package ru.wardreg.ccpower.mixin;

import dan200.computercraft.core.computer.Computer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Gates the power-on of a computer on its RF state.
 *
 * <p>Every turn-on path (the terminal button, the {@code /computercraft
 * turn-on} command, autostart, Lua reboots) funnels through {@code
 * queueStart()}, so a computer without enough buffered RF never boots at
 * all - it simply stays off. Running cost and the discharge shutdown are
 * handled in PowerManager via ServerComputer.tickServer().</p>
 */
// ComputerExecutor is package-private in CC:T, so it is targeted by name.
@Mixin(targets = "dan200.computercraft.core.computer.ComputerExecutor", remap = false)
public abstract class ComputerExecutorMixin {
    @Shadow
    @Final
    private Computer computer;

    /**
     * Deny power-on while no RF energy is available.
     */
    @Inject(method = "queueStart", at = @At("HEAD"), cancellable = true, remap = false)
    private void ccpower$gatePowerOn(CallbackInfo ci) {
        if (!PowerManager.canPowerOn(computer.getID())) {
            PowerManager.logPowerOnDenied(computer.getID());
            ci.cancel();
        }
    }

    /** Track that the Lua machine performed work during this server tick. */
    @Inject(method = "work", at = @At("TAIL"), remap = false)
    private void ccpower$markActive(CallbackInfo ci) {
        // Idempotent: the flag is consumed by PowerManager at the next server tick.
        PowerManager.markActive(computer.getID());
    }
}
