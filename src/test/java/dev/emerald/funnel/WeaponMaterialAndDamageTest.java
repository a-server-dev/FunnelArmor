package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WeaponMaterialAndDamageTest {
    @Test void twinBusterDamageReadsConfigWithoutFixedCeiling() throws Exception {
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/TwinBusterCombat.java"));
        String cfg=Files.readString(Path.of("src/main/resources/config.yml"));
        assertFalse(code.contains("DAMAGE_CAP"));
        assertTrue(code.contains("DamageSettings.read(plugin.getConfig()"));
        assertTrue(code.contains("shotDamage(\"dual-damage\")"));
        assertTrue(code.contains("shotDamage(\"combined-damage\")"));
        assertTrue(cfg.contains("dual-damage: 0.5"));
        assertTrue(cfg.contains("combined-damage: 1.5"));
    }

    @Test void twinBusterCustomBeamDamageIsNotCancelledByOwnMeleeListener() throws Exception {
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/TwinBusterCombat.java"));
        assertTrue(code.contains("!applyingBeamDamage.contains(player.getUniqueId())"));
        assertTrue(code.contains("applyingBeamDamage.add(owner)"));
        assertTrue(code.contains("try { living.damage(damage, shooter); }"));
        assertTrue(code.contains("finally { applyingBeamDamage.remove(owner); }"));
        assertTrue(code.contains("living.equals(shooter)"));
        assertTrue(code.contains("!world.getPVP()"));
        assertFalse(code.contains("|| living instanceof Player\n"));
    }

    @Test void diamondWeaponMaterialsPreserveCustomAttackLogicAndLegacyMetadata() throws Exception {
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamMeleeCombat.java"));
        assertTrue(code.contains("new ItemStack(Material.DIAMOND_SWORD)"));
        assertTrue(code.contains("new ItemStack(Material.DIAMOND_AXE)"));
        assertTrue(code.contains("old.getType() == target && !meta.isUnbreakable()"));
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
