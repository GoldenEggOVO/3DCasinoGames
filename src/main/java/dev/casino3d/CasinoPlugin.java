package dev.casino3d;

import static dev.casino3d.Language.text;
import dev.casino3d.api.EconomyProvider;
import dev.casino3d.economy.EconomyAccess;
import dev.casino3d.integration.AuthMeAccess;
import dev.casino3d.integration.CasinoPermissions;
import dev.casino3d.integration.VaultEconomyProvider;
import dev.casino3d.machine.MachineManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class CasinoPlugin extends JavaPlugin implements Listener {
    private CasinoRuntime runtime;
    private CasinoMenus menus;
    private EconomyAccess economy;
    private MachineManager machines;
    private MachineSettingsTargets machineSettings;
    private boolean vanillaAppearance;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            Language.use(Language.load(getDataFolder().toPath().resolve("languages"),
                    getConfig().getString("language", "en_US"), getLogger()::warning));
            String appearance = getConfig().getString("machine-appearance", "vanilla");
            if (!Set.of("resource-pack", "vanilla").contains(appearance))
                throw new IllegalArgumentException("machine-appearance must be resource-pack or vanilla");
            vanillaAppearance = appearance.equals("vanilla");
            EconomyProvider vault = VaultEconomyProvider.discover();
            if (vault != null) {
                getServer()
                        .getServicesManager()
                        .register(EconomyProvider.class, vault, this, ServicePriority.Lowest);
            }
            economy =
                    new EconomyAccess(
                            () -> getServer().getServicesManager().load(EconomyProvider.class));
            runtime = new CasinoRuntime(this, economy);
            if (menusEnabled()) {
                menus = new CasinoMenus(this, runtime);
                runtime.onRoundUpdated(menus::roundUpdated);
            }
            machineSettings = new MachineSettingsTargets(this);
            machines = new MachineManager(this);
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info(text("plugin.enabled"));
        } catch (Exception | LinkageError ex) {
            getLogger().log(Level.SEVERE, text("plugin.failed"), ex);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (machines != null) machines.close();
        if (machineSettings != null) machineSettings.close();
        if (menus != null) menus.close();
        if (runtime != null) runtime.close();
    }

    void machineCommand(Player player, String[] args) {
        machines.command(player, args);
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        if (menus != null) menus.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void commandSuggestions(PlayerCommandSendEvent event) {
        CommandCompletion.hideNamespacedRoot(event.getCommands());
    }

    public boolean allowed(Player player) {
        return player.isOnline()
                && !player.isDead()
                && CasinoPermissions.allowed(player, "use")
                && AuthMeAccess.authenticated(player);
    }

    public boolean machineAllowed(Player player) {
        return allowed(player) && CasinoPermissions.allowed(player, "machine");
    }

    public boolean menusEnabled() {
        return getConfig().getBoolean("menu-enabled", true);
    }

    public boolean vanillaAppearance() {
        return vanillaAppearance;
    }

    public MachineSettingsTargets machineSettings() {
        return machineSettings;
    }

    public void openMachineSettings(
            Player player,
            String game,
            java.util.function.LongSupplier stake,
            java.util.function.LongConsumer setStake,
            java.util.function.BooleanSupplier canEdit,
            java.util.function.BooleanSupplier exists,
            Runnable remove) {
        if (menus == null || !menusEnabled()) {
            menuHelp(player);
            return;
        }
        menus.machineSettings(player, game, stake, setStake, canEdit, exists, remove);
    }

    private void menuHelp(Player player) {
        player.sendMessage(
                text("menu.disabled"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0
                        && Set.of("create", "remove", "bet", "reload-models")
                                .contains(args[0].toLowerCase(java.util.Locale.ROOT))) {
            if (sender instanceof Player player) machineCommand(player, args);
            else sender.sendMessage(text("command.player-only"));
            return true;
        }
        boolean mines = args.length > 0 && args[0].equalsIgnoreCase("mines");
        if (mines) args = java.util.Arrays.copyOfRange(args, 1, args.length);
        if (args.length > 0 && args[0].equalsIgnoreCase("resolve")) {
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(text("command.console-only"));
                return true;
            }
            if (args.length != 4 || !Set.of("applied", "not-applied").contains(args[3])) {
                sender.sendMessage(text("command.resolve-usage", "command", mines ? "3dcasino mines" : "3dcasino"));
                return true;
            }
            UUID playerId;
            UUID roundId;
            try {
                playerId = UUID.fromString(args[1]);
                roundId = UUID.fromString(args[2]);
            } catch (IllegalArgumentException invalidId) {
                sender.sendMessage(text("command.resolve-usage", "command", mines ? "3dcasino mines" : "3dcasino"));
                return true;
            }
            try {
                runtime.resolve(
                        mines,
                        playerId,
                        roundId,
                        args[3].equals("applied"));
                getLogger()
                        .warning(text("command.resolve-audit", "command", command.getName(), "arguments", String.join(" ", args)));
                sender.sendMessage(text("command.resolved"));
            } catch (Exception ex) {
                sender.sendMessage(text("command.resolve-failed", "error", Language.error(ex)));
                if (ex.getMessage() == null || !ex.getMessage().startsWith("error."))
                    getLogger().log(Level.WARNING, "Settlement reconciliation failed", ex);
            }
            return true;
        }
        if (sender instanceof Player player) {
            if (!allowed(player)) {
                player.sendMessage(text("command.denied"));
            } else if (mines) {
                player.sendMessage(text("command.mines"));
            } else {
                if (menus != null && menusEnabled()) menus.open(player);
                else menuHelp(player);
            }
        } else {
            sender.sendMessage(text("command.console-help"));
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            if (!allowed(player)) return java.util.List.of();
            return machineAllowed(player) ? machines.complete(player, args) : CommandCompletion.menu(args);
        }
        return sender instanceof ConsoleCommandSender
                ? CommandCompletion.console(args) : java.util.List.of();
    }
}
