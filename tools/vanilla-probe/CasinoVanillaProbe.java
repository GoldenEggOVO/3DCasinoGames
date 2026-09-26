package dev.casino3d.probe;

import com.google.gson.GsonBuilder;
import dev.casino3d.CasinoPlugin;
import dev.casino3d.MachineGeometry;
import dev.casino3d.game.PracticeRound;
import dev.casino3d.machine.MachineManager;
import dev.casino3d.machine.PracticeMachine;
import dev.casino3d.model.VanillaGeometry;
import dev.casino3d.model.ModelItems;
import dev.casino3d.model.MachineDefinition;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/** Disposable server-side check; it cannot confirm what a real client renders. */
public final class CasinoVanillaProbe extends JavaPlugin {
    private static final UUID OWNER = UUID.fromString("c96ef2ec-31cb-4b54-976f-550525d2c11a");
    private static final List<String> GAMES = List.of("mines", "blackjack", "crash", "plinko",
            "slots", "duck_race", "wheel_of_fortune", "money_wheel", "penguin_cross",
            "keno", "hilo", "dragon_tower");
    private final AtomicReference<Location> eye = new AtomicReference<>();
    private float placementYaw;
    private boolean sneaking;
    private int shownDialogs;
    private boolean permissions = true;
    private final Map<UUID, Transformation> spawnPoses = new LinkedHashMap<>();
    private final Map<UUID, Integer> spawnDurations = new LinkedHashMap<>();
    private final Map<UUID, Boolean> spawnVisibility = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onSpawn(org.bukkit.event.entity.EntitySpawnEvent event) {
                if (event.getEntity() instanceof Display display) {
                    spawnPoses.put(display.getUniqueId(), display.getTransformation());
                    spawnDurations.put(display.getUniqueId(), display.getInterpolationDuration());
                    spawnVisibility.put(display.getUniqueId(), display.isVisibleByDefault());
                }
            }
        }, this);
        Bukkit.getScheduler().runTaskLater(this, this::check, 100);
    }

    private void check() {
        try {
            var casino = (CasinoPlugin) Bukkit.getPluginManager().getPlugin("3dcasino");
            require(casino != null && casino.isEnabled(), "Casino disabled");
            require(casino.vanillaAppearance(), "Vanilla appearance not enabled");
            require(Files.isRegularFile(casino.getDataFolder().toPath().resolve("languages/en_US.yml"))
                    && Files.isRegularFile(casino.getDataFolder().toPath().resolve("languages/zh_CN.yml")),
                    "Language files were not generated");
            for (String absent : List.of("CraftEngine", "ServerGames", "ServerBoards", "ServerMenu", "KaMenu"))
                require(Bukkit.getPluginManager().getPlugin(absent) == null, absent + " was installed");
            var manager = (MachineManager) field(casino, "machines");
            Player player = player();
            require(casino.getDataFolder().getName().equals("3dcasino"), "Wrong data namespace");
            for (String old : List.of("casino", "casino-demo", "servercasino"))
                require(Bukkit.getPluginCommand(old) == null, "Old command remains: " + old);
            var command = casino.getCommand("3dcasino");
            require(command != null, "New command missing");
            require(casino.onTabComplete(player, command, "3dcasino", new String[]{"CR"})
                    .equals(List.of("create")), "Root completion failed");
            require(casino.onTabComplete(player, command, "3dcasino", new String[]{"create", "d"})
                    .equals(List.of("dragon_tower", "duck_race")), "Game completion failed");
            permissions = false;
            require(casino.onTabComplete(player, command, "3dcasino", new String[]{""}).isEmpty(),
                    "Permission-denied player received completion");
            permissions = true;
            var consoleMessages = new ArrayList<String>();
            var console = (org.bukkit.command.ConsoleCommandSender) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{org.bukkit.command.ConsoleCommandSender.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("sendMessage")) {
                            for (Object argument : arguments) if (argument instanceof String value)
                                consoleMessages.add(value);
                        }
                        return null;
                    });
            casino.onCommand(console, command, "3dcasino", new String[]{"resolve", "invalid", "invalid", "applied"});
            require(consoleMessages.size() == 1 && consoleMessages.getFirst().contains("<player-uuid>"),
                    "Invalid reconciliation UUID should display usage");
            getLogger().info("CASINO_NAMESPACE_TAB_PASS old_aliases=false filtered=true");
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    Bukkit.getWorlds().getFirst().getChunkAt(x, z).setForceLoaded(true);
            var marker = casino.getDataFolder().toPath().resolve("vanilla-probe-phase.txt");
            if (!Files.exists(marker)) {
                checkMenus(casino, player, true);
                for (String game : GAMES) {
                    manager.command(player, new String[] {"create", game});
                    var machines = (Map<?, ?>) field(manager, "machines");
                    require(machines.size() == 1, "Create failed: " + game);
                    Object machine = machines.values().iterator().next();
                    if (game.equals("keno")) {
                        var origin = (Location) field(machine, "origin");
                        var target = ((List<?>) field(machine, "targets")).get(1);
                        var matrix = new org.joml.Matrix4d((org.joml.Matrix4d) field(target, "inverse")).invert();
                        var bounds = (org.bukkit.util.BoundingBox) field(target, "bounds");
                        double high = Double.NEGATIVE_INFINITY;
                        for (double x : new double[] {bounds.getMinX(), bounds.getMaxX()})
                            for (double y : new double[] {bounds.getMinY(), bounds.getMaxY()})
                                for (double z : new double[] {bounds.getMinZ(), bounds.getMaxZ()})
                                    high = Math.max(high, matrix.transformPosition(new org.joml.Vector3d(x,y,z)).y);
                        require(high - origin.getY() <= .89, "Keno click volume floats above the .885 tile surface: " + (high-origin.getY()));
                    }
                    checkVisuals(machine, game);
                    checkAim(machine, player, game);
                    if (game.equals("blackjack")) {
                        var displays = (List<Entity>) field(machine, "parts");
                        require(displays.stream().filter(ItemDisplay.class::isInstance)
                                .map(ItemDisplay.class::cast).noneMatch(display ->
                                        "cabinet_control_panel".equals(VanillaGeometry.name(display.getItemStack()))),
                                "Blackjack sloped button panel remains");
                    }
                    Set<UUID> dragonTilesBefore = Set.of();
                    if (game.equals("dragon_tower")) {
                        var displays = (List<Entity>) field(machine, "parts");
                        dragonTilesBefore = displays.stream().filter(BlockDisplay.class::isInstance)
                                .map(BlockDisplay.class::cast)
                                .filter(display -> display.getBlock().getMaterial() == Material.GRAY_TERRACOTTA)
                                .map(Entity::getUniqueId).collect(java.util.stream.Collectors.toSet());
                        require(dragonTilesBefore.size() == 24, "Dragon hidden tiles must be 24 gray terracotta cubes");
                    }
                    writePreview(machine, game);
                    if (game.equals("keno")) {
                        var tiles = (List<?>) field(machine, "tiles");
                        click(manager, player, (ItemDisplay) tiles.getFirst(), game);
                        ((PracticeMachine<?>) machine).tick();
                        writePreview(machine, "keno-selected");
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
                    if (game.equals("dragon_tower")) {
                        Thread.sleep(170);
                        click(manager, player, (ItemDisplay) ((List<?>) field(machine,"tiles")).getFirst(), game);
                        ((PracticeMachine<?>) machine).tick();
                        var revealedParts = (List<Entity>) field(machine, "parts");
                        var dragonTilesAfter = revealedParts.stream().filter(BlockDisplay.class::isInstance)
                                .map(BlockDisplay.class::cast)
                                .filter(display -> Set.of(Material.GRAY_TERRACOTTA, Material.EMERALD_BLOCK, Material.TNT)
                                        .contains(display.getBlock().getMaterial()))
                                .map(Entity::getUniqueId).collect(java.util.stream.Collectors.toSet());
                        require(dragonTilesBefore.equals(dragonTilesAfter),
                                "Dragon reveal replaced tile entities instead of recoloring them");
                        for (var material : List.of(Material.EMERALD_BLOCK, Material.TNT)) {
                            long count = revealedParts.stream().filter(BlockDisplay.class::isInstance)
                                    .map(BlockDisplay.class::cast)
                                    .filter(display -> display.getBlock().getMaterial() == material).count();
                            require(count == (material == Material.TNT ? 1 : 3),
                                    "Dragon reveal should show three emerald blocks and one TNT: " + material);
                        }
                        checkVisuals(machine, game);
                        writePreview(machine, "dragon-revealed");
                    }
                    if (game.equals("blackjack")) {
                        checkVanillaCards(machine);
                        // Force a dealer draw as well as a hole-card reveal, independent of shuffle.
                        var blackjack = (dev.casino3d.game.blackjack.BlackjackRound) round;
                        for (int attempt = 0; !round.active() && attempt < 100; attempt++)
                            blackjack.start(System.currentTimeMillis());
                        require(round.active(), "Could not start Blackjack probe hand");
                        var dealerHand = (List<Integer>) field(round, "dealer");
                        dealerHand.clear(); dealerHand.addAll(List.of(1, 2));
                        var playerHand = (List<Integer>) field(round, "player");
                        playerHand.clear(); playerHand.addAll(List.of(9, 6));
                        var deck = (List<Integer>) field(round, "deck");
                        int next = (int) field(round, "next");
                        deck.set(next, 7); deck.set(next + 1, 6);
                        var refresh = machine.getClass().getDeclaredMethod("refresh");
                        refresh.setAccessible(true); refresh.invoke(machine);
                        for (int frame = 0; frame < 10; frame++) ((PracticeMachine<?>) machine).tick();
                        if (round.active()) {
                            var cards = blackjackCards(machine);
                            var hole = (ItemDisplay) cards.get(1);
                            var playerCards = List.copyOf(cards.subList(2, cards.size()));
                            var playerPositions = playerCards.stream().map(ItemDisplay.class::cast)
                                    .map(Entity::getLocation).toList();
                            require(VanillaGeometry.name(((ItemDisplay) cards.get(1)).getItemStack()).equals("card_52"),
                                    "Dealer hole card is not hidden");
                            Thread.sleep(170);
                            var stand = buttons.entrySet().stream()
                                    .filter(entry -> entry.getValue().equals("stand"))
                                    .findFirst().orElseThrow();
                            click(manager, player, (ItemDisplay) stand.getKey(), game);
                            cards = blackjackCards(machine);
                            require(!VanillaGeometry.name(((ItemDisplay) cards.get(1)).getItemStack()).equals("card_52"),
                                    "Dealer hole card did not turn face up");
                            var slides = (Map<?, ?>) field(machine, "slides");
                            require(!slides.containsKey(hole), "Hole-card reveal restarted the dealing animation");
                            require(cards.subList(blackjack.dealer().size(), cards.size()).equals(playerCards),
                                    "Dealer draw reused player card entities");
                            for (int frame = 0; frame < 10; frame++) {
                                ((PracticeMachine<?>) machine).tick();
                                for (int i = 0; i < playerCards.size(); i++)
                                    require(((ItemDisplay) playerCards.get(i)).getLocation().distanceSquared(playerPositions.get(i)) < 1e-12,
                                            "Dealer reveal moved a player card");
                            }
                            getLogger().info("CASINO_VANILLA_CARD_PASS dealt=" + cards.size()
                                    + " hole_hidden_then_revealed=true");
                        }
                    }
                    for (int frame = 1; frame <= 80; frame++) {
                        ((PracticeMachine<?>) machine).tick();
                        if (game.equals("hilo") && List.of(1,10,14,20,28,40,42,50,56,62,70,80).contains(frame))
                            writePreview(machine, "hilo-motion/" + String.format("%02d",frame));
                    }
                    if (game.equals("blackjack")) {
                        var dealt = blackjackCards(machine);
                        dealt.get(0).setItemStack(ModelItems.resolve("3dcasino:card_47", true));
                        ((PracticeMachine<?>) machine).tick();
                        writePreview(machine, "blackjack-dealt");
                    }
                    checkVisuals(machine, game);
                    if (game.equals("keno")) {
                        for (var gem : (List<ItemDisplay>) field(machine, "gems"))
                            require(gem.getTeleportDuration() == 0 && gem.getInterpolationDuration() == 0,
                                    "Keno result gem can fly from its previous hidden position");
                    }
                    if (game.equals("penguin_cross")) checkZeroScale(machine);
                    checkSettings(casino, machine, player);
                    var previousParts = new ArrayList<>((List<Entity>) field(machine, "parts"));
                    manager.command(player, new String[] {"remove", game});
                    require(previousParts.stream().noneMatch(Entity::isValid), "Orphan display: " + game);
                    require(machines.isEmpty(), "Remove failed: " + game);
                    getLogger().info("CASINO_VANILLA_MACHINE_PASS game=" + game);
                }
                for (float yaw : new float[] {37, 90, 180}) {
                    placementYaw = yaw;
                    for (String game : List.of("wheel_of_fortune", "money_wheel", "dragon_tower", "keno", "mines",
                            "duck_race", "penguin_cross")) {
                        manager.command(player, new String[] {"create", game});
                        var machine = ((Map<?, ?>) field(manager, "machines")).values().iterator().next();
                        checkAim(machine, player, game);
                        manager.command(player, new String[] {"remove", game});
                    }
                }
                placementYaw = 0;
                for (String game : List.of("slots", "mines", "dragon_tower", "hilo", "duck_race", "penguin_cross"))
                    manager.command(player, new String[] {"create", game});
                Files.writeString(marker, "1");
                getLogger().info("CASINO_VANILLA_FIRST_PASS games=12 pack=false");
            } else if (Files.readString(marker).equals("1")) {
                checkMenus(casino, player, false);
                var machines = (Map<?, ?>) field(manager, "machines");
                require(machines.size() == 6, "Machines not restored");
                for (var machine : List.copyOf(machines.values())) {
                    String game = ((PracticeMachine<?>) machine).game();
                    checkVisuals(machine, game);
                    if (game.equals("duck_race")) {
                        var parts = (List<Entity>) field(machine, "parts");
                        require(parts.stream().filter(TextDisplay.class::isInstance).map(TextDisplay.class::cast)
                                .anyMatch(display -> PlainTextComponentSerializer.plainText()
                                        .serialize(display.text()).equals("CUSTOM PLAY")),
                                "Edited language was not applied to restored button");
                        getLogger().info("CASINO_VANILLA_LANGUAGE_PASS custom_button=true fallback=true");
                    }
                    checkAim(machine, player, game);
                    var buttons = (Map<ItemDisplay, String>) field(machine, "buttonActions");
                    var play = buttons.entrySet().stream().filter(entry ->
                            List.of("start", "play").contains(entry.getValue())).findFirst().orElseThrow();
                    var round = (PracticeRound) field(machine, "round");
                    String before = round.result();
                    click(manager, player, play.getKey(), game);
                    require(!round.result().equals(before) || round.active() || round.finished(),
                            "Menu-disabled PLAY failed: " + game);
                    manager.command(player, new String[] {"remove", game});
                }
                require(machines.isEmpty(), "Restored machine not removed");
                Files.writeString(marker, "2");
                getLogger().info("CASINO_VANILLA_RESTART_PASS restored=true removed=true menus=false played=true");
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

    private void checkMenus(CasinoPlugin casino, Player player, boolean enabled) throws Exception {
        require(casino.menusEnabled() == enabled, "Wrong menu setting");
        require((field(casino, "menus") != null) == enabled, "Wrong menu lifecycle");
        int before = shownDialogs;
        casino.onCommand(player, casino.getCommand("3dcasino"), "3dcasino", new String[0]);
        casino.openMachineSettings(player, "slots", () -> 1, value -> {}, () -> true, () -> true, () -> {});
        require(shownDialogs - before == (enabled ? 2 : 0), "Native Dialog switch failed");
        getLogger().info("CASINO_VANILLA_MENU_PASS enabled=" + enabled + " dialogs=" + (shownDialogs - before));
    }

    private void click(MachineManager manager, Player player, ItemDisplay visual, String game) {
        String name = VanillaGeometry.name(visual.getItemStack());
        var matrix = VanillaGeometry.matrix(visual.getTransformation());
        var local = matrix.transformPosition(new org.joml.Vector3f(0,
                name != null && name.contains("button") ? .2f : 0, .13f));
        var direction = matrix.transformDirection(new org.joml.Vector3f(0, 0, 1)).normalize();
        var offset = MachineGeometry.rotate(local.x, local.y, local.z, visual.getLocation().getYaw());
        var normal = MachineGeometry.rotate(direction.x, direction.y, direction.z, visual.getLocation().getYaw());
        Location target = visual.getLocation().add(offset.x(), offset.y(), offset.z());
        Location from = target.clone().add(normal.x()*2, normal.y()*2, normal.z()*2);
        from.setDirection(target.toVector().subtract(from.toVector()));
        eye.set(from);
        var picked = from.getWorld().rayTraceEntities(from, from.getDirection(), 3,
                entity -> entity instanceof Interaction);
        require(picked != null, "Client cannot pick an Interaction: " + game);
        var event = new PlayerInteractAtEntityEvent(player, picked.getHitEntity(),
                picked.getHitPosition().subtract(picked.getHitEntity().getLocation().toVector()), EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        require(event.isCancelled(), "Button did not receive ray: " + game);
    }

    private void checkAim(Object machine, Player player, String game) throws Exception {
        var ray = PracticeMachine.class.getDeclaredMethod("ray", Player.class, double.class);
        ray.setAccessible(true);
        var buttons = (Map<ItemDisplay, String>) field(machine, "buttonActions");
        int samples = 0;
        for (var entry : buttons.entrySet())
            for (float x : new float[] {-.16f, 0, .16f})
                for (float y : new float[] {.12f, .20f, .28f}) {
                    aim(entry.getKey(), new org.joml.Vector3f(x, y, .133f));
                    assertAim(ray, machine, player, entry.getValue());
                    samples++;
                }
        if (game.equals("dragon_tower") || game.equals("keno")) {
            var tiles = (List<ItemDisplay>) field(machine, "tiles");
            for (int index = 0; index < tiles.size(); index++)
                for (float x : new float[] {-.94f, 0, .94f}) {
                    float size = game.equals("keno") ? .12f : .14f;
                    aim(tiles.get(index), new org.joml.Vector3f(x*size, size*.7f,
                            game.equals("keno") ? .035f : .14f));
                    assertAim(ray, machine, player, "select:" + (game.equals("keno") ? index+1 : index%4));
                    if (game.equals("dragon_tower"))
                        require(field(field(ray.invoke(machine, player, 3d), "target"), "row").equals(index/4),
                                "Dragon aimed at the wrong floor");
                    samples++;
                }
        }
        if (game.equals("keno")) {
            var origin = ((PracticeMachine<?>) machine).origin();
            var offset = MachineGeometry.rotate(-1.15, .96, 3, origin.getYaw());
            var direction = MachineGeometry.rotate(0, 0, -1, origin.getYaw());
            var from = origin.clone().add(offset.x(), offset.y(), offset.z());
            from.setDirection(new org.bukkit.util.Vector(direction.x(), direction.y(), direction.z()));
            eye.set(from);
            require(ray.invoke(machine, player, 4d) == null, "Keno still hits empty air above tiles");
        }
        getLogger().info("CASINO_AIM_PASS game=" + game + " yaw=" + placementYaw + " samples=" + samples);
    }

    private void checkSettings(CasinoPlugin casino, Object machine, Player player) throws Exception {
        var settings = casino.machineSettings();
        var target = ((Map<?, ?>) field(settings, "targets")).get(machine);
        var opened = new int[] {0};
        settings.register(machine, OWNER, (Location) field(target, "origin"),
                (org.bukkit.util.BoundingBox) field(target, "bounds"), p -> opened[0]++);
        var button = ((Map<ItemDisplay, ?>) field(machine, "buttonActions")).keySet().iterator().next();
        aim(button, new org.joml.Vector3f(0, .2f, .13f));
        var picked = eye.get().getWorld().rayTraceEntities(eye.get(), eye.get().getDirection(), 3, e -> e instanceof Interaction);
        require(picked != null, "Missing settings interaction entity");
        sneaking = true;
        Bukkit.getPluginManager().callEvent(new PlayerInteractAtEntityEvent(player, picked.getHitEntity(), new org.bukkit.util.Vector(), EquipmentSlot.HAND));
        Bukkit.getPluginManager().callEvent(new org.bukkit.event.player.PlayerInteractEntityEvent(player, picked.getHitEntity(), EquipmentSlot.HAND));
        sneaking = false;
        require(opened[0] == 1, "Shift entity click must open settings exactly once: " + opened[0]);
    }

    private void aim(ItemDisplay visual, org.joml.Vector3f point) {
        var matrix = VanillaGeometry.matrix(visual.getTransformation());
        var p = matrix.transformPosition(point);
        var n = matrix.transformDirection(new org.joml.Vector3f(0, 0, 1)).normalize();
        var offset = MachineGeometry.rotate(p.x, p.y, p.z, visual.getLocation().getYaw());
        var normal = MachineGeometry.rotate(n.x, n.y, n.z, visual.getLocation().getYaw());
        var target = visual.getLocation().add(offset.x(), offset.y(), offset.z());
        var from = target.clone().add(normal.x()*2, normal.y()*2, normal.z()*2);
        from.setDirection(target.toVector().subtract(from.toVector()));
        eye.set(from);
    }

    private void assertAim(java.lang.reflect.Method ray, Object machine, Player player, String action) throws Exception {
        var picked = eye.get().getWorld().rayTraceEntities(eye.get(), eye.get().getDirection(), 3, e -> e instanceof Interaction);
        require(picked != null, "Visible surface has no client hitbox: " + action);
        var hit = ray.invoke(machine, player, 3d);
        require(hit != null && field(field(hit, "target"), "action").equals(action),
                "Visible surface picked wrong action: " + ((PracticeMachine<?>) machine).game() + " " + action);
    }

    private void checkVisuals(Object machine, String game) throws Exception {
        var parts = (List<?>) field(machine, "parts");
        require(parts.stream().filter(org.bukkit.entity.Interaction.class::isInstance).count()
                        >= ((List<?>) field(machine, "targets")).size(),
                "Missing client click hitboxes: " + game);
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
        require(texts.stream().noneMatch(value -> value.codePoints().anyMatch(code ->
                Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN)),
                "Default machine text is not English: " + game);
        require(texts.stream().anyMatch(s -> s.equals("PLAY") || s.equals("SPIN") || s.equals("CUSTOM PLAY")), "PLAY/SPIN label missing: " + game);
        require(texts.stream().noneMatch(s -> s.startsWith("FREE PLAY")), "Floating status label remains: " + game);
        var buttons = (Map<?, ?>) field(machine, "buttonActions");
        var origin = (Location) field(machine, "origin");
        double scale = game.equals("plinko") ? .75 : MachineGeometry.machineScale(game);
        for (var entry : buttons.entrySet()) {
            var expected = (game.equals("mines")
                    ? dev.casino3d.game.mines.MinesMachine.vanillaButton((String) entry.getValue())
                    : game.equals("dragon_tower")
                    ? dev.casino3d.game.dragon_tower.DragonTowerMachine.vanillaButton((String) entry.getValue())
                    : MachineDefinition.builtin(game).button((String) entry.getValue())).transform();
            double expectedX = expected.x(), expectedY = expected.y(), expectedZ = expected.z();
            if (game.equals("penguin_cross")) expectedZ += .20;
            if (game.equals("duck_race")) {
                if (entry.getValue().equals("play")) {
                    expectedX = 0; expectedY = .31; expectedZ = 1.73;
                } else expectedZ += .22;
            }
            var delta = ((ItemDisplay) entry.getKey()).getLocation().subtract(origin).toVector();
            var local = MachineGeometry.rotate(delta.getX(), delta.getY(), delta.getZ(), -origin.getYaw());
            require(Math.abs(local.x() - expectedX*scale) < .001
                    && Math.abs(local.y() - expectedY*scale) < .001
                    && Math.abs(local.z() - expectedZ*scale) < .001, "Approved button position: " + game);
        }
        for (Object part : parts) {
            if (part instanceof ItemDisplay item && VanillaGeometry.name(item.getItemStack()) != null)
                require(!item.isVisibleByDefault(), "Placeholder carrier is visible: " + game);
        }
        for (Object group : (List<?>) field(machine, "vanillaDisplays")) {
            var carrier = (ItemDisplay) field(group, "carrier");
            require(Boolean.FALSE.equals(spawnVisibility.get(carrier.getUniqueId())),
                    "Vanilla placeholder was visible at spawn: " + game);
            var parent = VanillaGeometry.matrix(carrier.getTransformation());
            for (Object child : (List<?>) field(group, "children")) {
                var display = (Display) field(child, "display");
                var initial = spawnPoses.get(display.getUniqueId());
                require(initial != null && !initial.getScale().equals(new org.joml.Vector3f(1)),
                        "Vanilla child spawned at default pose: " + game);
                require(spawnDurations.get(display.getUniqueId()) == 0,
                        "Vanilla child interpolates from default pose: " + game);
                var expected = new org.joml.Matrix4f(parent).mul((org.joml.Matrix4f) field(child, "local"));
                var pose = display.getTransformation();
                var actual = new org.joml.Matrix4f().translation(pose.getTranslation())
                        .rotate(pose.getLeftRotation()).scale(pose.getScale()).rotate(pose.getRightRotation());
                for (int corner = 0; corner < 8; corner++) {
                    var point = new org.joml.Vector3f(corner & 1, (corner >> 1) & 1, (corner >> 2) & 1);
                    float error = expected.transformPosition(new org.joml.Vector3f(point))
                            .distance(actual.transformPosition(point));
                    require(error < .0001, "Display geometry distorted: " + game + " / "
                            + VanillaGeometry.name(carrier.getItemStack()) + " error=" + error);
                }
            }
        }
    }

    private void checkVanillaCards(Object machine) throws Exception {
        var cards = blackjackCards(machine);
        require(cards.size() >= 4, "Blackjack did not deal vanilla cards");
        var parts = (List<?>) field(machine, "parts");
        require(cards.stream().map(ItemDisplay.class::cast)
                .allMatch(c -> VanillaGeometry.name(c.getItemStack()).startsWith("card_")),
                "Blackjack composite card geometry missing");
        require(parts.stream().filter(BlockDisplay.class::isInstance)
                .map(BlockDisplay.class::cast)
                .filter(display -> display.getBlock().getMaterial() == Material.WHITE_CONCRETE)
                .count() >= 4, "Blackjack card bases missing");
    }

    private List<ItemDisplay> blackjackCards(Object machine) throws Exception {
        var cards = new ArrayList<ItemDisplay>((List<ItemDisplay>) field(machine, "dealerCards"));
        cards.addAll((List<ItemDisplay>) field(machine, "playerCards"));
        return cards;
    }

    private void checkZeroScale(Object machine) throws Exception {
        var carrier = (ItemDisplay) ((List<?>) field(machine, "figures")).getFirst();
        var group = ((List<?>) field(machine, "vanillaDisplays")).stream().filter(value -> {
            try { return field(value, "carrier") == carrier; }
            catch (Exception e) { throw new RuntimeException(e); }
        }).findFirst().orElseThrow();
        var saved = carrier.getTransformation();
        var hidden = carrier.getTransformation();
        hidden.getScale().zero();
        carrier.setTransformation(hidden);
        var sync = group.getClass().getDeclaredMethod("sync");
        sync.setAccessible(true); sync.invoke(group);
        for (var child : (List<?>) field(group, "children")) {
            var pose = ((Display) field(child, "display")).getTransformation();
            require(pose.getScale().isFinite() && pose.getTranslation().isFinite()
                    && pose.getLeftRotation().isFinite() && pose.getRightRotation().isFinite(),
                    "Hiding a model produced non-finite geometry");
        }
        carrier.setTransformation(saved);
        sync.invoke(group);
        checkVisuals(machine, "penguin_cross");
    }

    private void writePreview(Object machine, String game) throws Exception {
        var origin = (Location) field(machine, "origin");
        var parts = (List<?>) field(machine, "parts");
        var entries = new ArrayList<Map<String, Object>>();
        var childIds = new java.util.HashSet<UUID>();
        for (Object group : (List<?>) field(machine, "vanillaDisplays"))
            for (Object child : (List<?>) field(group, "children"))
                childIds.add(((Entity) field(child, "display")).getUniqueId());
        for (Object part : parts) {
            if (!(part instanceof Display display)) continue;
            var location = display.getLocation();
            var local = MachineGeometry.rotate(location.getX() - origin.getX(),
                    location.getY() - origin.getY(), location.getZ() - origin.getZ(),
                    -origin.getYaw());
            Transformation pose = display.getTransformation();
            var entry = new LinkedHashMap<String, Object>();
            entry.put("kind", display.getClass().getSimpleName());
            entry.put("visible", display.isVisibleByDefault());
            entry.put("vanillaChild", childIds.contains(display.getUniqueId()));
            entry.put("position", List.of(local.x(), local.y(), local.z()));
            entry.put("translation", List.of(pose.getTranslation().x,
                    pose.getTranslation().y, pose.getTranslation().z));
            entry.put("scale", List.of(pose.getScale().x, pose.getScale().y,
                    pose.getScale().z));
            entry.put("leftRotation", List.of(pose.getLeftRotation().x,
                    pose.getLeftRotation().y, pose.getLeftRotation().z,
                    pose.getLeftRotation().w));
            entry.put("rightRotation", List.of(pose.getRightRotation().x,
                    pose.getRightRotation().y, pose.getRightRotation().z,
                    pose.getRightRotation().w));
            if (display instanceof BlockDisplay block)
                entry.put("material", block.getBlock().getMaterial().name());
            else if (display instanceof ItemDisplay item) {
                entry.put("material", item.getItemStack().getType().name());
                entry.put("model", VanillaGeometry.name(item.getItemStack()));
            }
            else if (display instanceof TextDisplay text) {
                entry.put("text", PlainTextComponentSerializer.plainText().serialize(text.text()));
                entry.put("color", text.text().color() == null ? 0xffffff : text.text().color().value());
            }
            entries.add(entry);
        }
        var folder = getDataFolder().toPath().resolve("preview-snapshots");
        Files.createDirectories(folder);
        Files.createDirectories(folder.resolve(game + ".json").getParent());
        Files.writeString(folder.resolve(game + ".json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(entries));
    }

    private Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> OWNER;
                    case "isOnline" -> true;
                    case "hasPermission", "isPermissionSet" -> permissions;
                    case "isDead" -> false;
                    case "isSneaking" -> sneaking;
                    case "showDialog" -> { shownDialogs++; yield null; }
                    case "sendMessage" -> {
                        for (Object value : args) if (value instanceof String message)
                            require(message.codePoints().noneMatch(code ->
                                    Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN),
                                    "Default command message is not English: " + message);
                        yield null;
                    }
                    case "getWorld" -> Bukkit.getWorlds().getFirst();
                    case "getLocation" -> new Location(Bukkit.getWorlds().getFirst(), 0, 90, 0, placementYaw, 0);
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
