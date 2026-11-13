package dev.emerald.funnel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Plain Java tab-completion logic so permission checks and argument filtering
 * can be tested without starting Paper.
 */
public final class FunnelTabSuggestions {
    private FunnelTabSuggestions() {}

    public static List<String> complete(String[] args, boolean admin,
                                        boolean player, Collection<String> onlineNames) {
        return complete(args, admin, player, false, onlineNames);
    }

    public static List<String> complete(String[] args, boolean admin,
                                        boolean player, boolean finFunnel,
                                        Collection<String> onlineNames) {
        if (args == null || args.length == 0) return List.of();
        List<String> candidates = new ArrayList<>();
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            candidates.add("pack");
            candidates.add("status");
            if (player) {
                candidates.add("formation");
                candidates.add("shield");
            }
            if (admin) {
                candidates.add("give");
                candidates.add("reload");
            }
        } else {
            switch (action) {
                case "formation" -> {
                    if (!player || args.length != 2) return List.of();
                    if (finFunnel) for (FinFormationMode mode : FinFormationMode.values()) candidates.add(mode.id());
                    else for (FormationMode mode : FormationMode.values()) candidates.add(mode.id());
                    candidates.add("next");
                }
                case "give" -> {
                    if (!admin) return List.of();
                    if (args.length == 2) candidates.addAll(List.of("1", "2", "3", "4", "all"));
                    else if (args.length == 3) candidates.addAll(onlineNames);
                    else return List.of();
                }
                case "pack" -> {
                    if (!admin || args.length != 2) return List.of();
                    candidates.addAll(onlineNames);
                }
                default -> { return List.of(); }
            }
        }
        String input = args[args.length - 1] == null ? "" : args[args.length - 1];
        String prefix = input.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
