package dev.casino3d;

import static dev.casino3d.Amounts.*;
import static dev.casino3d.Language.component;
import static dev.casino3d.Language.text;

import dev.casino3d.ui.PaperMenus;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import dev.casino3d.ui.MenuView;
import dev.casino3d.ui.MenuSessions;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

/** Optional presentation only; services and scheduled settlement live in CasinoRuntime. */
final class CasinoMenus {
    record Session(
            Map<String, Consumer<Map<String, String>>> actions,
            boolean crash) {}

    private final CasinoPlugin plugin;
    private final RoundRecoveryMenu recovery;
    private final MenuSessions<Session> sessions = new MenuSessions<>(PaperMenus.LIFETIME, System::nanoTime);
    private final BukkitTask cleanup;

    CasinoMenus(CasinoPlugin plugin, CasinoRuntime runtime) {
        this.plugin = plugin;
        recovery = new RoundRecoveryMenu(this, runtime.games());
        cleanup = plugin.getServer().getScheduler().runTaskTimer(plugin, sessions::expire, 1200, 1200);
    }

    void close() {
        cleanup.cancel();
        invalidate();
    }

    void invalidate() {
        for (UUID id : sessions.players()) {
            var player = Bukkit.getPlayer(id);
            if (player != null) player.closeDialog();
        }
        sessions.clear();
    }

    void forget(UUID player) {
        sessions.remove(player);
    }

    void roundUpdated(CasinoRound round) {
        if (!plugin.menusEnabled()) return;
        var entry = sessions.get(round.player);
        var player = Bukkit.getPlayer(round.player);
        if (entry == null || !entry.value().crash || player == null || !plugin.allowed(player)) return;
        player.sendActionBar(round.finished()
                ? component("menu.crash.finished", "amount", money(round.payout))
                : component("menu.crash.running", "multiplier", money(CasinoRules.crashMultiplier(
                        round.started, System.currentTimeMillis())), "target", money(round.parameter)));
    }

    private void handle(Player p, String action, Map<String, String> values) {
        if (!plugin.menusEnabled()) {
            forget(p.getUniqueId());
            p.closeDialog();
            return;
        }
        String[] parts = action.split(" ");
        if (parts.length != 2) return;
        var entry = sessions.get(p.getUniqueId());
        if (entry == null || !parts[0].equals("3dcasino:" + entry.token())
                || !entry.value().actions.containsKey(parts[1])) return;
        var s = sessions.consume(p.getUniqueId(), entry.token());
        if (s == null) return;
        if (!plugin.allowed(p)) {
            p.closeDialog();
            return;
        }
        try {
            s.actions.get(parts[1]).accept(values);
        } catch (Exception ex) {
            p.sendMessage(Component.text(Language.error(ex), net.kyori.adventure.text.format.NamedTextColor.RED));
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
                        component("menu.machine-settings.title", "name", name),
                        component("menu.machine-settings.body", "amount", money(stake.getAsLong()),
                                "status", component(canEdit.getAsBoolean()
                                        ? "menu.machine-settings.editable"
                                        : "menu.machine-settings.busy")));
        if (canEdit.getAsBoolean()) {
            page.input("stake", component("menu.machine-settings.stake-label"), Long.toString(stake.getAsLong() / 100));
            page.button(
                    "save",
                    component("menu.machine-settings.save"),
                    v -> {
                        if (!machineValid(p, exists)) return;
                        if (!canEdit.getAsBoolean()) {
                            p.sendMessage(component("menu.machine-settings.wait"));
                            p.closeDialog();
                            return;
                        }
                        try {
                            setStake.accept(parse(v.get("stake"), 100) * 100L);
                            p.sendMessage(component("menu.machine-settings.saved"));
                        } catch (IllegalArgumentException ex) {
                            p.sendMessage(component("menu.machine-settings.invalid-stake"));
                        }
                        machineSettings(p, name, stake, setStake, canEdit, exists, remove);
                    });
        }
        page.button(
                "delete",
                component("menu.machine-settings.delete"),
                v -> {
                    if (machineValid(p, exists)) {
                        remove.run();
                        p.closeDialog();
                        p.sendMessage(component("menu.machine-settings.deleted"));
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
        final Component title;
        final List<MenuView.Body> body = new ArrayList<>();
        final List<MenuView.Button> buttons = new ArrayList<>();
        final List<MenuView.Input> inputs = new ArrayList<>();
        final Map<String, Consumer<Map<String, String>>> actions = new LinkedHashMap<>();
        boolean crash;

        Page(Player player, Component title, Component content) {
            this.player = player;
            this.title = title;
            body.add(new MenuView.Body(content, 380));
        }

        void artwork(List<String> lines) {
            if (!lines.isEmpty()) body.addFirst(new MenuView.Body(Component.text(String.join("\n", lines)), 260));
        }

        void button(String key, Component label, Consumer<Map<String, String>> action) {
            if (actions.putIfAbsent(key, action) != null) throw new IllegalArgumentException("Duplicate action: " + key);
            buttons.add(new MenuView.Button(label, null, 150, "3dcasino:" + token + " " + key));
        }

        void input(String key, Component label, String value) {
            inputs.add(new MenuView.Input(key, label, value, 6));
        }

        void show() {
            if (!plugin.menusEnabled()) {
                forget(player.getUniqueId());
                player.sendMessage(component("menu.disabled"));
                return;
            }
            if (!plugin.allowed(player)) return;
            actions.put("close", v -> {
                forget(player.getUniqueId());
                player.closeDialog();
            });
            var exit = new MenuView.Button(component("menu.close"), null, 230, "3dcasino:" + token + " close");
            var view = new MenuView(title, body, inputs, buttons, exit, 2);
            sessions.put(player.getUniqueId(), token, new Session(Map.copyOf(actions), crash));
            PaperMenus.open(plugin, player, view, (action, values) -> handle(player, action, values));
        }
    }

    void open(Player p) {
        var page = new Page(p, component("menu.home.title"), component("menu.home.body"));
        if (plugin.machineAllowed(p)) page.button("machines", component("menu.machines.title"), v -> machines(p));
        if (recovery.pending(p)) page.button("resume", component("menu.home.resume"), v -> recovery.open(p));
        page.show();
    }

    private void machines(Player p) {
        if (!plugin.machineAllowed(p)) {
            p.closeDialog();
            return;
        }
        var page = new Page(p, component("menu.machines.title"), component("menu.machines.body"));
        for (var entry : MachineCatalog.ENTRIES)
            page.button(
                    entry.id(),
                    component(entry.labelKey()),
                    v -> {
                        forget(p.getUniqueId());
                        p.closeDialog();
                        plugin.machineCommand(p, new String[] {"create", entry.id()});
                    });
        page.button(
                "remove",
                component("menu.machines.remove-all"),
                v -> {
                    forget(p.getUniqueId());
                    p.closeDialog();
                    plugin.machineCommand(p, new String[] {"remove"});
                });
        page.button("back", component("menu.back-colored"), v -> open(p));
        page.show();
    }
}
