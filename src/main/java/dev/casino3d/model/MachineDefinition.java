package dev.casino3d.model;

import dev.casino3d.Language;
import java.util.*;

/** Immutable machine skin. Live machines retain this snapshot during registry reloads. */
public record MachineDefinition(
        String id,
        String game,
        Map<String, String> models,
        Map<String, ModelTransform> anchors,
        Map<String, ButtonDefinition> buttons,
        List<Part> parts,
        List<Double> settingsBounds) {
    public MachineDefinition {
        models = Map.copyOf(models);
        anchors = Map.copyOf(anchors);
        buttons = Map.copyOf(buttons);
        parts = List.copyOf(parts);
        settingsBounds = List.copyOf(settingsBounds);
        if (id == null || !id.matches("[a-z0-9_-]+")) throw new Language.Failure("error.model-id");
        if (game == null || !games().contains(game)) throw new Language.Failure("error.unknown-game", "game", game);
        models.forEach((key, value) -> {
            if (!key.matches("[a-z0-9_./-]+")) throw new Language.Failure("error.logical-model", "model", key);
            validateModel(value);
        });
        for (String anchor : anchors.keySet())
            if (!Set.of("body", "playfield").contains(anchor)) throw new Language.Failure("error.anchor", "anchor", anchor);
        if (!buttons.keySet().equals(BuiltinLayouts.buttons(game).keySet()))
            throw new Language.Failure("error.unknown-button", "action", buttons.keySet());
        if (parts.size() > 64) throw new Language.Failure("error.parts-limit");
        vector(settingsBounds, 6, "settings-bounds");
        for (int i = 0; i < 3; i++)
            if (settingsBounds.get(i) >= settingsBounds.get(i + 3)) throw new Language.Failure("error.settings-bounds");
    }

    public record Part(String model, ModelTransform transform) {
        public Part { validateModel(Objects.requireNonNull(model)); Objects.requireNonNull(transform); }
    }

    public static Set<String> games() {
        return BuiltinLayouts.GAMES;
    }

    public static MachineDefinition builtin(String game) {
        if (!games().contains(game)) throw new Language.Failure("error.unknown-game", "game", game);
        List<Double> bounds;
        if (game.equals("plinko"))
            bounds = List.of(-1.9 / .75, 0.0, -.8 / .75, 1.9 / .75, 3.8 / .75, .8 / .75);
        else if (Set.of("mines", "blackjack", "crash").contains(game)) {
            double depth = game.equals("mines") ? 2 : game.equals("blackjack") ? 1.75 : 1.1;
            bounds = List.of(-1.8, 0.0, -depth, 1.8, 3.4, depth);
        } else bounds = List.of(-1.7, 0.0, game.equals("duck_race") ? -4.85 : -1.2, 1.7, 3.3, 1.6);
        return new MachineDefinition(
                game,
                game,
                Map.of(),
                Map.of("body", ModelTransform.IDENTITY, "playfield", ModelTransform.IDENTITY),
                BuiltinLayouts.buttons(game),
                List.of(),
                bounds);
    }

    public String model(String logicalName) {
        return models.getOrDefault(logicalName, "3dcasino:" + logicalName);
    }

    public ModelTransform anchor(String name) {
        return anchors.getOrDefault(name, ModelTransform.IDENTITY);
    }

    public ButtonDefinition button(String action) {
        var button = buttons.get(action);
        if (button == null) throw new Language.Failure("error.unknown-button", "action", action);
        return button;
    }

    public static MachineDefinition parse(Map<String, Object> input, MachineDefinition base) {
        keys(
                input,
                Set.of(
                        "schema-version",
                        "id",
                        "game",
                        "models",
                        "anchors",
                        "buttons",
                        "parts",
                        "settings-bounds"),
                "root");
        if (number(input, "schema-version", 0) != 1)
            throw new Language.Failure("error.schema");
        String id = Objects.toString(input.get("id"), "");
        if (!id.matches("[a-z0-9_-]+"))
            throw new Language.Failure("error.model-id");
        if (!base.game.equals(input.get("game")))
            throw new Language.Failure("error.model-game");
        var models = new LinkedHashMap<>(base.models);
        map(input.get("models"), "models")
                .forEach(
                        (key, value) -> {
                            if (!key.matches("[a-z0-9_./-]+"))
                                throw new Language.Failure("error.logical-model", "model", key);
                            String reference = Objects.toString(value, "");
                            validateModel(reference);
                            models.put(key, reference);
                        });
        var anchors = new LinkedHashMap<>(base.anchors);
        map(input.get("anchors"), "anchors")
                .forEach(
                        (key, value) -> {
                            if (!anchors.containsKey(key))
                                throw new Language.Failure("error.anchor", "anchor", key);
                            anchors.put(
                                    key, transform(map(value, "anchors." + key), anchors.get(key)));
                        });
        var buttons = new LinkedHashMap<>(base.buttons);
        map(input.get("buttons"), "buttons")
                .forEach(
                        (action, value) -> {
                            var previous = buttons.get(action);
                            if (previous == null)
                                throw new Language.Failure("error.unknown-button", "action", action);
                            var data = map(value, "buttons." + action);
                            keys(
                                    data,
                                    Set.of(
                                            "position",
                                            "rotation",
                                            "width",
                                            "height",
                                            "depth",
                                            "size",
                                            "press"),
                                    "buttons." + action);
                            var poseData = new LinkedHashMap<String, Object>();
                            if (data.containsKey("position"))
                                poseData.put("position", data.get("position"));
                            if (data.containsKey("rotation"))
                                poseData.put("rotation", data.get("rotation"));
                            buttons.put(
                                    action,
                                    new ButtonDefinition(
                                            transform(poseData, previous.transform()),
                                            number(data, "width", previous.width()),
                                            number(data, "height", previous.height()),
                                            number(data, "depth", previous.depth()),
                                            number(data, "size", previous.size()),
                                            number(data, "press", previous.press())));
                        });
        var parts = new ArrayList<>(base.parts);
        if (input.containsKey("parts")) {
            if (!(input.get("parts") instanceof List<?> list))
                throw new Language.Failure("error.parts-list");
            if (list.size() > 64) throw new Language.Failure("error.parts-limit");
            for (Object entry : list) {
                var data = new LinkedHashMap<>(map(entry, "parts"));
                String reference = Objects.toString(data.remove("model"), "");
                validateModel(reference);
                parts.add(new Part(reference, transform(data, ModelTransform.IDENTITY)));
            }
        }
        List<Double> bounds = base.settingsBounds;
        if (input.containsKey("settings-bounds")) {
            bounds = vector(input.get("settings-bounds"), 6, "settings-bounds");
            for (int i = 0; i < 3; i++)
                if (bounds.get(i) >= bounds.get(i + 3))
                    throw new Language.Failure("error.settings-bounds");
        }
        return new MachineDefinition(id, base.game, models, anchors, buttons, parts, bounds);
    }

    private static ModelTransform transform(Map<String, Object> data, ModelTransform base) {
        keys(data, Set.of("position", "rotation", "scale"), "transform");
        var position =
                data.containsKey("position")
                        ? vector(data.get("position"), 3, "position")
                        : List.of(base.x(), base.y(), base.z());
        var rotation =
                data.containsKey("rotation")
                        ? vector(data.get("rotation"), 3, "rotation")
                        : List.of(base.pitch(), base.yaw(), base.roll());
        return new ModelTransform(
                position.get(0),
                position.get(1),
                position.get(2),
                rotation.get(0),
                rotation.get(1),
                rotation.get(2),
                number(data, "scale", base.scale()));
    }

    private static void validateModel(String value) {
        if (value.startsWith("material:")) {
            if (!value.matches("material:[A-Z0-9_]+"))
                throw new Language.Failure("error.material-case", "material", value);
            var material = org.bukkit.Material.getMaterial(value.substring(9));
            if (material == null || material.isAir() || !material.isItem())
                throw new Language.Failure("error.material-item", "material", value);
        } else if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new Language.Failure("error.model-reference", "reference", value);
        }
    }

    private static List<Double> vector(Object value, int length, String field) {
        if (!(value instanceof List<?> list) || list.size() != length)
            throw new Language.Failure("error.vector-length", "field", field, "length", length);
        var result = new ArrayList<Double>();
        for (Object entry : list) {
            if (!(entry instanceof Number n)
                    || !Double.isFinite(n.doubleValue())
                    || Math.abs(n.doubleValue()) > 360)
                throw new Language.Failure("error.vector-number", "field", field);
            result.add(n.doubleValue());
        }
        return List.copyOf(result);
    }

    private static double number(Map<String, Object> input, String field, double fallback) {
        if (!input.containsKey(field)) return fallback;
        if (!(input.get(field) instanceof Number n) || !Double.isFinite(n.doubleValue()))
            throw new Language.Failure("error.finite-number", "field", field);
        return n.doubleValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value, String field) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> map)
                || map.keySet().stream().anyMatch(k -> !(k instanceof String)))
            throw new Language.Failure("error.mapping", "field", field);
        return (Map<String, Object>) map;
    }

    private static void keys(Map<String, Object> data, Set<String> allowed, String field) {
        for (String key : data.keySet())
            if (!allowed.contains(key))
                throw new Language.Failure("error.unknown-field", "field", field, "key", key);
    }
}
