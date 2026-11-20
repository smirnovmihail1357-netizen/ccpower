package ru.wardreg.ccpower;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * All energy costs of the mod, in RF (Forge Energy) per game tick.
 *
 * <p>Fabric has no ModConfigSpec, so the mod manages its own minimal TOML file,
 * exactly like it already does for {@code ccpower-peripherals.toml}. The file is
 * polled every 40 ticks while the server runs, so edits apply live.
 * Keys and defaults are IDENTICAL to the NeoForge build.
 */
public final class CCPowerConfig {
    private static final Path FILE =
        FabricLoader.getInstance().getConfigDir().resolve("ccpower-common.toml");

    /** Defaults, in declaration order (also used to write a fresh file). */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();
    private static final Map<String, String> COMMENTS = new LinkedHashMap<>();

    private static volatile long lastMtime = -1L;
    private static int pollCounter;

    static {
        define("power.idleRFPerTick", "2.0", "RF/tick consumed by a powered-on idle computer");
        define("power.activeRFPerTick", "10.0", "Extra RF/tick while a program is executing");
        define("power.peripheralRFPerTick", "1.0", "Default RF/tick for a peripheral type without an explicit entry");
        define("power.peripheralCallRF", "5.0", "Base RF surcharge for a peripheral method call");
        define("power.surchargeCapPerTick", "1000.0", "Maximum carried-over call surcharge per tick");
        define("power.computerBufferRF", "2000", "Capacity of a stationary computer's internal RF buffer");
        define("power.computerPullRFPerTick", "1000", "RF/tick pulled from adjacent FE blocks into the buffer");
        define("turtle.actionRF", "4.0", "RF charged for every turtle action");
        define("turtle.bufferRF", "20000", "Capacity of the turtle internal RF battery");
        define("dock.bufferRF", "50000", "Internal buffer of the Recharge Dock, in RF");
        define("dock.pullRFPerTick", "1000", "RF/tick the Recharge Dock pulls from adjacent blocks");
        define("dock.chargeRFPerTick", "250", "RF/tick transferred into a turtle above the dock");
        define("logging.enabled", "true", "Enable CC: Power log messages");
        define("logging.stateTransitions", "true", "Log discharge shutdowns and denied power-ons");
        define("logging.peripheralDiscovery", "true", "Log every newly discovered peripheral type");
        reload();
    }

    private static void define(String key, String value, String comment) {
        DEFAULTS.put(key, value);
        COMMENTS.put(key, comment);
    }

    /** Called from the server tick entry point; re-reads the file every 40 ticks. */
    public static void watchServerTick() {
        if ((++pollCounter) % 40 != 0) return;
        reload();
    }

    private static synchronized void reload() {
        try {
            if (!Files.exists(FILE)) {
                VALUES.clear();
                VALUES.putAll(DEFAULTS);
                save();
            } else {
                Map<String, String> parsed = new LinkedHashMap<>();
                List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
                for (String raw : lines) {
                    int hash = raw.indexOf('#');
                    String line = (hash < 0 ? raw : raw.substring(0, hash)).trim();
                    if (line.isEmpty() || line.startsWith("[")) continue;
                    int eq = line.indexOf('=');
                    if (eq < 0) continue;
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if (DEFAULTS.containsKey(key)) parsed.put(key, value);
                }
                VALUES.clear();
                for (Map.Entry<String, String> def : DEFAULTS.entrySet()) {
                    VALUES.put(def.getKey(), parsed.getOrDefault(def.getKey(), def.getValue()));
                }
            }
            lastMtime = Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
        } catch (IOException | RuntimeException e) {
            CCPowerMod.LOG.error("Failed to read {}", FILE, e);
        }
    }

    private static void save() {
        StringBuilder out = new StringBuilder();
        out.append("# CC: Power - common configuration (RF per game tick).\n");
        out.append("# This file is re-read every 2 seconds while the server runs.\n\n");
        String section = "";
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            String key = e.getKey();
            String group = key.substring(0, key.indexOf('.'));
            if (!group.equals(section)) {
                section = group;
                out.append('[').append(section).append("]\n");
            }
            out.append("# ").append(COMMENTS.get(key)).append('\n');
            out.append(key).append(" = ").append(VALUES.getOrDefault(key, e.getValue())).append("\n\n");
        }
        try {
            Files.writeString(FILE, out.toString(), StandardCharsets.UTF_8);
            lastMtime = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            CCPowerMod.LOG.error("Failed to write {}", FILE, e);
        }
    }

    private static double number(String key) {
        try {
            return Double.parseDouble(VALUES.getOrDefault(key, DEFAULTS.get(key)));
        } catch (NumberFormatException e) {
            return Double.parseDouble(DEFAULTS.get(key));
        }
    }

    private static long whole(String key) {
        try {
            return Long.parseLong(VALUES.getOrDefault(key, DEFAULTS.get(key)));
        } catch (NumberFormatException e) {
            return Long.parseLong(DEFAULTS.get(key));
        }
    }

    private static boolean flag(String key) {
        return Boolean.parseBoolean(VALUES.getOrDefault(key, DEFAULTS.get(key)));
    }

    public static double idleRFPerTick() { return number("power.idleRFPerTick"); }
    public static double activeRFPerTick() { return number("power.activeRFPerTick"); }
    public static double peripheralRFPerTick() { return number("power.peripheralRFPerTick"); }
    public static double peripheralCallRF() { return number("power.peripheralCallRF"); }
    public static double surchargeCapPerTick() { return number("power.surchargeCapPerTick"); }
    public static long computerBufferRF() { return whole("power.computerBufferRF"); }
    public static long computerPullRFPerTick() { return whole("power.computerPullRFPerTick"); }
    public static double turtleActionRF() { return number("turtle.actionRF"); }
    public static long turtleBufferRF() { return whole("turtle.bufferRF"); }
    public static long dockBufferRF() { return whole("dock.bufferRF"); }
    public static long dockPullRFPerTick() { return whole("dock.pullRFPerTick"); }
    public static long dockChargeRFPerTick() { return whole("dock.chargeRFPerTick"); }
    public static boolean loggingEnabled() { return flag("logging.enabled"); }
    public static boolean logStateTransitions() { return flag("logging.stateTransitions"); }
    public static boolean logPeripheralDiscovery() { return flag("logging.peripheralDiscovery"); }

    /** Kept for parity with the NeoForge build (config reload log line). */
    public static void onConfigReloading() {
        if (loggingEnabled()) CCPowerMod.LOG.info("CC: Power configuration reloaded");
    }

    private CCPowerConfig() {}
}
