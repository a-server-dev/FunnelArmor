package dev.emerald.funnel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.DummyTracker;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Optional, reversible visual-only experiment. Combat stays in AerialCombat. */
final class AerialBetterModel {
    static final String ID = "funnel_aerial_escutcheon";
    private final JavaPlugin plugin;

    AerialBetterModel(JavaPlugin plugin) {
        this.plugin = plugin;
        try {
            var betterModel = plugin.getServer().getPluginManager().getPlugin("BetterModel");
            if (betterModel == null || !betterModel.isEnabled()) return;
            Path folder = betterModel.getDataFolder().toPath().resolve("models");
            Files.createDirectories(folder);
            Path target = folder.resolve(ID + ".bbmodel");
            // Never overwrite someone's custom version. This file is solely owned by the experiment.
            if (!Files.exists(target)) {
                try (InputStream in = plugin.getResource("aerial-escutcheon-experiment.bbmodel")) {
                    if (in != null) Files.copy(in, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
                plugin.getLogger().info("BetterModel experiment source copied to " + target
                        + "; reload BetterModel's models before enabling the experiment.");
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("BetterModel experiment source install skipped: " + ex.getMessage());
        }
    }

    boolean loaded() {
        try { return BetterModel.modelOrNull(ID) != null; }
        catch (RuntimeException | LinkageError ex) { return false; }
    }

    DummyTracker spawn(Location where) {
        try {
            var model = BetterModel.modelOrNull(ID);
            return model == null ? null : model.create(BukkitAdapter.adapt(where));
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("BetterModel shield unavailable, using vanilla rendering: " + ex.getMessage());
            return null;
        }
    }

    void move(DummyTracker model, Location where) {
        if (model != null && !model.isClosed())
            model.location(BukkitAdapter.adapt(where));
    }

    void close(DummyTracker model) {
        if (model != null && !model.isClosed()) model.close();
    }
}
