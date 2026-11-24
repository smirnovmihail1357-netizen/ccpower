package ru.wardreg.ccpower.power;

/**
 * How a portable device's per-tick RF cost is calculated. The two modes are
 * mutually exclusive by design - config exposes a single enum, not two flags.
 */
public enum CostMode {
    /** Multiply the normal computer block's tick cost by a configured factor. */
    MULTIPLIER,
    /** Use one explicit RF/tick value, ignoring the computer's cost formula. */
    FIXED
}
