package ru.wardreg.ccpower.power;

/**
 * Duck interface added to CC:Tweaked's {@code TurtleBrain} via mixin.
 * Exposes the turtle's internal RF battery and owning computer id.
 */
public interface TurtleEnergyView {
    /** Current RF stored in the turtle battery. */
    long ccpower$getEnergy();

    /** Configured battery capacity in RF. */
    long ccpower$getCapacity();

    /**
     * Push up to {@code amount} RF into the battery.
     *
     * @return the accepted amount
     */
    long ccpower$receiveEnergy(long amount);

    /**
     * Atomically consume {@code amount} RF from the battery.
     *
     * @return {@code true} if the full amount was available and consumed
     */
    boolean ccpower$tryConsume(long amount);

    /** Id of the computer driving this turtle, or -1 before the computer is attached. */
    int ccpower$computerId();
}
