package dev.emerald.funnel;
import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DamageSettingsTest {
    @Test void configCanSetAnyFiniteNonnegativeDamage() {
        var cfg=new YamlConfiguration();
        cfg.set("damage",27.5);
        assertEquals(27.5,DamageSettings.read(cfg,"damage",.5));
        cfg.set("damage",0);
        assertEquals(0,DamageSettings.read(cfg,"damage",.5));
        cfg.set("damage",-1);
        assertEquals(0,DamageSettings.read(cfg,"damage",.5));
    }
    @Test void upgradeUsesBundledValuesOnceAndPreservesOtherSettings() throws Exception {
        var defaults=YamlConfiguration.loadConfiguration(Path.of("src/main/resources/config.yml").toFile());
        var cfg=new YamlConfiguration();
        cfg.setDefaults(defaults);
        cfg.set("beam.damage",99.0);
        cfg.set("pack.require",true);
        cfg.set("beam-saber.damage",12.0);
        assertTrue(DamageSettings.needsUpgrade(cfg));
        DamageSettings.applyDefaults(cfg,defaults);
        assertEquals(.5,cfg.getDouble("beam.damage"));
        assertEquals(.5,cfg.getDouble("twin-buster.dual-damage"));
        assertEquals(1.5,cfg.getDouble("twin-buster.combined-damage"));
        assertEquals(3.0,cfg.getDouble("heavy-weapons.satellite.beam-damage"));
        assertEquals(.5,cfg.getDouble("aerial.rifle-damage"));
        assertTrue(cfg.getBoolean("pack.require"));
        assertEquals(12.0,cfg.getDouble("beam-saber.damage"));
        cfg.set("beam.damage",4.0);
        assertFalse(DamageSettings.needsUpgrade(cfg));
        assertEquals(4.0,DamageSettings.read(cfg,"beam.damage",.5));
    }
    @Test void materialAndHeavyarmsContracts() throws Exception {
        String heavy=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamHeavyWeapons.java"));
        String melee=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamMeleeCombat.java"));
        String advanced=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamAdvancedWeapons.java"));
        assertTrue(heavy.contains("Material.MACE"));
        assertTrue(heavy.contains("meta.setUnbreakable(false)"));
        assertFalse(melee.contains("setUnbreakable(true)"));
        assertTrue(advanced.contains("l.setNoDamageTicks(0)"));
        assertTrue(advanced.contains("hit.add(l.getUniqueId())"));
        assertTrue(advanced.contains("heavyarms.bypass-hurt-cooldown"));
    }
}
