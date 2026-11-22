package ru.wardreg.ccpower.mixin;

import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.core.BlockPos;
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
 * Turtles are handled by the exact same power code as computers (the tick
 * accounting and the discharge shutdown live in ServerComputerMixin /
 * PowerManager). This mixin adds only the turtle-specific parts:
 *
 * <ul>
 *   <li>the internal RF battery, persisted in the turtle's NBT, refilled at the
 *       Recharge Dock, from which every tick cost is paid;</li>
 *   <li>a per-action surcharge on {@code executeCommand} (movement, digging, ...),
 *       which is cheaper than a peripheral call and independent of vanilla fuel -
 *       the fuel mechanics stay completely untouched.</li>
 * </ul>
 * <p>A turtle whose battery cannot pay the tick cost has its computer shut
 * down, so it stops dead in place until recharged and turned on again.</p>
 */
@Mixin(value = TurtleBrain.class, remap = false)
public abstract class TurtleBrainMixin implements TurtleEnergyView {
    /** NBT key for the internal battery. */
    private static final String CCPower$TAG_BUFFER = "ccpower_rf";

    /** Current RF stored in the turtle battery. Guard: server thread + Lua thread. */
    @Unique
    private long ccpower$buffer;

    /** Computer id of the turtle's computer, -1 until the computer is attached. */
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
        if (accepted > 0 && ccpower$computerId >= 0) {
            PowerManager.state(ccpower$computerId).recordCharge(accepted);
        }
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

    /** Bind the brain to its computer for power accounting. */
    @Inject(method = "setupComputer", at = @At("TAIL"), remap = false)
    private void ccpower$setupComputer(ServerComputer computer, CallbackInfo ci) {
        ccpower$computerId = computer.getID();
        PowerManager.registerTurtle(ccpower$computerId, this);
    }

    /**
     * Charge the per-action RF cost. Vanilla fuel consumption happens later inside
     * the command itself and is not modified by this mod.
     */
    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true, remap = false)
    private void ccpower$chargeAction(TurtleCommand command, CallbackInfoReturnable<MethodResult> cir) {
        if (ccpower$computerId < 0) return;
        long cost = (long) CCPowerConfig.turtleActionRF();
        if (cost <= 0) return;
        if (ccpower$tryConsume(cost)) {
            PowerManager.state(ccpower$computerId).recordDischarge(cost);
            return;
        }
        cir.setReturnValue(MethodResult.of(Boolean.FALSE, "Out of RF energy"));
    }

    /** Restore the battery from NBT. */
    @Inject(method = "readCommon", at = @At("TAIL"), remap = false)
    private void ccpower$readBuffer(CompoundTag tag, CallbackInfo ci) {
        ccpower$buffer = Math.max(0, tag.getLong(CCPower$TAG_BUFFER));
    }

    /** Store the battery into NBT. */
    @Inject(method = "writeCommon", at = @At("TAIL"), remap = false)
    private void ccpower$writeBuffer(CompoundTag tag, CallbackInfo ci) {
        tag.putLong(CCPower$TAG_BUFFER, ccpower$buffer);
    }
}
