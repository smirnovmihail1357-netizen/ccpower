package ru.wardreg.ccpower.mixin;

import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.PowerManager;
import ru.wardreg.ccpower.power.TurtleEnergyView;

/**
 * Turtles use the same power code as computers; this mixin adds the internal
 * RF battery, its NBT persistence, the dock charging path and the per-action
 * surcharge. Vanilla fuel is untouched.
 *
 * <p>ERA NOTE (1.20.1 / 1.20): readCommon/writeCommon take a plain CompoundTag
 * here - the HolderLookup.Provider parameter only exists in 1.20.5+ CC:T.
 */
@Mixin(value = TurtleBrain.class, remap = false)
public abstract class TurtleBrainMixin implements TurtleEnergyView {
    private static final String CCPower$TAG_BUFFER = "ccpower_rf";

    @Unique
    private long ccpower$buffer;

    @Unique
    private int ccpower$computerId = -1;

    @Override
    public long ccpower$getEnergy() { return ccpower$buffer; }

    @Override
    public long ccpower$getCapacity() { return CCPowerConfig.turtleBufferRF(); }

    @Override
    public synchronized long ccpower$receiveEnergy(long amount) {
        long space = CCPowerConfig.turtleBufferRF() - ccpower$buffer;
        long accepted = Math.max(0, Math.min(amount, space));
        ccpower$buffer += accepted;
        return accepted;
    }

    @Override
    public synchronized boolean ccpower$tryConsume(long amount) {
        if (amount <= 0) return true;
        if (ccpower$buffer >= amount) {
            ccpower$buffer -= amount;
            return true;
        }
        return false;
    }

    @Override
    public int ccpower$computerId() { return ccpower$computerId; }

    @Inject(remap = false, method = "setupComputer", at = @At("TAIL"))
    private void ccpower$setupComputer(ServerComputer computer, CallbackInfo ci) {
        ccpower$computerId = computer.getID();
        PowerManager.registerTurtle(ccpower$computerId, this);
    }

    @Inject(remap = false, method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void ccpower$chargeAction(TurtleCommand command, CallbackInfoReturnable<MethodResult> cir) {
        if (ccpower$computerId < 0) return;
        long cost = (long) CCPowerConfig.turtleActionRF();
        if (cost <= 0 || ccpower$tryConsume(cost)) return;
        cir.setReturnValue(MethodResult.of(Boolean.FALSE, "Out of RF energy"));
    }

    @Inject(remap = false, method = "readCommon", at = @At("TAIL"))
    private void ccpower$readBuffer(CompoundTag tag, CallbackInfo ci) {
        ccpower$buffer = Math.max(0, tag.getLong(CCPower$TAG_BUFFER));
    }

    @Inject(remap = false, method = "writeCommon", at = @At("TAIL"))
    private void ccpower$writeBuffer(CompoundTag tag, CallbackInfo ci) {
        tag.putLong(CCPower$TAG_BUFFER, ccpower$buffer);
    }
}
