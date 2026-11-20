package ru.wardreg.ccpower.mixin;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.apis.PeripheralAPI;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wardreg.ccpower.access.PeripheralOwnerAccess;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Charges the per-type call surcharge at the point where the target peripheral
 * is actually known: {@code PeripheralWrapper.call} holds the resolved wrapper
 * (and its cached type string), so the surcharge can be looked up in the
 * {@code [rf_per_call]} table of {@code ccpower-peripherals.toml}.
 *
 * <p>The owning computer id is reached through the synthetic outer reference
 * ({@code this$0}) cast to the {@link PeripheralOwnerAccess} duck interface
 * implemented by the PeripheralAPI mixin. Failed calls (unknown method, error
 * result) are charged as well - the call attempt itself is the billable event.
 */
@Mixin(targets = "dan200.computercraft.core.apis.PeripheralAPI$PeripheralWrapper", remap = false)
public abstract class PeripheralWrapperMixin {
    @Shadow
    @Final
    private PeripheralAPI this$0;

    @Shadow
    @Final
    private String type;

    @Inject(method = "call", at = @At("HEAD"), remap = false)
    private void ccpower$chargeCall(ILuaContext context, String method, IArguments arguments,
        CallbackInfoReturnable<MethodResult> cir) {
        PowerManager.chargePeripheralCall(
            ((PeripheralOwnerAccess) (Object) this$0).ccpower$computerId(), type);
    }
}
