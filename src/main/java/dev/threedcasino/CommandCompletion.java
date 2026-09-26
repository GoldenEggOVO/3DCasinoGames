package dev.threedcasino;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Suggestions only; permission checks and ownership selection stay in the caller. */
public final class CommandCompletion {
    private CommandCompletion() {}

    public static List<String> player(String[] args, Collection<String> games,
            Collection<String> skins, Collection<String> ownedGames) {
        if (args.length == 0) return List.of();
        Collection<String> candidates = List.of();
        String command = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) candidates = List.of("create", "bet", "remove", "reload-models");
        else if (args.length == 2) candidates = switch (command) {
            case "create" -> games;
            case "bet", "remove" -> ownedGames;
            default -> List.of();
        };
        else if (args.length == 3) candidates = switch (command) {
            case "create" -> skins;
            case "bet" -> ownedGames.contains(args[1].toLowerCase(Locale.ROOT))
                    ? List.of("1", "5", "10", "25", "50", "100") : List.of();
            default -> List.of();
        };
        return matching(candidates, args[args.length - 1]);
    }

    public static List<String> console(String[] args) {
        if (args.length == 0) return List.of();
        Collection<String> candidates = List.of();
        boolean mines = args[0].equalsIgnoreCase("mines");
        if (args.length == 1) candidates = List.of("mines", "resolve");
        else if (mines && args.length == 2) candidates = List.of("resolve");
        else if ((!mines && args.length == 4 && args[0].equalsIgnoreCase("resolve"))
                || (mines && args.length == 5 && args[1].equalsIgnoreCase("resolve")))
            candidates = List.of("applied", "not-applied");
        return matching(candidates, args[args.length - 1]);
    }

    private static List<String> matching(Collection<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .distinct().sorted().toList();
    }
}
