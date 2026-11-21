package dev.emerald.funnel;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Brau-Bro's four wired mega-particle gun pods as a handheld controller.
 * Holding the controller in the MAIN hand deploys four cosmetic waist guns.
 * No actual projectiles or combat behavior in this initial version.
 * Original unit canon: two double-barreled pods and two single-barreled pods.
 */
final class BrauBroWaistCannons implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey controllerKey;
    private final NamespacedKey displayMarker;
    private final Map<UUID, List<ItemDisplay>> active = new HashMap<>();
    private final ItemStack singleVisual;
    private final ItemStack twinVisual;

    BrauBroWaistCannons(JavaPlugin plugin) {
        this.plugin = plugin;
        this.controllerKey = new NamespacedKey(plugin, "brau_bro_controller");
        this.displayMarker = new NamespacedKey(plugin, "brau_bro_display");
        this.singleVisual = visualItem("brau_bro_single_cannon");
        this.twinVisual = visualItem("brau_bro_twin_cannon");
    }

    private static ItemStack visualItem(String id) {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("funnel", id));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack controller() {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§d◆ ブラウ・ブロ メガ粒子砲（4基）"));
        meta.lore(List.of(
            Component.text("§7メインハンド保持中: 腰に砲塔4基を展開"),
            Component.text("§7連装メガ粒子砲×2 / 単装メガ粒子砲×2"),
            Component.text("§8現在は外観・腰部追従のみ（射撃未実装）")
        ));
        meta.setUnbreakable(true);
        meta.setItemModel(new NamespacedKey("funnel", "brau_bro_controller"));
        meta.getPersistentDataContainer().set(controllerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean isHeld(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType() == Material.LEATHER_HORSE_ARMOR && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
            .has(controllerKey, PersistentDataType.BYTE);
    }

    void start() {
        // Stale entities can remain when /reload or another plugin's forced
        // reload interrupted a previous instance. Never delete normal displays.
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display.getPersistentDataContainer().has(displayMarker, PersistentDataType.BYTE))
                    display.remove();
            }
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    void stop() {
        for (UUID owner : new ArrayList<>(active.keySet())) clear(owner);
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("brau-bro.enabled", true);
    }

    private ItemDisplay spawn(Player player, BrauBroWaistLayout.Mount mount) {
        return player.getWorld().spawn(player.getLocation(), ItemDisplay.class, display -> {
            display.setItemStack(mount.twin() ? twinVisual : singleVisual);
            // Use the resource model's FIXED pose. The model faces +Z in
            // its source and the 180-degree fixed flip is cancelled by yaw+180.
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            display.setPersistent(false);
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setSilent(true);
            display.setTeleportDuration(2);
            display.setInterpolationDuration(2);
            float scale = (float) plugin.getConfig().getDouble("brau-bro.model-scale", .74);
            if (!Float.isFinite(scale) || scale < .1f || scale > 1.5f) scale = .74f;
            display.setTransformation(new Transformation(new Vector3f(0, 0, 0),
                new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
            display.getPersistentDataContainer().set(displayMarker, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private void tick() {
        if (!enabled()) {
            stop();
            return;
        }
        for (UUID owner : new ArrayList<>(active.keySet())) {
            Player p = Bukkit.getPlayer(owner);
            if (p == null || !p.isOnline() || p.isDead() || !isHeld(p))
                clear(owner);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || !isHeld(player)) continue;
            UUID owner = player.getUniqueId();
            List<ItemDisplay> displays = active.get(owner);
            if (displays == null || displays.size() != BrauBroWaistLayout.MOUNTS.size()
                    || displays.stream().anyMatch(d -> !d.isValid() || d.getWorld() != player.getWorld())) {
                clear(owner);
                displays = new ArrayList<>(BrauBroWaistLayout.MOUNTS.size());
                for (BrauBroWaistLayout.Mount mount : BrauBroWaistLayout.MOUNTS)
                    displays.add(spawn(player, mount));
                active.put(owner, displays);
            }
            Location base = player.getLocation();
            Vector forward = base.getDirection().setY(0);
            if (forward.lengthSquared() < 1e-6) forward = new Vector(0, 0, 1);
            forward.normalize();
            Vector right = new Vector(-forward.getZ(), 0, forward.getX()).multiply(-1);
            // = (front.z, 0, -front.x), so positive 'side' is player's right.
            for (int i = 0; i < BrauBroWaistLayout.MOUNTS.size(); i++) {
                BrauBroWaistLayout.Mount mount = BrauBroWaistLayout.MOUNTS.get(i);
                Location goal = base.clone()
                    .add(right.clone().multiply(mount.side()))
                    .add(forward.clone().multiply(mount.forward()))
                    .add(0, mount.height(), 0);
                goal.setYaw(base.getYaw() + 180.0f + mount.yawOffset());
                goal.setPitch(0);
                displays.get(i).teleport(goal);
            }
        }
    }

    private void clear(UUID owner) {
        List<ItemDisplay> displays = active.remove(owner);
        if (displays != null) for (ItemDisplay d : displays)
            if (d.isValid()) d.remove();
    }

    boolean command(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("funnel.admin")) {
                sender.sendMessage("§c権限がありません。");
                return true;
            }
            if (args.length > 3) {
                sender.sendMessage("§7/funnel brau give [player]");
                return true;
            }
            Player target = args.length == 3 ? Bukkit.getPlayerExact(args[2])
                : sender instanceof Player p ? p : null;
            if (target == null) {
                sender.sendMessage("§c対象のプレイヤーが見つかりません。");
                return true;
            }
            target.getInventory().addItem(controller()).values().forEach(item ->
                target.getWorld().dropItemNaturally(target.getLocation(), item));
            sender.sendMessage("§a" + target.getName()
                + " にブラウ・ブロのメガ粒子砲コントローラーを渡しました。");
            return true;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§7ゲーム内で確認してください。");
                return true;
            }
            sender.sendMessage("§dブラウ・ブロ §7/ 手持ち: §f"
                + (isHeld(p) ? "あり" : "なし")
                + " §7/ 腰砲塔: §f"
                + active.getOrDefault(p.getUniqueId(), List.of()).size() + "/4"
                + " §7/ 射撃: §8未実装");
            return true;
        }
        sender.sendMessage("§b/funnel brau give [player] §8(OP) §7| §b/funnel brau status");
        return true;
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onWorld(PlayerChangedWorldEvent event) {
        clear(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onDeath(PlayerDeathEvent event) {
        clear(event.getEntity().getUniqueId());
    }
    @EventHandler public void onChangeHand(PlayerItemHeldEvent event) {
        clear(event.getPlayer().getUniqueId());
    }
}
