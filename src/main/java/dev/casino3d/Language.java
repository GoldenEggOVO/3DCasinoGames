package dev.casino3d;

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
            values.putAll(strings(yaml));
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
    }

    public static void reset() {
        current = new Language(ENGLISH);
    }

    public static String text(String key, Object... pairs) {
        return current.message(key, pairs);
    }

    public String message(String key, Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Expected named placeholder pairs");
        String template = messages.getOrDefault(key, key);
        var parameters = new LinkedHashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) parameters.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
        var matcher = java.util.regex.Pattern.compile("\\{([A-Za-z][A-Za-z0-9_-]*)}").matcher(template);
        return matcher.replaceAll(match -> java.util.regex.Matcher.quoteReplacement(
                parameters.getOrDefault(match.group(1), match.group())));
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
}
