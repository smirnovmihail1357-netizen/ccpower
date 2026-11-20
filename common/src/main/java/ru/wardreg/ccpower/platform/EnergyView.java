package ru.wardreg.ccpower.platform;

/**
 * Loader-neutral view of the Recharge Dock's internal RF buffer. Each loader
 * wraps this in its native energy API (ForgeCapabilities.ENERGY / Team Reborn
 * Energy) when registering the block entity.
 */
public interface EnergyView {
    /** Insert RF into the buffer, optionally only simulating the transfer. */
    long receive(long amount, boolean simulate);

    /** RF currently stored. */
    long getAmount();

    /** Configured capacity in RF. */
    long getCapacity();
}
