package dev.casino3d;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.casino3d.game.dragon_tower.DragonTowerRound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class LanguageTest {
    @TempDir Path folder;

    @AfterEach void restoreEnglish() { Language.reset(); }

    @Test void freshInstallCopiesBothLanguagesAndUsesEnglish() throws Exception {
        var language = Language.load(folder, "en_US", message -> fail(message));
        assertTrue(Files.exists(folder.resolve("en_US.yml")));
        assertTrue(Files.exists(folder.resolve("zh_CN.yml")));
        assertEquals("PLAY", language.message("models.cabinet_button_play.0"));
        assertEquals("FREE PLAY", Language.text("round.playing"));
    }

    @Test void editsSurviveAndMissingOrNonStringKeysFallBackToEnglish() throws Exception {
        Language.load(folder, "en_US", message -> fail(message));
        Files.writeString(folder.resolve("en_US.yml"), "menu.close: 'CUSTOM CLOSE'\n");
        Files.writeString(folder.resolve("zh_CN.yml"), "models.cabinet_button_play.0: '自定义开始'\nmenu.close: 42\n");
        String before = Files.readString(folder.resolve("zh_CN.yml"));
        var language = Language.load(folder, "zh_CN", message -> fail(message));
        assertEquals("自定义开始", language.message("models.cabinet_button_play.0"));
        assertEquals("CUSTOM CLOSE", language.message("menu.close"));
        assertEquals("FREE PLAY", language.message("round.playing"));
        assertEquals(before, Files.readString(folder.resolve("zh_CN.yml")));
    }

    @Test void malformedYamlIsPreservedAndFallsBackPredictably() throws Exception {
        Language.load(folder, "en_US", message -> fail(message));
        String broken = "round.playing: [unfinished\n";
        Files.writeString(folder.resolve("zh_CN.yml"), broken);
        var warnings = new ArrayList<String>();
        var language = Language.load(folder, "zh_CN", warnings::add);
        assertEquals("FREE PLAY", language.message("round.playing"));
        assertEquals(1, warnings.size());
        assertEquals(broken, Files.readString(folder.resolve("zh_CN.yml")));
        Files.writeString(folder.resolve("en_US.yml"), broken);
        assertEquals("FREE PLAY", Language.load(folder, "en_US", warnings::add).message("round.playing"));
        assertEquals(broken, Files.readString(folder.resolve("en_US.yml")));
    }

    @Test void unsafeAndMissingLanguageNamesUseEnglish() throws Exception {
        var warnings = new ArrayList<String>();
        assertEquals("FREE PLAY", Language.load(folder, "../other", warnings::add).message("round.playing"));
        assertEquals("FREE PLAY", Language.load(folder, "missing", warnings::add).message("round.playing"));
        assertEquals(2, warnings.size());
        try (var files = Files.list(folder)) {
            assertEquals(Set.of("en_US.yml", "zh_CN.yml"), files.map(p -> p.getFileName().toString()).collect(Collectors.toSet()));
        }
    }

    @Test void placeholdersAreNamedAndValuesAreNotRecursivelyInterpolated() throws Exception {
        var language = Language.load(folder, "en_US", message -> fail(message));
        assertEquals("{amount} / 500 PRACTICE POINTS", language.message("round.result", "message", "{amount}", "amount", 500));
        assertThrows(IllegalArgumentException.class, () -> language.message("round.result", "amount"));
    }

    @Test void allBundledKeysAndPlaceholdersMatchAndEveryModelLabelIsEditable() throws Exception {
        var english = bundled("en_US");
        var chinese = bundled("zh_CN");
        assertEquals(english.getKeys(true), chinese.getKeys(true));
        for (var entry : english.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof String value)) continue;
            assertFalse(Pattern.compile("\\p{IsHan}").matcher(value).find(), entry.getKey());
            assertEquals(placeholders(value), placeholders(chinese.getString(entry.getKey())), entry.getKey());
        }
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/vanilla-models.json"), StandardCharsets.UTF_8)) {
            JsonObject models = new Gson().fromJson(reader, JsonObject.class);
            for (var model : models.entrySet()) {
                var labels = model.getValue().getAsJsonObject().getAsJsonArray("labels");
                for (int index = 0; index < labels.size(); index++) {
                    String key = "models." + model.getKey() + "." + index;
                    assertEquals(labels.get(index).getAsJsonObject().get("text").getAsString(), english.getString(key), key);
                    assertTrue(chinese.isString(key), key);
                }
            }
        }
    }

    @Test void editedTranslationsCannotChangeRandomOutcomesOrActionAvailability() throws Exception {
        var english = new DragonTowerRound(new Random(17));
        Language.load(folder, "en_US", message -> fail(message));
        Files.writeString(folder.resolve("zh_CN.yml"), "dragon.choose: 'cash'\nround.playing: 'play'\nround.result: 'ignored'\n");
        Language.use(Language.load(folder, "zh_CN", message -> fail(message)));
        var translated = new DragonTowerRound(new Random(17));
        for (String action : new String[] {"play", "select:0", "select:3", "select:1", "cash", "play", "select:2"}) {
            assertEquals(english.available(action), translated.available(action));
            Language.reset();
            english.action(action);
            Language.use(Language.load(folder, "zh_CN", message -> fail(message)));
            translated.action(action);
            assertEquals(english.active(), translated.active());
            assertEquals(english.finished(), translated.finished());
            assertEquals(english.payout(), translated.payout());
            assertEquals(english.floors(), translated.floors());
            assertEquals(english.traps(), translated.traps());
        }
    }

    @Test void domainErrorsTranslateAtThePresentationBoundary() throws Exception {
        var error = assertThrows(IllegalArgumentException.class, () -> Amounts.parse("abc", 100));
        assertEquals("error.integer", error.getMessage());
        assertEquals("Enter an integer within the allowed range.", Language.error(error));
        Language.use(Language.load(folder, "zh_CN", message -> fail(message)));
        assertEquals("请输入范围内的整数", Language.error(error));
        assertEquals(Language.text("error.unexpected"), Language.error(new RuntimeException("private detail")));
    }

    @Test void modelValidationPreservesDetailsAcrossLanguagesAndWrappedErrors() throws Exception {
        var error = assertThrows(Language.Failure.class,
                () -> new dev.casino3d.model.MachineRegistry().get("mines", "custom_skin"));
        assertEquals("Unknown machine skin custom_skin for mines", Language.error(error));
        Language.use(Language.load(folder, "zh_CN", message -> fail(message)));
        assertEquals("游戏 mines 的机器皮肤 custom_skin 不存在", Language.error(error));
        assertEquals(Language.error(error), Language.error(new java.io.IOException("custom.yml", error)));
        assertEquals("Unknown machine skin custom_skin for mines", error.getMessage());
    }

    @Test void everyLiteralMessageKeyInSourceExistsInBothLanguages() throws Exception {
        var english = bundled("en_US");
        var chinese = bundled("zh_CN");
        var keys = Pattern.compile("(?:\\btext\\(\\s*\"|\"(?=error\\.))([a-z][a-z0-9_.-]+)\"");
        try (var source = Files.walk(Path.of("src/main/java"))) {
            for (Path file : source.filter(path -> path.toString().endsWith(".java")).toList()) {
                var matches = keys.matcher(Files.readString(file));
                while (matches.find()) {
                    String key = matches.group(1);
                    if (key.endsWith(".")) continue; // Dynamic game labels are covered by the catalog below.
                    assertTrue(english.isString(key), file + ": " + key);
                    assertTrue(chinese.isString(key), file + ": " + key);
                }
            }
        }
        for (var game : MachineCatalog.ENTRIES) {
            assertTrue(english.isString(game.labelKey()));
            assertTrue(chinese.isString(game.labelKey()));
        }
    }

    private static YamlConfiguration bundled(String language) throws Exception {
        try (var reader = new InputStreamReader(LanguageTest.class.getResourceAsStream("/languages/" + language + ".yml"), StandardCharsets.UTF_8)) {
            var yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        }
    }

    private static Set<String> placeholders(String value) {
        return Pattern.compile("\\{([A-Za-z][A-Za-z0-9_-]*)}").matcher(value).results().map(match -> match.group(1)).collect(Collectors.toSet());
    }
}
