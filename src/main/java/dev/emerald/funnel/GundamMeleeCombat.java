package dev.emerald.funnel;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vanilla resource-pack-powered melee weapons. All item IDs, state and models
 * are independent from the funnel, GN Sword V and Twin Buster Rifle systems.
 */
final class GundamMeleeCombat implements Listener {
    private enum Phase { OFF, EXTENDING, ACTIVE, RETRACTING }
    private record Transition(String itemId, boolean extending, long started) {}

    private final JavaPlugin plugin;
    private final NamespacedKey saberKey;
    private final NamespacedKey hawkKey;
    private final NamespacedKey uidKey;
    private final NamespacedKey saberPhaseKey;
    private final NamespacedKey saberStageKey;
    private final NamespacedKey hotKey;
    private final Map<UUID, Transition> transitions = new HashMap<>();
    private final Map<UUID, Long> toggleCooldowns = new HashMap<>();
    private final Map<UUID, Long> saberHitCooldowns = new HashMap<>();

    GundamMeleeCombat(JavaPlugin plugin) {
        this.plugin = plugin;
        saberKey = new NamespacedKey(plugin, "beam_saber_weapon");
        hawkKey = new NamespacedKey(plugin, "heat_hawk_weapon");
        uidKey = new NamespacedKey(plugin, "melee_weapon_id");
        saberPhaseKey = new NamespacedKey(plugin, "saber_phase");
        saberStageKey = new NamespacedKey(plugin, "saber_stage");
        hotKey = new NamespacedKey(plugin, "hawk_heated");
    }

    void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, 1L);
    }

    void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) resetSabers(player.getInventory());
        transitions.clear();
        toggleCooldowns.clear();
        saberHitCooldowns.clear();
    }

    private long now() { return Bukkit.getCurrentTick(); }
    private boolean enabled(String type) {
        return plugin.getConfig().getBoolean(type + ".enabled", true);
    }
    private int ticks(String path, int fallback) {
        return Math.max(1, plugin.getConfig().getInt(path, fallback));
    }
    private double damage(String path, double fallback) {
        return Math.max(0, plugin.getConfig().getDouble(path, fallback));
    }
    private boolean saber(ItemStack item) {
        return item != null && item.getType() == Material.FEATHER && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(saberKey, PersistentDataType.BYTE);
    }
    private boolean hawk(ItemStack item) {
        return item != null && item.getType() == Material.FEATHER && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(hawkKey, PersistentDataType.BYTE);
    }
    private String id(ItemStack item) {
        return item == null || !item.hasItemMeta() ? null :
                item.getItemMeta().getPersistentDataContainer().get(uidKey, PersistentDataType.STRING);
    }
    private Phase phase(ItemStack item) {
        if (!saber(item)) return Phase.OFF;
        String raw = item.getItemMeta().getPersistentDataContainer().get(
                saberPhaseKey, PersistentDataType.STRING);
        try { return raw == null ? Phase.OFF : Phase.valueOf(raw); }
        catch (IllegalArgumentException ignored) { return Phase.OFF; }
    }
    private int stage(ItemStack item) {
        if (!saber(item)) return 0;
        Integer result = item.getItemMeta().getPersistentDataContainer()
                .get(saberStageKey, PersistentDataType.INTEGER);
        return result == null ? 0 : Math.max(0, Math.min(4, result));
    }
    private boolean heated(ItemStack item) {
        return hawk(item) && Byte.valueOf((byte) 1).equals(item.getItemMeta()
                .getPersistentDataContainer().get(hotKey, PersistentDataType.BYTE));
    }

    private void setSaberState(ItemStack item, Phase phase, int stage) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(saberPhaseKey, PersistentDataType.STRING, phase.name());
        meta.getPersistentDataContainer().set(saberStageKey, PersistentDataType.INTEGER, stage);
        meta.setItemModel(new NamespacedKey("funnel", "beam_saber_" + stage));
        meta.displayName(Component.text("§dビームサーベル" + (phase == Phase.ACTIVE ? " §f[展開]" : "")));
        item.setItemMeta(meta);
    }

    private void setHawkState(ItemStack item, boolean heated) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(hotKey, PersistentDataType.BYTE, (byte)(heated ? 1 : 0));
        meta.setItemModel(new NamespacedKey("funnel", heated ? "heat_hawk_on" : "heat_hawk_off"));
        meta.displayName(Component.text(heated ? "§6ヒートホーク §c[加熱]" : "§7ヒートホーク"));
        item.setItemMeta(meta);
    }

    ItemStack giftItem(String type) {
        return "saber".equals(type) ? newSaber() : newHawk();
    }

    private ItemStack newSaber() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(saberKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(uidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.lore(java.util.List.of(
                Component.text("§7右クリック: ビーム刃の展開 / 収納"),
                Component.text("§7刃が完全に伸びたときのみ攻撃可能"),
                Component.text("§7展開中の近接攻撃は無効")));
        item.setItemMeta(meta);
        setSaberState(item, Phase.OFF, 0);
        return item;
    }

    private ItemStack newHawk() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(hawkKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(uidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.lore(java.util.List.of(
                Component.text("§7右クリック: ヒートホーク加熱 ON / OFF"),
                Component.text("§7加熱時: 近接ダメージ増加・短時間の燃焼"),
                Component.text("§7通常状態でも近接攻撃可能")));
        item.setItemMeta(meta);
        setHawkState(item, false);
        return item;
    }

    boolean command(CommandSender sender, String[] args, String type) {
        boolean isSaber = type.equals("saber");
        if (args.length >= 2 && args[1].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player player)) sender.sendMessage("§cプレイヤー専用です。");
            else {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (isSaber) sender.sendMessage(saber(item)
                        ? "§dビームサーベル: " + phase(item) + " / 刃の展開段階 " + stage(item) + "/4"
                        : "§7ビームサーベルをメインハンドに持ってください。");
                else sender.sendMessage(hawk(item)
                        ? "§6ヒートホーク: " + (heated(item) ? "加熱ON" : "加熱OFF")
                        : "§7ヒートホークをメインハンドに持ってください。");
            }
            return true;
        }
        sender.sendMessage("§b/funnel " + type + " status §7| 配布: /funnel give " + type + " [player]");
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND ||
                (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean blade = saber(item),axe = hawk(item);
        if (!blade && !axe) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        long time = now();
        UUID owner = player.getUniqueId();
        if (time < toggleCooldowns.getOrDefault(owner, 0L)) return;
        toggleCooldowns.put(owner, time + ticks("melee-weapons.toggle-debounce-ticks", 5));
        if (blade && enabled("beam-saber")) {
            Phase state = phase(item);
            if (state == Phase.EXTENDING || state == Phase.RETRACTING) return;
            boolean extending = state != Phase.ACTIVE;
            setSaberState(item, extending ? Phase.EXTENDING : Phase.RETRACTING,
                    extending ? 0 : 4);
            player.getInventory().setItemInMainHand(item);
            transitions.put(owner, new Transition(id(item), extending, time));
            player.getWorld().playSound(player.getLocation(),
                    Sound.BLOCK_BEACON_ACTIVATE, .38f, extending ? 1.8f : .8f);
            player.sendActionBar(Component.text(extending ? "§dビームサーベル 展開中…" : "§7ビームサーベル 収納中…"));
        } else if (axe && enabled("heat-hawk")) {
            boolean next = !heated(item);
            setHawkState(item, next);
            player.getInventory().setItemInMainHand(item);
            player.getWorld().playSound(player.getLocation(),
                    Sound.BLOCK_FIRE_AMBIENT, .5f, next ? .95f : .7f);
            player.sendActionBar(Component.text(next ? "§6ヒートホーク：加熱開始" : "§7ヒートホーク：加熱停止"));
        }
    }

    private void update() {
        long time = now();
        int step = ticks("beam-saber.stage-ticks", 3);
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID owner = player.getUniqueId();
            ItemStack held = player.getInventory().getItemInMainHand();
            Transition transition = transitions.get(owner);
            if (transition != null) {
                if (!saber(held) || !transition.itemId().equals(id(held))
                        || !enabled("beam-saber") || player.isDead()) {
                    transitions.remove(owner);
                    resetSabers(player.getInventory());
                } else {
                    long elapsed = Math.max(0, time - transition.started());
                    int progress = Math.min(4, (int)(elapsed / step));
                    int target = MeleeStageTiming.stageAt(elapsed, step, transition.extending());
                    Phase state = progress == 4
                            ? transition.extending() ? Phase.ACTIVE : Phase.OFF
                            : transition.extending() ? Phase.EXTENDING : Phase.RETRACTING;
                    if (stage(held) != target || phase(held) != state) {
                        setSaberState(held, state, target);
                        player.getInventory().setItemInMainHand(held);
                    }
                    if (progress >= 4) {
                        transitions.remove(owner);
                        player.sendActionBar(Component.text(transition.extending()
                                ? "§dビームサーベル 展開完了" : "§7ビームサーベル 収納完了"));
                    }
                }
            }
            if (time % 10 == 0 && enabled("heat-hawk") && heated(held)) {
                Location point = player.getEyeLocation().add(
                        player.getEyeLocation().getDirection().multiply(.9)).add(0, -.5, 0);
                player.getWorld().spawnParticle(Particle.DUST, point, 2,
                        .08, .08, .08, 0,
                        new Particle.DustOptions(Color.fromRGB(204, 91, 40), 1.1f));
            }
        }
        toggleCooldowns.entrySet().removeIf(e -> e.getValue() <= time);
        saberHitCooldowns.entrySet().removeIf(e -> e.getValue() <= time);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (saber(item)) {
            // A handle, partially growing blade, or retracting blade cannot hurt anything.
            if (!enabled("beam-saber")
                    || !MeleeStageTiming.canHit(phase(item) == Phase.ACTIVE, stage(item))
                    || !(event.getEntity() instanceof Mob mob)) {
                event.setCancelled(true);
                return;
            }
            long t = now();
            if (t < saberHitCooldowns.getOrDefault(player.getUniqueId(), 0L)) {
                event.setCancelled(true);
                return;
            }
            saberHitCooldowns.put(player.getUniqueId(), t + ticks("beam-saber.hit-cooldown-ticks", 8));
            event.setDamage(damage("beam-saber.damage", 6.0));
            mob.getWorld().spawnParticle(Particle.DUST, mob.getLocation().add(0, mob.getHeight()*.5, 0),
                    6, .22, .3, .22, 0,
                    new Particle.DustOptions(Color.fromRGB(210, 100, 187), 1.1f));
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, .75f, 1.3f);
        } else if (hawk(item)) {
            if (!enabled("heat-hawk") || !(event.getEntity() instanceof Mob mob)) {
                event.setCancelled(true);
                return;
            }
            boolean on = heated(item);
            event.setDamage(damage(on ? "heat-hawk.hot-damage" : "heat-hawk.damage",
                    on ? 8.0 : 5.0));
            if (on) {
                mob.setFireTicks(Math.max(mob.getFireTicks(),
                        ticks("heat-hawk.fire-ticks", 40)));
                mob.getWorld().spawnParticle(Particle.SMALL_FLAME,
                        mob.getLocation().add(0, mob.getHeight()*.55, 0), 4,
                        .22, .25, .22, .01);
            }
        }
    }

    private void resetSabers(PlayerInventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (saber(item) && (phase(item) != Phase.OFF || stage(item) != 0)) {
                setSaberState(item, Phase.OFF, 0);
                inventory.setItem(slot, item);
            }
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        transitions.remove(event.getPlayer().getUniqueId());
        resetSabers(event.getPlayer().getInventory());
    }
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!saber(item)) return;
        transitions.remove(event.getPlayer().getUniqueId());
        setSaberState(item, Phase.OFF, 0);
        event.getItemDrop().setItemStack(item);
    }
    @EventHandler public void onWorld(PlayerChangedWorldEvent event) {
        transitions.remove(event.getPlayer().getUniqueId());
        resetSabers(event.getPlayer().getInventory());
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        transitions.remove(event.getPlayer().getUniqueId());
        resetSabers(event.getPlayer().getInventory());
    }
    @EventHandler public void onDeath(PlayerDeathEvent event) {
        transitions.remove(event.getEntity().getUniqueId());
        resetSabers(event.getEntity().getInventory());
        for (ItemStack item : event.getDrops())
            if (saber(item)) setSaberState(item, Phase.OFF, 0);
    }
}
