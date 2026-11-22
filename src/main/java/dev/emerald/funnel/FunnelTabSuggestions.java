package dev.emerald.funnel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Single /funnel command completion policy. The caller must pass the result
 * of FunnelCommandAccess.allowed(sender), not a generic LuckPerms permission.
 */
public final class FunnelTabSuggestions {
    private FunnelTabSuggestions() {}

    public static List<String> complete(String[] args, boolean admin,
                                        boolean player, Collection<String> onlineNames) {
        return complete(args, admin, player, 0, onlineNames);
    }

    public static List<String> complete(String[] args, boolean admin,
                                        boolean player, boolean finFunnel,
                                        Collection<String> onlineNames) {
        return complete(args, admin, player, finFunnel ? 1 : 0, onlineNames);
    }

    public static List<String> complete(String[] args, boolean admin,
                                        boolean player, int variant,
                                        Collection<String> onlineNames) {
        // No command or argument hints for unauthorized players.
        if (!admin || args == null || args.length == 0) return List.of();
        List<String> candidates = new ArrayList<>();
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            candidates.addAll(List.of("aerial", "brau", "buster", "gn", "gn2",
                "gnv", "saber", "hawk", "pack", "status", "diagnose",
                "give", "angle", "reload"));
            if (player) candidates.addAll(List.of("formation", "shield", "barrier"));
        } else {
            switch (action) {
                case "saber", "hawk", "gn", "gn2", "gnv", "brau", "buster" -> {
                    if (args.length != 2) return List.of();
                    candidates.add("status");
                }
                case "aerial" -> {
                    if (args.length == 2) {
                        candidates.add("status");
                        if (player) candidates.add("preview");
                    } else if (args.length == 3 && player
                            && args[1].equalsIgnoreCase("preview")) {
                        for (int bit = 1; bit <= 11; bit++)
                            candidates.add(Integer.toString(bit));
                        candidates.add("shield");
                        candidates.add("clear");
                    } else return List.of();
                }
                case "formation" -> {
                    if (!player || args.length != 2) return List.of();
                    if (variant == 1)
                        for (FinFormationMode mode : FinFormationMode.values())
                            candidates.add(mode.id());
                    else
                        for (FormationMode mode : FormationMode.values())
                            if (mode.availableFor(variant)) candidates.add(mode.id());
                    candidates.add("next");
                }
                case "give" -> {
                    if (args.length == 2)
                        candidates.addAll(FunnelGiftCatalog.OPTIONS);
                    else if (args.length == 3 && FunnelGiftCatalog.supported(
                            args[1].toLowerCase(Locale.ROOT)))
                        candidates.addAll(onlineNames);
                    else return List.of();
                }
                case "pack" -> {
                    if (args.length != 2) return List.of();
                    candidates.addAll(onlineNames);
                }
                case "angle" -> {
                    if (args.length != 2) return List.of();
                    candidates.addAll(List.of("-15", "-10", "-6", "0", "6", "10", "15"));
                }
                default -> { return List.of(); }
            }
        }
        String partial = args[args.length-1] == null ? "" : args[args.length-1];
        String prefix = partial.toLowerCase(Locale.ROOT);
        return candidates.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
            .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
