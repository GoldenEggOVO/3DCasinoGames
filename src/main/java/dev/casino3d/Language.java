package dev.casino3d;

import dev.casino3d.ui.MessageText;
import net.kyori.adventure.text.Component;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Editable server language, with immutable bundled English as the final fallback. */
public final class Language {
    private static final Map<String, String> ENGLISH = bundled("en_US");
    private static volatile Language current = new Language(ENGLISH);
    private static volatile long revision;
    private final Map<String, String> messages;

    /** Validation details retain a stable key and values until the UI chooses a language. */
    public static final class Failure extends IllegalArgumentException {
        private final String key;
        private final Object[] values;

        public Failure(String key, Object... values) {
            super(new Language(ENGLISH).message(key, values));
            this.key = key;
            this.values = values.clone();
        }
    }

    private Language(Map<String, String> messages) {
        this.messages = Map.copyOf(messages);
    }

    public static Language load(Path folder, String locale, Consumer<String> warning) throws IOException {
        try {
            Files.createDirectories(folder);
            for (String builtIn : new String[] {"en_US", "zh_CN"}) {
                Path file = folder.resolve(builtIn + ".yml");
                if (!Files.exists(file)) {
                    try (var stream = Language.class.getResourceAsStream("/languages/" + builtIn + ".yml")) {
                        if (stream == null) throw new IOException("Missing bundled language: " + builtIn);
                        Files.copy(stream, file);
                    }
                }
            }
        } catch (IOException ex) {
            warning.accept("Cannot initialize languages at " + folder + ": " + ex.getMessage());
            return new Language(ENGLISH);
        }
        var values = new LinkedHashMap<>(ENGLISH);
        readFile(folder.resolve("en_US.yml"), values, warning);
        if (locale == null || !locale.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")) {
            warning.accept("Invalid language filename; using en_US.");
            locale = "en_US";
        }
        if (!locale.equals("en_US")) readFile(folder.resolve(locale + ".yml"), values, warning);
        return new Language(values);
    }

    private static void readFile(Path file, Map<String, String> values, Consumer<String> warning) {
        try {
            var yaml = new YamlConfiguration();
            yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
            for (var entry : yaml.getValues(true).entrySet()) {
                String key = entry.getKey();
                if (entry.getValue() instanceof org.bukkit.configuration.ConfigurationSection) continue;
                try {
                    if (!ENGLISH.containsKey(key)) throw new IllegalArgumentException("Unknown message key");
                    if (!(entry.getValue() instanceof String value))
                        throw new IllegalArgumentException("Expected a string");
                    if (!MessageText.placeholders(ENGLISH.get(key)).containsAll(MessageText.placeholders(value)))
                        throw new IllegalArgumentException("Unknown placeholder; expected " + MessageText.placeholders(ENGLISH.get(key)));
                    MessageText.validate(value);
                    values.put(key, value);
                } catch (IllegalArgumentException ex) {
                    warning.accept(file.getFileName() + " [" + key + "]: " + ex.getMessage());
                }
            }
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException ex) {
            warning.accept("Cannot load language " + file.getFileName() + "; using English fallback: " + ex.getMessage());
        }
    }

    private static Map<String, String> bundled(String name) {
        try (var stream = Language.class.getResourceAsStream("/languages/" + name + ".yml")) {
            if (stream == null) throw new IOException("Missing language: " + name);
            var yaml = new YamlConfiguration();
            yaml.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return Map.copyOf(strings(yaml));
        } catch (IOException | InvalidConfigurationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static Map<String, String> strings(YamlConfiguration yaml) {
        var values = new LinkedHashMap<String, String>();
        yaml.getValues(true).forEach((key, value) -> {
            if (value instanceof String string) values.put(key, string);
        });
        return values;
    }

    public static void use(Language language) {
        current = java.util.Objects.requireNonNull(language);
        revision++;
    }

    public static void reset() {
        use(new Language(ENGLISH));
    }

    public static long revision() { return revision; }

    public static boolean reload(Path folder, String locale, Consumer<String> warning) {
        var problems = new java.util.ArrayList<String>();
        Language candidate;
        try {
            candidate = load(folder, locale, problems::add);
        } catch (IOException ex) {
            problems.add("Cannot load languages at " + folder + ": " + ex.getMessage());
            candidate = null;
        }
        problems.forEach(warning);
        if (!problems.isEmpty()) return false;
        use(candidate);
        return true;
    }

    public static Component component(String key, Object... pairs) {
        return current.render(key, pairs);
    }

    public Component render(String key, Object... pairs) {
        return MessageText.render(messages.getOrDefault(key, key), pairs);
    }

    public static String text(String key, Object... pairs) {
        return current.message(key, pairs);
    }

    public String message(String key, Object... pairs) {
        return MessageText.plain(render(key, pairs));
    }

    /** Services report stable message keys; presentation translates them at the boundary. */
    public static String error(Throwable error) {
        if (error instanceof Failure failure) return text(failure.key, failure.values);
        String key = error.getMessage();
        if (key != null && current.messages.containsKey(key)) return text(key);
        if (error.getCause() != null && error.getCause() != error) return error(error.getCause());
        return text("error.unexpected");
    }

    public static String modelLabel(String model, int index, String fallback) {
        return current.messages.getOrDefault("models." + model + "." + index, fallback);
    }

    public static Component modelComponent(String model, int index, String fallback) {
        return MessageText.render(modelLabel(model, index, fallback));
    }
}
