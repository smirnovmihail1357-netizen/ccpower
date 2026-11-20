package ru.wardreg.ccpower.access;

/**
 * Duck interface implemented by the PeripheralAPI mixin so that the mixin
 * into the inner {@code PeripheralWrapper} class can reach the owning
 * computer's id through the synthetic {@code this$0} reference.
 *
 * <p>Lives outside the mixin package on purpose: classes inside a mixin
 * config's package cannot be referenced directly by other code.
 */
public interface PeripheralOwnerAccess {
    /** Id of the computer the PeripheralAPI belongs to. */
    int ccpower$computerId();
}
