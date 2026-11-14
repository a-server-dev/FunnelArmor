package dev.emerald.funnel;

/** A fresh damage budget for each active ν-fin or Unicorn barrier. */
public final class BarrierDurability {
    private BarrierDurability() {}

    public record State(int hits, double totalDamage) {
        public State {
            if (hits < 0 || !Double.isFinite(totalDamage) || totalDamage < 0)
                throw new IllegalArgumentException("Invalid barrier durability state");
        }
        public State absorb(double damage) {
            if (!Double.isFinite(damage) || damage < 0)
                throw new IllegalArgumentException("Invalid incoming damage");
            return new State(hits + 1, totalDamage + damage);
        }
        public boolean broken(int maxHits, double maxDamage) {
            return hits >= maxHits || totalDamage >= maxDamage;
        }
    }
}
