package dev.emerald.funnel;

import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public final class FunnelArmorPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private NamespacedKey itemKey;
    private NamespacedKey displayKey;
    private NamespacedKey variantKey;
    private NamespacedKey formationKey;
    private NamespacedKey finFormationKey;
    private final Map<UUID, List<ItemDisplay>> activeDrones = new ConcurrentHashMap<>();
    private long visualTick = 0;
    private final Map<UUID, Long> lastShots = new ConcurrentHashMap<>();
    private final Set<UUID> shieldGuards = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<ItemDisplay>> shieldDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, Long> shieldToggleTicks = new ConcurrentHashMap<>();
    private final Set<UUID> finBarrierGuards = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> finBarrierProgress = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemDisplay>> finBarrierDisplays = new ConcurrentHashMap<>();
    /** Five non-colliding white stained-glass BlockDisplays: 4 walls and a roof. */
    private final Map<UUID, List<BlockDisplay>> finGlassDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, Long> finBarrierToggleTicks = new ConcurrentHashMap<>();
    /** 0 = docked, 1 = protective wall; retained while closing. */
    private final Map<UUID, Double> shieldProgress = new ConcurrentHashMap<>();
    private final Map<UUID, Long> shotAnimationStart = new ConcurrentHashMap<>();
    private final Map<UUID, Vector> shotAimTargets = new ConcurrentHashMap<>();
    private long animationTick = 0;
    private HttpServer httpServer;
    private byte[] packBytes;
    private String sha1;
    private Path packPath;

    @Override public void onEnable() {
        saveDefaultConfig();
        itemKey = new NamespacedKey(this, "funnel_weapon");
        displayKey = new NamespacedKey(this, "funnel_drone_display");
        variantKey = new NamespacedKey(this, "funnel_variant");
        formationKey = new NamespacedKey(this, "funnel_formation");
        finFormationKey = new NamespacedKey(this, "fin_funnel_formation");
        try {
            Path folder = getDataFolder().toPath();
            Files.createDirectories(folder);
            packPath = folder.resolve("FunnelArmor-pack.zip");
            try (InputStream stream = getResource("funnel-pack.zip")) {
                if (stream == null) throw new IOException("Bundled funnel-pack.zip not found");
                Files.copy(stream, packPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            packBytes = Files.readAllBytes(packPath);
            sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(packBytes));
            startHttp();
        } catch (Exception ex) {
            getLogger().severe("Resource pack initialization failed: " + ex.getMessage());
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        cleanupOrphans();
        Bukkit.getScheduler().runTaskTimer(this, this::updateDrones, 10L, Math.max(1L, getConfig().getLong("drones.update-ticks", 2L)));
        Bukkit.getScheduler().runTaskTimer(this, this::updateShields, 1L, 1L);
        if (getCommand("funnel") != null) {
            getCommand("funnel").setExecutor(this);
            getCommand("funnel").setTabCompleter(this);
        }
        getLogger().info("FunnelArmor " + getPluginMeta().getVersion()
                + " ready. Built-in resource pack SHA1: " + sha1);
    }
    @Override public void onDisable() {
        if (httpServer != null) httpServer.stop(0);
        lastShots.clear();
        shieldToggleTicks.clear();
        shieldProgress.clear();
        shotAnimationStart.clear();
        shotAimTargets.clear();
        for (UUID uuid : new ArrayList<>(shieldGuards)) stopShield(uuid);
        for (UUID uuid : new ArrayList<>(finBarrierGuards)) stopFinBarrier(uuid);
        for (UUID uuid : new ArrayList<>(finBarrierDisplays.keySet())) stopFinBarrier(uuid);
        for (UUID uuid : new ArrayList<>(finGlassDisplays.keySet())) stopFinBarrier(uuid);
        for (UUID uuid : new ArrayList<>(shieldDisplays.keySet())) removeShieldDisplays(uuid);
        for (UUID uuid : new ArrayList<>(activeDrones.keySet())) removeDrones(uuid);
    }
    private void startHttp() {
        if (httpServer != null) httpServer.stop(0);
        httpServer = null;
        // GitHub raw, Releases or another external download host requires no
        // inbound HTTP listener (and no port forwarding) on the Paper machine.
        if (usesGithubRaw() || usesGitHubReleases()
                || !getConfig().getBoolean("pack.embedded-http.enabled", false)
                || packBytes == null
                || !getConfig().getString("pack.external-url", "").isBlank()
                || !getConfig().getString("pack.external-base-url", "").isBlank()) return;
        try {
            String bind = getConfig().getString("pack.embedded-http.bind-host", "0.0.0.0");
            int port = getConfig().getInt("pack.embedded-http.port", 8765);
            httpServer = HttpServer.create(new InetSocketAddress(bind, port), 0);
            httpServer.createContext("/funnel-pack.zip", exchange -> {
                try {
                    if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1); return;
                    }
                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    if ("HEAD".equals(exchange.getRequestMethod())) {
                        exchange.getResponseHeaders().set("Content-Length", Integer.toString(packBytes.length));
                        exchange.sendResponseHeaders(200, -1);
                    } else {
                        getLogger().info("Resource-pack download: " + exchange.getRemoteAddress() + " requested " + exchange.getRequestURI());
                        exchange.sendResponseHeaders(200, packBytes.length);
                        exchange.getResponseBody().write(packBytes);
                    }
                } finally { exchange.close(); }
            });
            httpServer.start();
            getLogger().info("Pack HTTP listening on " + bind + ":" + port + " (configured URL: " + publicUrl() + ")");
        } catch (IOException ex) {
            getLogger().warning("Cannot start pack HTTP server: " + ex.getMessage());
            httpServer = null;
        }
    }
    private boolean usesGithubRaw() {
        // Default true even if the server has an old config.yml.
        // Explicit external URLs always take precedence.
        return getConfig().getBoolean("pack.github-raw.enabled", true)
                && getConfig().getString("pack.external-url", "").isBlank()
                && getConfig().getString("pack.external-base-url", "").isBlank();
    }
    private boolean usesGitHubReleases() {
        return !usesGithubRaw()
                && getConfig().getBoolean("pack.github-releases.enabled", true)
                && getConfig().getString("pack.external-url", "").isBlank()
                && getConfig().getString("pack.external-base-url", "").isBlank();
    }
    private String publicUrl() {
        return publicUrl(null);
    }
    private String publicUrl(Player player) {
        String external = getConfig().getString("pack.external-url", "").trim();
        if (!external.isEmpty()) return external;
        String externalBase = getConfig().getString("pack.external-base-url", "").trim();
        if (!externalBase.isEmpty()) return ResourcePackUrl.fromBase(externalBase, sha1);
        if (usesGithubRaw()) {
            return ResourcePackUrl.githubRaw(
                    getConfig().getString("pack.github-raw.repository", "a-server-dev/FunnelArmor"),
                    sha1);
        }
        if (usesGitHubReleases()) {
            return ResourcePackUrl.githubRelease(
                    getConfig().getString("pack.github-releases.repository", "a-server-dev/FunnelArmor"),
                    sha1);
        }
        String host = getConfig().getString("pack.embedded-http.public-host", "auto").trim();
        if (host.equalsIgnoreCase("auto") || host.isBlank()) {
            // This is the hostname the player used to connect. It avoids accidentally
            // sending 127.0.0.1 to players who connected using a remote address.
            java.net.InetSocketAddress virtualHost = player == null ? null : player.getVirtualHost();
            if (virtualHost != null && virtualHost.getHostString() != null && !virtualHost.getHostString().isBlank()) {
                host = virtualHost.getHostString();
            } else {
                host = "127.0.0.1";
            }
        }
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        return "http://" + host + ":" + getConfig().getInt("pack.embedded-http.port", 8765) + "/funnel-pack.zip";
    }
    private boolean applyPack(Player player) {
        if (sha1 == null) { player.sendMessage("§cリソースパックが生成されていません。"); return false; }
        if (!usesGithubRaw() && !usesGitHubReleases()
                && getConfig().getString("pack.external-url", "").isBlank()
                && getConfig().getString("pack.external-base-url", "").isBlank()
                && httpServer == null) {
            player.sendMessage("§cパック配信サーバーが起動していません。plugins/FunnelArmor/FunnelArmor-pack.zip を手動適用してください。");
            return false;
        }
        String url;
        try {
            url = publicUrl(player);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("§cリソースパックの公開URL設定が不正です: " + ex.getMessage());
            getLogger().warning("Invalid resource-pack URL: " + ex.getMessage());
            return false;
        }
        player.setResourcePack(url, sha1, getConfig().getBoolean("pack.require", false),
                Component.text("ファンネル用3Dモデルを表示するリソースパックです。"));
        // player.sendMessage("§bファンネルのパックを要求しました: §7" + url);
        // getLogger().info("Requested resource pack for " + player.getName() + " from " + url);
        if (url.contains("127.0.0.1") && player.getAddress() != null
                && !player.getAddress().getAddress().isLoopbackAddress()) {
            player.sendMessage("§e警告: URLが127.0.0.1です。別PCの場合は config.yml の pack.embedded-http.public-host にサーバーの到達可能なホスト名/IPを設定してください。");
        }
        return true;
    }
    private ItemStack weapon(int variant) {
        if (variant < 1 || variant > 4) throw new IllegalArgumentException("Invalid drone variant");
        ItemStack stack = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("§b◆ " + switch (variant) {
            case 1 -> "フィン・ファンネル（6基）";
            case 2 -> "重装ファンネル（6基）";
            case 3 -> "ペタル・ビット（12基）";
            default -> "シールド・ファンネル（3基）";
        }));
        meta.lore(java.util.List.of(Component.text("§7右クリック: ビーム攻撃"),Component.text("§7周囲に " + droneCount(variant) + " 基展開"),Component.text("§7展開：" + (variant == 1 ? "格納" : "周回")),Component.text("§7しゃがみ＋右クリックで展開切替"),Component.text("§7射程32 / ダメージ0.5HP")));
        meta.setItemModel(new NamespacedKey("funnel", "funnel_drone_" + variant));
        meta.getPersistentDataContainer().set(variantKey, PersistentDataType.INTEGER, variant);
        meta.getPersistentDataContainer().set(formationKey, PersistentDataType.STRING, FormationMode.ORBIT.id());
        if (variant == 1) meta.getPersistentDataContainer().set(
                finFormationKey, PersistentDataType.STRING, FinFormationMode.STORED.id());
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }
    private boolean isWeapon(ItemStack stack) {
        return stack != null && stack.getType() == Material.LEATHER_HORSE_ARMOR && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }
    private int selectedVariant(Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!isWeapon(stack)) return 0;
        Integer variant = stack.getItemMeta().getPersistentDataContainer().get(variantKey, PersistentDataType.INTEGER);
        return variant == null ? 1 : Math.max(1, Math.min(4, variant));
    }
    private FormationMode selectedFormation(Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!isWeapon(stack)) return FormationMode.ORBIT;
        String saved = stack.getItemMeta().getPersistentDataContainer()
                .get(formationKey, PersistentDataType.STRING);
        return FormationMode.orDefault(saved);
    }
    private FinFormationMode selectedFinFormation(Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!isWeapon(stack) || selectedVariant(player) != 1) return FinFormationMode.STORED;
        var pdc = stack.getItemMeta().getPersistentDataContainer();
        String saved = pdc.get(finFormationKey, PersistentDataType.STRING);
        if (saved != null) return FinFormationMode.orDefault(saved);
        return FinFormationMode.fromLegacy(pdc.get(formationKey, PersistentDataType.STRING));
    }

    private void changeFinFormation(Player player, FinFormationMode mode) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!isWeapon(stack) || selectedVariant(player) != 1) return;
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(finFormationKey, PersistentDataType.STRING, mode.id());
        meta.lore(java.util.List.of(
                Component.text("§7右クリック: ビーム攻撃"),
                Component.text("§7周囲に 6 基展開"),
                Component.text("§7展開：" + mode.displayName()),
                Component.text("§7しゃがみ＋右クリックで展開切替"),
                Component.text("§7射程32 / ダメージ0.5HP")));
        stack.setItemMeta(meta);
        player.getInventory().setItemInMainHand(stack);
    }

    private void changeFormation(Player player, FormationMode mode) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!isWeapon(stack)) {
            player.sendMessage("§cファンネルをメインハンドに持ってください。");
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(formationKey, PersistentDataType.STRING, mode.id());
        int variant = selectedVariant(player);
        meta.lore(java.util.List.of(
                Component.text("§7右クリック: ビーム攻撃"),
                Component.text("§7周囲に " + droneCount(variant) + " 基展開"),
                Component.text("§7展開：" + mode.displayName()),
                Component.text("§7しゃがみ＋右クリックで展開切替"),
                Component.text("§7射程32 / ダメージ0.5HP")));
        stack.setItemMeta(meta);
        player.getInventory().setItemInMainHand(stack);
        // player.sendActionBar(Component.text("§b[FunnelArmor] 展開：" + mode.displayName()));
        // player.sendMessage("§bファンネル展開モード: §f" + mode.displayName());
    }
    private int displayedVariant(ItemDisplay display) {
        ItemStack stack = display.getItemStack();
        if (stack == null || !stack.hasItemMeta()) return 0;
        Integer variant = stack.getItemMeta().getPersistentDataContainer().get(variantKey, PersistentDataType.INTEGER);
        return variant == null ? 0 : variant;
    }
    private void cleanupOrphans() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display.getPersistentDataContainer().has(displayKey, PersistentDataType.BYTE)) display.remove();
            }
            for (BlockDisplay display : world.getEntitiesByClass(BlockDisplay.class)) {
                if (display.getPersistentDataContainer().has(displayKey, PersistentDataType.BYTE)) display.remove();
            }
        }
    }
    private void removeDrones(UUID uuid) {
        List<ItemDisplay> displays = activeDrones.remove(uuid);
        if (displays == null) return;
        for (ItemDisplay d : displays) if (d.isValid()) d.remove();
    }
    private boolean hasFinOffhand(Player player) {
        ItemStack item = player.getInventory().getItemInOffHand();
        if (!isWeapon(item)) return false;
        Integer variant = item.getItemMeta().getPersistentDataContainer()
                .get(variantKey, PersistentDataType.INTEGER);
        return variant != null && variant == 1;
    }
    private boolean finBarrierActive(Player player) {
        return player.isOnline() && !player.isDead() && hasFinOffhand(player)
                && finBarrierGuards.contains(player.getUniqueId())
                && finBarrierProgress.getOrDefault(player.getUniqueId(), 0.0) >= 0.999;
    }
    private void stopFinBarrier(UUID id) {
        finBarrierGuards.remove(id);
        finBarrierProgress.remove(id);
        List<ItemDisplay> old = finBarrierDisplays.remove(id);
        if (old != null) for (ItemDisplay d : old) if (d.isValid()) d.remove();
        removeFinGlass(id);
        finBarrierToggleTicks.remove(id);
    }
    private void toggleFinBarrier(Player player) {
        if (!hasFinOffhand(player)) {
            player.sendMessage("§cフィン・ファンネルをオフハンドに持ってください。");
            return;
        }
        UUID id = player.getUniqueId();
        long tick = player.getWorld().getGameTime();
        Long prev = finBarrierToggleTicks.get(id);
        if (prev != null && tick >= prev && tick - prev < 5) return;
        finBarrierToggleTicks.put(id, tick);
        if (!finBarrierGuards.add(id)) finBarrierGuards.remove(id);
        updateFinBarrierDisplays(player, finBarrierProgress.getOrDefault(id, 0.0));
    }
    private Location finBarrierLocation(Player player, int i, double progress) {
        FinBarrierGeometry.Pose pose = FinBarrierGeometry.pose(i, progress);
        double yaw = Math.toRadians(player.getLocation().getYaw());
        Location point = player.getLocation().add(
                Math.cos(yaw) * pose.right() - Math.sin(yaw) * pose.forward(),
                pose.up(),
                Math.sin(yaw) * pose.right() + Math.cos(yaw) * pose.forward());
        // Keep the validated formation yaw. The separate upper/lower arm
        // displays articulate vertically; no whole-funnel yaw splay.
        point.setYaw(FinFunnelOrientation.barrierYaw(
                player.getLocation().getYaw(), pose.right(), pose.forward(), i,
                getConfig().getDouble("fin-funnel.yaw-adjustment-degrees", 180.0)));
        return point;
    }
    /**
     * Four ν-fin funnels each render as three synchronized display parts:
     * upper arm, lower arm, and the fixed hinge. The arms open independently
     * in their vertical model plane, so the U tips spread without yawing the
     * entire funnel diagonally across the glass roof.
     * Two folded units remain as ordinary single displays.
     */
    private static final int FIN_BARRIER_DISPLAYS = 14;

    private void updateFinBarrierDisplays(Player player, double progress) {
        UUID id = player.getUniqueId();
        List<ItemDisplay> displays = finBarrierDisplays.get(id);
        boolean rebuild = displays == null || displays.size() != FIN_BARRIER_DISPLAYS;
        if (!rebuild) for (ItemDisplay d : displays) {
            if (!d.isValid() || d.getWorld() != player.getWorld()) { rebuild = true; break; }
        }
        if (rebuild) {
            List<ItemDisplay> old = finBarrierDisplays.remove(id);
            if (old != null) for (ItemDisplay d : old) if (d.isValid()) d.remove();
            List<ItemDisplay> created = new ArrayList<>(FIN_BARRIER_DISPLAYS);
            try {
                for (int slot = 0; slot < FIN_BARRIER_DISPLAYS; slot++) {
                    final int finIndex = slot < 12 ? slot / 3 : slot - 8;
                    final String model = slot >= 12 ? "funnel_drone_1_folded"
                            : "funnel_drone_1_barrier_" + switch (slot % 3) {
                                case 0 -> "upper";
                                case 1 -> "lower";
                                default -> "core";
                            };
                    ItemDisplay display = player.getWorld().spawn(
                            finBarrierLocation(player, finIndex, progress),
                            ItemDisplay.class, d -> {
                        ItemStack stack = new ItemStack(Material.LEATHER_HORSE_ARMOR);
                        ItemMeta meta = stack.getItemMeta();
                        meta.setItemModel(new NamespacedKey("funnel", model));
                        meta.getPersistentDataContainer().set(
                                variantKey, PersistentDataType.INTEGER, 1);
                        stack.setItemMeta(meta);
                        d.setItemStack(stack);
                        d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                        d.setPersistent(false);
                        d.setGravity(false);
                        d.setInvulnerable(true);
                        d.setTeleportDuration(1);
                        d.setInterpolationDuration(1);
                        d.getPersistentDataContainer().set(
                                displayKey, PersistentDataType.BYTE, (byte) 1);
                    });
                    created.add(display);
                }
                finBarrierDisplays.put(id, created);
                displays = created;
            } catch (RuntimeException ex) {
                for (ItemDisplay d : created) if (d.isValid()) d.remove();
                stopFinBarrier(id);
                getLogger().warning("Fin barrier display failed: " + ex.getMessage());
                return;
            }
        }
        float scale = (float) Math.max(0.1, Math.min(3.0,
                getConfig().getDouble("drones.model-scale", 1.45)));
        // Signed: positive opens in the corrected direction; negative reverses.
        double opening = FinTipAngleSetting.effective(
                getConfig().get("fin-barrier.fin-tip-open-degrees"));
        for (int slot = 0; slot < FIN_BARRIER_DISPLAYS; slot++) {
            ItemDisplay display = displays.get(slot);
            int finIndex = slot < 12 ? slot / 3 : slot - 8;
            display.teleport(finBarrierLocation(player, finIndex, progress));
            if (slot < 12 && slot % 3 != 2) {
                boolean upper = slot % 3 == 0;
                FinBarrierGeometry.ArmPose pose = FinBarrierGeometry.armPose(
                        upper, progress, opening, scale);
                display.setTransformation(new Transformation(
                        new Vector3f((float) pose.shiftX(), (float) pose.shiftY(), 0f),
                        new AxisAngle4f((float) Math.toRadians(pose.angleDegrees()),
                                0f, 0f, 1f),
                        new Vector3f(scale, scale, scale), new AxisAngle4f()));
            } else {
                display.setTransformation(new Transformation(
                        new Vector3f(), slot >= 12 ? finFoldRotation() : new AxisAngle4f(),
                        new Vector3f(scale, scale, scale), new AxisAngle4f()));
            }
        }
    }
    private void removeFinGlass(UUID id) {
        List<BlockDisplay> old = finGlassDisplays.remove(id);
        if (old != null) for (BlockDisplay pane : old) if (pane.isValid()) pane.remove();
    }

    private void updateFinGlass(Player player) {
        UUID id = player.getUniqueId();
        if (!getConfig().getBoolean("fin-barrier.glass-enabled", true)) {
            removeFinGlass(id);
            return;
        }
        double thickness = Math.max(0.015, Math.min(0.15,
                getConfig().getDouble("fin-barrier.glass-thickness", 0.035)));
        List<FinBarrierGlassGeometry.Panel> panels = FinBarrierGlassGeometry.panels(thickness);
        List<BlockDisplay> current = finGlassDisplays.get(id);
        boolean respawn = current == null || current.size() != panels.size();
        if (!respawn) for (BlockDisplay pane : current) {
            if (!pane.isValid() || pane.getWorld() != player.getWorld()) {
                respawn = true;
                break;
            }
        }
        if (respawn) {
            removeFinGlass(id);
            List<BlockDisplay> created = new ArrayList<>(panels.size());
            try {
                for (int i = 0; i < panels.size(); i++) {
                    // Do not inherit player yaw/pitch: the block transform
                    // already contains the wall's full player-relative yaw.
                    Location spawn = player.getLocation();
                    spawn.setYaw(0f);
                    spawn.setPitch(0f);
                    BlockDisplay pane = player.getWorld().spawn(
                            spawn, BlockDisplay.class, d -> {
                        d.setBlock(Bukkit.createBlockData(Material.WHITE_STAINED_GLASS));
                        d.setGravity(false);
                        d.setInvulnerable(true);
                        d.setPersistent(false);
                        d.setTeleportDuration(1);
                        d.setInterpolationDuration(1);
                        d.getPersistentDataContainer().set(displayKey,
                                PersistentDataType.BYTE, (byte) 1);
                    });
                    created.add(pane);
                }
                finGlassDisplays.put(id, created);
                current = created;
            } catch (RuntimeException ex) {
                for (BlockDisplay pane : created) if (pane.isValid()) pane.remove();
                removeFinGlass(id);
                getLogger().warning("Cannot create fin-barrier glass: " + ex.getMessage());
                return;
            }
        }
        double yawDegrees = player.getLocation().getYaw();
        // Compute each bottom corner and its model yaw using exactly one
        // coordinate frame. Display entity rotation stays neutral (zero);
        // the model transform supplies the entire rotation around that corner.
        for (int i = 0; i < panels.size(); i++) {
            FinBarrierGlassGeometry.Panel panel = panels.get(i);
            FinBarrierGlassGeometry.Placement pose =
                    FinBarrierGlassGeometry.placement(panel, yawDegrees);
            Location pos = player.getLocation().add(pose.x(), pose.y(), pose.z());
            pos.setYaw(0f);
            pos.setPitch(0f);
            BlockDisplay pane = current.get(i);
            pane.teleport(pos);
            pane.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) pose.modelYawRadians(), 0f, 1f, 0f),
                    new Vector3f((float) panel.sizeRight(),
                            (float) panel.sizeUp(), (float) panel.sizeForward()),
                    new AxisAngle4f()));
        }
    }

    private void drawFinBarrier(Player player) {
        if (animationTick % 4 != 0) return;
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double hw = FinBarrierGeometry.HALF_WIDTH;
        double top = FinBarrierGeometry.TOP;
        Particle.DustOptions mist = new Particle.DustOptions(Color.fromRGB(100, 235, 255), 0.7f);
        for (int ix = -4; ix <= 4; ix++) for (int iz = -4; iz <= 4; iz++) {
            if (Math.abs(ix) % 2 != 0 && Math.abs(iz) % 2 != 0) continue;
            double right = hw * ix / 4.0, forward = hw * iz / 4.0;
            Location pt = player.getLocation().add(
                    Math.cos(yaw) * right - Math.sin(yaw) * forward, top,
                    Math.sin(yaw) * right + Math.cos(yaw) * forward);
            player.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, mist);
        }
    }
    private boolean finBarrierIntersects(Player player, Projectile projectile) {
        Vector motion = projectile.getVelocity();
        if (motion.lengthSquared() < 1.0e-8) return false;
        Location base = player.getLocation();
        double yaw = Math.toRadians(base.getYaw());
        Vector right = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
        Vector front = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector rel = projectile.getLocation().toVector().subtract(base.toVector());
        Vector from = rel.clone().subtract(motion);
        Vector to = rel.clone().add(motion.clone().multiply(1.5));
        return Double.isFinite(FinBarrierGeometry.entryT(
                from.dot(right), from.getY(), from.dot(front),
                to.dot(right), to.getY(), to.dot(front)));
    }
    private void updateFinBarriers() {
        Set<UUID> tracked = new HashSet<>(finBarrierGuards);
        tracked.addAll(finBarrierDisplays.keySet());
        for (UUID id : tracked) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.isDead() || !hasFinOffhand(player)) {
                stopFinBarrier(id);
                continue;
            }
            boolean opening = finBarrierGuards.contains(id);
            double previous = finBarrierProgress.getOrDefault(id, 0.0);
            int duration = Math.max(1, Math.min(60, getConfig().getInt(
                    opening ? "fin-barrier.deploy-ticks" : "fin-barrier.retract-ticks", 10)));
            double progress = FinBarrierGeometry.approach(previous, opening, duration);
            finBarrierProgress.put(id, progress);
            if (!opening && progress <= 0) { stopFinBarrier(id); continue; }
            updateFinBarrierDisplays(player, progress);
            if (!finBarrierActive(player)) {
                removeFinGlass(id);
                continue;
            }
            updateFinGlass(player);
            for (Projectile projectile : player.getWorld().getNearbyEntitiesByType(
                    Projectile.class, player.getLocation(), 4.0, 4.0, 4.0)) {
                if (!projectile.isValid() || projectile.getShooter() == player) continue;
                if (finBarrierIntersects(player, projectile)) interceptProjectile(player, projectile);
            }
        }
    }
    private boolean hasUnicornOffhand(Player player) {
        ItemStack item = player.getInventory().getItemInOffHand();
        if (!isWeapon(item)) return false;
        Integer variant = item.getItemMeta().getPersistentDataContainer()
                .get(variantKey, PersistentDataType.INTEGER);
        return variant != null && variant == 4;
    }
    private boolean shieldActive(Player player) {
        return player.isOnline() && !player.isDead() && hasUnicornOffhand(player)
                && shieldGuards.contains(player.getUniqueId())
                && shieldProgress.getOrDefault(player.getUniqueId(), 0.0) >= 0.999;
    }
    private void removeShieldDisplays(UUID uuid) {
        List<ItemDisplay> displays = shieldDisplays.remove(uuid);
        if (displays == null) return;
        for (ItemDisplay display : displays) if (display.isValid()) display.remove();
    }
    private void stopShield(UUID uuid) {
        shieldGuards.remove(uuid);
        shieldProgress.remove(uuid);
        removeShieldDisplays(uuid);
    }
    private void toggleShield(Player player) {
        if (!hasUnicornOffhand(player)) {
            player.sendMessage("§cユニコーン風シールドファンネルをオフハンドに持ってください。");
            return;
        }
        long tick = player.getWorld().getGameTime();
        Long last = shieldToggleTicks.get(player.getUniqueId());
        if (last != null && tick >= last && tick - last < 5) return;
        shieldToggleTicks.put(player.getUniqueId(), tick);
        if (shieldGuards.contains(player.getUniqueId())) {
            shieldGuards.remove(player.getUniqueId());
            // player.sendActionBar(Component.text("§7シールド防御：収納中"));
            // player.sendMessage("§7シールドファンネルを収納します。");
        } else {
            shieldGuards.add(player.getUniqueId());
            shieldProgress.putIfAbsent(player.getUniqueId(), 0.0);
            // Spawn behind the shoulders, then glide forward over several ticks.
            updateShieldDisplays(player, shieldProgress.get(player.getUniqueId()));
            // player.sendActionBar(Component.text("§bシールド防御：展開中"));
            // player.sendMessage("§b3枚のシールドファンネルを前方に展開します。");
        }
    }
    private Location shieldLocation(Player player, int index, double progress) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        ShieldAnimationGeometry.Pose pose = ShieldAnimationGeometry.pose(index, progress);
        Location pos = player.getLocation().add(
                rightX * pose.right() + forwardX * pose.forward(),
                pose.up(), rightZ * pose.right() + forwardZ * pose.forward());
        // v0.7.1 model was shown back-to-front with +180. Use the opposite side.
        // If a user-defined model swaps faces, this offset is configurable.
        pos.setYaw(player.getLocation().getYaw()
                + (float) getConfig().getDouble("shield.facing-yaw-offset", 0.0)
                + (float) pose.yawOffset());
        pos.setPitch(0);
        return pos;
    }
    private void updateShieldDisplays(Player player, double progress) {
        UUID id = player.getUniqueId();
        List<ItemDisplay> displays = shieldDisplays.get(id);
        boolean respawn = displays == null || displays.size() != 3;
        if (!respawn) for (ItemDisplay d : displays) {
            if (!d.isValid() || d.getWorld() != player.getWorld()) { respawn = true; break; }
        }
        if (respawn) {
            removeShieldDisplays(id);
            List<ItemDisplay> created = new ArrayList<>(3);
            try {
                for (int i = 0; i < 3; i++) {
                    final int index = i;
                    ItemDisplay display = player.getWorld().spawn(shieldLocation(player, index, progress), ItemDisplay.class, d -> {
                        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
                        ItemMeta meta = item.getItemMeta();
                        meta.setItemModel(new NamespacedKey("funnel", "funnel_drone_4"));
                        meta.getPersistentDataContainer().set(variantKey, PersistentDataType.INTEGER, 4);
                        item.setItemMeta(meta);
                        d.setItemStack(item);
                        d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                        float scale = (float) Math.max(0.1, Math.min(3.0,
                                getConfig().getDouble("drones.model-scale", 1.45) * 1.1));
                        d.setTransformation(new Transformation(new Vector3f(0, 0, 0),
                                new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
                        d.setPersistent(false);
                        d.setGravity(false);
                        d.setInvulnerable(true);
                        // One-tick interpolation avoids trailing noticeably behind the moving player.
                        d.setTeleportDuration(1);
                        d.setInterpolationDuration(1);
                        d.getPersistentDataContainer().set(displayKey, PersistentDataType.BYTE, (byte) 1);
                    });
                    created.add(display);
                }
                shieldDisplays.put(id, created);
                displays = created;
            } catch (RuntimeException ex) {
                for (ItemDisplay d : created) if (d.isValid()) d.remove();
                stopShield(id);
                getLogger().warning("Failed to spawn shield funnels: " + ex.getMessage());
                return;
            }
        }
        for (int i = 0; i < 3; i++) {
            ItemDisplay display = displays.get(i);
            ShieldAnimationGeometry.Pose pose = ShieldAnimationGeometry.pose(i, progress);
            display.teleport(shieldLocation(player, i, progress));
            float scale = (float) Math.max(0.1, Math.min(3.0,
                    getConfig().getDouble("drones.model-scale", 1.45) * 1.1));
            display.setTransformation(new Transformation(new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(pose.rollDegrees()), 0f, 0f, 1f),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
        }
    }
    private boolean shieldIntersects(Player player, Projectile projectile) {
        Vector motion = projectile.getVelocity();
        if (motion.lengthSquared() < 1.0e-6) return false;
        Location owner = player.getLocation();
        double yaw = Math.toRadians(owner.getYaw());
        Vector front = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
        Vector relative = projectile.getLocation().toVector().subtract(owner.toVector());
        Vector from = relative.clone().subtract(motion);
        Vector to = relative.clone().add(motion.clone().multiply(1.5));
        return ShieldGuardGeometry.intercepts(
                from.dot(right), from.getY(), from.dot(front),
                to.dot(right), to.getY(), to.dot(front));
    }
    private void interceptProjectile(Player player, Projectile projectile) {
        if (!projectile.isValid()) return;
        Location hit = projectile.getLocation();
        projectile.remove();
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hit, 9, 0.18, 0.18, 0.18, 0.04);
        player.getWorld().playSound(hit, Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.3f);
    }
    private void updateShields() {
        animationTick++;
        updateFinBarriers();
        Set<UUID> tracked = new HashSet<>(shieldGuards);
        tracked.addAll(shieldDisplays.keySet());
        for (UUID uuid : tracked) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || player.isDead() || !hasUnicornOffhand(player)) {
                stopShield(uuid);
                continue;
            }
            boolean opening = shieldGuards.contains(uuid);
            double previous = shieldProgress.getOrDefault(uuid, 0.0);
            int duration = Math.max(1, Math.min(60, getConfig().getInt(
                    opening ? "shield.deploy-ticks" : "shield.retract-ticks", 12)));
            double progress = ShieldAnimationGeometry.approach(previous, opening, duration);
            shieldProgress.put(uuid, progress);
            if (!opening && progress <= 0.0) {
                stopShield(uuid);
                continue;
            }
            updateShieldDisplays(player, progress);
            if (!shieldActive(player)) continue;
            for (Projectile projectile : player.getWorld().getNearbyEntitiesByType(
                    Projectile.class, player.getLocation(), 5.0, 3.5, 5.0)) {
                if (!projectile.isValid() || projectile.getShooter() == player) continue;
                if (shieldIntersects(player, projectile)) interceptProjectile(player, projectile);
            }
        }
        // Remove completed recoil animations; one tick update cadence.
        shotAnimationStart.entrySet().removeIf(entry -> animationTick - entry.getValue() > 8);
        shotAimTargets.keySet().removeIf(uuid -> !shotAnimationStart.containsKey(uuid));
    }
    @EventHandler(ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getDamager() instanceof Projectile projectile)
                || projectile.getShooter() == player) return;
        if (finBarrierActive(player)) {
            if (finBarrierIntersects(player, projectile)
                    || projectile.getLocation().distanceSquared(player.getLocation().add(0, 1.25, 0)) < 3.5) {
                event.setCancelled(true);
                interceptProjectile(player, projectile);
                return;
            }
        }
        if (!shieldActive(player)) return;
        double yaw = Math.toRadians(player.getLocation().getYaw());
        Vector front = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector relative = projectile.getLocation().toVector().subtract(player.getLocation().toVector());
        if (ShieldGuardGeometry.blocksImpact(relative.dot(front), projectile.getVelocity().dot(front))) {
            event.setCancelled(true);
            interceptProjectile(player, projectile);
        }
    }
    private boolean hasController(Player player) {
        return player.isOnline() && !player.isDead() && isWeapon(player.getInventory().getItemInMainHand());
    }
    private int droneCount(int variant) {
        return FormationGeometry.countForVariant(variant);
    }
    private List<ItemDisplay> spawnDrones(Player player) {
        List<ItemDisplay> displays = new ArrayList<>(droneCount(selectedVariant(player)));
        try {
            final int number = selectedVariant(player);
            if (number == 0) return displays;
            for (int index = 0; index < droneCount(number); index++) {
                ItemDisplay d = player.getWorld().spawn(droneLocation(player, number, index), ItemDisplay.class, display -> {
                    ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
                    ItemMeta meta = item.getItemMeta();
                    meta.setItemModel(new NamespacedKey("funnel", number == 1
                            && selectedFinFormation(player) == FinFormationMode.STORED
                            ? "funnel_drone_1_folded" : "funnel_drone_" + number));
                    meta.getPersistentDataContainer().set(variantKey, PersistentDataType.INTEGER, number);
                    item.setItemMeta(meta);
                    display.setItemStack(item);
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    float visualScale = (float)Math.max(0.1, Math.min(3.0, getConfig().getDouble("drones.model-scale", 1.45)));
                    display.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0), new AxisAngle4f(),
                        new Vector3f(visualScale, visualScale, visualScale), new AxisAngle4f()));
                    display.setPersistent(false);
                    display.setGravity(false);
                    display.setInvulnerable(true);
                    display.setTeleportDuration(Math.min(59, Math.max(0, getConfig().getInt("drones.teleport-duration", 2))));
                    display.setInterpolationDuration(2);
                    display.getPersistentDataContainer().set(displayKey, PersistentDataType.BYTE, (byte)1);
                });
                displays.add(d);
            }
        } catch (RuntimeException ex) {
            for (ItemDisplay d : displays) if (d.isValid()) d.remove();
            throw ex;
        }
        return displays;
    }
    private void updateDrones() {
        visualTick += Math.max(1L, getConfig().getLong("drones.update-ticks", 2L));
        for (UUID uuid : new ArrayList<>(activeDrones.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !hasController(p) || !getConfig().getBoolean("drones.enabled", true)
                    || (shieldDisplays.containsKey(uuid) && selectedVariant(p) == 4)) {
                removeDrones(uuid);
            }
        }
        if (!getConfig().getBoolean("drones.enabled", true)) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hasController(p) || (shieldDisplays.containsKey(p.getUniqueId()) && selectedVariant(p) == 4)) continue;
            List<ItemDisplay> displays = activeDrones.get(p.getUniqueId());
            if (displays == null) {
                displays = spawnDrones(p);
                activeDrones.put(p.getUniqueId(), displays);
            }
            for (int i = 0; i < displays.size(); i++) {
                ItemDisplay d = displays.get(i);
                if (!d.isValid() || d.getWorld() != p.getWorld()
                        || displayedVariant(d) != selectedVariant(p)
                        || displays.size() != droneCount(selectedVariant(p))) {
                    removeDrones(p.getUniqueId());
                    break;
                }
                updateDroneModel(p, d);
                d.teleport(droneLocation(p, selectedVariant(p), i));
                updateGunAim(p, d, false);
            }
        }
    }
    private void updateDroneModel(Player player, ItemDisplay display) {
        int variant = selectedVariant(player);
        String name = variant == 1 && selectedFinFormation(player) == FinFormationMode.STORED
                ? "funnel_drone_1_folded" : "funnel_drone_" + variant;
        NamespacedKey expected = new NamespacedKey("funnel", name);
        ItemStack stack = display.getItemStack();
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (expected.equals(meta.getItemModel())) return;
        meta.setItemModel(expected);
        stack.setItemMeta(meta);
        display.setItemStack(stack);
    }

    private AxisAngle4f finFoldRotation() {
        return new AxisAngle4f((float) Math.toRadians(
                getConfig().getDouble("fin-funnel.folded-roll-degrees", 90.0)),
                0f, 0f, 1f);
    }

    /**
     * Variant 1: deployed fin funnel's beam emitter is on local -X.
     * Variant 4: unicorn shield gun barrels are on local -Y.
     * Both snap toward the actual ray-trace result on the click frame,
     * stay aimed through tick 3, and return to the normal formation by tick 8.
     * Stored variant-1 funnels instead rest horizontal and cannot shoot.
     */
    private void updateGunAim(Player player, ItemDisplay display, boolean snapNow) {
        int variant = selectedVariant(player);
        Vector target = (variant == 1 || variant == 4)
                ? shotAimTargets.get(player.getUniqueId()) : null;
        Long start = shotAnimationStart.get(player.getUniqueId());
        float scale = (float) Math.max(0.1, Math.min(3.0,
                getConfig().getDouble("drones.model-scale", 1.45)));
        AxisAngle4f rotation = new AxisAngle4f();
        if (variant == 1 && selectedFinFormation(player) == FinFormationMode.STORED) {
            rotation = finFoldRotation();
        } else if (target != null && start != null) {
            Vector toward = target.clone().subtract(display.getLocation().toVector());
            double weight = FunnelGunAim.aimWeight(animationTick - start);
            if (variant == 1) {
                FinFunnelGunAim.Rotation aim = FinFunnelGunAim.toward(
                        toward.getX(), toward.getY(), toward.getZ(),
                        display.getLocation().getYaw());
                rotation = new AxisAngle4f((float) (aim.radians() * weight),
                        (float) aim.axisX(), (float) aim.axisY(), (float) aim.axisZ());
            } else if (variant == 4) {
                FunnelGunAim.Rotation aim = FunnelGunAim.toward(
                        toward.getX(), toward.getY(), toward.getZ(),
                        display.getLocation().getYaw());
                rotation = new AxisAngle4f((float) (aim.radians() * weight),
                        (float) aim.axisX(), (float) aim.axisY(), (float) aim.axisZ());
            }
        }
        display.setInterpolationDuration(snapNow ? 0 : 2);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0), rotation,
                new Vector3f(scale, scale, scale), new AxisAngle4f()));
    }
    private Location droneLocation(Player p, int variant, int index) {
        double speed = getConfig().getDouble("drones.angular-speed", 0.055);
        double radius = getConfig().getDouble("drones.orbit-radius", 1.65);
        double height = getConfig().getDouble("drones.height", 1.55);
        double bob = getConfig().getDouble("drones.bob-amplitude", 0.22);
        FormationGeometry.Position pos = variant == 1
                ? FinFormationGeometry.position(selectedFinFormation(p), index,
                        visualTick, speed, radius, height, bob)
                : FormationGeometry.position(selectedFormation(p), variant, index,
                        visualTick, speed, radius, height, bob);
        double yaw = Math.toRadians(p.getLocation().getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        Location loc = p.getLocation().add(
                rightX * pos.right() + forwardX * pos.forward(),
                pos.up(),
                rightZ * pos.right() + forwardZ * pos.forward());
        Long shotStart = shotAnimationStart.get(p.getUniqueId());
        if (shotStart != null) {
            double pulse = ShieldAnimationGeometry.shotPulse(animationTick - shotStart);
            loc.add(forwardX * pulse * getConfig().getDouble("drones.shot-recoil-blocks", 0.38),
                    pulse * 0.12,
                    forwardZ * pulse * getConfig().getDouble("drones.shot-recoil-blocks", 0.38));
        }
        if (variant == 1) {
            loc.setYaw(FinFunnelOrientation.finFormationYaw(
                    p.getLocation().getYaw(), selectedFinFormation(p),
                    pos.right(), pos.forward(),
                    getConfig().getDouble("fin-funnel.yaw-adjustment-degrees", 0.0)));
        } else {
            loc.setYaw(p.getLocation().getYaw() + (float) pos.yawOffset());
        }
        loc.setPitch(variant == 4 ? -12f : 0f);
        return loc;
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        stopShield(e.getPlayer().getUniqueId());
        stopFinBarrier(e.getPlayer().getUniqueId());
        shieldToggleTicks.remove(e.getPlayer().getUniqueId());
        shotAnimationStart.remove(e.getPlayer().getUniqueId());
        shotAimTargets.remove(e.getPlayer().getUniqueId());
        removeDrones(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onChangedWorld(PlayerChangedWorldEvent e) {
        stopFinBarrier(e.getPlayer().getUniqueId());
        stopShield(e.getPlayer().getUniqueId());
        removeDrones(e.getPlayer().getUniqueId());
        shotAnimationStart.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onDeath(PlayerDeathEvent e) {
        stopFinBarrier(e.getEntity().getUniqueId());
        stopShield(e.getEntity().getUniqueId());
        removeDrones(e.getEntity().getUniqueId());
        shotAnimationStart.remove(e.getEntity().getUniqueId());
        shotAimTargets.remove(e.getEntity().getUniqueId());
    }
    @EventHandler public void onPack(PlayerResourcePackStatusEvent e) {
        // getLogger().info("Resource pack " + e.getPlayer().getName() + ": " + e.getStatus());
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> { /* e.getPlayer().sendMessage("§a[FunnelArmor] リソースパックの適用に成功しました。"); */ }
            case FAILED_DOWNLOAD -> {
                e.getPlayer().sendMessage("§c[FunnelArmor] リソースパックのダウンロードに失敗しました。/funnel status のURLとクライアントのlogs/latest.logを確認してください。");
                getLogger().warning("Resource-pack download failed for " + e.getPlayer().getName()
                        + ". Check public URL with /funnel status, and ask the player for client latest.log.");
            }
            case DECLINED -> e.getPlayer().sendMessage("§e[FunnelArmor] リソースパックが拒否されました。サーバー一覧のリソースパック設定を確認してください。");
            default -> { }
        }
    }
    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            if (hasFinOffhand(event.getPlayer())) {
                event.setCancelled(true);
                toggleFinBarrier(event.getPlayer());
                return;
            }
            if (!hasUnicornOffhand(event.getPlayer())) return;
            event.setCancelled(true);
            toggleShield(event.getPlayer());
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND ||
                !isWeapon(event.getPlayer().getInventory().getItemInMainHand())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            if (selectedVariant(player) == 1)
                changeFinFormation(player, selectedFinFormation(player).next());
            else changeFormation(player, selectedFormation(player).next());
            return;
        }
        if (selectedVariant(player) == 1 && selectedFinFormation(player) == FinFormationMode.STORED) return;
        long tick = player.getWorld().getGameTime();
        int cooldown = Math.max(1, getConfig().getInt("beam.cooldown-ticks", 12));
        Long previous = lastShots.get(player.getUniqueId());
        if (previous != null && tick >= previous && tick - previous < cooldown) return;
        lastShots.put(player.getUniqueId(), tick);
        shotAnimationStart.put(player.getUniqueId(), animationTick);
        fire(player);
    }
    private double firstBeamShieldHit(Player shooter, Location eye, Vector dir, double length) {
        double closest = Double.POSITIVE_INFINITY;
        if (length <= 0) return closest;
        Vector a = eye.toVector();
        Vector b = a.clone().add(dir.clone().multiply(length));
        for (Player owner : Bukkit.getOnlinePlayers()) {
            if (owner == shooter || owner.getWorld() != shooter.getWorld() || owner.isDead()) continue;
            Location origin = owner.getLocation();
            double yaw = Math.toRadians(origin.getYaw());
            Vector right = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
            Vector front = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
            Vector ra = a.clone().subtract(origin.toVector());
            Vector rb = b.clone().subtract(origin.toVector());
            double aR = ra.dot(right), aF = ra.dot(front), aY = ra.getY();
            double bR = rb.dot(right), bF = rb.dot(front), bY = rb.getY();
            if (shieldActive(owner) && ShieldGuardGeometry.intercepts(aR, aY, aF, bR, bY, bF)) {
                double t = (aF - ShieldGuardGeometry.PLANE_DISTANCE) / (aF - bF);
                closest = Math.min(closest, t * length);
            }
            if (finBarrierActive(owner)) {
                double t = FinBarrierGeometry.entryT(aR, aY, aF, bR, bY, bF);
                if (Double.isFinite(t)) closest = Math.min(closest, t * length);
            }
        }
        return closest;
    }

    private void fire(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double range = getConfig().getDouble("beam.range", 32.0);
        RayTraceResult blockHit = world.rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);
        double blockDist = blockHit == null ? range : Math.sqrt(eye.toVector().distanceSquared(blockHit.getHitPosition()));
        RayTraceResult entityHit = world.rayTraceEntities(eye, dir, blockDist, 0.3,
                entity -> entity instanceof LivingEntity && entity != player && !entity.isDead());
        double dist = entityHit == null ? blockDist : eye.toVector().distance(entityHit.getHitPosition());
        double interception = firstBeamShieldHit(player, eye, dir, dist);
        boolean blockedByBarrier = Double.isFinite(interception) && interception <= dist + 1.0e-6;
        if (blockedByBarrier) dist = Math.max(0, interception);
        Vector target = eye.toVector().add(dir.clone().multiply(dist));
        if (selectedVariant(player) == 1 || selectedVariant(player) == 4) {
            shotAimTargets.put(player.getUniqueId(), target.clone());
            // Aim immediately during the click rather than waiting for the
            // normal 2-tick drone position refresh.
            List<ItemDisplay> firingDisplays = activeDrones.get(player.getUniqueId());
            if (firingDisplays != null) {
                for (ItemDisplay display : firingDisplays) {
                    if (display.isValid() && display.getWorld() == player.getWorld()) {
                        updateGunAim(player, display, true);
                    }
                }
            }
        } else {
            shotAimTargets.remove(player.getUniqueId());
        }
        double step = Math.max(0.1, getConfig().getDouble("beam.particle-step", 0.45));
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(75,224,255), 1.1f);
        List<ItemDisplay> drones = activeDrones.get(player.getUniqueId());
        int variant = selectedVariant(player);
        int count = droneCount(variant);
        for (int i = 0; i < count; i++) {
            Vector start = drones != null && drones.size() == count && drones.get(i).isValid()
                ? drones.get(i).getLocation().toVector()
                : droneLocation(player, variant, i).toVector();
            Vector beam = target.clone().subtract(start);
            double len = beam.length();
            for (double t=0; t<=len; t+=step) {
                Vector point = start.clone().add(beam.clone().multiply(t/Math.max(len,0.0001)));
                world.spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(), 1, 0,0,0,0, dust);
                if (t > 60) break;
            }
        }
        world.spawnParticle(Particle.END_ROD,target.getX(),target.getY(),target.getZ(),8,0.15,0.15,0.15,0.02);
        world.playSound(eye, Sound.ENTITY_GUARDIAN_ATTACK, 0.45f, 1.65f);
        if (blockedByBarrier) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, target.getX(), target.getY(), target.getZ(),
                    10, 0.12, 0.12, 0.12, 0.04);
            world.playSound(target.toLocation(world), Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.35f);
            return;
        }
        Entity hit = entityHit == null ? null : entityHit.getHitEntity();
        if (hit instanceof LivingEntity living) living.damage(
                Math.min(0.5, Math.max(0, getConfig().getDouble("beam.damage", 0.5))), player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("funnel")) return List.of();
        // Never disclose admin-only actions or offer player targets to non-admins.
        List<String> online = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName).toList();
        return FunnelTabSuggestions.complete(args, sender.hasPermission("funnel.admin"),
                sender instanceof Player, sender instanceof Player p && selectedVariant(p) == 1, online);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sender.sendMessage("§b/funnel give <1-4|all> [player] §7| §b/funnel formation <orbit|wings|rear|front|spiral|next> §7| §b/funnel pack [player] §7| §b/funnel shield §7| §b/funnel barrier §7| §b/funnel angle <-15..15> §7| §b/funnel reload §7| §b/funnel status"); return true; }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "give" -> {
                if (!sender.hasPermission("funnel.admin")) { sender.sendMessage("§c権限がありません。"); return true; }
                int variant = 1;
                boolean all = args.length > 1 && args[1].equalsIgnoreCase("all");
                if (args.length > 1 && !all) {
                    try { variant = Integer.parseInt(args[1]); }
                    catch (NumberFormatException ex) {
                        sender.sendMessage("§c指定: /funnel give <1-4|all> [player]");
                        return true;
                    }
                    if (variant < 1 || variant > 4) {
                        sender.sendMessage("§cファンネル番号は1～4です。");
                        return true;
                    }
                }
                Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player p ? p : null;
                if (target == null) { sender.sendMessage("§cプレイヤーが見つかりません。"); return true; }
                if (all) {
                    for (int n = 1; n <= 4; n++) target.getInventory().addItem(weapon(n));
                    sender.sendMessage("§a" + target.getName() + " に4種類のファンネルを渡しました。");
                } else {
                    target.getInventory().addItem(weapon(variant));
                    sender.sendMessage("§a" + target.getName() + " にファンネル" + variant + "を渡しました。");
                }
            }
            case "formation" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c展開モード変更はプレイヤーのみ利用できます。");
                    return true;
                }
                if (!hasController(player)) {
                    sender.sendMessage("§cファンネルをメインハンドに持ってください。");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§b現在の展開: §f" + (selectedVariant(player) == 1
                            ? selectedFinFormation(player).displayName() : selectedFormation(player).displayName()));
                    sender.sendMessage(selectedVariant(player) == 1
                            ? "§7/stored 格納 /orbit 周回 /wings 左右展開 /rear 背面 /front 前方 /spiral 螺旋"
                            : "§7/orbit 周回 /wings 左右展開 /rear 背面 /front 前方 /spiral 螺旋");
                    sender.sendMessage("§7/funnel formation <orbit|wings|rear|front|spiral|next>");
                    return true;
                }
                if (selectedVariant(player) == 1) {
                    FinFormationMode mode = args[1].equalsIgnoreCase("next")
                            ? selectedFinFormation(player).next() : FinFormationMode.parse(args[1]);
                    if (mode == null) {
                        sender.sendMessage("§c不明なフィン・ファンネル陣形です。stored / orbit / wings / rear / front / spiral");
                        return true;
                    }
                    changeFinFormation(player, mode);
                } else {
                    FormationMode mode = args[1].equalsIgnoreCase("next")
                            ? selectedFormation(player).next() : FormationMode.parse(args[1]);
                    if (mode == null) {
                        sender.sendMessage("§c不明な展開モードです。orbit / wings / rear / front / spiral");
                        return true;
                    }
                    changeFormation(player, mode);
                }
            }
            case "barrier" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cバリアはプレイヤー専用です。");
                    return true;
                }
                toggleFinBarrier(player);
            }
            case "shield" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cシールド防御はプレイヤー専用です。");
                    return true;
                }
                toggleShield(player);
            }
            case "pack" -> {
                Player target;
                if (args.length > 1) {
                    if (!sender.hasPermission("funnel.admin")) { sender.sendMessage("§c権限がありません。"); return true; }
                    target = Bukkit.getPlayerExact(args[1]);
                } else target = sender instanceof Player p ? p : null;
                if (target == null) { sender.sendMessage("§c対象プレイヤーが見つかりません。"); return true; }
                applyPack(target);
            }
            case "angle" -> {
                if (!sender.hasPermission("funnel.admin")) {
                    sender.sendMessage("§c権限がありません。");
                    return true;
                }
                if (args.length == 1) {
                    Object raw = getConfig().get("fin-barrier.fin-tip-open-degrees");
                    sender.sendMessage("§b現在のフィン先端角度: §f"
                            + FinTipAngleSetting.effective(raw)
                            + "° §7(config: " + raw + ")");
                    sender.sendMessage("§7/funnel angle <-15..15> で変更・保存できます。");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage("§c使用法: /funnel angle <-15..15>");
                    return true;
                }
                final double value;
                try {
                    value = FinTipAngleSetting.validate(args[1]);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("§c角度は -15～15 の数値で指定してください。例: /funnel angle -6");
                    return true;
                }
                getConfig().set("fin-barrier.fin-tip-open-degrees", value);
                saveConfig();
                sender.sendMessage("§aフィン先端角度を §f" + value
                        + "° §aに変更・保存しました（展開中のバリアにも反映）。");
            }
            case "reload" -> {
                if (!sender.hasPermission("funnel.admin")) { sender.sendMessage("§c権限がありません。"); return true; }
                reloadConfig(); startHttp();
                Object raw = getConfig().get("fin-barrier.fin-tip-open-degrees");
                sender.sendMessage("§a設定を再読み込みしました。フィン先端角度: §f"
                        + FinTipAngleSetting.effective(raw) + "° §7(config: " + raw + ")");
                sender.sendMessage("§7モデル変更はJAR更新とサーバー再起動が必要です。");
            }
            case "status" -> {
                sender.sendMessage("§bFunnelArmor version: §f" + getPluginMeta().getVersion());
                Object raw = getConfig().get("fin-barrier.fin-tip-open-degrees");
                sender.sendMessage("§bFin tip angle: §f" + FinTipAngleSetting.effective(raw)
                        + "° §7(config: " + raw + ")");
                sender.sendMessage("§bPack SHA1: §f" + sha1);
                sender.sendMessage("§bPack URL: §f" + (sender instanceof Player p ? publicUrl(p) : publicUrl()));
                sender.sendMessage("§bConfigured public host: §f" + getConfig().getString("pack.embedded-http.public-host", "auto"));
                sender.sendMessage("§bPack bytes: §f" + (packBytes == null ? 0 : packBytes.length));
                sender.sendMessage("§bEmbedded HTTP: §f" + (httpServer == null ? "OFF" : "ON"));
                sender.sendMessage("§bPack file: §f" + packPath);
            }
            default -> sender.sendMessage("§c不明なサブコマンドです。");
        }
        return true;
    }
}
