package dev.emerald.funnel;

import java.util.List;

/** One place to enumerate all currently distributable weapons. */
final class FunnelGiftCatalog {
    static final List<String> ALL = List.of(
        "1", "3", "4", "aerial", "brau", "buster",
        "saber", "hawk", "atomic", "mace", "satellite", "epyon", "heavyarms", "freedom", "hammer", "destiny");
    static final List<String> OPTIONS = List.of("all");

    private FunnelGiftCatalog() {}

    static boolean supported(String type) {
        return OPTIONS.contains(type);
    }
}
