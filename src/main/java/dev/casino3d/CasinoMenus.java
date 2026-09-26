package dev.casino3d;

import static dev.casino3d.Amounts.*;
import static dev.casino3d.Language.text;

import dev.casino3d.ui.PaperMenus;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

/** Optional presentation only; services and scheduled settlement live in CasinoRuntime. */
final class CasinoMenus {
    record Session(
            UUID token,
            long expires,
            Map<String, Consumer<Map<String, String>>> actions,
            boolean crash) {}

    private final CasinoPlugin plugin;
    private final RoundRecoveryMenu recovery;
    private final Map<UUID, Session> sessions = new HashMap<>();

    CasinoMenus(CasinoPlugin plugin, CasinoRuntime runtime) {
        this.plugin = plugin;
        recovery = new RoundRecoveryMenu(this, runtime.games());
    }

    void close() {
        sessions.clear();
    }

    void forget(UUID player) {
        sessions.remove(player);
    }

    void roundUpdated(CasinoRound round) {
        if (!plugin.menusEnabled()) return;
        var session = sessions.get(round.player);
        var player = Bukkit.getPlayer(round.player);
        if (session == null
                || !session.crash
                || session.expires < System.currentTimeMillis()
                || player == null
                || !plugin.allowed(player)) return;
        player.sendActionBar(
                Component.text(
                        round.finished()
                                ? text("menu.crash.finished", "amount", money(round.payout))
                                : text("menu.crash.running", "multiplier", money(CasinoRules.crashMultiplier(
                                        round.started, System.currentTimeMillis())),
                                        "target", money(round.parameter))));
        if (round.finished())
            sessions.put(
                    round.player,
                    new Session(session.token, session.expires, session.actions, false));
    }

    private void handle(Player p, String action, Map<String, String> values) {
        if (!plugin.menusEnabled()) {
            forget(p.getUniqueId());
            p.closeDialog();
            return;
        }
        String[] parts = action.split(" ");
        if (parts.length != 2) return;
        var s = sessions.get(p.getUniqueId());
        if (s == null
                || !parts[0].equals("3dcasino:" + s.token)
                || s.expires < System.currentTimeMillis()
                || !s.actions.containsKey(parts[1])) return;
        sessions.remove(p.getUniqueId());
        if (!plugin.allowed(p)) {
            p.closeDialog();
            return;
        }
        try {
            s.actions.get(parts[1]).accept(values);
        } catch (Exception ex) {
            p.sendMessage("§c" + Language.error(ex));
            plugin.getLogger()
                    .log(java.util.logging.Level.WARNING, text("menu.log.action-paused", "player", p.getUniqueId()), ex);
            open(p);
        }
    }

    public void machineSettings(
            Player p,
            String name,
            java.util.function.LongSupplier stake,
            java.util.function.LongConsumer setStake,
            java.util.function.BooleanSupplier canEdit,
            java.util.function.BooleanSupplier exists,
            Runnable remove) {
        if (!plugin.allowed(p) || !plugin.machineAllowed(p) || !exists.getAsBoolean()) {
            p.closeDialog();
            return;
        }
        var page =
                new Page(
                        p,
                        text("menu.machine-settings.title", "name", name),
                        text("menu.machine-settings.body", "amount", money(stake.getAsLong()),
                                "status", text(canEdit.getAsBoolean()
                                        ? "menu.machine-settings.editable"
                                        : "menu.machine-settings.busy")));
        if (canEdit.getAsBoolean()) {
            page.input("stake", text("menu.machine-settings.stake-label"), Long.toString(stake.getAsLong() / 100));
            page.button(
                    "save",
                    text("menu.machine-settings.save"),
                    v -> {
                        if (!machineValid(p, exists)) return;
                        if (!canEdit.getAsBoolean()) {
                            p.sendMessage(text("menu.machine-settings.wait"));
                            p.closeDialog();
                            return;
                        }
                        try {
                            setStake.accept(parse(v.get("stake"), 100) * 100L);
                            p.sendMessage(text("menu.machine-settings.saved"));
                        } catch (IllegalArgumentException ex) {
                            p.sendMessage(text("menu.machine-settings.invalid-stake"));
                        }
                        machineSettings(p, name, stake, setStake, canEdit, exists, remove);
                    });
        }
        page.button(
                "delete",
                text("menu.machine-settings.delete"),
                v -> {
                    if (machineValid(p, exists)) {
                        remove.run();
                        p.closeDialog();
                        p.sendMessage(text("menu.machine-settings.deleted"));
                    }
                });
        page.show();
    }

    private boolean machineValid(Player p, java.util.function.BooleanSupplier exists) {
        if (plugin.allowed(p) && plugin.machineAllowed(p) && exists.getAsBoolean()) return true;
        p.closeDialog();
        return false;
    }

    final class Page {
        final Player player;
        final UUID token = UUID.randomUUID();
        final YamlConfiguration config = new YamlConfiguration();
        final Map<String, Consumer<Map<String, String>>> actions = new LinkedHashMap<>();
        boolean crash;

        Page(Player player, String title, String body) {
            this.player = player;
            config.set("Title", title);
            config.set("Body.content.type", "message");
            config.set("Body.content.width", 380);
            config.set("Body.content.text", body);
        }

        void artwork(List<String> lines) {
            var content = config.getConfigurationSection("Body.content").getValues(false);
            config.set("Body", null);
            config.set("Body.visual.type", "message");
            config.set("Body.visual.width", 260);
            config.set("Body.visual.text", lines);
            config.set("Body.content", content);
        }

        void button(String key, String text, Consumer<Map<String, String>> action) {
            config.set("Bottom.buttons." + key + ".text", text);
            config.set(
                    "Bottom.buttons." + key + ".actions", List.of("3dcasino:" + token + " " + key));
            actions.put(key, action);
        }

        void input(String key, String label, String value) {
            config.set("Inputs." + key + ".type", "input");
            config.set("Inputs." + key + ".text", label);
            config.set("Inputs." + key + ".default", value);
            config.set("Inputs." + key + ".max_length", 6);
        }

        void show() {
            if (!plugin.menusEnabled()) {
                forget(player.getUniqueId());
                player.sendMessage(
                        text("menu.disabled"));
                return;
            }
            if (!plugin.allowed(player)) return;
            config.set("Settings.can_escape", true);
            config.set("Settings.after_action", "NONE");
            config.set("Settings.lifetime", "300s");
            config.set("Bottom.type", "multi");
            config.set("Bottom.columns", 2);
            config.set("Bottom.exit.text", text("menu.close"));
            config.set("Bottom.exit.actions", List.of("3dcasino:" + token + " close"));
            actions.put(
                    "close",
                    v -> {
                        forget(player.getUniqueId());
                        player.closeDialog();
                    });
            sessions.put(
                    player.getUniqueId(),
                    new Session(token, System.currentTimeMillis() + 300000, actions, crash));
            PaperMenus.open(
                    plugin, player, config, (action, values) -> handle(player, action, values));
        }
    }

    void open(Player p) {
        var page = new Page(p, text("menu.home.title"), text("menu.home.body"));
        if (plugin.machineAllowed(p)) page.button("machines", text("menu.machines.title"), v -> machines(p));
        if (recovery.pending(p)) page.button("resume", text("menu.home.resume"), v -> recovery.open(p));
        page.show();
    }

    private void machines(Player p) {
        if (!plugin.machineAllowed(p)) {
            p.closeDialog();
            return;
        }
        var page = new Page(p, text("menu.machines.title"), text("menu.machines.body"));
        for (var entry : MachineCatalog.ENTRIES)
            page.button(
                    entry.id(),
                    entry.label(),
                    v -> {
                        forget(p.getUniqueId());
                        p.closeDialog();
                        plugin.machineCommand(p, new String[] {"create", entry.id()});
                    });
        page.button(
                "remove",
                text("menu.machines.remove-all"),
                v -> {
                    forget(p.getUniqueId());
                    p.closeDialog();
                    plugin.machineCommand(p, new String[] {"remove"});
                });
        page.button("back", text("menu.back-colored"), v -> open(p));
        page.show();
    }
}
