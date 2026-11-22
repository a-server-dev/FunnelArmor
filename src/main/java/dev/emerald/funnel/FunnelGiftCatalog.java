package dev.emerald.funnel;

import java.util.List;

/** One place to enumerate all currently distributable weapons. */
final class FunnelGiftCatalog {
    static final List<String> ALL = List.of(
        "1", "3", "4", "aerial", "brau", "buster", "gn", "gn2", "saber", "hawk");
    static final List<String> OPTIONS = List.of(
        "1", "3", "4", "aerial", "brau", "buster", "gn", "gn2",
        "gnv", "saber", "hawk", "all");

    private FunnelGiftCatalog() {}

    static boolean supported(String type) {
        return OPTIONS.contains(type);
    }
}
