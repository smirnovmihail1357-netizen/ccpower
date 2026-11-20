package ru.wardreg.ccpower.mixin;

import dan200.computercraft.shared.computer.core.ServerComputer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Hook into the per-tick entry point of every server computer. In CC:T 1.97
 * (MC 1.15.2) this is {@code update()} (1.101 renamed it to tickServer()).
 */
@Mixin(value = ServerComputer.class, remap = false)
public abstract class ServerComputerMixin {
    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void ccpower$accountEnergy(CallbackInfo ci) {
        PowerManager.onTickServer((ServerComputer) (Object) this);
    }
}
