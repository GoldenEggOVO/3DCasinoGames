package dev.casino3d.game.blackjack;

import dev.casino3d.Language;
import dev.casino3d.CasinoRules;
import dev.casino3d.MachineGeometry;
import dev.casino3d.machine.*;
import dev.casino3d.model.MachineDefinition;

import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.entity.*;

import java.security.SecureRandom;
import java.util.*;

public final class BlackjackMachine extends PracticeMachine<BlackjackRound> {
    private final List<ItemDisplay> dealerCards = new ArrayList<>(), playerCards = new ArrayList<>();
    private final Map<ItemDisplay, Slide> slides = new HashMap<>();
    private final Map<ItemDisplay, String> faces = new HashMap<>();
    private TextDisplay readout;
    private boolean wasActive;

    public BlackjackMachine(
            MachineManager manager, UUID owner, Location origin, MachineDefinition definition) {
        super(manager, owner, origin, definition, new BlackjackRound(new SecureRandom()));
    }

    public static String readout(List<Integer> player, List<Integer> dealer, boolean active) {
        return Language.text("blackjack.readout", "player", player.isEmpty() ? "—" : CasinoRules.total(player), "dealer", dealer.isEmpty() ? "—" : active ? "?" : CasinoRules.total(dealer));
    }

    public static String vanillaReadout(List<Integer> player, List<Integer> dealer, boolean active) {
        var visibleDealer = active ? dealer.subList(0, Math.min(1, dealer.size())) : dealer;
        return readout(player, dealer, active) + "\n" + CasinoRules.cards(player) + "\n"
                + CasinoRules.cards(visibleDealer) + (active && dealer.size() > 1 ? "  ?" : "");
    }

    @Override
    protected void buildGame() {
        body("cabinet_blackjack");
        for (String action : List.of("double", "stand", "hit", "start"))
            button(action, "cabinet_button_" + (action.equals("start") ? "play" : action));
        model("cabinet_blackjack_screen", 0, 0, 0, 4);
        readout = text(0, 1.24, -.76, .25);
    }

    @Override
    protected boolean available(String action) {
        return switch (action) {
            case "start" -> !round.active();
            case "double" -> round.active() && round.player().size() == 2;
            case "hit", "stand" -> round.active();
            default -> false;
        };
    }

    @Override
    protected void action(String action) {
        switch (action) {
            case "start" -> {
                trim(dealerCards, 0);
                trim(playerCards, 0);
                round.start(System.currentTimeMillis());
            }
            case "double" -> round.doubleDown();
            case "hit" -> round.hit();
            case "stand" -> round.stand();
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }
        refresh();
    }

    @Override
    protected void refresh() {
        hand(round.dealer(), dealerCards, -.32, round.active());
        hand(round.player(), playerCards, .35, false);
        readout.text(Language.component("blackjack.readout",
                "player", round.player().isEmpty() ? "—" : CasinoRules.total(round.player()),
                "dealer", round.dealer().isEmpty() ? "—" : round.active() ? "?" : CasinoRules.total(round.dealer()))
                .append(Component.text("\n" + CasinoRules.cards(round.player()) + "\n"
                        + CasinoRules.cards(round.active() ? round.dealer().subList(0, Math.min(1, round.dealer().size())) : round.dealer())
                        + (round.active() && round.dealer().size() > 1 ? "  ?" : ""))));
        if (wasActive && !round.active())
            origin.getWorld()
                    .playSound(
                            origin,
                            round.payout() > 0
                                    ? Sound.BLOCK_AMETHYST_BLOCK_CHIME
                                    : Sound.BLOCK_NOTE_BLOCK_BASS,
                            .45f,
                            round.payout() > 0 ? 1.4f : .65f);
        wasActive = round.active();
    }

    private void trim(List<ItemDisplay> cards, int count) {
        while (cards.size() > count) {
            var card = cards.removeLast();
            slides.remove(card);
            faces.remove(card);
            parts.remove(card);
            card.remove();
        }
    }

    private void hand(List<Integer> hand, List<ItemDisplay> cards, double z, boolean hidden) {
        trim(cards, hand.size());
        for (int i = 0; i < hand.size(); i++) {
            String face = "card_" + (hidden && i > 0 ? 52 : hand.get(i));
            var target = at(MachineGeometry.cardX(i, hand.size()), .96 + i * .004, z);
            ItemDisplay card;
            if (i < cards.size()) card = cards.get(i);
            else {
                card = item(model(face), 1.3, 1.1, -.55, .43, -Math.PI / 2);
                cards.add(card);
                // Only newly dealt cards travel from the shoe; revealing a face stays in place.
                slides.put(card, new Slide(card.getLocation(), target, age));
            }
            if (!face.equals(faces.get(card))) {
                card.setItemStack(model(face));
                faces.put(card, face);
            }
            if (slides.containsKey(card)) {
                var previous = slides.get(card);
                slides.put(card, new Slide(previous.from, target, previous.start));
            } else card.teleport(target);
        }
    }

    @Override
    protected void animate() {
        for (var iterator = slides.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            var slide = entry.getValue();
            double t = Math.min(1, (age - slide.start) / 8.0);
            entry.getKey()
                    .teleport(
                            slide.from
                                    .clone()
                                    .add(
                                            slide.to
                                                    .toVector()
                                                    .subtract(slide.from.toVector())
                                                    .multiply(t)));
            if (t >= 1) iterator.remove();
        }
    }

    private record Slide(Location from, Location to, int start) {}
}
