package ru.wardreg.ccpower.mixin;

import dan200.computercraft.shared.computer.core.ServerComputer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Hook into the per-tick entry point of every server computer. Performs the RF
 * accounting before the rest of the tick runs: the tick cost is drawn from
 * the internal buffer, and a computer that cannot pay is shut down.
 */
@Mixin(value = ServerComputer.class, remap = false)
public abstract class ServerComputerMixin {
    @Inject(remap = false, method = "tickServer", at = @At("HEAD"))
    private void ccpower$accountEnergy(CallbackInfo ci) {
        PowerManager.onTickServer((ServerComputer) (Object) this);
    }
}
