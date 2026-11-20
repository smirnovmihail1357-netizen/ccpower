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
 * <p>Fabric has no NeoForge config system, so the mod owns this file itself,
 * using the same minimal TOML reader/writer style as {@code PeripheralCosts}.
 * The file is watched while the server runs and reloaded on change.
 */
public final class CCPowerConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("ccpower-common.toml");

    /** key -> value, in declaration order. */
    private static final Map<String, Object> VALUES = new LinkedHashMap<>();

    private static volatile long lastMtime = -1;
    private static int pollCounter;

    private CCPowerConfig() {}

    private static void defaults() {
        VALUES.clear();
        VALUES.put("power.idleRFPerTick", 2.0d);
        VALUES.put("power.activeRFPerTick", 10.0d);
        VALUES.put("power.peripheralRFPerTick", 1.0d);
        VALUES.put("power.peripheralCallRF", 5.0d);
        VALUES.put("power.surchargeCapPerTick", 1000.0d);
        VALUES.put("power.computerBufferRF", 2000L);
        VALUES.put("power.computerPullRFPerTick", 1000L);
        VALUES.put("turtle.actionRF", 4.0d);
        VALUES.put("turtle.bufferRF", 20000L);
        VALUES.put("dock.bufferRF", 50000L);
        VALUES.put("dock.pullRFPerTick", 1000L);
        VALUES.put("dock.chargeRFPerTick", 250L);
        VALUES.put("logging.enabled", true);
        VALUES.put("logging.stateTransitions", true);
        VALUES.put("logging.peripheralDiscovery", true);
    }

    static {
        defaults();
        load();
    }

    /** Load the file (or write defaults on first boot). */
    public static void load() {
        synchronized (CCPowerConfig.class) {
            defaults();
            try {
                if (Files.exists(FILE)) {
                    for (String raw : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                        String line = raw;
                        int hash = line.indexOf('#');
                        if (hash >= 0) line = line.substring(0, hash);
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        int eq = line.indexOf('=');
                        if (eq < 0) continue;
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        putParsed(key, value);
                    }
                } else {
                    save();
                }
                lastMtime = Files.exists(FILE)
                    ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
                CCPowerMod.LOG.info("CC: Power configuration loaded from {}", FILE);
            } catch (IOException | RuntimeException e) {
                CCPowerMod.LOG.error("Failed to read {}", FILE, e);
            }
        }
    }

    private static void putParsed(String key, String value) {
        if (!VALUES.containsKey(key)) return;
        Object current = VALUES.get(key);
        try {
            if (current instanceof Double) {
                VALUES.put(key, Double.parseDouble(value));
            } else if (current instanceof Long) {
                VALUES.put(key, Long.parseLong(value));
            } else if (current instanceof Boolean) {
                VALUES.put(key, Boolean.parseBoolean(value));
            }
        } catch (NumberFormatException ignored) {
            // Keep the default for malformed values.
        }
    }

    /** Poll the file mtime every 40 ticks (2 seconds). */
    public static void watchServerTick() {
        if ((++pollCounter) % 40 != 0) return;
        synchronized (CCPowerConfig.class) {
            try {
                if (!Files.exists(FILE)) return;
                FileTime mtime = Files.getLastModifiedTime(FILE);
                if (mtime.toMillis() != lastMtime) {
                    load();
                    CCPowerMod.LOG.info("CC: Power configuration reloaded");
                }
            } catch (IOException ignored) {
                // Retried on the next poll.
            }
        }
    }

    private static void save() {
        try {
            StringBuilder out = new StringBuilder();
            out.append("# CC: Power - common configuration (RF per game tick).\n");
            out.append("# Edit freely; this file is re-read while the server runs.\n\n");
            for (Map.Entry<String, Object> e : VALUES.entrySet()) {
                out.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
            }
            Files.writeString(FILE, out.toString(), StandardCharsets.UTF_8);
            lastMtime = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            CCPowerMod.LOG.error("Failed to write {}", FILE, e);
        }
    }

    private static double dbl(String key) {
        Object v = VALUES.get(key);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static long lng(String key) {
        Object v = VALUES.get(key);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private static boolean boolValue(String key) {
        Object v = VALUES.get(key);
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
    }

    public static double idleRFPerTick() { return dbl("power.idleRFPerTick"); }
    public static double activeRFPerTick() { return dbl("power.activeRFPerTick"); }
    public static double peripheralRFPerTick() { return dbl("power.peripheralRFPerTick"); }
    public static double peripheralCallRF() { return dbl("power.peripheralCallRF"); }
    public static double surchargeCapPerTick() { return dbl("power.surchargeCapPerTick"); }
    public static long computerBufferRF() { return lng("power.computerBufferRF"); }
    public static long computerPullRFPerTick() { return lng("power.computerPullRFPerTick"); }
    public static double turtleActionRF() { return dbl("turtle.actionRF"); }
    public static long turtleBufferRF() { return lng("turtle.bufferRF"); }
    public static long dockBufferRF() { return lng("dock.bufferRF"); }
    public static long dockPullRFPerTick() { return lng("dock.pullRFPerTick"); }
    public static long dockChargeRFPerTick() { return lng("dock.chargeRFPerTick"); }
    public static boolean loggingEnabled() { return boolValue("logging.enabled"); }
    public static boolean logStateTransitions() { return boolValue("logging.stateTransitions"); }
    public static boolean logPeripheralDiscovery() { return boolValue("logging.peripheralDiscovery"); }
}
