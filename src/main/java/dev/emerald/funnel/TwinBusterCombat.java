package dev.emerald.funnel;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Original, no-client-mod Twin Buster Rifle prototype. No overlap with funnel controllers. */
final class TwinBusterCombat implements Listener {
    private enum Mode { DUAL, COMBINED }
    private enum Side { LEFT, RIGHT }
    private record Charge(String weaponId, UUID worldId, long started) {}

    private final JavaPlugin plugin;
    private final NamespacedKey weaponKey;
    private final NamespacedKey modeKey;
    private final NamespacedKey identityKey;
    private final Map<UUID, Charge> charging = new HashMap<>();
    private final Map<UUID, Long> combinedCooldowns = new HashMap<>();
    private final Map<UUID, Long> leftCooldowns = new HashMap<>();
    private final Map<UUID, Long> rightCooldowns = new HashMap<>();
    /** Client-visible left gun is a temporary server-owned item in the empty offhand. */
    private final NamespacedKey visualKey;
    private ItemStack virtualLeftRifle;
    private final Map<UUID, Long> modeCooldowns = new HashMap<>();

    TwinBusterCombat(JavaPlugin plugin) {
        this.plugin = plugin;
        weaponKey = new NamespacedKey(plugin, "twin_buster_weapon");
        modeKey = new NamespacedKey(plugin, "twin_buster_mode");
        identityKey = new NamespacedKey(plugin, "twin_buster_identity");
        visualKey = new NamespacedKey(plugin, "twin_buster_display_only");
    }

    void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        virtualLeftRifle = new ItemStack(Material.WARPED_FUNGUS_ON_A_STICK);
        ItemMeta visual = virtualLeftRifle.getItemMeta();
        visual.setUnbreakable(true);
        visual.setItemModel(new NamespacedKey("funnel", "twin_buster_rifle_dual_left"));
        visual.displayName(Component.text("§7左手ライフル（表示専用）"));
        visual.getPersistentDataContainer().set(visualKey, PersistentDataType.BYTE, (byte) 1);
        virtualLeftRifle.setItemMeta(visual);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVisual(player.getInventory().getItemInOffHand())) removeLeftVisual(player);
            syncLeftHand(player);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, 1L);
    }

    void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) removeLeftVisual(player);
        charging.clear();
        combinedCooldowns.clear();
        leftCooldowns.clear();
        rightCooldowns.clear();
        modeCooldowns.clear();
    }

    private long tick() { return Bukkit.getCurrentTick(); }

    private int cfg(String name, int fallback) {
        return Math.max(1, plugin.getConfig().getInt("twin-buster." + name, fallback));
    }

    private double cfg(String name, double fallback) {
        double value = plugin.getConfig().getDouble("twin-buster." + name, fallback);
        return Double.isFinite(value) && value > 0 ? value : fallback;
    }

    private boolean isVisual(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta()
                .getPersistentDataContainer().has(visualKey, PersistentDataType.BYTE);
    }

    private void removeLeftVisual(Player player) {
        if (isVisual(player.getInventory().getItemInOffHand()))
            player.getInventory().setItemInOffHand(null);
    }

    private void syncLeftHand(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        // Warped fungus on a stick triggers vanilla right-hand use animations.
        // Migrate older plugin-only controllers to an inert FEATHER, preserving metadata.
        if (weapon(main) && main.getType() == Material.WARPED_FUNGUS_ON_A_STICK) {
            ItemStack migrated = new ItemStack(Material.FEATHER);
            migrated.setItemMeta(main.getItemMeta());
            player.getInventory().setItemInMainHand(migrated);
            main = migrated;
        }
        boolean dual = plugin.getConfig().getBoolean("twin-buster.enabled", true)
                && !player.isDead() && weapon(main) && mode(main) == Mode.DUAL;
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (dual && offhand.getType().isAir())
            player.getInventory().setItemInOffHand(virtualLeftRifle.clone());
        else if (!dual) removeLeftVisual(player);
    }

    private boolean weapon(ItemStack item) {
        return item != null && (item.getType() == Material.FEATHER
                || item.getType() == Material.WARPED_FUNGUS_ON_A_STICK)
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(weaponKey, PersistentDataType.BYTE);
    }

    private String weaponId(ItemStack item) {
        if (!weapon(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(identityKey, PersistentDataType.STRING);
    }

    private Mode mode(ItemStack item) {
        if (!weapon(item)) return Mode.DUAL;
        String value = item.getItemMeta().getPersistentDataContainer()
                .get(modeKey, PersistentDataType.STRING);
        return "combined".equals(value) ? Mode.COMBINED : Mode.DUAL;
    }

    ItemStack giftItem() { return newWeapon(); }

    private ItemStack newWeapon() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        applyMode(meta, Mode.DUAL);
        item.setItemMeta(meta);
        return item;
    }

    private void applyMode(ItemMeta meta, Mode mode) {
        meta.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING,
                mode == Mode.DUAL ? "dual" : "combined");
        meta.setItemModel(new NamespacedKey("funnel", mode == Mode.DUAL
                ? "twin_buster_rifle_dual" : "twin_buster_rifle_combined"));
        meta.displayName(Component.text("§bツインバスターライフル " +
                (mode == Mode.DUAL ? "§7【分離】" : "§e【連結】")));
        meta.lore(List.of(
                Component.text(mode == Mode.DUAL
                        ? "§7左クリック: 左ライフル / 右クリック: 右ライフル"
                        : "§7右クリック: チャージして極太ビーム"),
                Component.text("§7しゃがみ＋右クリック: 分離／連結切替"),
                Component.text("§8ウイングゼロ風・独立兵装")));
    }

    boolean command(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage("§b/funnel buster status §7| 配布: /funnel give buster [player]");
            return true;
        }
        if (args[1].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§c状態確認はプレイヤー専用です。");
                return true;
            }
            ItemStack item = p.getInventory().getItemInMainHand();
            if (!weapon(item)) {
                sender.sendMessage("§7ツインバスターライフルをメインハンドに持ってください。");
                return true;
            }
            long remaining = Math.max(0L, combinedCooldowns.getOrDefault(
                    p.getUniqueId(), 0L) - tick());
            sender.sendMessage("§bツインバスターライフル: §f"
                    + (mode(item) == Mode.DUAL ? "分離" : "連結")
                    + " §7/ チャージ "
                    + (charging.containsKey(p.getUniqueId()) ? "進行中" : "なし")
                    + " §7/ 冷却 " + (remaining + 19) / 20 + "秒");
            return true;
        }
        sender.sendMessage("§7/funnel buster status §7| 配布: /funnel give buster [player]");
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        boolean leftClick = event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK;
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (!leftClick && !rightClick) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!weapon(item)) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        if (!plugin.getConfig().getBoolean("twin-buster.enabled", true)) return;
        UUID id = player.getUniqueId();
        long now = tick();
        if (leftClick) {
            if (mode(item) == Mode.DUAL && !player.isSneaking()
                    && TwinBusterTiming.ready(now, modeCooldowns.getOrDefault(id, 0L)))
                shootSide(player, Side.LEFT);
            return;
        }
        if (player.isSneaking()) {
            if (!TwinBusterTiming.ready(now, modeCooldowns.getOrDefault(id, 0L))) return;
            charging.remove(id);
            removeLeftVisual(player);
            Mode next = mode(item) == Mode.DUAL ? Mode.COMBINED : Mode.DUAL;
            ItemMeta meta = item.getItemMeta();
            applyMode(meta, next);
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            syncLeftHand(player);
            modeCooldowns.put(id, TwinBusterTiming.until(now, cfg("mode-switch-ticks", 10)));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, .6f, 1.25f);
            return;
        }
        if (!TwinBusterTiming.ready(now, modeCooldowns.getOrDefault(id, 0L))) return;
        if (mode(item) == Mode.DUAL) {
            shootSide(player, Side.RIGHT);
        } else {
            if (charging.containsKey(id)) return;
            long until = combinedCooldowns.getOrDefault(id, 0L);
            if (!TwinBusterTiming.ready(now, until)) {
                player.sendActionBar(Component.text("§cツインバスターライフル冷却中: "
                        + ((until - now + 19) / 20) + "秒"));
                return;
            }
            charging.put(id, new Charge(weaponId(item), player.getWorld().getUID(), now));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, .6f, .8f);
            player.sendActionBar(Component.text("§bツインバスターライフル：エネルギーチャージ開始"));
        }
    }

    /** Paper reports which arm swung; never re-trigger from the generated left-arm swing. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!weapon(item) || mode(item) != Mode.DUAL
                || !plugin.getConfig().getBoolean("twin-buster.enabled", true)) return;
        if (!TwinBusterTiming.ready(tick(),
                modeCooldowns.getOrDefault(player.getUniqueId(), 0L))) return;
        // Cancel vanilla right-arm swing and animate the left hand instead.
        event.setCancelled(true);
        if (shootSide(player, Side.LEFT)) player.swingOffHand();
    }

    /** Prevent ordinary sword-like hit damage on top of left-click beam damage. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player
                && weapon(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    private boolean shootSide(Player player, Side side) {
        UUID id = player.getUniqueId();
        Map<UUID, Long> cooldowns = side == Side.LEFT ? leftCooldowns : rightCooldowns;
        long now = tick();
        if (!TwinBusterTiming.ready(now, cooldowns.getOrDefault(id, 0L))) return false;
        cooldowns.put(id, TwinBusterTiming.until(now, cfg("dual-interval-ticks", 10)));
        try {
            fireDualSide(player, side);
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Twin Buster " + side + " firing failed for " + player.getName(), ex);
            player.sendMessage(Component.text("§c射撃処理エラー（ログを確認）"));
            return false;
        }
    }

    private void update() {
        long now = tick();
        if (now % 2 == 0) for (Player player : Bukkit.getOnlinePlayers()) syncLeftHand(player);
        for (UUID id : Set.copyOf(charging.keySet())) {
            Charge charge = charging.get(id);
            if (charge == null) continue;
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.isDead()
                    || !player.getWorld().getUID().equals(charge.worldId())
                    || !weapon(player.getInventory().getItemInMainHand())
                    || mode(player.getInventory().getItemInMainHand()) != Mode.COMBINED
                    || !charge.weaponId().equals(weaponId(player.getInventory().getItemInMainHand()))) {
                charging.remove(id);
                continue;
            }
            int ticks = cfg("charge-ticks", 30);
            if (TwinBusterTiming.charged(now, charge.started(), ticks)) {
                charging.remove(id);
                try {
                    fireCombined(player);
                    combinedCooldowns.put(id, TwinBusterTiming.until(now,
                            cfg("combined-cooldown-ticks", 200)));
                } catch (RuntimeException ex) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "Twin Buster combined firing failed for " + player.getName(), ex);
                    player.sendMessage(Component.text("§c連結射撃エラー（ログを確認）"));
                }
            } else if (now % 2 == 0) {
                Location muzzle = player.getEyeLocation().add(
                        player.getEyeLocation().getDirection().multiply(1.2)).add(0, -.28, 0);
                double progress = (double)(now - charge.started()) / ticks;
                // Reduced light near the player's camera to avoid overbright flicker.
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, muzzle,
                        2 + (int) (progress * 3), .10, .10, .10, .015);
                player.getWorld().spawnParticle(Particle.DUST, muzzle, 2,
                        .08, .08, .08, 0,
                        new Particle.DustOptions(Color.fromRGB(85, 175, 190), 1.15f));
                if (now % 12 == 0) player.getWorld().playSound(muzzle,
                        Sound.BLOCK_BEACON_AMBIENT, .24f, (float)(.7 + progress * .5));
            }
        }
        combinedCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        leftCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        rightCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        modeCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
    }

    private void fireDualSide(Player player, Side side) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Vector right = horizontalRight(dir);
        double sign = side == Side.LEFT ? -1 : 1;
        Location muzzle = eye.clone().add(right.multiply(sign * .72))
                .add(dir.clone().multiply(1.15)).add(0, -.43, 0);
        World world = player.getWorld();
        // Only a small light pulse at the muzzle, not a full-screen white flash.
        world.spawnParticle(Particle.DUST, muzzle, 2, .06, .06, .06, 0,
                new Particle.DustOptions(Color.fromRGB(85, 175, 190), 1.15f));
        world.spawnParticle(Particle.ELECTRIC_SPARK, muzzle, 3,
                .10, .10, .10, .015);
        fireBeam(player, muzzle, dir, cfg("dual-range", 30.0), .24,
                cfg("dual-damage", 1.25), Color.fromRGB(95, 219, 255),
                new HashSet<>(), false);
        world.playSound(muzzle, Sound.ENTITY_GUARDIAN_ATTACK, .62f,
                side == Side.LEFT ? 1.40f : 1.65f);
    }

    private void fireCombined(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        // Begin the multi-layer beam ahead of the face so rings do not obscure vision.
        Location muzzle = eye.clone().add(dir.clone().multiply(2.25)).add(0, -.32, 0);
        World world = player.getWorld();
        world.spawnParticle(Particle.DUST, muzzle, 4, .12, .12, .12, 0,
                new Particle.DustOptions(Color.fromRGB(100, 185, 210), 1.15f));
        world.spawnParticle(Particle.ELECTRIC_SPARK, muzzle, 8, .20, .20, .20, .025);
        fireBeam(player, muzzle, dir, cfg("combined-range", 48),
                cfg("combined-radius", 1.20),
                cfg("combined-damage", 8.0), Color.fromRGB(140, 235, 255),
                new HashSet<>(), true);
        world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, .68f);
        world.playSound(muzzle, Sound.ENTITY_GUARDIAN_ATTACK, 1.3f, .58f);
    }

    /** Multi-layer luminous beam; visuals do not change the monster hit radius. */
    private void powerBeam(World world, Location start, Vector direction,
                           double length, double hitRadius, boolean impacted) {
        Vector right = horizontalRight(direction);
        Vector up = direction.clone().crossProduct(right);
        if (up.lengthSquared() < .00001) up = new Vector(0, 1, 0);
        up.normalize();
        double visualRadius = Math.max(1.05, hitRadius * 1.35);
        Particle.DustOptions white = new Particle.DustOptions(Color.fromRGB(205, 230, 236), 1.35f);
        Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(45, 142, 205), 1.5f);
        Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(86, 188, 215), 1.65f);
        int budget = Math.max(200, Math.min(4000, cfg("particle-budget", 1800)));
        int used = 0;
        // Multiple concentric rings create volume; the white center is bright.
        for (double dist = 0; dist <= length && used < budget; dist += .75) {
            Location at = start.clone().add(direction.clone().multiply(dist));
            world.spawnParticle(Particle.DUST, at, 2, .09, .09, .09, 0, white);
            used += 2;
            for (int k = 0; k < 12 && used < budget; k++) {
                double angle = k * Math.PI / 6 + dist * .28;
                double radius = visualRadius * .66;
                Location halo = at.clone().add(
                        right.clone().multiply(Math.cos(angle) * radius)).add(
                        up.clone().multiply(Math.sin(angle) * radius));
                world.spawnParticle(Particle.DUST, halo, 1, 0, 0, 0, 0, cyan);
                used++;
            }
            if (((int)(dist * 4)) % 6 == 0) {
                for (int k = 0; k < 8 && used < budget; k++) {
                    double angle = k * Math.PI / 4 + dist * -.25;
                    Location edge = at.clone().add(
                            right.clone().multiply(Math.cos(angle) * visualRadius)).add(
                            up.clone().multiply(Math.sin(angle) * visualRadius));
                    world.spawnParticle(Particle.DUST, edge, 1, 0, 0, 0, 0, blue);
                    used++;
                }
                if (used < budget) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, at, 1,
                            .45, .45, .45, .025);
                    used++;
                }
            }
        }
        // Warden shockwave rings are confined to the distant beam, never the user's eyes.
        for (double d = 3.0; d <= length; d += 3.2) {
            Location ring = start.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.SONIC_BOOM, ring, 1, 0, 0, 0, 0);
        }
        Location impact = start.clone().add(direction.clone().multiply(length));
        world.spawnParticle(Particle.SONIC_BOOM, impact, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.END_ROD, impact, 25, 1.0, 1.0, 1.0, .08);
        world.spawnParticle(Particle.ELECTRIC_SPARK, impact, 50, .9, .9, .9, .12);
        if (impacted) {
            world.spawnParticle(Particle.EXPLOSION, impact, 4, .5, .5, .5, 0);
            world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, .92f);
        }
    }

    /** Block-clamped cylindrical hitscan; damage living mobs (including iron golems). */
    private void fireBeam(Player shooter, Location muzzle, Vector direction,
                          double range, double radius, double damage,
                          Color color, Set<UUID> hit, boolean heavy) {
        World world = shooter.getWorld();
        RayTraceResult obstacle = world.rayTraceBlocks(
                muzzle, direction, range, FluidCollisionMode.NEVER, true);
        double length = obstacle == null ? range
                : Math.min(range, muzzle.toVector().distance(obstacle.getHitPosition()));
        if (heavy) {
            powerBeam(world, muzzle, direction, length, radius, obstacle != null);
        } else {
            Particle.DustOptions dust = new Particle.DustOptions(color, 1.6f);
            for (double d = 0; d <= length; d += .35) {
                Location at = muzzle.clone().add(direction.clone().multiply(d));
                world.spawnParticle(Particle.DUST, at, 2, .06, .06, .06, 0, dust);
            }
            Location tip = muzzle.clone().add(direction.clone().multiply(length));
            world.spawnParticle(Particle.END_ROD, tip, 8, .23, .23, .23, .03);
        }
        Vector middle = muzzle.toVector().add(direction.clone().multiply(length / 2));
        Vector start = muzzle.toVector();
        for (Entity entity : world.getNearbyEntities(middle.toLocation(world),
                length / 2 + 2, length / 2 + 2, length / 2 + 2)) {
            if (!(entity instanceof LivingEntity living) || living instanceof Player
                    || !living.isValid() || living.isDead()
                    || hit.contains(living.getUniqueId())) continue;
            Vector center = living.getLocation().add(0, living.getHeight() * .52, 0).toVector();
            Vector fromStart = center.subtract(start);
            double projected = fromStart.dot(direction);
            if (projected < 0 || projected > length) continue;
            Vector closest = direction.clone().multiply(projected);
            if (fromStart.subtract(closest).lengthSquared()
                    > Math.pow(radius + living.getWidth() * .5, 2)) continue;
            hit.add(living.getUniqueId());
            living.damage(damage, shooter);
        }
    }

    /** Never allow the auto-generated visual item to be moved to other slots. */
    @EventHandler(ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (isVisual(event.getCurrentItem()) || isVisual(event.getCursor())
                || (event.getClickedInventory() == player.getInventory()
                    && event.getSlot() == 40
                    && isVisual(player.getInventory().getItemInOffHand())))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isVisual(event.getOldCursor())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isVisual(event.getOffHandItem()) || isVisual(event.getMainHandItem())
                || isVisual(event.getPlayer().getInventory().getItemInOffHand()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        if (isVisual(event.getItemDrop().getItemStack()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        removeLeftVisual(event.getPlayer());
        Bukkit.getScheduler().runTask(plugin, () -> syncLeftHand(event.getPlayer()));
    }

    private Vector horizontalRight(Vector dir) {
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        if (right.lengthSquared() < 1.0e-7) return new Vector(1, 0, 0);
        return right.normalize();
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        charging.remove(e.getPlayer().getUniqueId());
        removeLeftVisual(e.getPlayer());
    }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) {
        charging.remove(e.getPlayer().getUniqueId());
        removeLeftVisual(e.getPlayer());
    }
    @EventHandler public void onDeath(PlayerDeathEvent e) {
        charging.remove(e.getEntity().getUniqueId());
        e.getDrops().removeIf(this::isVisual);
        removeLeftVisual(e.getEntity());
    }
    @EventHandler public void onItemSwap(PlayerItemHeldEvent e) {
        charging.remove(e.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> syncLeftHand(e.getPlayer()));
    }
}
