package ru.wardreg.ccpower.power;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * All energy costs of the mod, in RF (Forge Energy) per game tick.
 *
 * <p>Fabric has no config API, so the mod manages a tiny flat TOML file itself
 * ({@code config/ccpower-common.toml}). Dotted keys mirror the Forge config
 * names; the file is re-read while the server runs.</p>
 */
public final class CCPowerConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ccpower-common.toml");

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
        Map.entry("power.idleRFPerTick", "2.0"),
        Map.entry("power.activeRFPerTick", "10.0"),
        Map.entry("power.peripheralRFPerTick", "1.0"),
        Map.entry("power.peripheralCallRF", "5.0"),
        Map.entry("power.surchargeCapPerTick", "1000.0"),
        Map.entry("power.computerBufferRF", "2000"),
        Map.entry("power.computerPullRFPerTick", "1000"),
        Map.entry("turtle.actionRF", "4.0"),
        Map.entry("turtle.bufferRF", "20000"),
        Map.entry("dock.bufferRF", "50000"),
        Map.entry("dock.pullRFPerTick", "1000"),
        Map.entry("dock.chargeRFPerTick", "250"),
        Map.entry("logging.enabled", "true"),
        Map.entry("logging.stateTransitions", "true"),
        Map.entry("logging.peripheralDiscovery", "true")
    );

    private static final Map<String, String> VALUES = new HashMap<>();
    private static volatile long lastMtime = -1;
    private static int pollCounter;

    static { load(); }

    private CCPowerConfig() {}

    private static void load() {
        VALUES.clear();
        VALUES.putAll(DEFAULTS);
        try {
            if (Files.exists(FILE)) {
                List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
                for (String raw : lines) {
                    String line = raw;
                    int hash = line.indexOf('#');
                    if (hash >= 0) line = line.substring(0, hash);
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("[")) continue;
                    int eq = line.indexOf('=');
                    if (eq < 0) continue;
                    VALUES.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
                lastMtime = Files.getLastModifiedTime(FILE).toMillis();
            } else {
                save();
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[CCPower] Failed to read " + FILE + ": " + e);
        }
    }

    private static void save() {
        StringBuilder out = new StringBuilder();
        out.append("# CC: Power - common energy costs (RF / tick).\n");
        out.append("# Values are reloaded while the server runs.\n\n");
        DEFAULTS.forEach((key, value) -> out.append(key).append(" = ").append(VALUES.getOrDefault(key, value)).append('\n'));
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, out.toString(), StandardCharsets.UTF_8);
            lastMtime = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            System.err.println("[CCPower] Failed to write " + FILE + ": " + e);
        }
    }

    /** Called by the server tick watcher: re-read the file when it changed on disk. */
    public static void watch() {
        if ((++pollCounter) % 40 != 0) return;
        try {
            if (Files.exists(FILE) && Files.getLastModifiedTime(FILE).toMillis() != lastMtime) load();
        } catch (IOException ignored) { }
    }

    private static double dbl(String key) {
        try { return Double.parseDouble(VALUES.get(key)); }
        catch (RuntimeException e) { return Double.parseDouble(DEFAULTS.get(key)); }
    }
    private static long lng(String key) {
        try { return Long.parseLong(VALUES.get(key)); }
        catch (RuntimeException e) { return Long.parseLong(DEFAULTS.get(key)); }
    }
    private static boolean bool(String key) {
        return Boolean.parseBoolean(VALUES.getOrDefault(key, DEFAULTS.get(key)));
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
    public static boolean loggingEnabled() { return bool("logging.enabled"); }
    public static boolean logStateTransitions() { return bool("logging.stateTransitions"); }
    public static boolean logPeripheralDiscovery() { return bool("logging.peripheralDiscovery"); }
}
