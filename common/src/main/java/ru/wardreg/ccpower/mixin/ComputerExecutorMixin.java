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
 * Gates the power-on of a computer on its RF state. Every turn-on path funnels
 * through {@code queueStart()}, so a computer without enough buffered RF never
 * boots at all. Running cost and discharge live in PowerManager.
 */
// ComputerExecutor is package-private in CC:T, so it is targeted by name.
@Mixin(targets = "dan200.computercraft.core.computer.ComputerExecutor", remap = false)
public abstract class ComputerExecutorMixin {
    @Shadow(remap = false)
    @Final
    private Computer computer;

    @Inject(remap = false, method = "queueStart", at = @At("HEAD"), cancellable = true)
    private void ccpower$gatePowerOn(CallbackInfo ci) {
        if (!PowerManager.canPowerOn(computer.getID())) {
            PowerManager.logPowerOnDenied(computer.getID());
            ci.cancel();
        }
    }

    /** Track that the Lua machine performed work during this server tick. */
    @Inject(remap = false, method = "work", at = @At("TAIL"))
    private void ccpower$markActive(CallbackInfo ci) {
        PowerManager.markActive(computer.getID());
    }
}
