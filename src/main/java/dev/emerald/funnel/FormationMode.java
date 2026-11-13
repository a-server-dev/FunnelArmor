package dev.emerald.funnel;

import java.util.Locale;

/** Selectable deployment styles stored individually on each controller item. */
public enum FormationMode {
    ORBIT("orbit", "周回"),
    WINGS("wings", "左右展開"),
    REAR("rear", "背面待機"),
    FRONT("front", "前方防御"),
    SPIRAL("spiral", "螺旋");

    private final String id;
    private final String displayName;

    FormationMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }

    public FormationMode next() {
        FormationMode[] choices = values();
        return choices[(ordinal() + 1) % choices.length];
    }

    public static FormationMode parse(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (FormationMode mode : values()) {
            if (mode.id.equals(value) || mode.displayName.equals(raw.trim())) return mode;
        }
        return null;
    }

    public static FormationMode orDefault(String raw) {
        FormationMode parsed = parse(raw);
        return parsed == null ? ORBIT : parsed;
    }
}
