package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WeaponMaterialAndDamageTest {
    @Test void twinBusterDamageIsCappedAndLegacyDefaultsUpgrade() throws Exception {
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/TwinBusterCombat.java"));
        String cfg=Files.readString(Path.of("src/main/resources/config.yml"));
        assertTrue(code.contains("DAMAGE_CAP = 1.5"));
        assertTrue(code.contains("Math.min(DAMAGE_CAP, value)"));
        assertTrue(code.contains("value == 1.25"));
        assertTrue(code.contains("shotDamage(\"dual-damage\")"));
        assertTrue(code.contains("shotDamage(\"combined-damage\")"));
        assertTrue(cfg.contains("dual-damage: 1.5"));
        assertTrue(cfg.contains("combined-damage: 1.5"));
    }

    @Test void diamondWeaponMaterialsPreserveCustomAttackLogicAndLegacyMetadata() throws Exception {
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamMeleeCombat.java"));
        assertTrue(code.contains("new ItemStack(Material.DIAMOND_SWORD)"));
        assertTrue(code.contains("new ItemStack(Material.DIAMOND_AXE)"));
        assertTrue(code.contains("old.getType() != Material.FEATHER"));
        assertTrue(code.contains("migrated.setItemMeta(meta)"));
        assertTrue(code.contains("Attribute.ATTACK_SPEED"));
        assertTrue(code.contains("Attribute.ATTACK_DAMAGE"));
        assertTrue(code.contains("DamageCause.ENTITY_SWEEP_ATTACK"));
        assertTrue(code.contains("damage(\"beam-saber.damage\", 6.0)"));
        assertTrue(code.contains("on ? 8.0 : 5.0"));
        assertTrue(code.contains("MeleeStageTiming.canHit"));
        assertTrue(code.contains("ticks(\"heat-hawk.fire-ticks\", 40)"));
    }
}
