package dev.emerald.funnel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.ModelScaler;
import kr.toxicity.model.api.tracker.TrackerUpdateAction;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
            // Only replace our own exact first-generation copy. Never overwrite a custom model.
            byte[] original;
            byte[] cleaned;
            try (InputStream in = plugin.getResource("aerial-escutcheon-original.bbmodel");
                 InputStream fixed = plugin.getResource("aerial-escutcheon-experiment.bbmodel")) {
                if (in == null || fixed == null) throw new IllegalStateException("Missing bundled shield model");
                original = in.readAllBytes();
                cleaned = fixed.readAllBytes();
            }
            boolean unmodifiedLegacy = Files.exists(target)
                    && java.util.Arrays.equals(Files.readAllBytes(target), original);
            if (!Files.exists(target) || unmodifiedLegacy) {
                Files.write(target, cleaned);
                plugin.getLogger().info("Installed sanitized BetterModel shield: " + target
                        + ". Run /bettermodel reload before using the experiment.");
            } else {
                plugin.getLogger().info("Retaining customized BetterModel shield: " + target);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("BetterModel experiment source install skipped: " + ex.getMessage());
        }
    }

    boolean loaded() {
        try { return BetterModel.modelOrNull(ID) != null; }
        catch (RuntimeException | LinkageError ex) { return false; }
    }

    Object spawn(Location where, Player owner) {
        try {
            var model = BetterModel.modelOrNull(ID);
            if (model == null) return null;
            DummyTracker tracker = model.create(BukkitAdapter.adapt(where));
            try {
                // Source geometry stays untouched; tune only this FunnelArmor-owned display.
                double configured = plugin.getConfig().getDouble("aerial.bettermodel.model-scale", 2.0);
                if (!Double.isFinite(configured)) configured = 2.0;
                float scale = (float) Math.max(0.1, Math.min(8.0, configured));
                tracker.scaler(ModelScaler.value(scale));
                for (Player viewer : where.getWorld().getPlayers())
                    tracker.spawn(BukkitAdapter.adapt(viewer));
                // Diagnostic lighting: if the model remains black at full light,
                // inspect the generated BetterModel mesh/UV/texture pack instead.
                boolean fullBright = plugin.getConfig().getBoolean("aerial.bettermodel.full-bright", true);
                if (fullBright) tracker.update(TrackerUpdateAction.brightness(15, 15));
                plugin.getLogger().info("BetterModel Escutcheon display: scale=" + scale
                        + ", full-bright=" + fullBright + ", viewers=" + where.getWorld().getPlayers().size());
                return tracker;
            } catch (RuntimeException | LinkageError ex) {
                tracker.close();
                throw ex;
            }
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("BetterModel shield unavailable, using vanilla rendering: " + ex.getMessage());
            return null;
        }
    }

    void move(Object instance, Location where) {
        DummyTracker model = instance instanceof DummyTracker d ? d : null;
        if (model != null && !model.isClosed())
            model.location(BukkitAdapter.adapt(where));
    }

    void close(Object instance) {
        DummyTracker model = instance instanceof DummyTracker d ? d : null;
        if (model != null && !model.isClosed()) model.close();
    }
}
