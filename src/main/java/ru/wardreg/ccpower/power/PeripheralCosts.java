package ru.wardreg.ccpower.power;

import dan200.computercraft.api.peripheral.IPeripheral;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.loader.api.FabricLoader;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.CCPowerMod;

/**
 * Per-peripheral-type energy costs, stored in {@code config/ccpower-peripherals.toml}.
 *
 * <p>The file holds two sections:
 * <ul>
 *   <li>{@code [rf_per_tick]} - RF per tick for each peripheral <b>attached</b>
 *       to a running computer, billed every tick;</li>
 *   <li>{@code [rf_per_call]} - RF surcharge for each peripheral method
 *       <b>call</b>, settled with the next tick's cost.</li>
 * </ul>
 *
 * <p>NeoForge's config system cannot grow new keys at runtime, so this file is
 * managed by the mod itself:
 * <ul>
 *   <li>when the server starts, every installed mod's block entities are probed
 *       for CC:Tweaked peripherals and all found types are written to the file,
 *       grouped into one section per mod;</li>
 *   <li>types the pre-scan cannot see (generic sources such as inventories,
 *       or peripherals created only in world) are appended automatically the
 *       first time a computer attaches to or calls them. The default tick
 *       price comes from {@code power.peripheralRFPerTick}; the default call
 *       price is type-specific - well-known types start at a curated
 *       multiplier of {@code power.peripheralCallRF} (item handlers cost
 *       more than displays), everything else at the plain base value;</li>
 *   <li>the file is polled while the server runs, so edited values are picked
 *       up without a restart.</li>
 * </ul>
 * Peripheral types are the exact strings Lua's {@code peripheral.getType()}
 * returns (for example {@code minecraft:chest} or {@code speaker}).
 *
 * <p>Note: devices behind a wired modem are not attached to the computer as
 * side peripherals - the modem itself is the peripheral and is billed as type
 * {@code modem}.
 */
public final class PeripheralCosts {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ccpower-peripherals.toml");

    /** RF per tick for each attached peripheral type. */
    private static final Map<String, Double> TICK_COSTS = new ConcurrentHashMap<>();

    /** RF per method call for each peripheral type. */
    private static final Map<String, Double> CALL_COSTS = new ConcurrentHashMap<>();

    /** Owning mod namespace per peripheral type, used to group the config file. */
    private static final Map<String, String> TYPE_OWNER = new ConcurrentHashMap<>();

    /** Bucket for types whose providing mod is unknown (discovered at runtime). */
    private static final String UNKNOWN_OWNER = "";

    private static final Object IO_LOCK = new Object();

    /** Modification time of the file as it was last read, for change polling. */
    private static volatile long lastMtime = -1;

    /** Tick counter for the polling cadence (every 40 ticks = 2 seconds). */
    private static int pollCounter;

    /** Mod id of CC:Tweaked itself, whose built-in types have no namespace. */
    private static final String CCT_NAMESPACE = "computercraft";

    /** Known CC:Tweaked built-in types, seeded into a freshly created file. */
    private static final Map<String, Double> SEEDS = Map.of(
        "modem", 1.0,
        "monitor", 1.0,
        "speaker", 1.0,
        "printer", 1.0,
        "drive", 1.0,
        "redstone_relay", 1.0
    );

    /**
     * Curated default call-cost multipliers, applied to
     * {@code power.peripheralCallRF} when a type is first discovered. Every
     * well-known type starts with its OWN price instead of the flat default:
     * item-handling peripherals (chests, hoppers, furnaces) are expensive
     * because a call moves item data, while displays and simple I/O
     * (monitors, modems, speakers) are cheap.
     */
    private static final Map<String, Double> CALL_SEED_MULTIPLIERS = Map.ofEntries(
        // CC: Tweaked built-ins: displays and simple I/O are cheap.
        Map.entry("modem", 0.4),
        Map.entry("redstone_relay", 0.4),
        Map.entry("speaker", 0.6),
        Map.entry("drive", 0.6),
        Map.entry("monitor", 0.8),
        Map.entry("printer", 1.2),
        Map.entry("computer", 1.0),
        Map.entry("turtle", 1.0),
        // Vanilla blocks whose peripherals shuffle item data: expensive.
        Map.entry("minecraft:chest", 3.0),
        Map.entry("minecraft:trapped_chest", 3.0),
        Map.entry("minecraft:barrel", 3.0),
        Map.entry("minecraft:hopper", 3.0),
        Map.entry("minecraft:dispenser", 3.0),
        Map.entry("minecraft:dropper", 3.0),
        Map.entry("minecraft:furnace", 3.0),
        Map.entry("minecraft:blast_furnace", 3.0),
        Map.entry("minecraft:smoker", 3.0),
        Map.entry("minecraft:brewing_stand", 3.0),
        // Lighter vanilla peripherals.
        Map.entry("minecraft:lectern", 0.8),
        Map.entry("minecraft:jukebox", 1.0),
        Map.entry("minecraft:command_block", 1.0)
    );

    /** Suffix rules for families of blocks, applied after exact lookups. */
    private static final Map<String, Double> CALL_SEED_SUFFIX_MULTIPLIERS = Map.of(
        // All 17 shulker box colours share one heavy-item price.
        "_shulker_box", 3.0
    );

    private PeripheralCosts() {}

    /**
     * Default RF/call for a freshly discovered type: the curated multiplier
     * for that type applied to {@code power.peripheralCallRF}, or the plain
     * base value for types without a curated entry. Values are rounded to
     * two decimal places so the file stays readable.
     */
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

    /**
     * Read the attached-peripheral tick cost of a type. A type seen for the
     * first time is appended to the config file with the default cost and logged.
     */
    public static double tickCost(String type) {
        Double known = TICK_COSTS.get(type);
        if (known != null) return known;
        return discoverTick(type);
    }

    /**
     * Read the per-call surcharge of a type. A type called for the first time
     * is appended to the config file with the default cost and logged.
     */
    public static double callCost(String type) {
        Double known = CALL_COSTS.get(type);
        if (known != null) return known;
        return discoverCall(type);
    }

    /** Called by the server tick watcher: reload the file when it changed on disk. */
    public static void watchServerTick() {
        if ((++pollCounter) % 40 != 0) return;
        synchronized (IO_LOCK) {
            try {
                FileTime mtime = Files.getLastModifiedTime(FILE);
                if (mtime.toMillis() != lastMtime) {
                    reload();
                }
            } catch (IOException notFoundOrUnreadable) {
                // A missing or unreadable file is retried on the next poll.
            }
        }
    }

    /**
     * Load (or create) the file and pre-scan every installed mod for peripherals.
     * Called once the server has started.
     */
    public static void load(Level level) {
        synchronized (IO_LOCK) {
            reload();
            scanInstalledPeripherals(level);
            // Normalize the file layout (per-mod sections) once per boot; the
            // user's values were parsed first, so nothing is lost here.
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
                SEEDS.keySet().forEach(type -> {
                    TYPE_OWNER.put(type, CCT_NAMESPACE);
                    CALL_COSTS.put(type, defaultCallCost(type));
                });
            }
            // Every tick-priced type must also have a call price: older files
            // (or a deleted section) leave gaps that are seeded here, so the
            // [rf_per_call] section always lists the same types as [rf_per_tick].
            int seeded = syncCallCosts();
            if (seeded > 0) {
                save();
                CCPowerMod.LOG.info(
                    "Seeded {} missing per-call prices into ccpower-peripherals.toml", seeded);
            }
            // A missing file (first boot, before save() creates it) must not
            // log an error; the watcher retries and picks it up once written.
            lastMtime = Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
            CCPowerMod.LOG.info("Peripheral cost table loaded: {} types", TICK_COSTS.size());
        } catch (IOException | RuntimeException e) {
            CCPowerMod.LOG.error("Failed to read {}", FILE, e);
        }
    }

    /**
     * Ensure every type with a tick price also has a call price. Types whose
     * call entry was never written (file created before the per-call table
     * existed, or a hand-deleted line) get the type-specific default. Values
     * the user already set are never touched.
     *
     * @return how many call entries were added.
     */
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

    /**
     * Walk every registered block entity type, instantiate it without a level
     * and ask it (or its CC:Tweaked capability) for the peripheral type string.
     * This populates the config with all peripherals the installed mods can
     * ever provide, before any computer has even been placed.
     */
    private static void scanInstalledPeripherals(Level level) {
        int discovered = 0;
        int mods = 0;
        List<String> seenOwners = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        List<net.minecraft.world.level.block.state.BlockState> states = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            blocks.add(block);
            states.add(block.defaultBlockState());
        }
        try {
            outer:
            for (BlockEntityType<?> bet : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
                for (int i = 0; i < blocks.size(); i++) {
                    // Vanilla has no getValidBlocks(); isValid() is the
                    // loader-neutral way to find blocks a BE type applies to.
                    if (!bet.isValid(states.get(i))) continue;
                    Block block = blocks.get(i);
                    String type = probePeripheralType(bet, block, level);
                    if (type == null) continue;
                    String owner = namespaceOf(block);
                    TYPE_OWNER.putIfAbsent(type, owner);
                    double tickDefault = CCPowerConfig.peripheralRFPerTick();
                    // The call price is seeded for EVERY found type, not just
                    // brand-new ones: a file written before per-call prices
                    // existed must not leave gaps in [rf_per_call].
                    CALL_COSTS.putIfAbsent(type, defaultCallCost(type));
                    if (TICK_COSTS.putIfAbsent(type, tickDefault) == null) {
                        discovered++;
                        if (!seenOwners.contains(owner)) {
                            seenOwners.add(owner);
                            mods++;
                        }
                        CCPowerMod.LOG.debug(
                            "Pre-scan found peripheral type \"{}\" provided by mod {}",
                            type, owner);
                    }
                    continue outer; // one representative block per BE type is enough
                }
            }
        } catch (Throwable t) {
            // A broken registry entry must never prevent the server from starting.
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

    /**
     * Instantiate a block entity without a level and look for a CC:Tweaked
     * peripheral: directly on the block entity, or through the peripheral
     * capability the way CC:Tweaked and most integration mods register theirs.
     * Anything that throws is simply not a probeable peripheral.
     */
    private static String probePeripheralType(BlockEntityType<?> bet, Block block, Level level) {
        try {
            var state = block.defaultBlockState();
            BlockEntity be = bet.create(BlockPos.ZERO, state);
            if (be == null) return null;

            if (be instanceof IPeripheral direct && direct.getType() != null
                && !direct.getType().isBlank()) {
                return direct.getType();
            }

            var capability = PeripheralLookup.get();
            IPeripheral peripheral = capability.find(level, BlockPos.ZERO, state, be, null);
            if (peripheral == null) {
                for (Direction side : Direction.values()) {
                    peripheral = capability.find(level, BlockPos.ZERO, state, be, side);
                    if (peripheral != null) break;
                }
            }
            if (peripheral == null) return null;
            String type = peripheral.getType();
            return type == null || type.isBlank() ? null : type;
        } catch (Throwable ignored) {
            // Constructor or provider that needs a live level - skip it.
            return null;
        }
    }

    /** Registry namespace of a block, i.e. the mod id that provides it. */
    private static String namespaceOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace();
    }

    /** Human-friendly display name of a mod namespace, with a sane fallback. */
    private static String displayNameOf(String namespace) {
        if (namespace.equals(CCT_NAMESPACE)) return "CC: Tweaked";
        return FabricLoader.getInstance().getModContainer(namespace)
            .map(container -> container.getMetadata().getName())
            .filter(name -> name != null && !name.isBlank())
            .orElse(namespace);
    }

    /**
     * Register a type that has never been attached before: use the default
     * tick cost, persist it to the file so the user can edit it.
     */
    private static synchronized double discoverTick(String type) {
        Double known = TICK_COSTS.get(type);
        if (known != null) return known;

        double fallback = CCPowerConfig.peripheralRFPerTick();
        TICK_COSTS.put(type, fallback);
        CALL_COSTS.putIfAbsent(type, defaultCallCost(type));
        // Namespaced type strings carry their mod id; bare strings stay unknown
        // unless the pre-scan already attributed them.
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

    /**
     * Register a type that has never been called before: use the default
     * call cost, persist it to the file so the user can edit it.
     */
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

    /** Guess the providing mod from a namespaced type string like "minecraft:chest". */
    private static String ownerFromType(String type) {
        int sep = type.indexOf(':');
        return sep > 0 ? type.substring(0, sep) : UNKNOWN_OWNER;
    }

    /** Parse the minimal TOML subset this file uses. */
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
            if (section.equals("[rf_per_tick]")) {
                tick.put(key, value);
            } else if (section.equals("[rf_per_call]")) {
                call.put(key, value);
            }
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

    /**
     * Rewrite the file from the current tables, both sections grouped into
     * one block per mod. The user's values were loaded first, so they survive.
     * CC:Tweaked's own types come first, other mods follow alphabetically, and
     * types whose provider is unknown go last.
     */
    private static void save() {
        Map<String, TreeMap<String, Double>> tickGroups = group(TICK_COSTS);
        Map<String, TreeMap<String, Double>> callGroups = group(CALL_COSTS);

        StringBuilder out = new StringBuilder();
        out.append("# CC: Power - per-peripheral-type energy costs (RF).\n");
        out.append("#\n");
        out.append("# This file is maintained automatically:\n");
        out.append("#  - at server start every installed mod is scanned for CC:Tweaked\n");
        out.append("#    peripherals and all found types are listed here, grouped by mod;\n");
        out.append("#  - types only visible in the world (generic sources like inventories)\n");
        out.append("#    are appended with defaults: the tick price from\n");
        out.append("#    power.peripheralRFPerTick and a TYPE-SPECIFIC call price\n");
        out.append("#    (well-known types start at a curated multiple of\n");
        out.append("#    power.peripheralCallRF, e.g. chests cost more than monitors);\n");
        out.append("#  - edit the values freely, the file is re-read while the server runs.\n");
        out.append("#\n");
        out.append("# Every type listed in [rf_per_tick] automatically gets a\n");
        out.append("# matching entry in [rf_per_call] (seeded with its default call\n");
        out.append("# price). To make a price free, set it to 0.0 - do not delete the\n");
        out.append("# line, the mod would only re-seed it.\n");
        out.append("#\n");
        out.append("# [rf_per_tick]: RF per tick for each ATTACHED peripheral (idle drain).\n");
        out.append("# [rf_per_call]:  RF surcharge for each peripheral method CALL.\n");
        out.append("# Types are the same strings Lua's peripheral.getType() returns.\n");
        out.append("\n");
        appendSection(out, "[rf_per_tick]", tickGroups);
        appendSection(out, "[rf_per_call]", callGroups);

        try {
            Files.writeString(FILE, out.toString(), StandardCharsets.UTF_8);
            lastMtime = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            CCPowerMod.LOG.error("Failed to write {}", FILE, e);
        }
    }

    /** Group a cost table by owning mod, ready for ordered rendering. */
    private static Map<String, TreeMap<String, Double>> group(Map<String, Double> costs) {
        Map<String, TreeMap<String, Double>> groups = new TreeMap<>();
        costs.forEach((type, cost) -> groups
            .computeIfAbsent(TYPE_OWNER.getOrDefault(type, UNKNOWN_OWNER), owner -> new TreeMap<>())
            .put(type, cost));
        return groups;
    }

    /** Render one TOML section with per-mod comment headers inside it. */
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
            if (owner.equals(UNKNOWN_OWNER)) {
                out.append("# ==============================================================\n");
                out.append("#  Unknown provider (discovered at runtime)\n");
                out.append("# ==============================================================\n");
            } else {
                out.append("# ==============================================================\n");
                out.append("#  ").append(displayNameOf(owner)).append(" (").append(owner).append(")\n");
                out.append("# ==============================================================\n");
            }
            groups.get(owner).forEach((type, cost) ->
                out.append('"').append(type).append("\" = ").append(cost).append('\n'));
        }
        out.append('\n');
    }
}
