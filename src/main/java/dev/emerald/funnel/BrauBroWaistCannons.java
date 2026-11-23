package dev.emerald.funnel;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MAN-03 Brau-Bro: a HANDHELD controller launches four real ItemDisplay
 * cannon pods while 4 separate, physical, continuously updated tethers
 * remain attached between waist harness and moving cannon roots.
 *
 * Left click OR sneak + right click toggles extension and retrieval.
 * No damage, firing, inventory duplication or modifications to other weapons.
 */
final class BrauBroWaistCannons implements Listener {
    private static final int CABLE_SEGMENTS = 1;
    private static final float CABLE_THICKNESS = .034f;

    private final JavaPlugin plugin;
    private final NamespacedKey controllerKey;
    private final NamespacedKey displayMarker;
    private final Map<UUID, State> active = new HashMap<>();
    private final ItemStack singleVisual;
    private final ItemStack twinVisual;

    private static final class State {
        final List<ItemDisplay> pods = new ArrayList<>(4);
        final List<BlockDisplay> cables = new ArrayList<>(4 * CABLE_SEGMENTS);
        // Independent world-space pod locations and frozen launch destinations:
        // rotating the player does NOT drag deployed cannons around in an orbit.
        final Location[] positions = new Location[4];
        final Location[] launchTargets = new Location[4];
        final float[] launchYaws = new float[4];
        boolean extended;
        boolean moving;
        long nextToggleTick;
    }

    BrauBroWaistCannons(JavaPlugin plugin) {
        this.plugin = plugin;
        controllerKey = new NamespacedKey(plugin, "brau_bro_controller");
        displayMarker = new NamespacedKey(plugin, "brau_bro_display");
        singleVisual = visualItem("brau_bro_single_cannon");
        twinVisual = visualItem("brau_bro_twin_cannon");
    }

    private static ItemStack visualItem(String id) {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("funnel", id));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack giftItem() { return controller(); }

    private ItemStack controller() {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§d◆ ブラウ・ブロ メガ粒子砲（4基）"));
        meta.lore(List.of(
            Component.text("§7メインハンド保持中: 腰に砲塔4基を接続"),
            Component.text("§7左クリック / しゃがみ右クリック: 有線射出・収納"),
            Component.text("§7連装メガ粒子砲×2 / 単装メガ粒子砲×2"),
            Component.text("§8射撃・ダメージなし（有線展開のみ）")
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

    private boolean enabled() {
        return plugin.getConfig().getBoolean("brau-bro.enabled", true);
    }

    private int extensionTicks() {
        return Math.max(4, Math.min(80,
            plugin.getConfig().getInt("brau-bro.extension-ticks", 16)));
    }

    private double reach() {
        double n = plugin.getConfig().getDouble("brau-bro.extension-distance", 4.2);
        return Double.isFinite(n) ? Math.max(2.0, Math.min(8.0, n)) : 4.2;
    }

    void start() {
        // Unload stale visual entities after server reloads, including cable
        // BlockDisplays (the previous implementation only removed ItemDisplays).
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if ((entity instanceof ItemDisplay || entity instanceof BlockDisplay)
                    && entity.getPersistentDataContainer()
                        .has(displayMarker, PersistentDataType.BYTE))
                    entity.remove();
            }
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void stop() {
        for (UUID owner : new ArrayList<>(active.keySet())) clear(owner);
    }

    private ItemDisplay spawnPod(Player player, BrauBroWaistLayout.Mount mount) {
        return player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(mount.twin() ? twinVisual : singleVisual);
            // Leave the existing v0.18.0 geometry and fixed display transform.
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setGravity(false);
            d.setInvulnerable(true);
            d.setSilent(true);
            d.setTeleportDuration(0);
            d.setInterpolationDuration(0);
            float scale = (float) plugin.getConfig().getDouble("brau-bro.model-scale", .74);
            if (!Float.isFinite(scale) || scale < .1f || scale > 1.5f) scale = .74f;
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
            d.getPersistentDataContainer().set(displayMarker, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private BlockDisplay spawnWire(Player player) {
        return player.getWorld().spawn(player.getLocation(), BlockDisplay.class, d -> {
            // One continuous cable from harness to pod. Segmented rods were
            // seen disconnected and additionally inherited the player's yaw.
            d.setBlock(Material.GRAY_CONCRETE.createBlockData());
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setGravity(false);
            d.setInvulnerable(true);
            d.setSilent(true);
            // Both ends and the cable rotate in one server tick; independent
            // interpolation of rotation, scale and teleport broke cable joins.
            d.setTeleportDuration(0);
            d.setInterpolationDuration(0);
            d.setDisplayWidth(16);
            d.setDisplayHeight(16);
            d.setTransformation(new Transformation(new Vector3f(),
                new Quaternionf(), new Vector3f(.001f, .001f, .001f),
                new Quaternionf()));
            d.getPersistentDataContainer().set(displayMarker, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private void ensurePods(Player player, State state) {
        if (state.pods.size() == BrauBroWaistLayout.MOUNTS.size()
            && state.pods.stream().allMatch(d ->
                d.isValid() && d.getWorld().equals(player.getWorld()))) return;

        for (ItemDisplay d : state.pods) if (d.isValid()) d.remove();
        state.pods.clear();
        for (var mount : BrauBroWaistLayout.MOUNTS)
            state.pods.add(spawnPod(player, mount));
    }

    private void ensureCables(Player player, State state) {
        if (state.cables.size() == 4 * CABLE_SEGMENTS
            && state.cables.stream().allMatch(d ->
                d.isValid() && d.getWorld().equals(player.getWorld()))) return;
        removeCables(state);
        for (int n = 0; n < 4 * CABLE_SEGMENTS; n++)
            state.cables.add(spawnWire(player));
    }

    private static void removeCables(State state) {
        for (BlockDisplay cable : state.cables)
            if (cable.isValid()) cable.remove();
        state.cables.clear();
    }

    private void toggle(Player player) {
        if (!enabled() || !isHeld(player) || player.isDead()) return;
        State state = active.computeIfAbsent(player.getUniqueId(), ignored -> new State());
        long tick = Bukkit.getCurrentTick();
        // PlayerInteractEvent and PlayerArmSwingEvent may fire from one click.
        // Prevent the same gesture from toggling outward then inward.
        if (tick < state.nextToggleTick) return;
        state.nextToggleTick = tick + 6L;
        state.extended = !state.extended;
        if (state.extended) {
            // Snapshot the four world-space deployment destinations at launch.
            // Turning or moving afterward changes only the waist end of the
            // tether; deployed cannons do not rotate with the player.
            Location base = player.getLocation();
            Vector forward = horizontalForward(base);
            Vector right = horizontalRight(forward);
            double sneakShift = player.isSneaking() ? -.22 : 0;
            for (int i = 0; i < BrauBroWaistLayout.MOUNTS.size(); i++) {
                var mount = BrauBroWaistLayout.MOUNTS.get(i);
                Location destination = worldOffset(base, forward, right,
                    BrauBroWaistLayout.launched(mount, reach()), sneakShift);
                state.launchYaws[i] = base.getYaw() + 180f + mount.yawOffset();
                destination.setYaw(state.launchYaws[i]);
                destination.setPitch(0);
                state.launchTargets[i] = destination;
            }
        }
        player.getWorld().playSound(player.getLocation(),
            state.extended ? Sound.BLOCK_PISTON_EXTEND : Sound.BLOCK_PISTON_CONTRACT,
            .64f, state.extended ? 1.26f : .92f);
        player.sendActionBar(Component.text(state.extended
            ? "§dブラウ・ブロ §f有線メガ粒子砲4基：射出"
            : "§dブラウ・ブロ §f有線メガ粒子砲4基：帰還"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!isHeld(player) || !enabled()) return;
        // Applies to air clicks and entity left-clicks, which do not always
        // produce PlayerInteractEvent on Paper.
        event.setCancelled(true);
        toggle(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!isHeld(player) || !enabled()) return;
        Action action = event.getAction();
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean shiftRight = player.isSneaking()
            && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);
        if (!left && !shiftRight) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        toggle(player);
    }

    private static Location worldOffset(Location base, Vector front, Vector right,
                                        BrauBroWaistLayout.Offset offset,
                                        double yCorrection) {
        return base.clone().add(right.clone().multiply(offset.side()))
            .add(front.clone().multiply(offset.forward()))
            .add(0, offset.height() + yCorrection, 0);
    }

    private static Vector horizontalForward(Location base) {
        Vector front = base.getDirection().setY(0);
        if (front.lengthSquared() < 1e-6) front = new Vector(0, 0, 1);
        return front.normalize();
    }

    private static Vector horizontalRight(Vector forward) {
        return new Vector(forward.getZ(), 0, -forward.getX());
    }

    private static Location approach(Location previous, Location target, double step) {
        // This movement is purely world-space. In particular, it does not
        // transform already-launched locations by the player's current yaw.
        Location result = previous.clone();
        Vector delta = target.toVector().subtract(previous.toVector());
        double distance = delta.length();
        if (distance <= step || distance <= 1e-6) {
            result.setX(target.getX());
            result.setY(target.getY());
            result.setZ(target.getZ());
        } else {
            result.add(delta.multiply(step / distance));
        }
        result.setYaw(target.getYaw());
        result.setPitch(0);
        return result;
    }

    private static void segment(BlockDisplay segment, Location a, Location b) {
        Vector vector = b.toVector().subtract(a.toVector());
        double length = vector.length();
        if (length <= 1e-4) return;
        Vector3f direction = new Vector3f((float)(vector.getX()/length),
            (float)(vector.getY()/length), (float)(vector.getZ()/length));
        Quaternionf orientation = new Quaternionf().rotationTo(
            new Vector3f(0, 1, 0), direction);
        // BlockDisplay's unit cube is centered around the Y segment line:
        // rotate the thickness offset but leave the start of the rod at a.
        Vector3f centering = orientation.transform(
            new Vector3f(-CABLE_THICKNESS / 2, 0, -CABLE_THICKNESS / 2),
            new Vector3f());
        // CRITICAL: entity yaw/pitch must be neutral. Otherwise Minecraft
        // applies the inherited player yaw on top of 'orientation', making
        // cables miss their endpoints and scatter as detached black rods.
        Location neutral = a.clone();
        neutral.setYaw(0f);
        neutral.setPitch(0f);
        segment.teleport(neutral);
        segment.setTransformation(new Transformation(centering, orientation,
            new Vector3f(CABLE_THICKNESS, (float) length, CABLE_THICKNESS),
            new Quaternionf()));
    }

    private void updateWires(Player player, State state, Location[] podLocations,
                             Vector forward, Vector right, double sneakShift) {
        if (!state.extended && !state.moving) {
            removeCables(state);
            return;
        }
        ensureCables(player, state);
        for (int i=0; i<BrauBroWaistLayout.MOUNTS.size(); i++) {
            var mount = BrauBroWaistLayout.MOUNTS.get(i);
            Location harness = worldOffset(player.getLocation(), forward, right,
                BrauBroWaistLayout.cableBase(mount), sneakShift);
            // The cannon model's rear coupling is approximately 0.2 blocks
            // behind its display pivot, measured along its own held yaw.
            // Do NOT use the player's current facing to locate this end.
            double modelYaw = Math.toRadians(podLocations[i].getYaw() - 180.0);
            Vector muzzle = new Vector(-Math.sin(modelYaw), 0, Math.cos(modelYaw));
            Location coupling = podLocations[i].clone().subtract(muzzle.multiply(.20));
            segment(state.cables.get(i), harness, coupling);
        }
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
            State state = active.computeIfAbsent(player.getUniqueId(), unused -> new State());
            ensurePods(player, state);

            Location base = player.getLocation();
            Vector forward = horizontalForward(base);
            Vector right = horizontalRight(forward);
            double sneakShift = player.isSneaking() ? -.22 : 0;
            double speed = reach() / extensionTicks() * 1.18;
            Location[] positions = new Location[4];
            boolean stillMoving = false;

            for (int i=0; i<4; i++) {
                var mount = BrauBroWaistLayout.MOUNTS.get(i);
                Location home = worldOffset(base, forward, right,
                    BrauBroWaistLayout.stowed(mount), sneakShift);
                home.setYaw(base.getYaw()+180f+mount.yawOffset());
                home.setPitch(0);

                if (state.positions[i] == null
                        || !state.positions[i].getWorld().equals(base.getWorld()))
                    state.positions[i] = home.clone();

                // Fixed world-space target, generated ONCE per launch rather
                // than every tick from current player position and facing.
                Location target = state.extended && state.launchTargets[i] != null
                    ? state.launchTargets[i] : home;
                double distance = state.positions[i].distance(target);
                // Auto-retrieve if the player travels far beyond physical
                // cable reach; prevents limitless stretches across the world.
                if (state.extended && state.positions[i].distance(base) > reach() + 5.0) {
                    state.extended = false;
                    target = home;
                    distance = state.positions[i].distance(target);
                }
                if (distance > .018) {
                    // On return, the target can move with the waist: chase in
                    // world space at a bounded speed instead of orbiting.
                    double step = state.extended ? speed : Math.max(.43, speed * 1.5);
                    state.positions[i] = approach(state.positions[i], target, step);
                    stillMoving = true;
                } else {
                    state.positions[i] = target.clone();
                }
                // When docked, keep the controller's original fitted waist
                // orientation. While in flight, retain launch-facing yaw.
                if (!state.extended && state.positions[i].distance(home) < .10)
                    state.positions[i].setYaw(home.getYaw());
                else if (state.launchTargets[i] != null)
                    state.positions[i].setYaw(state.launchYaws[i]);
                state.positions[i].setPitch(0);
                state.pods.get(i).teleport(state.positions[i]);
                positions[i] = state.positions[i];
            }

            state.moving = stillMoving;
            updateWires(player, state, positions, forward, right, sneakShift);
        }
    }

    private void clear(UUID owner) {
        State state = active.remove(owner);
        if (state == null) return;
        for (ItemDisplay d : state.pods) if (d.isValid()) d.remove();
        state.pods.clear();
        removeCables(state);
        for (int i=0; i<4; i++) {
            state.positions[i] = null;
            state.launchTargets[i] = null;
        }
    }

    boolean command(CommandSender sender, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§7ゲーム内で確認してください。");
                return true;
            }
            State state = active.get(p.getUniqueId());
            String mode = state == null ? "未装備"
                : state.moving ? "射出・帰還中"
                : state.extended ? "有線射出中" : "腰部格納";
            sender.sendMessage("§dブラウ・ブロ §7/ 手持ち: §f"
                + (isHeld(p) ? "あり" : "なし")
                + " §7/ 状態: §f" + mode
                + " §7/ 砲塔: §f" + (state == null ? 0 : state.pods.size()) + "/4"
                + " §7/ ワイヤー: §f" + (state == null ? 0 : state.cables.size()) + "節"
                + " §7/ 射撃: §8未実装");
            return true;
        }
        sender.sendMessage("§b/funnel brau status §7| 配布: /funnel give brau [player]");
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
