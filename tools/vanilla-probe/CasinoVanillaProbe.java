package dev.server.casino.probe;

import dev.server.casino.CasinoPlugin;
import dev.server.casino.game.PracticeRound;
import dev.server.casino.machine.MachineManager;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/** Disposable server-side check; it cannot confirm what a real client renders. */
public final class CasinoVanillaProbe extends JavaPlugin {
    private static final UUID OWNER = UUID.fromString("c96ef2ec-31cb-4b54-976f-550525d2c11a");
    private static final List<String> GAMES = List.of("mines", "blackjack", "crash", "plinko",
            "slots", "duck_race", "wheel_of_fortune", "money_wheel", "penguin_cross",
            "keno", "hilo", "dragon_tower");
    private final AtomicReference<Location> eye = new AtomicReference<>();

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, this::check, 100);
    }

    private void check() {
        try {
            var casino = (CasinoPlugin) Bukkit.getPluginManager().getPlugin("ServerCasino");
            require(casino != null && casino.isEnabled(), "Casino disabled");
            require(casino.vanillaAppearance(), "Vanilla appearance not enabled");
            for (String absent : List.of("CraftEngine", "ServerGames", "ServerBoards", "ServerMenu", "KaMenu"))
                require(Bukkit.getPluginManager().getPlugin(absent) == null, absent + " was installed");
            var manager = (MachineManager) field(casino, "machines");
            Player player = player();
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    Bukkit.getWorlds().getFirst().getChunkAt(x, z).setForceLoaded(true);
            var marker = casino.getDataFolder().toPath().resolve("vanilla-probe-phase.txt");
            if (!Files.exists(marker)) {
                for (String game : GAMES) {
                    manager.command(player, new String[] {"create", game});
                    var machines = (Map<?, ?>) field(manager, "machines");
                    require(machines.size() == 1, "Create failed: " + game);
                    Object machine = machines.values().iterator().next();
                    checkVisuals(machine, game);
                    if (game.equals("keno")) {
                        var tiles = (List<?>) field(machine, "tiles");
                        click(manager, player, (ItemDisplay) tiles.getFirst(), game);
                        Thread.sleep(170);
                    }
                    var buttons = (Map<?, ?>) field(machine, "buttonActions");
                    var play = buttons.entrySet().stream().filter(entry ->
                            List.of("start", "play").contains(entry.getValue())).findFirst()
                            .orElseThrow(() -> new AssertionError("Missing PLAY button: " + game));
                    var round = (PracticeRound) field(machine, "round");
                    String before = round.result();
                    click(manager, player, (ItemDisplay) play.getKey(), game);
                    require(!round.result().equals(before) || round.active() || round.finished()
                            || game.equals("plinko") && !((Map<?, ?>) field(machine, "balls")).isEmpty(),
                            "PLAY did not advance game: " + game);
                    checkVisuals(machine, game);
                    manager.command(player, new String[] {"remove", game});
                    require(machines.isEmpty(), "Remove failed: " + game);
                    getLogger().info("CASINO_VANILLA_MACHINE_PASS game=" + game);
                }
                manager.command(player, new String[] {"create", "slots"});
                Files.writeString(marker, "1");
                getLogger().info("CASINO_VANILLA_FIRST_PASS games=12 pack=false");
            } else if (Files.readString(marker).equals("1")) {
                var machines = (Map<?, ?>) field(manager, "machines");
                require(machines.size() == 1, "Machine not restored");
                checkVisuals(machines.values().iterator().next(), "slots");
                manager.command(player, new String[] {"remove", "slots"});
                require(machines.isEmpty(), "Restored machine not removed");
                Files.writeString(marker, "2");
                getLogger().info("CASINO_VANILLA_RESTART_PASS restored=true removed=true");
            } else {
                require(((Map<?, ?>) field(manager, "machines")).isEmpty(), "Deleted machine returned");
                getLogger().info("CASINO_VANILLA_DELETE_RESTART_PASS");
            }
        } catch (Throwable failure) {
            getLogger().log(Level.SEVERE, "CASINO_VANILLA_FAIL", failure);
        } finally {
            Bukkit.shutdown();
        }
    }

    private void click(MachineManager manager, Player player, ItemDisplay visual, String game) {
        Location target = visual.getLocation().add(0, .1, 0);
        Location from = target.clone().add(0, 0, 2);
        from.setDirection(target.toVector().subtract(from.toVector()));
        eye.set(from);
        var event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR,
                null, null, null, EquipmentSlot.HAND);
        manager.interact(event);
        require(event.isCancelled(), "Button did not receive ray: " + game);
    }

    private void checkVisuals(Object machine, String game) throws Exception {
        var parts = (List<?>) field(machine, "parts");
        require(parts.stream().anyMatch(BlockDisplay.class::isInstance), "No vanilla cabinet: " + game);
        require(parts.stream().anyMatch(TextDisplay.class::isInstance), "No text labels: " + game);
        for (Object part : parts) {
            if (part instanceof ItemDisplay item && item.getItemStack().getItemMeta() != null)
                require(item.getItemStack().getItemMeta().getItemModel() == null,
                        "Custom item model remains: " + game);
            require(((Entity) part).isValid(), "Invalid display: " + game);
        }
        if (game.equals("keno"))
            require(parts.stream().filter(TextDisplay.class::isInstance).count() >= 40,
                    "Keno numbers missing");
        var texts = parts.stream().filter(TextDisplay.class::isInstance).map(TextDisplay.class::cast)
                .map(display -> PlainTextComponentSerializer.plainText().serialize(display.text()))
                .toList();
        require(texts.stream().anyMatch(s -> s.equals("PLAY")), "PLAY label missing: " + game);
    }

    private Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> OWNER;
                    case "isOnline", "hasPermission", "isPermissionSet" -> true;
                    case "isDead", "isSneaking" -> false;
                    case "getWorld" -> Bukkit.getWorlds().getFirst();
                    case "getLocation" -> new Location(Bukkit.getWorlds().getFirst(), 0, 90, 0, 0, 0);
                    case "getEyeLocation" -> eye.get() == null
                            ? new Location(Bukkit.getWorlds().getFirst(), 0, 91.6, 0, 0, 0) : eye.get();
                    case "getName", "getDisplayName", "getPlayerListName" -> "VanillaCasinoProbe";
                    case "hashCode" -> OWNER.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> {
                        if (method.getReturnType() == boolean.class) yield false;
                        if (method.getReturnType() == int.class) yield 0;
                        if (method.getReturnType() == long.class) yield 0L;
                        if (method.getReturnType() == float.class) yield 0f;
                        if (method.getReturnType() == double.class) yield 0d;
                        yield null;
                    }
                });
    }

    private static Object field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
