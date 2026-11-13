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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;
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
import org.bukkit.event.player.PlayerJoinEvent;
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
import java.util.ArrayList;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FunnelArmorPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey itemKey;
    private NamespacedKey displayKey;
    private NamespacedKey variantKey;
    private NamespacedKey formationKey;
    private final Map<UUID, List<ItemDisplay>> activeDrones = new ConcurrentHashMap<>();
    private long visualTick = 0;
    private final Map<UUID, Long> lastShots = new ConcurrentHashMap<>();
    private final Set<UUID> shieldGuards = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<ItemDisplay>> shieldDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, Long> shieldToggleTicks = new ConcurrentHashMap<>();
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
        if (getCommand("funnel") != null) getCommand("funnel").setExecutor(this);
        getLogger().info("FunnelArmor 0.7.1 ready. Built-in resource pack SHA1: " + sha1);
    }
    @Override public void onDisable() {
        if (httpServer != null) httpServer.stop(0);
        lastShots.clear();
        shieldToggleTicks.clear();
        for (UUID uuid : new ArrayList<>(shieldGuards)) stopShield(uuid);
        for (UUID uuid : new ArrayList<>(shieldDisplays.keySet())) removeShieldDisplays(uuid);
        for (UUID uuid : new ArrayList<>(activeDrones.keySet())) removeDrones(uuid);
    }
    private void startHttp() {
        if (httpServer != null) httpServer.stop(0);
        httpServer = null;
        if (!getConfig().getBoolean("pack.embedded-http.enabled", true) || packBytes == null) return;
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
    private String publicUrl() {
        return publicUrl(null);
    }
    private String publicUrl(Player player) {
        String external = getConfig().getString("pack.external-url", "").trim();
        if (!external.isEmpty()) return external;
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
        if (getConfig().getString("pack.external-url", "").isBlank() && httpServer == null) {
            player.sendMessage("§cパック配信サーバーが起動していません。plugins/FunnelArmor/FunnelArmor-pack.zip を手動適用してください。");
            return false;
        }
        String url = publicUrl(player);
        player.setResourcePack(url, sha1, getConfig().getBoolean("pack.require", false),
                Component.text("ファンネル用3Dモデルを表示するリソースパックです。"));
        player.sendMessage("§bファンネルのパックを要求しました: §7" + url);
        getLogger().info("Requested resource pack for " + player.getName() + " from " + url);
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
        meta.lore(java.util.List.of(Component.text("§7右クリック: ビーム攻撃"),Component.text("§7周囲に " + droneCount(variant) + " 基展開"),Component.text("§7展開：周回"),Component.text("§7しゃがみ＋右クリックで展開切替"),Component.text("§7射程32 / ダメージ2HP")));
        meta.setItemModel(new NamespacedKey("funnel", "funnel_drone_" + variant));
        meta.getPersistentDataContainer().set(variantKey, PersistentDataType.INTEGER, variant);
        meta.getPersistentDataContainer().set(formationKey, PersistentDataType.STRING, FormationMode.ORBIT.id());
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
                Component.text("§7射程32 / ダメージ2HP")));
        stack.setItemMeta(meta);
        player.getInventory().setItemInMainHand(stack);
        player.sendActionBar(Component.text("§b[FunnelArmor] 展開：" + mode.displayName()));
        player.sendMessage("§bファンネル展開モード: §f" + mode.displayName());
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
        }
    }
    private void removeDrones(UUID uuid) {
        List<ItemDisplay> displays = activeDrones.remove(uuid);
        if (displays == null) return;
        for (ItemDisplay d : displays) if (d.isValid()) d.remove();
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
                && shieldGuards.contains(player.getUniqueId());
    }
    private void removeShieldDisplays(UUID uuid) {
        List<ItemDisplay> displays = shieldDisplays.remove(uuid);
        if (displays == null) return;
        for (ItemDisplay display : displays) if (display.isValid()) display.remove();
    }
    private void stopShield(UUID uuid) {
        shieldGuards.remove(uuid);
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
            stopShield(player.getUniqueId());
            player.sendActionBar(Component.text("§7シールド防御：解除"));
            player.sendMessage("§7シールドファンネル防御を解除しました。");
        } else {
            shieldGuards.add(player.getUniqueId());
            // Spawn the defensive formation immediately; the 1-tick task updates it.
            updateShieldDisplays(player);
            player.sendActionBar(Component.text("§bシールド防御：展開"));
            player.sendMessage("§b3枚のシールドファンネルを前方に展開しました。矢などを防御します。");
        }
    }
    private Location shieldLocation(Player player, int index) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double right = (index - 1) * 1.08;
        double up = index == 1 ? 1.50 : 1.32;
        Location pos = player.getLocation().add(
                rightX * right + forwardX * ShieldGuardGeometry.PLANE_DISTANCE,
                up, rightZ * right + forwardZ * ShieldGuardGeometry.PLANE_DISTANCE);
        // The model's north/front surface points toward the incoming shot.
        pos.setYaw(player.getLocation().getYaw() + 180f);
        pos.setPitch(0);
        return pos;
    }
    private void updateShieldDisplays(Player player) {
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
                    ItemDisplay display = player.getWorld().spawn(shieldLocation(player, index), ItemDisplay.class, d -> {
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
                        d.setTeleportDuration(Math.min(59, Math.max(0,
                                getConfig().getInt("drones.teleport-duration", 2))));
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
        for (int i = 0; i < 3; i++) displays.get(i).teleport(shieldLocation(player, i));
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
        for (UUID uuid : new ArrayList<>(shieldGuards)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !shieldActive(player)) {
                stopShield(uuid);
                continue;
            }
            updateShieldDisplays(player);
            if (!shieldGuards.contains(uuid)) continue;
            for (Projectile projectile : player.getWorld().getNearbyEntitiesByType(
                    Projectile.class, player.getLocation(), 5.0, 3.5, 5.0)) {
                if (!projectile.isValid() || projectile.getShooter() == player) continue;
                if (shieldIntersects(player, projectile)) interceptProjectile(player, projectile);
            }
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getDamager() instanceof Projectile projectile)
                || !shieldActive(player) || projectile.getShooter() == player) return;
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
                    meta.setItemModel(new NamespacedKey("funnel", "funnel_drone_" + number));
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
                    || (shieldGuards.contains(uuid) && selectedVariant(p) == 4)) {
                removeDrones(uuid);
            }
        }
        if (!getConfig().getBoolean("drones.enabled", true)) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hasController(p) || (shieldGuards.contains(p.getUniqueId()) && selectedVariant(p) == 4)) continue;
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
                d.teleport(droneLocation(p, selectedVariant(p), i));
            }
        }
    }
    private Location droneLocation(Player p, int variant, int index) {
        FormationGeometry.Position pos = FormationGeometry.position(
                selectedFormation(p), variant, index, visualTick,
                getConfig().getDouble("drones.angular-speed", 0.055),
                getConfig().getDouble("drones.orbit-radius", 1.65),
                getConfig().getDouble("drones.height", 1.55),
                getConfig().getDouble("drones.bob-amplitude", 0.22));
        double yaw = Math.toRadians(p.getLocation().getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        Location loc = p.getLocation().add(
                rightX * pos.right() + forwardX * pos.forward(),
                pos.up(),
                rightZ * pos.right() + forwardZ * pos.forward());
        loc.setYaw(p.getLocation().getYaw() + (float) pos.yawOffset());
        loc.setPitch(variant == 4 ? -12f : 0f);
        return loc;
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        stopShield(e.getPlayer().getUniqueId());
        shieldToggleTicks.remove(e.getPlayer().getUniqueId());
        removeDrones(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onChangedWorld(PlayerChangedWorldEvent e) {
        stopShield(e.getPlayer().getUniqueId());
        removeDrones(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onDeath(PlayerDeathEvent e) {
        stopShield(e.getEntity().getUniqueId());
        removeDrones(e.getEntity().getUniqueId());
    }
    @EventHandler public void onJoin(PlayerJoinEvent e) {
        if (getConfig().getBoolean("pack.send-on-join", true))
            Bukkit.getScheduler().runTaskLater(this, () -> { if (e.getPlayer().isOnline()) applyPack(e.getPlayer()); }, 30L);
    }
    @EventHandler public void onPack(PlayerResourcePackStatusEvent e) {
        getLogger().info("Resource pack " + e.getPlayer().getName() + ": " + e.getStatus());
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> e.getPlayer().sendMessage("§a[FunnelArmor] リソースパックの適用に成功しました。");
            case FAILED_DOWNLOAD -> e.getPlayer().sendMessage("§c[FunnelArmor] リソースパックのダウンロードに失敗しました。/funnel status の配布URLと8765/TCPの接続を確認してください。");
            case DECLINED -> e.getPlayer().sendMessage("§e[FunnelArmor] リソースパックが拒否されました。サーバー一覧のリソースパック設定を確認してください。");
            default -> { }
        }
    }
    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
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
            changeFormation(player, selectedFormation(player).next());
            return;
        }
        long tick = player.getWorld().getGameTime();
        int cooldown = Math.max(1, getConfig().getInt("beam.cooldown-ticks", 12));
        Long previous = lastShots.get(player.getUniqueId());
        if (previous != null && tick >= previous && tick - previous < cooldown) return;
        lastShots.put(player.getUniqueId(), tick);
        fire(player);
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
        Vector target = eye.toVector().add(dir.clone().multiply(dist));
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
        Entity hit = entityHit == null ? null : entityHit.getHitEntity();
        if (hit instanceof LivingEntity living) living.damage(Math.max(0, getConfig().getDouble("beam.damage",2.0)), player);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sender.sendMessage("§b/funnel give <1-4|all> [player] §7| §b/funnel formation <orbit|wings|rear|front|spiral|next> §7| §b/funnel pack [player] §7| §b/funnel shield §7| §b/funnel reload §7| §b/funnel status"); return true; }
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
                    sender.sendMessage("§b現在の展開: §f" + selectedFormation(player).displayName());
                    sender.sendMessage("§7/orbit 周回 /wings 左右展開 /rear 背面 /front 前方 /spiral 螺旋");
                    sender.sendMessage("§7/funnel formation <orbit|wings|rear|front|spiral|next>");
                    return true;
                }
                FormationMode mode = args[1].equalsIgnoreCase("next")
                        ? selectedFormation(player).next() : FormationMode.parse(args[1]);
                if (mode == null) {
                    sender.sendMessage("§c不明な展開モードです。orbit / wings / rear / front / spiral");
                    return true;
                }
                changeFormation(player, mode);
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
            case "reload" -> {
                if (!sender.hasPermission("funnel.admin")) { sender.sendMessage("§c権限がありません。"); return true; }
                reloadConfig(); startHttp(); sender.sendMessage("§a設定を再読み込みしました。モデル変更はJAR更新とサーバー再起動が必要です。");
            }
            case "status" -> {
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
