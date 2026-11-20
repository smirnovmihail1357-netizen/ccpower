package ru.wardreg.ccpower;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

/**
 * Minimal self-managed TOML config (Fabric has no ModConfigSpec). Same keys and
 * defaults as the NeoForge build; the file is re-read while the server runs.
 */
public final class CCPowerConfig {
    private static final Path FILE = net.fabricmc.loader.api.FabricLoader.getInstance()
        .getConfigDir().resolve("ccpower-common.toml");

    private static volatile double idleRFPerTick = 2.0;
    private static volatile double activeRFPerTick = 10.0;
    private static volatile double peripheralRFPerTick = 1.0;
    private static volatile double peripheralCallRF = 5.0;
    private static volatile double surchargeCapPerTick = 1000.0;
    private static volatile long computerBufferRF = 2000L;
    private static volatile long computerPullRFPerTick = 1000L;
    private static volatile double turtleActionRF = 4.0;
    private static volatile long turtleBufferRF = 20000L;
    private static volatile long dockBufferRF = 50000L;
    private static volatile long dockPullRFPerTick = 1000L;
    private static volatile long dockChargeRFPerTick = 250L;
    private static volatile boolean loggingEnabled = true;
    private static volatile boolean logStateTransitions = true;
    private static volatile boolean logPeripheralDiscovery = true;

    private static volatile long lastMtime = -1L;
    private static int pollCounter;

    private CCPowerConfig() {}

    /** Load the file, creating it with defaults on first boot. */
    public static synchronized void load() {
        if (!Files.exists(FILE)) saveDefaults();
        reload();
    }

    /** Called at the end of each server tick; re-read every 40 ticks (2 seconds). */
    public static void watch() {
        if ((++pollCounter) % 40 != 0) return;
        try {
            long mtime = Files.getLastModifiedTime(FILE).toMillis();
            if (mtime != lastMtime) reload();
        } catch (IOException ignored) {
            // Missing or unreadable file: retried on the next poll.
        }
    }

    private static synchronized void reload() {
        try {
            for (String raw : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String line = raw;
                int hash = line.indexOf('#');
                if (hash >= 0) line = line.substring(0, hash);
                line = line.trim();
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                try {
                    switch (key) {
                        case "power.idleRFPerTick" -> idleRFPerTick = Double.parseDouble(value);
                        case "power.activeRFPerTick" -> activeRFPerTick = Double.parseDouble(value);
                        case "power.peripheralRFPerTick" -> peripheralRFPerTick = Double.parseDouble(value);
                        case "power.peripheralCallRF" -> peripheralCallRF = Double.parseDouble(value);
                        case "power.surchargeCapPerTick" -> surchargeCapPerTick = Double.parseDouble(value);
                        case "power.computerBufferRF" -> computerBufferRF = Long.parseLong(value);
                        case "power.computerPullRFPerTick" -> computerPullRFPerTick = Long.parseLong(value);
                        case "turtle.actionRF" -> turtleActionRF = Double.parseDouble(value);
                        case "turtle.bufferRF" -> turtleBufferRF = Long.parseLong(value);
                        case "dock.bufferRF" -> dockBufferRF = Long.parseLong(value);
                        case "dock.pullRFPerTick" -> dockPullRFPerTick = Long.parseLong(value);
                        case "dock.chargeRFPerTick" -> dockChargeRFPerTick = Long.parseLong(value);
                        case "logging.enabled" -> loggingEnabled = Boolean.parseBoolean(value);
                        case "logging.stateTransitions" -> logStateTransitions = Boolean.parseBoolean(value);
                        case "logging.peripheralDiscovery" -> logPeripheralDiscovery = Boolean.parseBoolean(value);
                        default -> { }
                    }
                } catch (NumberFormatException bad) {
                    CCPowerMod.LOG.warn("Invalid value for {} in ccpower-common.toml", key);
                }
            }
            lastMtime = Files.getLastModifiedTime(FILE).toMillis();
            CCPowerMod.LOG.info("CC: Power configuration reloaded");
        } catch (IOException e) {
            CCPowerMod.LOG.error("Failed to read {}", FILE, e);
        }
    }

    private static void saveDefaults() {
        StringBuilder out = new StringBuilder();
        out.append("# CC: Power - RF (Forge Energy) costs, per game tick.\n");
        out.append("#\n#  power.idleRFPerTick = 2.0\n#  power.activeRFPerTick = 10.0\n");
        out.append("power.idleRFPerTick = 2.0\n");
        out.append("power.activeRFPerTick = 10.0\n");
        out.append("power.peripheralRFPerTick = 1.0\n");
        out.append("power.peripheralCallRF = 5.0\n");
        out.append("power.surchargeCapPerTick = 1000.0\n");
        out.append("power.computerBufferRF = 2000\n");
        out.append("power.computerPullRFPerTick = 1000\n");
        out.append("turtle.actionRF = 4.0\n");
        out.append("turtle.bufferRF = 20000\n");
        out.append("dock.bufferRF = 50000\n");
        out.append("dock.pullRFPerTick = 1000\n");
        out.append("dock.chargeRFPerTick = 250\n");
        out.append("logging.enabled = true\n");
        out.append("logging.stateTransitions = true\n");
        out.append("logging.peripheralDiscovery = true\n");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CCPowerMod.LOG.error("Failed to write {}", FILE, e);
        }
    }

    public static double idleRFPerTick() { return idleRFPerTick; }
    public static double activeRFPerTick() { return activeRFPerTick; }
    public static double peripheralRFPerTick() { return peripheralRFPerTick; }
    public static double peripheralCallRF() { return peripheralCallRF; }
    public static double surchargeCapPerTick() { return surchargeCapPerTick; }
    public static long computerBufferRF() { return computerBufferRF; }
    public static long computerPullRFPerTick() { return computerPullRFPerTick; }
    public static double turtleActionRF() { return turtleActionRF; }
    public static long turtleBufferRF() { return turtleBufferRF; }
    public static long dockBufferRF() { return dockBufferRF; }
    public static long dockPullRFPerTick() { return dockPullRFPerTick; }
    public static long dockChargeRFPerTick() { return dockChargeRFPerTick; }
    public static boolean loggingEnabled() { return loggingEnabled; }
    public static boolean logStateTransitions() { return logStateTransitions; }
    public static boolean logPeripheralDiscovery() { return logPeripheralDiscovery; }
}
