package ru.wardreg.ccpower.platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal TOML reader/writer used by the Fabric config (and reusable by any
 * loader that needs a self-managed watched file). Only flat {@code key=value}
 * pairs with an optional {@code # comment} are supported - exactly the subset
 * this mod uses. Values are re-read when the file's mtime changes.
 */
public final class TomlStore {
    private final Path file;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, String> defaults = new LinkedHashMap<>();
    private final Map<String, String> comments = new LinkedHashMap<>();
    private long lastMtime = Long.MIN_VALUE;
    private int pollCounter;

    public TomlStore(Path file) {
        this.file = file;
    }

    public void define(String key, Object defaultValue, String comment) {
        defaults.put(key, String.valueOf(defaultValue));
        comments.put(key, comment);
    }

    /** Load or create the config file. */
    public void load() {
        try {
            if (Files.exists(file)) {
                parse(Files.readAllLines(file, StandardCharsets.UTF_8));
                // Re-seed any keys missing from an older file and rewrite once.
                boolean changed = false;
                for (Map.Entry<String, String> e : defaults.entrySet()) {
                    if (!values.containsKey(e.getKey())) {
                        values.put(e.getKey(), e.getValue());
                        changed = true;
                    }
                }
                if (changed) save();
            } else {
                values.clear();
                values.putAll(defaults);
                save();
            }
            lastMtime = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + file, e);
        }
    }

    /** Call every server tick; reloads at most twice a second. */
    public void tick() {
        if ((++pollCounter) % 40 != 0) return;
        try {
            if (!Files.exists(file)) return;
            long mtime = Files.getLastModifiedTime(file).toMillis();
            if (mtime != lastMtime) {
                parse(Files.readAllLines(file, StandardCharsets.UTF_8));
                lastMtime = mtime;
            }
        } catch (IOException ignored) {
            // Retried on the next poll.
        }
    }

    private void parse(List<String> lines) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw;
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            line = line.trim();
            if (line.isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            parsed.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        values.clear();
        values.putAll(parsed);
    }

    public double getDouble(String key) {
        try {
            return Double.parseDouble(values.getOrDefault(key, defaults.get(key)));
        } catch (RuntimeException e) {
            return Double.parseDouble(defaults.get(key));
        }
    }

    public long getLong(String key) {
        try {
            return Long.parseLong(values.getOrDefault(key, defaults.get(key)));
        } catch (RuntimeException e) {
            return Long.parseLong(defaults.get(key));
        }
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(values.getOrDefault(key, defaults.get(key)));
    }

    private void save() {
        try {
            StringBuilder out = new StringBuilder("# CC: Power - common settings (RF / Forge Energy).\n");
            out.append("# Edit while the server is running; changes are picked up automatically.\n\n");
            for (Map.Entry<String, String> e : defaults.entrySet()) {
                String key = e.getKey();
                String comment = comments.get(key);
                if (comment != null && !comment.isEmpty()) {
                    out.append("# ").append(comment.replace("\n", "\n# ")).append('\n');
                }
                out.append(key).append(" = ").append(values.getOrDefault(key, e.getValue())).append("\n\n");
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
            lastMtime = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + file, e);
        }
    }
}
