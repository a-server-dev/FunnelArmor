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
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Aerial gameplay isolated from variants 1-4.
 * The approved free-mesh BBModels live in models/, but a vanilla item model
 * cannot render a Blockbench free mesh. Pack models here are explicit
 * temporary approximations until a compatible mesh renderer is integrated.
 * Gameplay is functional independently of custom rendering.
 */
final class AerialCombat implements Listener {
    private static final int BITS = 11;
    private final FunnelArmorPlugin plugin;
    private final NamespacedKey controllerKey;
    private final NamespacedKey formKey;
    private final NamespacedKey displayKey;
    private final Map<UUID, State> active = new HashMap<>();
    // Isolated OP-only visual previews: never replace approved live equipment.
    private final Map<UUID, ItemDisplay> bitFirstPreviews = new HashMap<>();
    // Retain cooldowns even when a player unequips a controller or changes world.
    private final Map<UUID, Long> shieldCooldowns = new HashMap<>();
    private final Map<UUID, Long> rifleCooldowns = new HashMap<>();
    private long tick;


    private enum Mode { FREE, SHIELD, RIFLE }
    private static final class State {
        final UUID owner;
        State(UUID owner) { this.owner = owner; }
        Mode mode = Mode.FREE;
        final List<ItemDisplay> bits = new ArrayList<>();
        ItemDisplay assembled;
        long started;
        long lastToggle = -100;
        long shieldCooldown, rifleCooldown, nextAuto;
        AerialFlightMath.Offset[] transitionFrom;
        int lastShotBit = -1;
        long lastShotTick = -100;
        Location shotTarget;
        int rifleShots;
        long rifleChargeUntil = -1;
        long rifleNextShot = -1; // earliest next manual shot; -1 while uncharged
        BarrierDurability.State health = new BarrierDurability.State(0, 0);
    }

    AerialCombat(FunnelArmorPlugin plugin, NamespacedKey displayKey) {
        this.plugin = plugin;
        this.displayKey = displayKey;
        this.controllerKey = new NamespacedKey(plugin, "aerial_controller");
        this.formKey = new NamespacedKey(plugin, "aerial_equipment_form");
    }

    private State state(UUID id) {
        return active.computeIfAbsent(id, ignored -> {
            State st = new State(id);
            st.shieldCooldown = shieldCooldowns.getOrDefault(id, 0L);
            st.rifleCooldown = rifleCooldowns.getOrDefault(id, 0L);
            return st;
        });
    }

    private int intCfg(String key, int fallback, int min) {
        return Math.max(min, plugin.getConfig().getInt("aerial." + key, fallback));
    }
    private double dblCfg(String key, double fallback, double min) {
        return Math.max(min, plugin.getConfig().getDouble("aerial." + key, fallback));
    }
    private boolean isAerial(ItemStack item) {
        return item != null && (item.getType() == Material.LEATHER_HORSE_ARMOR
                || item.getType() == Material.SHIELD || item.getType() == Material.BOW)
                && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                .has(controllerKey, PersistentDataType.BYTE);
    }
    private boolean isForm(ItemStack item, Material type) {
        return isAerial(item) && item.getType() == type;
    }

    private ItemStack formItem(Material material, String form, String model, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.setUnbreakable(true);
        meta.setItemModel(new NamespacedKey("funnel", model));
        meta.getPersistentDataContainer().set(controllerKey, PersistentDataType.BYTE, (byte)1);
        meta.getPersistentDataContainer().set(formKey, PersistentDataType.STRING, form);
        if (material == Material.SHIELD)
            meta.lore(List.of(Component.text("§7右クリック長押し: 盾を構えて防御"),
                    Component.text("§7しゃがみ右クリック: 収納")));
        else
            meta.lore(List.of(Component.text("§7初回右クリック: チャージ / 以降5回右クリックで射撃"),
                    Component.text("§7しゃがみ右クリック: 収納")));
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack shieldItem() {
        return formItem(Material.SHIELD, "shield", "aerial_escutcheon", "§bエスカッシャン（GUNDシールド）");
    }
    private ItemStack rifleItem() {
        return formItem(Material.BOW, "rifle", "aerial_gunbit_rifle", "§bガンビットライフル");
    }
    private void restoreForms(Player player) {
        var inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();
        for (int slot=0;slot<storage.length;slot++) {
            ItemStack item = storage[slot];
            if (isForm(item, Material.SHIELD) || isForm(item, Material.BOW))
                inv.setItem(slot, device());
        }
        ItemStack offhand = inv.getItemInOffHand();
        if (isForm(offhand, Material.SHIELD) || isForm(offhand, Material.BOW))
            inv.setItemInOffHand(device());
    }
    private void retractShield(Player player, State st) {
        if (isForm(player.getInventory().getItemInOffHand(), Material.SHIELD))
            player.getInventory().setItemInOffHand(device());
        changeMode(player, st, Mode.FREE);
        restoreForms(player);
    }

    private boolean main(Player p) { return isAerial(p.getInventory().getItemInMainHand()); }
    private boolean off(Player p) { return isAerial(p.getInventory().getItemInOffHand()); }

    private ItemStack device() {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§b◆ エアリアル GUNDビット（11基）"));
        meta.lore(List.of(
                Component.text("§7メインハンド: 11基が不規則に飛行し敵対Mobを自律射撃"),
                Component.text("§7オフハンド右クリック: 盾に変形 / 長押しでガード"),
                Component.text("§7盾を収納: しゃがみ＋オフハンド右クリック"),
                Component.text("§7メインハンド＋しゃがみ右クリック: ライフル変形/解除"),
                Component.text("§7ライフル中右クリック: チャージ後5発手動射撃")));
        meta.setItemModel(new NamespacedKey("funnel", "aerial_v2_bit_01"));
        meta.getPersistentDataContainer().set(controllerKey, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    void grant(Player target) {
        var leftovers = target.getInventory().addItem(device());
        leftovers.values().forEach(item ->
                target.getWorld().dropItemNaturally(target.getLocation(), item));
    }

    boolean command(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("preview")) {
            if (!sender.hasPermission("funnel.admin")) {
                sender.sendMessage("§c権限がありません。");
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cプレビューはゲーム内のプレイヤー専用です。");
                return true;
            }
            if (args.length != 3) {
                sender.sendMessage("§7/funnel aerial preview <1..11|shield|clear>");
                return true;
            }
            String selection = args[2].toLowerCase(java.util.Locale.ROOT);
            if (selection.equals("clear")) {
                removeBitFirstPreview(player.getUniqueId());
                player.sendMessage("§7GUNDビット試作プレビューを削除しました。");
                return true;
            }
            String model;
            String label;
            float scale;
            if (selection.equals("shield")) {
                model = "aerial_v2_escutcheon";
                label = "11基結合エスカッシャン";
                scale = 1.05f;
            } else {
                final int number;
                try { number = Integer.parseInt(selection); }
                catch (NumberFormatException ex) {
                    player.sendMessage("§c番号は1～11、またはshield/clearを指定してください。");
                    return true;
                }
                if (number < 1 || number > 11) {
                    player.sendMessage("§cビット番号は1～11です。");
                    return true;
                }
                model = String.format(java.util.Locale.ROOT, "aerial_v2_bit_%02d", number);
                label = "GUNDビット試作" + number;
                scale = 1.8f;
            }
            removeBitFirstPreview(player.getUniqueId());
            Vector forward = player.getLocation().getDirection().setY(0);
            if (forward.lengthSquared() < 1.0e-6) forward = new Vector(0,0,1);
            Location at = player.getLocation().add(forward.normalize().multiply(2.6)).add(0, 1.7, 0);
            at.setYaw(player.getLocation().getYaw() + 180f);
            at.setPitch(0);
            ItemDisplay display = spawn(player, model, scale);
            // Model-review lighting is stable regardless of the spawn block's
            // light level. Live combat displays retain ordinary world lighting.
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTeleportDuration(0);
            display.teleport(at);
            bitFirstPreviews.put(player.getUniqueId(), display);
            player.sendMessage("§b" + label + "§7を前方に表示しました。"
                    + " §7(/funnel aerial preview clear で削除)");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("funnel.admin")) {
                sender.sendMessage("§c権限がありません。"); return true;
            }
            Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2])
                    : sender instanceof Player p ? p : null;
            if (target == null) { sender.sendMessage("§c対象のプレイヤーが見つかりません。"); return true; }
            grant(target);
            sender.sendMessage("§aエアリアル装備を " + target.getName() + " に渡しました。");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("status") && sender instanceof Player p) {
            State st = active.get(p.getUniqueId());
            if (st == null) { sender.sendMessage("§7エアリアルは現在未展開です。"); return true; }
            sender.sendMessage("§bAerial: §f" + st.mode
                    + " §7/ 残弾 " + st.rifleShots
                    + " / 盾CD " + Math.max(0,st.shieldCooldown-tick) + "t"
                    + " / ライフルCD " + Math.max(0,st.rifleCooldown-tick) + "t");
            return true;
        }
        sender.sendMessage("§7/funnel aerial give [player] §8(OP) §7| /funnel aerial status §7| /funnel aerial preview <1..11|shield|clear> §8(OP)");
        return true;
    }

    void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getOnlinePlayers().forEach(this::restoreForms);
        Bukkit.getScheduler().runTaskTimer(plugin, this::update, 4L, 2L);
    }

    void stop() {
        for (UUID id : new ArrayList<>(active.keySet())) clear(id);
        for (UUID id : new ArrayList<>(bitFirstPreviews.keySet())) removeBitFirstPreview(id);
        for (Player p : Bukkit.getOnlinePlayers()) restoreForms(p);
    }

    /** Capture current visual positions before mode switches (no teleport at activation). */
    private void changeMode(Player player, State st, Mode mode) {
        AerialFlightMath.Offset[] start = new AerialFlightMath.Offset[BITS];
        Vector front = horizontal(player);
        Vector right = new Vector(front.getZ(), 0, -front.getX());
        for (int i = 0; i < BITS; i++) {
            if (i < st.bits.size() && st.bits.get(i).isValid()
                    && st.bits.get(i).getWorld() == player.getWorld()) {
                Vector rel = st.bits.get(i).getLocation().toVector()
                        .subtract(player.getLocation().toVector());
                start[i] = new AerialFlightMath.Offset(rel.dot(right), rel.getY(), rel.dot(front));
            } else {
                start[i] = AerialFlightMath.free(tick, i);
            }
        }
        st.transitionFrom = start;
        st.mode = mode;
        st.started = tick;
    }

    private void removeBitFirstPreview(UUID id) {
        ItemDisplay display = bitFirstPreviews.remove(id);
        if (display != null && display.isValid()) display.remove();
    }

    private void clear(UUID id) {
        removeBitFirstPreview(id);
        State st = active.remove(id);
        if (st == null) return;
        st.bits.forEach(display -> { if (display.isValid()) display.remove(); });
        if (st.assembled != null && st.assembled.isValid()) st.assembled.remove();
        Player p = Bukkit.getPlayer(id);
        if (p != null) restoreForms(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // A server restart can leave a transformed item in saved inventory.
        Bukkit.getScheduler().runTask(plugin, () -> restoreForms(event.getPlayer()));
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clear(e.getPlayer().getUniqueId());
        restoreForms(e.getPlayer());
    }
    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) {
        clear(e.getPlayer().getUniqueId());
        restoreForms(e.getPlayer());
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().replaceAll(item -> isForm(item, Material.SHIELD)
                || isForm(item, Material.BOW) ? device() : item);
        clear(e.getEntity().getUniqueId());
        restoreForms(e.getEntity());
    }

    // Bukkit may pre-cancel RIGHT_CLICK_AIR when vanilla has no use action
    // (such as leather horse armor). Never ignore such events for controllers.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (!plugin.getConfig().getBoolean("aerial.enabled", true)) return;
        if (e.getHand() != EquipmentSlot.HAND && e.getHand() != EquipmentSlot.OFF_HAND)
            return;
        Player player = e.getPlayer();
        UUID id = player.getUniqueId();
        State previous = active.get(id);
        ItemStack heldOff = player.getInventory().getItemInOffHand();
        ItemStack heldMain = player.getInventory().getItemInMainHand();

        // A deployed native SHIELD must receive ordinary right clicks to raise
        // the arm and block. Intercept only the deliberate sneak+right-click
        // retraction gesture; never cancel the normal shield-use packet.
        if (previous != null && previous.mode == Mode.SHIELD
                && isForm(heldOff, Material.SHIELD)) {
            if (e.getHand() == EquipmentSlot.HAND) return;
            if (!player.isSneaking()) return;
            e.setCancelled(true);
            if (tick - previous.lastToggle < 5) return;
            previous.lastToggle = tick;
            retractShield(player,previous);
            player.sendActionBar(Component.text("§7エスカッシャン収納"));
            return;
        }
        boolean mainHand = e.getHand() == EquipmentSlot.HAND;
        AerialInputRouting.Intent intent = AerialInputRouting.route(
                mainHand, main(player), off(player), player.isSneaking(),
                previous != null && previous.mode == Mode.RIFLE);
        if (intent == AerialInputRouting.Intent.NONE) return;
        e.setCancelled(true);
        State st = state(id);
        switch (intent) {
            case SHIELD_TOGGLE -> {
                if (tick - st.lastToggle < 5) return;
                st.lastToggle = tick;
                if (st.mode == Mode.SHIELD) {
                    retractShield(player,st);
                    player.sendActionBar(Component.text("§7エスカッシャン収納"));
                } else if (tick < st.shieldCooldown) {
                    player.sendActionBar(Component.text("§c盾クールダウン: " + (st.shieldCooldown-tick) + "t"));
                } else if (!off(player)) {
                    player.sendActionBar(Component.text("§cGUNDビットをオフハンドに持ってください"));
                } else {
                    if (st.mode == Mode.RIFLE) endRifle(player, st);
                    changeMode(player, st, Mode.SHIELD);
                    st.health = new BarrierDurability.State(0,0);
                    ItemStack shield = shieldItem();
                    player.getInventory().setItemInOffHand(shield);
                    player.sendActionBar(Component.text("§bエスカッシャン展開：右クリック長押しで防御"));
                }
            }
            case RIFLE_TOGGLE -> {
                if (tick - st.lastToggle < 5) return;
                st.lastToggle = tick;
                if (st.mode == Mode.RIFLE) {
                    endRifle(player,st);
                    player.sendActionBar(Component.text("§7ライフル収納・冷却開始"));
                } else if (tick < st.rifleCooldown) {
                    player.sendActionBar(Component.text("§cライフル冷却中: " + (st.rifleCooldown-tick) + "t"));
                } else {
                    if (st.mode == Mode.SHIELD) retractShield(player,st);
                    changeMode(player, st, Mode.RIFLE);
                    st.rifleShots = 5;
                    st.rifleChargeUntil = -1;
                    st.rifleNextShot = -1;
                    player.getInventory().setItemInMainHand(rifleItem());
                    player.sendActionBar(Component.text("§bGUNDビットライフルを装備：右クリックで射撃"));
                }
            }
            case RIFLE_FIRE -> {
                if (st.mode != Mode.RIFLE || !isForm(heldMain,Material.BOW)) return;
                if (st.rifleChargeUntil >= 0) return;
                if (tick-st.started < intCfg("assembly-ticks",10,1)) return;
                if (st.rifleNextShot < 0) {
                    st.rifleChargeUntil = tick + intCfg("rifle-charge-ticks",20,1);
                    player.getWorld().playSound(player.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.5f,1.8f);
                    player.sendActionBar(Component.text("§bGUNDライフル: チャージ中…"));
                } else if (tick >= st.rifleNextShot) {
                    fireRifle(player,st);
                    st.rifleShots--;
                    st.rifleNextShot = tick + intCfg("rifle-shot-interval-ticks",4,1);
                    player.swingMainHand();
                    if (st.rifleShots <= 0) {
                        endRifle(player,st);
                        player.sendActionBar(Component.text("§e5発射撃終了・冷却開始"));
                    } else {
                        player.sendActionBar(Component.text("§bGUNDライフル: 残弾 " + st.rifleShots));
                    }
                }
            }
            default -> {}
        }
    }

    private void endRifle(Player player, State st) {
        if (isForm(player.getInventory().getItemInMainHand(), Material.BOW))
            player.getInventory().setItemInMainHand(device());
        changeMode(player, st, Mode.FREE);
        restoreForms(player);
        st.rifleShots = 0;
        st.rifleChargeUntil = -1;
        st.rifleNextShot = -1;
        st.rifleCooldown = tick + intCfg("rifle-cooldown-ticks", 200, 1);
        rifleCooldowns.put(st.owner, st.rifleCooldown);
    }

    private ItemDisplay spawn(Player p, String model, float size) {
        return p.getWorld().spawn(p.getLocation().add(0, 2.0, 0), ItemDisplay.class, display -> {
            ItemStack stack = new ItemStack(Material.LEATHER_HORSE_ARMOR);
            ItemMeta meta = stack.getItemMeta();
            meta.setItemModel(new NamespacedKey("funnel", model));
            stack.setItemMeta(meta);
            display.setItemStack(stack);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setPersistent(false);
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setTeleportDuration(2);
            display.setInterpolationDuration(2);
            display.setTransformation(new Transformation(new Vector3f(0,0,0),
                    new AxisAngle4f(), new Vector3f(size,size,size), new AxisAngle4f()));
            display.getPersistentDataContainer().set(displayKey, PersistentDataType.BYTE, (byte)1);
        });
    }

    // New bit-first model heights vary; normalize each live ItemDisplay to
    // approximately 12.8 model pixels (0.8 blocks), not the old meshes' scale.
    private static final float[] NORMALIZED_BIT_SCALES = {
            2.06452f, 1.94529f, 1.94529f, 2.65010f, 4.02516f, 4.02516f,
            0.86022f, 0.86022f, 2.86996f, 1.53477f, 1.53477f
    };

    private float bitScale(int i) {
        return NORMALIZED_BIT_SCALES[i]
                * (float) dblCfg("bit-size-multiplier", 1.0, .1);
    }

    private void ensureDisplays(Player p, State st) {
        if (st.bits.size() != BITS || st.bits.stream().anyMatch(d -> !d.isValid() || d.getWorld()!=p.getWorld())) {
            for (ItemDisplay display : st.bits) if (display.isValid()) display.remove();
            st.bits.clear();
            for (int i=0; i<BITS; i++) st.bits.add(spawn(p,
                    "aerial_v2_bit_" + String.format(java.util.Locale.ROOT, "%02d", i + 1),
                    bitScale(i)));
        }
        // Assembled items are now carried in the player's actual hands:
        // SHIELD in offhand / BOW in main hand. Do not spawn another floating
        // shield or rifle model, which would double-render the equipment.
        if (st.assembled != null && st.assembled.isValid()) st.assembled.remove();
        st.assembled = null;
    }

    private Vector horizontal(Player p) {
        Vector v = p.getLocation().getDirection().setY(0);
        return v.lengthSquared() < 1.0e-6 ? new Vector(0,0,1) : v.normalize();
    }

    private void animate(Player p, State st) {
        Vector front = horizontal(p);
        Vector right = new Vector(front.getZ(), 0, -front.getX());
        double progress = AerialFlightMath.progress(tick, st.started,
                intCfg("assembly-ticks", 10, 1));
        for (int i = 0; i < BITS; i++) {
            AerialFlightMath.Offset destination = switch (st.mode) {
                case FREE -> AerialFlightMath.free(tick, i);
                case SHIELD -> AerialFlightMath.shield(i);
                case RIFLE -> AerialFlightMath.rifle(i);
            };
            AerialFlightMath.Offset target = st.transitionFrom != null
                    ? AerialFlightMath.mix(st.transitionFrom[i], destination, progress)
                    : destination;
            ItemDisplay bit = st.bits.get(i);
            float size = st.mode != Mode.FREE && progress >= 1.0 ? 0.001f : bitScale(i);
            if (Math.abs(bit.getTransformation().getScale().x() - size) > .0001f)
                bit.setTransformation(new Transformation(new Vector3f(0, 0, 0),
                        new AxisAngle4f(), new Vector3f(size,size,size), new AxisAngle4f()));
            Location goal = p.getLocation().add(
                    right.clone().multiply(target.side()))
                    .add(front.clone().multiply(target.forward()))
                    .add(0,target.height(),0);
            if (st.mode == Mode.FREE && st.lastShotBit == i && tick-st.lastShotTick <= 6
                    && st.shotTarget != null && st.shotTarget.getWorld() == p.getWorld()) {
                Vector aim = st.shotTarget.toVector().subtract(goal.toVector());
                if (aim.lengthSquared() > .001) {
                    goal.setDirection(aim);
                    goal.setYaw(goal.getYaw() +
                            (float) plugin.getConfig().getDouble("aerial.bit-yaw-offset-degrees", 90));
                }
            } else {
                goal.setYaw((float)(tick * 2.0 + i * 137.5));
            }
            bit.teleport(goal);
        }
        if (progress >= 1) st.transitionFrom = null;
        if (st.assembled != null && st.assembled.isValid()) {
            Vector center = front.clone().multiply(st.mode == Mode.SHIELD ? 1.15 : .92)
                    .add(right.clone().multiply(st.mode == Mode.RIFLE ? .42 : 0))
                    .add(new Vector(0,1.55,0));
            Location goal = p.getLocation().add(center);
            float yawOffset = (float) plugin.getConfig().getDouble(
                    st.mode == Mode.RIFLE ? "aerial.rifle-yaw-offset-degrees"
                            : "aerial.shield-yaw-offset-degrees", st.mode == Mode.RIFLE ? 90 : 0);
            goal.setYaw(p.getLocation().getYaw() + yawOffset);
            goal.setPitch(st.mode == Mode.RIFLE ? p.getLocation().getPitch() : 0);
            st.assembled.teleport(goal);
        }
    }

    private LivingEntity findTarget(Player p) {
        double range = dblCfg("target-range", 20.0, 1);
        LivingEntity best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(),range,range,range)) {
            if (!(e instanceof Monster m) || m.isDead() || !m.isValid()) continue;
            double distance = m.getLocation().distanceSquared(p.getLocation());
            if (distance >= bestDist || distance > range*range) continue;
            bestDist = distance;
            best = m;
        }
        return best;
    }

    private boolean clearLine(Location start, Location target) {
        if (start.getWorld() != target.getWorld()) return false;
        Vector ray = target.toVector().subtract(start.toVector());
        double dist = ray.length();
        return dist > 0.001 && start.getWorld().rayTraceBlocks(start,ray.normalize(),dist,
                FluidCollisionMode.NEVER,true) == null;
    }

    private void beam(World world, Location start, Location end, Color color) {
        Vector diff = end.toVector().subtract(start.toVector());
        double len = diff.length();
        if (len < .01) return;
        Vector step = diff.multiply(1/len);
        Particle.DustOptions dust = new Particle.DustOptions(color, .95f);
        for (double i=0;i<=Math.min(len,36);i+=.55) {
            Location at = start.clone().add(step.clone().multiply(i));
            world.spawnParticle(Particle.DUST,at,1,0,0,0,0,dust);
        }
        world.spawnParticle(Particle.END_ROD,end,4,.1,.1,.1,.02);
        world.playSound(start,Sound.ENTITY_GUARDIAN_ATTACK,.32f,1.9f);
    }

    private void autoFire(Player p, State st) {
        LivingEntity target = findTarget(p);
        if (target == null) return;
        int shooterIndex = ThreadLocalRandom.current().nextInt(BITS);
        ItemDisplay bit = st.bits.get(shooterIndex);
        Location end = target.getEyeLocation();
        Vector aim = end.toVector().subtract(bit.getLocation().toVector());
        if (aim.lengthSquared() <= 0.0001) return;
        Location start = bit.getLocation().add(aim.clone().normalize().multiply(.20));
        if (!clearLine(start,end)) return;
        st.lastShotBit = shooterIndex;
        st.lastShotTick = tick;
        st.shotTarget = end.clone();
        Location facing = bit.getLocation().clone();
        facing.setDirection(aim);
        facing.setYaw(facing.getYaw() +
                (float) plugin.getConfig().getDouble("aerial.bit-yaw-offset-degrees", 90));
        bit.teleport(facing);
        beam(p.getWorld(),start,end,Color.fromRGB(60,245,255));
        target.damage(dblCfg("auto-damage", .5, 0), p);
    }

    private void fireRifle(Player p, State st) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double range = dblCfg("rifle-range",32,1);
        RayTraceResult blocks = p.getWorld().rayTraceBlocks(eye,dir,range,FluidCollisionMode.NEVER,true);
        double dist = blocks == null ? range :
                blocks.getHitPosition().distance(eye.toVector());
        RayTraceResult hit = p.getWorld().rayTraceEntities(eye,dir,dist,.27,
                e -> e instanceof LivingEntity && e != p && !e.isDead());
        LivingEntity victim = hit != null && hit.getHitEntity() instanceof LivingEntity living ? living : null;
        Location end = victim == null ? eye.clone().add(dir.clone().multiply(dist))
                : hit.getHitPosition().toLocation(p.getWorld());
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        if (right.lengthSquared() < .0001) right = new Vector(1,0,0);
        else right.normalize();
        // Rifle is held in the right hand; originate the beam just ahead
        // of that weapon instead of at an old floating display position.
        Location muzzle = eye.clone()
                .add(dir.clone().multiply(dblCfg("rifle-muzzle-forward-offset", 1.1, 0)))
                .add(right.multiply(plugin.getConfig().getDouble(
                        "aerial.rifle-muzzle-right-offset", .33)))
                .add(0,plugin.getConfig().getDouble("aerial.rifle-muzzle-up-offset", -.28),0);
        // Never start the beam beyond its target if the target is very close.
        if (muzzle.distanceSquared(end) > 0 && muzzle.toVector().subtract(eye.toVector())
                .dot(dir) >= eye.distance(end)) muzzle = eye;
        beam(p.getWorld(),muzzle,end,Color.fromRGB(100,200,255));
        if (victim != null) victim.damage(dblCfg("rifle-damage", 1.5, .01),p);
    }

    private void breakShield(Player p, State st) {
        retractShield(p,st);
        st.shieldCooldown = tick + intCfg("shield-cooldown-ticks",200,1);
        shieldCooldowns.put(p.getUniqueId(), st.shieldCooldown);
        p.getWorld().playSound(p.getLocation(),Sound.ITEM_SHIELD_BREAK,1f,1.2f);
        p.sendMessage("§cエスカッシャン耐久限界！ §7クールダウン開始");
    }

    private void shieldHit(Player p, State st, double damage) {
        st.health = st.health.absorb(Math.max(0, Double.isFinite(damage) ? damage : 0));
        p.getWorld().playSound(p.getLocation(),Sound.ITEM_SHIELD_BLOCK,.7f,1.0f);
        int hits = intCfg("shield-max-hits",5,1);
        double hp = dblCfg("shield-max-absorbed-damage",20,0.1);
        if (st.health.broken(hits,hp)) breakShield(p,st);
        else p.sendActionBar(Component.text("§bエスカッシャン §7" + st.health.hits()
                + "/" + hits + " 回 / " + String.format(java.util.Locale.ROOT,"%.1f",st.health.totalDamage())
                + "/" + hp + " HP"));
    }

    private boolean shieldReady(Player p, State st) {
        return st.mode == Mode.SHIELD &&
                isForm(p.getInventory().getItemInOffHand(),Material.SHIELD) &&
                p.isBlocking() &&
                tick-st.started >= intCfg("assembly-ticks",10,1);
    }
    private boolean fromFront(Player p, Location source) {
        if (source.getWorld()!=p.getWorld()) return false;
        Vector relative = source.toVector().subtract(p.getLocation().toVector()).setY(0);
        return relative.dot(horizontal(p)) >= .08;
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player p) || event.getDamager()==p) return;
        State st=active.get(p.getUniqueId());
        if (st==null || !shieldReady(p,st) || !fromFront(p,event.getDamager().getLocation())) return;
        double dmg=Math.max(0,event.getDamage());
        event.setCancelled(true);
        if (event.getDamager() instanceof Projectile projectile && projectile.isValid()) projectile.remove();
        shieldHit(p,st,dmg);
    }

    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = false)
    public void onVanillaBowShot(EntityShootBowEvent e) {
        // Our rifle uses five hitscan beams. Never fire vanilla arrows when
        // a player has ammunition in their inventory.
        if (isForm(e.getBow(), Material.BOW)) e.setCancelled(true);
    }

    private void projectiles(Player p, State st) {
        Vector front=horizontal(p);
        Location plane = p.getLocation().add(front.clone().multiply(1.2)).add(0,1.45,0);
        for (Projectile projectile : p.getWorld().getNearbyEntitiesByType(Projectile.class,plane,1.45,1.7,1.45)) {
            if (!projectile.isValid() || projectile.getShooter()==p) continue;
            if (projectile.getVelocity().dot(front) >= -0.03) continue;
            if (!fromFront(p,projectile.getLocation())) continue;
            double damage=projectile instanceof AbstractArrow a ? a.getDamage() : 2;
            projectile.remove();
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,plane,12,.2,.2,.2,.05);
            shieldHit(p,st,damage);
            if (st.mode!=Mode.SHIELD) return;
        }
    }

    void update() {
        tick+=2;
        if (!plugin.getConfig().getBoolean("aerial.enabled", true)) { stop(); return; }
        // Expire stored cooldowns to avoid retaining old UUIDs indefinitely.
        shieldCooldowns.values().removeIf(until -> until <= tick);
        rifleCooldowns.values().removeIf(until -> until <= tick);
        for (UUID id : new ArrayList<>(active.keySet())) {
            Player p=Bukkit.getPlayer(id);
            if (p==null || !p.isOnline() || p.isDead() || (!main(p) && !off(p))) {
                clear(id); continue;
            }
            State st=active.get(id);
            if (st.mode==Mode.RIFLE && !isForm(p.getInventory().getItemInMainHand(),Material.BOW)) endRifle(p, st);
            if (st.mode==Mode.SHIELD && !isForm(p.getInventory().getItemInOffHand(),Material.SHIELD))
                retractShield(p, st);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || (!main(p) && !off(p))) continue;
            State st=state(p.getUniqueId());
            ensureDisplays(p,st);
            animate(p,st);
            if (st.mode == Mode.RIFLE) {
                if (st.rifleChargeUntil >= 0 && tick >= st.rifleChargeUntil) {
                    st.rifleChargeUntil = -1;
                    st.rifleNextShot = tick;
                    p.sendActionBar(Component.text("§bチャージ完了！ 右クリックで5発まで手動射撃"));
                }

            }
            if (shieldReady(p,st)) {
                projectiles(p,st);
            } else if (st.mode==Mode.FREE && main(p) && tick>=st.nextAuto) {
                st.nextAuto=tick + ThreadLocalRandom.current().nextInt(
                        intCfg("auto-min-interval-ticks",10,1),
                        Math.max(intCfg("auto-min-interval-ticks",10,1)+1,
                                 intCfg("auto-max-interval-ticks",28,2)+1));
                autoFire(p,st);
            }
        }
    }
}
