package dev.emerald.funnel;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;

/** Config owns balance; the only validation is finite nonnegative HP. */
final class DamageSettings {
    static final String REVISION="damage-config-revision";
    static final List<String> MIGRATED=List.of(
        "beam.damage","qubeley.damage",
        "aerial.auto-damage","aerial.rifle-damage",
        "twin-buster.dual-damage","twin-buster.combined-damage",
        "brau-bro.fire-damage-per-beam","brau-bro.max-burst-damage",
        "heavy-weapons.atomic.damage","heavy-weapons.mace.hit-damage",
        "heavy-weapons.mace.slam-damage","heavy-weapons.mace.pile-damage",
        "heavy-weapons.satellite.beam-damage",
        "advanced-weapons.epyon.damage","advanced-weapons.heavyarms.damage",
        "advanced-weapons.freedom.damage");
    private DamageSettings(){}
    static double nonnegative(double value,double fallback) {
        return Double.isFinite(value)?Math.max(0,value):fallback;
    }
    static double read(Configuration cfg,String path,double fallback) {
        return nonnegative(cfg.getDouble(path,fallback),fallback);
    }
    static boolean needsUpgrade(FileConfiguration cfg) {
        return !cfg.contains(REVISION,true)||cfg.getInt(REVISION)<1;
    }
    static void applyDefaults(FileConfiguration cfg,Configuration defaults) {
        for(String path:MIGRATED) {
            if(!defaults.isDouble(path)&&!defaults.isInt(path))
                throw new IllegalStateException("Missing damage default: "+path);
        }
        for(String path:MIGRATED)cfg.set(path,defaults.getDouble(path));
        cfg.set(REVISION,1);
    }
    static void upgradeOnce(JavaPlugin plugin) {
        FileConfiguration cfg=plugin.getConfig();
        if(!needsUpgrade(cfg))return;
        try {
            Configuration defaults=cfg.getDefaults();
            if(defaults==null)throw new IllegalStateException("Bundled config defaults missing");
            Path file=plugin.getDataFolder().toPath().resolve("config.yml");
            Path backup=Files.createTempFile(file.getParent(),"config-before-damage-v043-",".yml");
            Files.copy(file,backup,StandardCopyOption.REPLACE_EXISTING);
            applyDefaults(cfg,defaults);
            cfg.save(file.toFile());
            plugin.getLogger().info("Damage settings migrated once; backup: "+backup.getFileName());
        }catch(Exception e){
            plugin.getLogger().log(Level.SEVERE,"Damage config migration failed; check config.yml before using weapons",e);
            // Avoid running uncapped old settings when the one-time migration fails.
            throw new IllegalStateException("Damage config migration failed",e);
        }
    }
}
