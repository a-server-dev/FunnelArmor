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
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Original, no-client-mod Twin Buster Rifle prototype. No overlap with funnel controllers. */
final class TwinBusterCombat implements Listener {
    private enum Mode { DUAL, COMBINED }
    private record Charge(String weaponId, UUID worldId, long started) {}

    private final JavaPlugin plugin;
    private final NamespacedKey weaponKey;
    private final NamespacedKey modeKey;
    private final NamespacedKey identityKey;
    private final Map<UUID, Charge> charging = new HashMap<>();
    private final Map<UUID, Long> combinedCooldowns = new HashMap<>();
    private final Map<UUID, Long> dualCooldowns = new HashMap<>();
    private final Map<UUID, Long> modeCooldowns = new HashMap<>();

    TwinBusterCombat(JavaPlugin plugin) {
        this.plugin = plugin;
        weaponKey = new NamespacedKey(plugin, "twin_buster_weapon");
        modeKey = new NamespacedKey(plugin, "twin_buster_mode");
        identityKey = new NamespacedKey(plugin, "twin_buster_identity");
    }

    void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, 1L);
    }

    void stop() {
        charging.clear();
        combinedCooldowns.clear();
        dualCooldowns.clear();
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

    private boolean weapon(ItemStack item) {
        return item != null && item.getType() == Material.WARPED_FUNGUS_ON_A_STICK
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

    private ItemStack newWeapon() {
        ItemStack item = new ItemStack(Material.WARPED_FUNGUS_ON_A_STICK);
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
                Component.text("§7右クリック: " + (mode == Mode.DUAL
                        ? "2丁から同時射撃" : "チャージして極太ビーム")),
                Component.text("§7しゃがみ＋右クリック: 分離／連結切替"),
                Component.text("§8ウイングゼロ風・独立兵装")));
    }

    boolean command(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage("§b/funnel buster give [player] §8(OP) §7| §b/funnel buster status");
            return true;
        }
        if (args[1].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("funnel.admin")) {
                sender.sendMessage("§c権限がありません。");
                return true;
            }
            Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2])
                    : sender instanceof Player p ? p : null;
            if (target == null) {
                sender.sendMessage("§c対象プレイヤーが見つかりません。");
                return true;
            }
            target.getInventory().addItem(newWeapon()).values().forEach(item ->
                    target.getWorld().dropItemNaturally(target.getLocation(), item));
            sender.sendMessage("§a" + target.getName() + " にツインバスターライフルを渡しました。");
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
        sender.sendMessage("§c/funnel buster <give|status>");
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND ||
                (event.getAction() != Action.RIGHT_CLICK_AIR
                 && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!weapon(item)) return;
        event.setCancelled(true);
        if (!plugin.getConfig().getBoolean("twin-buster.enabled", true)) return;

        UUID id = player.getUniqueId();
        long now = tick();
        if (player.isSneaking()) {
            if (!TwinBusterTiming.ready(now, modeCooldowns.getOrDefault(id, 0L))) return;
            charging.remove(id);
            Mode next = mode(item) == Mode.DUAL ? Mode.COMBINED : Mode.DUAL;
            ItemMeta meta = item.getItemMeta();
            applyMode(meta, next);
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            modeCooldowns.put(id, TwinBusterTiming.until(now, cfg("mode-switch-ticks", 10)));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, .6f, 1.25f);
            player.sendActionBar(Component.text(next == Mode.DUAL
                    ? "§bバスターライフル：分離" : "§bツインバスターライフル：連結"));
            return;
        }
        if (!TwinBusterTiming.ready(now, modeCooldowns.getOrDefault(id, 0L))) return;
        if (mode(item) == Mode.DUAL) {
            if (!TwinBusterTiming.ready(now, dualCooldowns.getOrDefault(id, 0L))) return;
            dualCooldowns.put(id, TwinBusterTiming.until(now, cfg("dual-interval-ticks", 10)));
            fireDual(player);
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

    private void update() {
        long now = tick();
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
                fireCombined(player);
                combinedCooldowns.put(id, TwinBusterTiming.until(now,
                        cfg("combined-cooldown-ticks", 200)));
            } else if (now % 2 == 0) {
                Location muzzle = player.getEyeLocation().add(
                        player.getEyeLocation().getDirection().multiply(.95)).add(0, -.25, 0);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, muzzle,
                        4, .35, .25, .35, .02);
            }
        }
        combinedCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        dualCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        modeCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
    }

    private void fireDual(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Vector right = horizontalRight(dir);
        Set<UUID> hit = new HashSet<>();
        for (double sign : new double[]{-1, 1}) {
            Location muzzle = eye.clone().add(right.clone().multiply(sign * .4))
                    .add(dir.clone().multiply(.6)).add(0, -.30, 0);
            fireBeam(player, muzzle, dir, cfg("dual-range", 30), .28,
                    cfg("dual-damage", 1.25), Color.fromRGB(95, 219, 255), hit);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, .65f, 1.5f);
    }

    private void fireCombined(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Location muzzle = eye.clone().add(dir.clone().multiply(.65)).add(0, -.3, 0);
        fireBeam(player, muzzle, dir, cfg("combined-range", 48),
                cfg("combined-radius", 1.20),
                cfg("combined-damage", 8.0), Color.fromRGB(140, 235, 255), new HashSet<>());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, .72f);
        player.sendActionBar(Component.text("§bツインバスターライフル：発射！"));
    }

    /** Block-clamped cylindrical hitscan; only hostile monsters are damaged. */
    private void fireBeam(Player shooter, Location muzzle, Vector direction,
                          double range, double radius, double damage,
                          Color color, Set<UUID> hit) {
        World world = shooter.getWorld();
        RayTraceResult obstacle = world.rayTraceBlocks(
                muzzle, direction, range, FluidCollisionMode.NEVER, true);
        double length = obstacle == null ? range
                : Math.min(range, muzzle.toVector().distance(obstacle.getHitPosition()));
        Vector start = muzzle.toVector();
        Particle.DustOptions dust = new Particle.DustOptions(color,
                (float) Math.max(.8, Math.min(2.5, radius * 1.6)));
        for (double dist = 0; dist <= length; dist += .65) {
            Location at = muzzle.clone().add(direction.clone().multiply(dist));
            world.spawnParticle(Particle.DUST, at, radius > .5 ? 4 : 1,
                    radius * .30, radius * .30, radius * .30, 0, dust);
        }
        Location tip = muzzle.clone().add(direction.clone().multiply(length));
        world.spawnParticle(Particle.END_ROD, tip, 8, .23, .23, .23, .03);
        Vector middle = start.clone().add(direction.clone().multiply(length / 2));
        for (Entity entity : world.getNearbyEntities(middle.toLocation(world),
                length / 2 + 2, length / 2 + 2, length / 2 + 2)) {
            if (!(entity instanceof Monster monster) || !monster.isValid()
                    || monster.isDead() || hit.contains(monster.getUniqueId())) continue;
            Vector center = monster.getLocation().add(0, monster.getHeight() * .52, 0).toVector();
            Vector fromStart = center.subtract(start);
            double projected = fromStart.dot(direction);
            if (projected < 0 || projected > length) continue;
            Vector closest = direction.clone().multiply(projected);
            if (fromStart.subtract(closest).lengthSquared()
                    > Math.pow(radius + monster.getWidth() * .5, 2)) continue;
            hit.add(monster.getUniqueId());
            monster.damage(damage, shooter);
        }
    }

    private Vector horizontalRight(Vector dir) {
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        if (right.lengthSquared() < 1.0e-7) return new Vector(1, 0, 0);
        return right.normalize();
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        charging.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) {
        charging.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onDeath(PlayerDeathEvent e) {
        charging.remove(e.getEntity().getUniqueId());
    }
    @EventHandler public void onItemSwap(PlayerItemHeldEvent e) {
        charging.remove(e.getPlayer().getUniqueId());
    }
}
