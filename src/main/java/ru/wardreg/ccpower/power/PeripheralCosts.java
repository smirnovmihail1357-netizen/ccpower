package ru.wardreg.ccpower.power;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.Capabilities;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.CCPowerMod;

/**
 * Per-peripheral-type energy costs in {@code config/ccpower-peripherals.toml}.
 * Java 8 / Forge 1.16.5 port: ImmutableMap instead of Map.of, Registry instead
 * of BuiltInRegistries, CC:T's CAPABILITY_PERIPHERAL instead of
 * PeripheralCapability.
 */
public final class PeripheralCosts {
    private static final Path FILE = Paths.get("config").resolve("ccpower-peripherals.toml");

    private static final Map<String, Double> TICK_COSTS = new ConcurrentHashMap<>();
    private static final Map<String, Double> CALL_COSTS = new ConcurrentHashMap<>();
    private static final Map<String, String> TYPE_OWNER = new ConcurrentHashMap<>();
    private static final String UNKNOWN_OWNER = "";
    private static final Object IO_LOCK = new Object();
    private static volatile long lastMtime = -1;
    private static int pollCounter;
    private static final String CCT_NAMESPACE = "computercraft";

    private static final Map<String, Double> SEEDS;
    static {
        ImmutableMap.Builder<String, Double> seedBuilder = ImmutableMap.builder();
        seedBuilder.put("modem", 1.0);
        seedBuilder.put("monitor", 1.0);
        seedBuilder.put("speaker", 1.0);
        seedBuilder.put("printer", 1.0);
        seedBuilder.put("drive", 1.0);
        seedBuilder.put("redstone_relay", 1.0);
        SEEDS = seedBuilder.build();
    }

    private static final Map<String, Double> CALL_SEED_MULTIPLIERS;
    static {
        ImmutableMap.Builder<String, Double> b = ImmutableMap.builder();
        b.put("modem", 0.4);
        b.put("redstone_relay", 0.4);
        b.put("speaker", 0.6);
        b.put("drive", 0.6);
        b.put("monitor", 0.8);
        b.put("printer", 1.2);
        b.put("computer", 1.0);
        b.put("turtle", 1.0);
        b.put("minecraft:chest", 3.0);
        b.put("minecraft:trapped_chest", 3.0);
        b.put("minecraft:barrel", 3.0);
        b.put("minecraft:hopper", 3.0);
        b.put("minecraft:dispenser", 3.0);
        b.put("minecraft:dropper", 3.0);
        b.put("minecraft:furnace", 3.0);
        b.put("minecraft:blast_furnace", 3.0);
        b.put("minecraft:smoker", 3.0);
        b.put("minecraft:brewing_stand", 3.0);
        b.put("minecraft:lectern", 0.8);
        b.put("minecraft:jukebox", 1.0);
        b.put("minecraft:command_block", 1.0);
        CALL_SEED_MULTIPLIERS = b.build();
    }

    private static final Map<String, Double> CALL_SEED_SUFFIX_MULTIPLIERS =
        ImmutableMap.of("_shulker_box", 3.0);

    private PeripheralCosts() {}

    private static double defaultCallCost(String type) {
        Double multiplier = CALL_SEED_MULTIPLIERS.get(type);
        if (multiplier == null) {
            for (Map.Entry<String, Double> rule : CALL_SEED_SUFFIX_MULTIPLIERS.entrySet()) {
                if (type.endsWith(rule.getKey())) {
                    multiplier = rule.getValue();
                    break;
                }
            }
        }
        double base = CCPowerConfig.peripheralCallRF();
        double cost = base * (multiplier != null ? multiplier : 1.0);
        return Math.round(cost * 100.0) / 100.0;
    }

    public static double tickCost(String type) {
        Double known = TICK_COSTS.get(type);
        if (known != null) return known;
        return discoverTick(type);
    }

    public static double callCost(String type) {
        Double known = CALL_COSTS.get(type);
        if (known != null) return known;
        return discoverCall(type);
    }

    public static void watchServerTick() {
        if ((++pollCounter) % 40 != 0) return;
        synchronized (IO_LOCK) {
            try {
                FileTime mtime = Files.getLastModifiedTime(FILE);
                if (mtime.toMillis() != lastMtime) reload();
            } catch (IOException ignored) {
                // A missing or unreadable file is retried on the next poll.
            }
        }
    }

    public static void load(World level) {
        synchronized (IO_LOCK) {
            reload();
            scanInstalledPeripherals(level);
            save();
        }
    }

    private static void reload() {
        try {
            if (Files.exists(FILE)) {
                parse(Files.readAllLines(FILE, StandardCharsets.UTF_8));
            } else {
                TICK_COSTS.clear();
                CALL_COSTS.clear();
                TYPE_OWNER.clear();
                TICK_COSTS.putAll(SEEDS);
                for (String type : SEEDS.keySet()) {
                    TYPE_OWNER.put(type, CCT_NAMESPACE);
                    CALL_COSTS.put(type, defaultCallCost(type));
                }
            }
            int seeded = syncCallCosts();
            if (seeded > 0) {
                save();
                CCPowerMod.LOG.info(
                    "Seeded {} missing per-call prices into ccpower-peripherals.toml", seeded);
            }
            lastMtime = Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
            CCPowerMod.LOG.info("Peripheral cost table loaded: {} types", TICK_COSTS.size());
        } catch (IOException | RuntimeException e) {
            CCPowerMod.LOG.error("Failed to read {}", FILE, e);
        }
    }

    private static int syncCallCosts() {
        int added = 0;
        for (String type : TICK_COSTS.keySet()) {
            if (!CALL_COSTS.containsKey(type)) {
                CALL_COSTS.put(type, defaultCallCost(type));
                added++;
            }
        }
        return added;
    }

    /** Walk every registered block entity type and probe it for peripherals. */
    private static void scanInstalledPeripherals(World level) {
        int discovered = 0;
        int mods = 0;
        List<String> seenOwners = new ArrayList<>();
        try {
            outer:
            for (TileEntityType<?> bet : Registry.BLOCK_ENTITY_TYPE) {
                for (Block block : Registry.BLOCK) {
                    if (!bet.isValid(block)) continue;
                    String type = probePeripheralType(bet, block, level);
                    if (type == null) continue;
                    String owner = namespaceOf(block);
                    TYPE_OWNER.putIfAbsent(type, owner);
                    double tickDefault = CCPowerConfig.peripheralRFPerTick();
                    CALL_COSTS.putIfAbsent(type, defaultCallCost(type));
                    if (TICK_COSTS.putIfAbsent(type, tickDefault) == null) {
                        discovered++;
                        if (!seenOwners.contains(owner)) {
                            seenOwners.add(owner);
                            mods++;
                        }
                        CCPowerMod.LOG.debug(
                            "Pre-scan found peripheral type \"{}\" provided by mod {}", type, owner);
                    }
                    continue outer;
                }
            }
        } catch (Throwable t) {
            CCPowerMod.LOG.error("Peripheral pre-scan failed part-way through", t);
        }
        if (discovered > 0) {
            CCPowerMod.LOG.info(
                "Peripheral pre-scan: {} new types from {} mods appended to ccpower-peripherals.toml",
                discovered, mods);
        } else {
            CCPowerMod.LOG.info("Peripheral pre-scan: cost table already covers all {} known types",
                TICK_COSTS.size());
        }
    }

    private static String probePeripheralType(TileEntityType<?> bet, Block block, World level) {
        try {
            BlockState state = block.defaultBlockState();
            TileEntity be = bet.create();
            if (be == null) return null;
            be.setLevelAndPosition(level, BlockPos.ZERO);

            if (be instanceof IPeripheral) {
                String type = ((IPeripheral) be).getType();
                if (type != null && !type.trim().isEmpty()) return type;
            }

            Capability<IPeripheral> capability = Capabilities.CAPABILITY_PERIPHERAL;
            LazyOptional<IPeripheral> direct = be.getCapability(capability, null);
            IPeripheral peripheral = direct.orElse(null);
            if (peripheral == null) {
                for (Direction side : Direction.values()) {
                    peripheral = be.getCapability(capability, side).orElse(null);
                    if (peripheral != null) break;
                }
            }
            if (peripheral == null) return null;
            String type = peripheral.getType();
            return type == null || type.trim().isEmpty() ? null : type;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String namespaceOf(Block block) {
        ResourceLocation key = Registry.BLOCK.getKey(block);
        return key == null ? UNKNOWN_OWNER : key.getNamespace();
    }

    private static String displayNameOf(String namespace) {
        if (namespace.equals(CCT_NAMESPACE)) return "CC: Tweaked";
        if (ModList.get() == null) return namespace;
        return ModList.get().getModContainerById(namespace)
            .map(container -> container.getModInfo().getDisplayName())
            .filter(name -> name != null && !name.trim().isEmpty())
            .orElse(namespace);
    }

    private static synchronized double discoverTick(String type) {
        Double known = TICK_COSTS.get(type);
        if (known != null) return known;

        double fallback = CCPowerConfig.peripheralRFPerTick();
        TICK_COSTS.put(type, fallback);
        CALL_COSTS.putIfAbsent(type, defaultCallCost(type));
        TYPE_OWNER.computeIfAbsent(type, PeripheralCosts::ownerFromType);
        if (CCPowerConfig.loggingEnabled() && CCPowerConfig.logPeripheralDiscovery()) {
            CCPowerMod.LOG.info(
                "Discovered peripheral type \"{}\", using the defaults {} RF/tick and {} RF/call - adjust them in ccpower-peripherals.toml",
                type, fallback, CALL_COSTS.get(type));
        }
        synchronized (IO_LOCK) {
            save();
        }
        return fallback;
    }

    private static synchronized double discoverCall(String type) {
        Double known = CALL_COSTS.get(type);
        if (known != null) return known;

        double fallback = defaultCallCost(type);
        CALL_COSTS.put(type, fallback);
        TICK_COSTS.putIfAbsent(type, CCPowerConfig.peripheralRFPerTick());
        TYPE_OWNER.computeIfAbsent(type, PeripheralCosts::ownerFromType);
        if (CCPowerConfig.loggingEnabled() && CCPowerConfig.logPeripheralDiscovery()) {
            CCPowerMod.LOG.info(
                "Discovered peripheral type \"{}\", using the default {} RF/call - adjust it in ccpower-peripherals.toml",
                type, fallback);
        }
        synchronized (IO_LOCK) {
            save();
        }
        return fallback;
    }

    private static String ownerFromType(String type) {
        int sep = type.indexOf(':');
        return sep > 0 ? type.substring(0, sep) : UNKNOWN_OWNER;
    }

    private static void parse(List<String> lines) {
        Map<String, Double> tick = new TreeMap<>();
        Map<String, Double> call = new TreeMap<>();
        String section = "";
        for (String raw : lines) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[")) {
                section = line;
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = unquote(line.substring(0, eq).trim());
            double value;
            try {
                value = Double.parseDouble(line.substring(eq + 1).trim());
            } catch (NumberFormatException invalidValue) {
                CCPowerMod.LOG.warn("Invalid cost for peripheral type \"{}\" in ccpower-peripherals.toml", key);
                continue;
            }
            if (section.equals("[rf_per_tick]")) tick.put(key, value);
            else if (section.equals("[rf_per_call]")) call.put(key, value);
        }
        TICK_COSTS.clear();
        TICK_COSTS.putAll(tick);
        CALL_COSTS.clear();
        CALL_COSTS.putAll(call);
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    private static String unquote(String key) {
        if (key.length() >= 2 && key.startsWith("\"") && key.endsWith("\"")) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    private static void save() {
        Map<String, TreeMap<String, Double>> tickGroups = group(TICK_COSTS);
        Map<String, TreeMap<String, Double>> callGroups = group(CALL_COSTS);

        StringBuilder out = new StringBuilder();
        out.append("# CC: Power - per-peripheral-type energy costs (RF).\n");
        out.append("#\n# This file is maintained automatically:\n");
        out.append("#  - at server start every installed mod is scanned for CC:Tweaked\n");
        out.append("#    peripherals and all found types are listed here, grouped by mod;\n");
        out.append("#  - types only visible in the world are appended with defaults;\n");
        out.append("#  - edit the values freely, the file is re-read while the server runs.\n");
        out.append("#\n# Every type listed in [rf_per_tick] automatically gets a matching\n");
        out.append("# entry in [rf_per_call]. Set a price to 0.0 to disable it - do not\n");
        out.append("# delete the line, the mod would only re-seed it.\n");
        out.append("#\n# [rf_per_tick]: RF per tick for each ATTACHED peripheral.\n");
        out.append("# [rf_per_call]: RF surcharge for each peripheral method CALL.\n\n");
        appendSection(out, "[rf_per_tick]", tickGroups);
        appendSection(out, "[rf_per_call]", callGroups);

        try {
            Files.write(FILE, out.toString().getBytes(StandardCharsets.UTF_8));
            lastMtime = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            CCPowerMod.LOG.error("Failed to write {}", FILE, e);
        }
    }

    private static Map<String, TreeMap<String, Double>> group(Map<String, Double> costs) {
        Map<String, TreeMap<String, Double>> groups = new TreeMap<>();
        costs.forEach((type, cost) -> groups
            .computeIfAbsent(TYPE_OWNER.getOrDefault(type, UNKNOWN_OWNER), owner -> new TreeMap<>())
            .put(type, cost));
        return groups;
    }

    private static void appendSection(StringBuilder out, String header,
        Map<String, TreeMap<String, Double>> groups) {
        out.append(header).append('\n');

        List<String> owners = new ArrayList<>(groups.keySet());
        owners.sort((a, b) -> {
            if (a.equals(b)) return 0;
            if (a.equals(UNKNOWN_OWNER)) return 1;
            if (b.equals(UNKNOWN_OWNER)) return -1;
            if (a.equals(CCT_NAMESPACE)) return -1;
            if (b.equals(CCT_NAMESPACE)) return 1;
            return displayNameOf(a).compareToIgnoreCase(displayNameOf(b));
        });

        for (String owner : owners) {
            out.append('\n');
            out.append("# ==============================================================\n");
            if (owner.equals(UNKNOWN_OWNER)) {
                out.append("#  Unknown provider (discovered at runtime)\n");
            } else {
                out.append("#  ").append(displayNameOf(owner)).append(" (").append(owner).append(")\n");
            }
            out.append("# ==============================================================\n");
            final String currentOwner = owner;
            groups.get(owner).forEach((type, cost) ->
                out.append('"').append(type).append("\" = ").append(cost).append('\n'));
        }
        out.append('\n');
    }
}
