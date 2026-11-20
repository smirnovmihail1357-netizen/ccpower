package ru.wardreg.ccpower.mixin;

import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.nbt.CompoundNBT;
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
 * Turtle-specific parts: an RF battery persisted in NBT, charged at the dock.
 * Era note: CC:T 1.101 takes {@code ITurtleCommand} (1.122 uses TurtleCommand)
 * and plain {@code CompoundNBT} in readCommon/writeCommon (no HolderLookup).
 */
@Mixin(value = TurtleBrain.class, remap = false)
public abstract class TurtleBrainMixin implements TurtleEnergyView {
    private static final String CCPower$TAG_BUFFER = "ccpower_rf";

    @Unique
    private long ccpower$buffer;

    @Unique
    private int ccpower$computerId = -1;

    @Override
    public long ccpower$getEnergy() {
        return ccpower$buffer;
    }

    @Override
    public long ccpower$getCapacity() {
        return CCPowerConfig.turtleBufferRF();
    }

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
    public int ccpower$computerId() {
        return ccpower$computerId;
    }

    @Inject(method = "setupComputer", at = @At("TAIL"), remap = false)
    private void ccpower$setupComputer(ServerComputer computer, CallbackInfo ci) {
        ccpower$computerId = computer.getID();
        PowerManager.registerTurtle(ccpower$computerId, this);
    }

    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true, remap = false)
    private void ccpower$chargeAction(ITurtleCommand command, CallbackInfoReturnable<MethodResult> cir) {
        if (ccpower$computerId < 0) return;
        long cost = (long) CCPowerConfig.turtleActionRF();
        if (cost <= 0 || ccpower$tryConsume(cost)) return;
        cir.setReturnValue(MethodResult.of(Boolean.FALSE, "Out of RF energy"));
    }

    @Inject(method = "readCommon", at = @At("TAIL"), remap = false)
    private void ccpower$readBuffer(CompoundNBT tag, CallbackInfo ci) {
        ccpower$buffer = Math.max(0, tag.getLong(CCPower$TAG_BUFFER));
    }

    @Inject(method = "writeCommon", at = @At("TAIL"), remap = false)
    private void ccpower$writeBuffer(CompoundNBT tag, CallbackInfo ci) {
        tag.putLong(CCPower$TAG_BUFFER, ccpower$buffer);
    }
}
