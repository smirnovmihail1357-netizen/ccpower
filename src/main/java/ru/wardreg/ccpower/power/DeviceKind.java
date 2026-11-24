package ru.wardreg.ccpower.power;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import net.neoforged.fml.ModList;

/**
 * Which kind of CC computer is being accounted for.
 *
 * <p>Smart Glasses come from an optional mod, so this class never references
 * its classes directly: presence is checked through {@link ModList}, and the
 * computer class is identified by name. When Advanced Peripherals is absent
 * the glasses are simply never detected and everything keeps working.
 */
public enum DeviceKind {
    STATIONARY,
    POCKET,
    SMART_GLASSES;

    private static final String SMART_GLASSES_CLASS =
        "de.srendi.advancedperipherals.common.smartglasses.SmartGlassesComputer";
    private static final String SMART_GLASSES_ITEM_CLASS =
        "de.srendi.advancedperipherals.common.items.SmartGlassesItem";

    private static volatile Boolean advancedPeripheralsLoaded;

    /** Classify a computer without a hard dependency on Advanced Peripherals. */
    public static DeviceKind of(ServerComputer computer) {
        if (computer instanceof PocketServerComputer) return POCKET;
        if (isAdvancedPeripheralsLoaded()) {
            for (Class<?> type = computer.getClass(); type != null && type != Object.class;
                 type = type.getSuperclass()) {
                if (SMART_GLASSES_CLASS.equals(type.getName())) return SMART_GLASSES;
            }
        }
        return STATIONARY;
    }

    /** Public for optional integrations that must not hard-link Advanced Peripherals. */
    public static boolean advancedPeripheralsLoaded() {
        return isAdvancedPeripheralsLoaded();
    }

    /** @return true when the stack is a Smart Glasses item (by class name). */
    public static boolean isSmartGlassesItem(Object item) {
        if (!isAdvancedPeripheralsLoaded() || item == null) return false;
        for (Class<?> type = item.getClass(); type != null && type != Object.class;
             type = type.getSuperclass()) {
            if (SMART_GLASSES_ITEM_CLASS.equals(type.getName())) return true;
        }
        return false;
    }

    private static boolean isAdvancedPeripheralsLoaded() {
        Boolean cached = advancedPeripheralsLoaded;
        if (cached == null) {
            ModList list = ModList.get();
            cached = list != null && (list.isLoaded("advancedperipherals")
                || list.isLoaded("advanced_peripherals"));
            advancedPeripheralsLoaded = cached;
        }
        return cached;
    }

    /** Portable devices carry their RF in an internal buffer, not in a block. */
    public boolean isPortable() {
        return this == POCKET || this == SMART_GLASSES;
    }
}
