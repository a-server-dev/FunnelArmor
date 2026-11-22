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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Separate Paper-only GN Sword II weapon. Six orbiting GN Sword Bits fold into
 * the Buster Sword / Buster Rifle form. Requires only the bundled resource pack.
 */
final class GnSwordIICombat implements Listener {
    enum Mode {
        SWORD("sword", "GNソードII・二刀"),
        RIFLE("rifle", "GNソードII・二挺ライフル");

        final String key;
        final String name;
        Mode(String key, String name) { this.key = key; this.name = name; }
        Mode next() { return values()[(ordinal() + 1) % values().length]; }
        boolean combined() { return false; }
        boolean sword() { return this == SWORD; }
        static Mode parse(String key) {
            for (Mode mode : values()) if (mode.key.equals(key)) return mode;
            return SWORD;
        }
    }

    private final JavaPlugin plugin;
    private final NamespacedKey weaponKey;
    private final NamespacedKey modeKey;
    private final NamespacedKey dualVisualMarker;
    private final Map<UUID, Long> actionCooldown = new HashMap<>();
    private final Map<UUID, Long> modeCooldown = new HashMap<>();
    private final Map<UUID, ItemDisplay> leftSwordDisplays = new HashMap<>();
    private final Set<UUID> internalDamage = new HashSet<>();

    GnSwordIICombat(JavaPlugin plugin) {
        this.plugin = plugin;
        weaponKey = new NamespacedKey(plugin, "gn_sword_ii_weapon");
        modeKey = new NamespacedKey(plugin, "gn_sword_ii_mode");
        dualVisualMarker = new NamespacedKey(plugin, "gn_sword_ii_left_hand_visual");
    }

    void start() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display.getPersistentDataContainer().has(dualVisualMarker, PersistentDataType.BYTE))
                    display.remove();
            }
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    void stop() {
        for (UUID owner : new ArrayList<>(leftSwordDisplays.keySet())) clearLeftSword(owner);
        actionCooldown.clear();
        modeCooldown.clear();
        internalDamage.clear();
    }

    private long now() { return Bukkit.getCurrentTick(); }

    private double value(String key, double fallback) {
        double v = plugin.getConfig().getDouble("gn-sword-ii." + key, fallback);
        return Double.isFinite(v) && v > 0 ? v : fallback;
    }
    private int interval(String key, int fallback) {
        return Math.max(1, plugin.getConfig().getInt("gn-sword-ii." + key, fallback));
    }
    private boolean enabled() { return plugin.getConfig().getBoolean("gn-sword-ii.enabled", true); }

    private boolean owns(ItemStack item) {
        return item != null && item.getType() == Material.FEATHER && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(weaponKey, PersistentDataType.BYTE);
    }
    private Mode mode(ItemStack item) {
        if (!owns(item)) return Mode.SWORD;
        return Mode.parse(item.getItemMeta().getPersistentDataContainer()
                .get(modeKey, PersistentDataType.STRING));
    }

    private void apply(ItemMeta meta, Mode mode) {
        meta.setItemModel(new NamespacedKey("funnel", "gn_sword_ii_" + mode.key));
        meta.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, mode.key);
        meta.displayName(Component.text("§b" + mode.name));
        meta.setUnbreakable(true);
        meta.lore(List.of(
                Component.text("§7右クリック: " + (mode.sword() ? "前方斬撃" : "GNビーム射撃")),
                Component.text("§7しゃがみ＋右クリック: 次の形態"),
                Component.text("§7ダブルオー用GNソードII（2本）"),
                Component.text("§7剣／ライフル形態を同時切替")));
    }

    ItemStack giftItem() { return swordItem(); }

    private ItemStack swordItem() {
        ItemStack sword = new ItemStack(Material.FEATHER);
        ItemMeta meta = sword.getItemMeta();
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.BYTE, (byte) 1);
        apply(meta, Mode.SWORD);
        sword.setItemMeta(meta);
        return sword;
    }

    boolean command(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cプレイヤー専用です。");
            } else {
                ItemStack held = player.getInventory().getItemInMainHand();
                sender.sendMessage(owns(held) ? "§bGNソードII: §f" + mode(held).name
                        + " §7/ 残り冷却 " + Math.max(0,
                        (actionCooldown.getOrDefault(player.getUniqueId(), 0L) - now() + 19) / 20) + "秒"
                        : "§7GNソードIIをメインハンドに持ってください。");
            }
            return true;
        }
        sender.sendMessage("§b/funnel gn2 status §7| 配布: /funnel give gn2 [player]");
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!owns(held)) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        if (!enabled()) return;
        UUID id = player.getUniqueId();
        long time = now();
        if (player.isSneaking()) {
            if (time < modeCooldown.getOrDefault(id, 0L)) return;
            Mode next = mode(held).next();
            ItemMeta meta = held.getItemMeta();
            apply(meta, next);
            held.setItemMeta(meta);
            player.getInventory().setItemInMainHand(held);
            modeCooldown.put(id, time + interval("switch-ticks", 12));
            // Old attached bits are discarded; next tick spawns the required formation.
            clearLeftSword(id);
            if (next == Mode.RIFLE) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, .65f, .75f);
                player.getWorld().spawnParticle(Particle.END_ROD,
                        player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.0)),
                        12, .25, .28, .25, .03);
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, .5f, 1.3f);
            }
            player.sendActionBar(Component.text("§bGNソードII: " + next.name));
            return;
        }
        if (time < actionCooldown.getOrDefault(id, 0L)
                || time < modeCooldown.getOrDefault(id, 0L)) return;
        Mode current = mode(held);
        long end = time + interval(current.combined() ? "buster-cooldown-ticks" : "attack-cooldown-ticks",
                current.combined() ? 45 : 16);
        try {
            if (current.sword()) slash(player, current);
            else fire(player, current);
            actionCooldown.put(id, end);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "GN Sword II attack failed for " + player.getName(), ex);
            player.sendActionBar(Component.text("§cGNソードII攻撃エラー（コンソールを確認）"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !owns(player.getInventory().getItemInMainHand())) return;
        if (internalDamage.contains(player.getUniqueId())) return;
        if (!(event.getEntity() instanceof Mob)) {
            event.setCancelled(true); // The custom sword cannot be used for PVP.
            return;
        }
        Mode mode = mode(player.getInventory().getItemInMainHand());
        if (mode.sword()) event.setDamage(value("melee-damage", 5.0));
        else event.setCancelled(true);
    }

    private void slash(Player player, Mode mode) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double range = value(mode.combined() ? "buster-slash-range" : "slash-range",
                mode.combined() ? 6.0 : 3.5);
        double damage = value(mode.combined() ? "buster-slash-damage" : "slash-damage",
                mode.combined() ? 10.0 : 5.0);
        Particle.DustOptions green = new Particle.DustOptions(
                Color.fromRGB(93, 225, 200), mode.combined() ? 1.8f : 1.1f);
        Vector right = new Vector(-direction.getZ(), 0, direction.getX());
        if (right.lengthSquared() < 1.0e-4) right = new Vector(1, 0, 0);
        right.normalize();
        for (int i = 0; i <= 16; i++) {
            double a = -1.15 + i * (2.3 / 16);
            Vector slashDirection = direction.clone().multiply(Math.cos(a))
                    .add(right.clone().multiply(Math.sin(a)));
            for (double length = 1.0; length <= range; length += .65) {
                Location point = eye.clone().add(slashDirection.clone().multiply(length));
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, green);
            }
        }
        for (Entity entity : world.getNearbyEntities(eye, range + 1, range + 1, range + 1)) {
            if (!(entity instanceof Mob mob) || !mob.isValid() || mob.isDead()) continue;
            Vector delta = mob.getLocation().add(0, mob.getHeight() * .5, 0)
                    .toVector().subtract(eye.toVector());
            double distance = delta.length();
            if (distance > range + mob.getWidth() / 2.0 || distance < .01) continue;
            if (direction.dot(delta.normalize()) < .4) continue;
            RayTraceResult barrier = world.rayTraceBlocks(eye, delta, distance,
                    FluidCollisionMode.NEVER, true);
            if (barrier != null) continue;
            internalDamage.add(player.getUniqueId());
            try { mob.damage(damage, player); }
            finally { internalDamage.remove(player.getUniqueId()); }
        }
        player.swingMainHand();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                .8f, mode.combined() ? .75f : 1.2f);
    }

    private void fire(Player player, Mode mode) {
        World world = player.getWorld();
        Location start = player.getEyeLocation().add(
                player.getEyeLocation().getDirection().multiply(1.15)).add(0, -.24, 0);
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double max = value(mode.combined() ? "buster-rifle-range" : "rifle-range",
                mode.combined() ? 44.0 : 28.0);
        RayTraceResult wall = world.rayTraceBlocks(start, direction, max,
                FluidCollisionMode.NEVER, true);
        double length = wall == null ? max
                : Math.min(max, wall.getHitPosition().distance(start.toVector()));
        double damage = value(mode.combined() ? "buster-rifle-damage" : "rifle-damage",
                mode.combined() ? 9.0 : 3.5);
        RayTraceResult entity = world.rayTraceEntities(start, direction, length,
                mode.combined() ? .8 : .3, candidate -> candidate instanceof Mob
                        && candidate.isValid() && !candidate.isDead());
        if (entity != null && entity.getHitEntity() instanceof Mob mob) {
            internalDamage.add(player.getUniqueId());
            try { mob.damage(damage, player); }
            finally { internalDamage.remove(player.getUniqueId()); }
        }
        var particle = new Particle.DustOptions(Color.fromRGB(65, 213, 182),
                mode.combined() ? 1.9f : 1.2f);
        for (double distance = 0; distance < length; distance += mode.combined() ? .6 : .40) {
            Location at = start.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.DUST, at, mode.combined() ? 3 : 1,
                    mode.combined() ? .27 : .03,
                    mode.combined() ? .27 : .03,
                    mode.combined() ? .27 : .03, 0, particle);
            if (mode.combined() && distance >= 3 && ((int)(distance * 10)) % 40 < 6)
                world.spawnParticle(Particle.END_ROD, at, 1, .09, .09, .09, .01);
        }
        Location impact = start.clone().add(direction.clone().multiply(length));
        world.spawnParticle(Particle.ELECTRIC_SPARK, impact, 8, .2, .2, .2, .04);
        world.playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, .8f,
                mode.combined() ? .63f : 1.35f);
    }


    private void tick() {
        if (!enabled()) {
            for (UUID owner : new ArrayList<>(leftSwordDisplays.keySet())) clearLeftSword(owner);
            return;
        }
        for (UUID owner : new ArrayList<>(leftSwordDisplays.keySet())) {
            Player player = Bukkit.getPlayer(owner);
            ItemDisplay display = leftSwordDisplays.get(owner);
            if (player == null || !player.isOnline() || player.isDead()
                    || !owns(player.getInventory().getItemInMainHand())
                    || display == null || !display.isValid() || display.getWorld() != player.getWorld()) {
                clearLeftSword(owner);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers())
            if (!player.isDead() && owns(player.getInventory().getItemInMainHand()))
                updateLeftSword(player);
    }

    /**
     * Render the second GN Sword beside the left hand without replacing the
     * player's actual offhand inventory item (and without copying a PDC weapon).
     * The tracked, nonpersistent display is removed on all transitions.
     */
    private ItemStack visualItem(Mode mode) {
        ItemStack visual = new ItemStack(Material.FEATHER);
        ItemMeta itemMeta = visual.getItemMeta();
        itemMeta.setUnbreakable(true);
        itemMeta.setItemModel(new NamespacedKey("funnel", "gn_sword_ii_" + mode.key));
        visual.setItemMeta(itemMeta);
        return visual;
    }

    private void updateLeftSword(Player player) {
        UUID owner = player.getUniqueId();
        ItemDisplay display = leftSwordDisplays.get(owner);
        if (display == null || !display.isValid()) {
            clearLeftSword(owner);
            display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
                d.setItemStack(visualItem(mode(player.getInventory().getItemInMainHand())));
                // Render the same weapon in its world-forward orientation, instead of
                // reapplying an in-hand pose to a free-floating ItemDisplay.
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setTeleportDuration(2);
                d.setPersistent(false);
                d.getPersistentDataContainer().set(dualVisualMarker, PersistentDataType.BYTE, (byte) 1);
            });
            leftSwordDisplays.put(owner, display);
        }
        Location base = player.getLocation();
        Vector facing = base.getDirection().setY(0);
        if (facing.lengthSquared() < 1e-5) facing = new Vector(0, 0, 1);
        facing.normalize();
        // Facing-right is (-z, 0, x); negate it to anchor on the *left* arm.
        // The previous positive-right offset put both swords on the same side.
        Vector left = new Vector(facing.getZ(), 0, -facing.getX());
        // The editable GN Sword has its grip at model Z ~= -7, not at
        // Minecraft's model pivot Z = 8. Anchor THAT grip on the left wrist.
        // With FIXED scale 0.76: (8 - (-7)) * 0.76 / 16 ~= 0.71 blocks.
        // The source +Z blade direction must face away from the wrist.
        double gripToPivot = (8.0 - 7.0) * 0.72 / 16.0;
        Location leftHand = base.clone()
                .add(left.multiply(0.40))
                .add(facing.multiply(0.31 + gripToPivot))
                .add(0, 1.22, 0);
        // FIXED's own Y=180 rotation plus entity yaw+180 sets the
        // green cutting blade ahead of the blue/gray rear handle.
        leftHand.setYaw(base.getYaw() + 180.0f);
        leftHand.setPitch(0.0f);
        display.setItemStack(visualItem(mode(player.getInventory().getItemInMainHand())));
        display.teleport(leftHand);
    }

    private void clearLeftSword(UUID owner) {
        ItemDisplay display = leftSwordDisplays.remove(owner);
        if (display != null && display.isValid()) display.remove();
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        clearLeftSword(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) {
        clearLeftSword(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onHeld(PlayerItemHeldEvent event) {
        clearLeftSword(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onDeath(PlayerDeathEvent event) {
        clearLeftSword(event.getEntity().getUniqueId());
    }
}
