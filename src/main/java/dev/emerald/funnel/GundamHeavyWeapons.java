package dev.emerald.funnel;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Three independent main-hand weapons.
 *
 * - GP02 atomic bazooka: a short warning/charge, animated shell and
 *   mushroom-cloud AoE, NEVER destroys the world.
 * - ASW-G-08 ultra-large mace: heavy vanilla contact strike and a
 *   cooldown-controlled right-click ground shockwave.
 * - GX-9901-DX twin satellite cannon: six deployed backpack reflectors,
 *   two shoulder cannon barrels, charge and double continuous beam.
 *
 * Resource-pack visuals are ItemDisplays. No client mod is required.
 * /funnel give {atomic|mace|satellite} is the only distribution route.
 */
final class GundamHeavyWeapons implements Listener {
    static final String ATOMIC = "atomic";
    static final String MACE = "mace";
    static final String SATELLITE = "satellite";

    private final JavaPlugin plugin;
    private final NamespacedKey kindKey;
    private final NamespacedKey uidKey;
    private final NamespacedKey markerKey;
    private final Map<UUID, Visuals> visuals = new HashMap<>();
    private final Map<UUID, Charge> charging = new HashMap<>();
    private final Map<UUID, Long> cooldownsAtomic = new HashMap<>();
    private final Map<UUID, Long> cooldownsSatellite = new HashMap<>();
    private final Map<UUID, Long> cooldownsMaceSlam = new HashMap<>();
    private final Map<UUID, Long> cooldownsMaceHit = new HashMap<>();
    private final Map<UUID, Long> cooldownsMacePile = new HashMap<>();
    private final Map<UUID, Long> toggleDebounce = new HashMap<>();
    private final Set<UUID> slamDamage = new HashSet<>();

    private final ItemStack shieldVisual = displayItem("gp02_atomic_shield");
    private final ItemStack reflectorVisual = displayItem("gx_satellite_reflector");
    private final ItemStack cannonVisual = displayItem("gx_satellite_barrel");
    private final ItemStack backpackVisual = displayItem("gx_twin_satellite_controller");
    private static final double DX_MUZZLE_DISTANCE = 1.70;

    private static final class Visuals {
        String kind;
        ItemDisplay shield;
        final List<ItemDisplay> reflectors = new ArrayList<>();
        final List<ItemDisplay> barrels = new ArrayList<>();
        boolean opened;
        double progress;
        double barrelPitch;
    }

    private record Charge(String kind, String itemId, UUID world, HeavyChargeState state) {}
    private final Map<UUID, Long> fireDebounce = new HashMap<>();
    private final Map<UUID, Long> lastRightInput = new HashMap<>();

    GundamHeavyWeapons(JavaPlugin plugin) {
        this.plugin = plugin;
        this.kindKey = new NamespacedKey(plugin,"heavy_weapon_type");
        this.uidKey = new NamespacedKey(plugin,"heavy_weapon_serial");
        this.markerKey = new NamespacedKey(plugin,"heavy_weapon_visual");
    }

    private static ItemStack displayItem(String model) {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta=item.getItemMeta();
        meta.setItemModel(new NamespacedKey("funnel",model));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack giftItem(String kind) {
        if (!List.of(ATOMIC,MACE,SATELLITE).contains(kind))
            throw new IllegalArgumentException("Unknown heavy weapon: "+kind);
        ItemStack result = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = result.getItemMeta();
        meta.setUnbreakable(true);
        switch(kind) {
            case ATOMIC -> {
                meta.displayName(Component.text("§bGP02A アトミック・バズーカ"));
                meta.setItemModel(new NamespacedKey("funnel","gp02_atomic_bazooka"));
                meta.lore(List.of(
                    Component.text("§7保持中: 耐熱シールドが左側へ展開"),
                    Component.text("§e右クリック: 装填開始 / 装填完了後に手動発射"),
                    Component.text("§8演出のみ地形を変更しません")));
            }
            case MACE -> {
                meta.displayName(Component.text("§7バルバトスルプスレクス 超大型メイス"));
                meta.setItemModel(new NamespacedKey("funnel","barbatos_ultra_mace"));
                meta.lore(List.of(
                    Component.text("§7左クリック: 強力な近接打撃"),
                    Component.text("§e右クリック: 衝撃波つき叩きつけ"),
                    Component.text("§bしゃがみ右クリック: パイルバンカー刺突"),
                    Component.text("§8地形破壊なし")));
            }
            case SATELLITE -> {
                meta.displayName(Component.text("§eダブルエックス ツインサテライトキャノン"));
                meta.setItemModel(new NamespacedKey("funnel","gx_twin_satellite_controller"));
                meta.lore(List.of(
                    Component.text("§7保持中: 背面ユニットと6枚のリフレクター（手持ち非表示）"),
                    Component.text("§e左クリック / しゃがみ右クリック: 展開・収納"),
                    Component.text("§b通常右クリック: 充填開始・再開 / 完了後に最大3回手動発射")));
            }
        }
        meta.getPersistentDataContainer().set(kindKey,PersistentDataType.STRING,kind);
        meta.getPersistentDataContainer().set(uidKey,PersistentDataType.STRING,
            UUID.randomUUID().toString());
        result.setItemMeta(meta);
        return result;
    }

    private String kind(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType()!=Material.LEATHER_HORSE_ARMOR || !item.hasItemMeta())
            return "";
        String value=item.getItemMeta().getPersistentDataContainer()
            .get(kindKey,PersistentDataType.STRING);
        return value!=null && List.of(ATOMIC,MACE,SATELLITE).contains(value) ? value : "";
    }

    private String weaponSerial(Player player) {
        ItemStack item=player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
            .get(uidKey,PersistentDataType.STRING);
    }

    private boolean enabled(String kind) {
        return plugin.getConfig().getBoolean("heavy-weapons."+kind+".enabled",true);
    }

    private int ticks(String path,int fallback,int minimum,int maximum) {
        return Math.max(minimum,Math.min(maximum,
            plugin.getConfig().getInt("heavy-weapons."+path,fallback)));
    }

    private double number(String path,double fallback,double minimum,double maximum) {
        double v=plugin.getConfig().getDouble("heavy-weapons."+path,fallback);
        return Double.isFinite(v)?Math.max(minimum,Math.min(maximum,v)):fallback;
    }

    void start() {
        for(World world:Bukkit.getWorlds())
            for(ItemDisplay d:world.getEntitiesByClass(ItemDisplay.class))
                if(d.getPersistentDataContainer().has(markerKey,PersistentDataType.BYTE))
                    d.remove();
        Bukkit.getPluginManager().registerEvents(this,plugin);
        Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1L,1L);
    }

    void stop() {
        for(UUID owner:new ArrayList<>(visuals.keySet())) clear(owner);
        charging.clear();
        fireDebounce.clear();
        lastRightInput.clear();
        cooldownsAtomic.clear();
        cooldownsSatellite.clear();
        cooldownsMaceSlam.clear();
        cooldownsMaceHit.clear();
        cooldownsMacePile.clear();
        for(Player player:Bukkit.getOnlinePlayers())restoreMace(player);
        toggleDebounce.clear();
        slamDamage.clear();
    }

    private ItemDisplay display(Player owner,ItemStack visual,float scale) {
        return owner.getWorld().spawn(owner.getLocation(), ItemDisplay.class,d-> {
            d.setItemStack(visual);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setGravity(false);
            d.setSilent(true);
            d.setInvulnerable(true);
            d.setTeleportDuration(0);
            d.setInterpolationDuration(1);
            d.setTransformation(new Transformation(new Vector3f(),
                new Quaternionf(),new Vector3f(scale,scale,scale),new Quaternionf()));
            d.getPersistentDataContainer().set(markerKey,PersistentDataType.BYTE,(byte)1);
        });
    }

    private void ensureVisuals(Player player,String kind) {
        UUID owner=player.getUniqueId();
        Visuals v=visuals.get(owner);
        if(v!=null && !v.kind.equals(kind)) {clear(owner);v=null;}
        if(v==null){v=new Visuals();v.kind=kind;visuals.put(owner,v);}
        if(ATOMIC.equals(kind)) {
            if(v.shield==null || !v.shield.isValid()
                    || !v.shield.getWorld().equals(player.getWorld())) {
                if(v.shield!=null&&v.shield.isValid())v.shield.remove();
                v.shield=display(player,shieldVisual,1.15f);
            }
        } else if(SATELLITE.equals(kind)) {
            if(v.shield==null || !v.shield.isValid() || v.shield.getWorld()!=player.getWorld()) {
                if(v.shield!=null&&v.shield.isValid())v.shield.remove();
                v.shield=display(player,backpackVisual,1.08f);
            }
            boolean rebuild=v.reflectors.size()!=6 || v.barrels.size()!=2
                || v.reflectors.stream().anyMatch(d->!d.isValid()
                    || d.getWorld()!=player.getWorld())
                || v.barrels.stream().anyMatch(d->!d.isValid()
                    || d.getWorld()!=player.getWorld());
            if(rebuild){
                for(ItemDisplay d:v.reflectors)if(d.isValid())d.remove();
                for(ItemDisplay d:v.barrels)if(d.isValid())d.remove();
                v.reflectors.clear();v.barrels.clear();
                for(int i=0;i<6;i++)v.reflectors.add(display(player,reflectorVisual,1.03f));
                for(int i=0;i<2;i++)v.barrels.add(display(player,cannonVisual,1.15f));
            }
        }
    }

    private static Vector front(Location location) {
        Vector d=location.getDirection().setY(0);
        return d.lengthSquared()<1e-8?new Vector(0,0,1):d.normalize();
    }

    private static Location around(Location base,Vector front,Vector right,
                                   double side,double height,double forward) {
        return base.clone().add(right.clone().multiply(side))
            .add(front.clone().multiply(forward)).add(0,height,0);
    }

    private static void pose(ItemDisplay display,Quaternionf turn,float scale) {
        // Rebuild around the display origin; a quaternion gives each
        // solar radiator its own fan rotation, unlike teleport yaw alone.
        display.setTransformation(new Transformation(new Vector3f(),
            turn,new Vector3f(scale,scale,scale),new Quaternionf()));
    }

    private void updateVisuals(Player player,Visuals v) {
        Location base=player.getLocation();
        Vector front=front(base),right=new Vector(front.getZ(),0,-front.getX());
        if(ATOMIC.equals(v.kind)) {
            // The physical GP02 shield is a large front-facing radiator,
            // close to the LEFT forearm and out of the bazooka's muzzle.
            Location position=around(base,front,right,.80,.63,.18);
            position.setYaw(base.getYaw()+7);
            position.setPitch(0);
            v.shield.teleport(position);
        } else if(SATELLITE.equals(v.kind)) {
            double rate=1.0/ticks("satellite.open-ticks",22,5,80);
            v.progress=Math.max(0,Math.min(1,v.progress+(v.opened?rate:-rate)));
            double p=v.progress*v.progress*(3-2*v.progress);
            Location pack=around(base,front,right,0,1.44,-.36);
            pack.setYaw(base.getYaw());pack.setPitch(0);v.shield.teleport(pack);
            Charge current=charging.get(player.getUniqueId());
            boolean aiming=current!=null && SATELLITE.equals(current.kind)
                && (current.state.active() || current.state.ready());
            // Double X really has 3 independently articulated radiator
            // petals on EACH side. The tip of each plate sweeps up/out/down
            // in a six-point fan rather than remaining vertical.
            for(int i=0;i<6;i++) {
                int side=i<3?-1:1,row=i%3;
                double foldLateral=.23+row*.085;
                double fanLateral=.49;
                double sideOffset=side*(foldLateral+(fanLateral-foldLateral)*p);
                double foldedHeight=1.55;
                double openedHeight=row==0?1.72:row==1?1.50:1.28;
                double y=foldedHeight+(openedHeight-foldedHeight)*p;
                double backwards=-.43-.07*p;
                Location pos=around(base,front,right,sideOffset,y,backwards);
                pos.setYaw(base.getYaw());
                pos.setPitch(0);
                ItemDisplay panel=v.reflectors.get(i);
                panel.teleport(pos);
                float roll=(float)Math.toRadians(-side*(row==0?48:row==1?90:132)*p);
                float hingePitch=(float)Math.toRadians(-9*p);
                pose(panel,new Quaternionf().rotateZ(roll).rotateX(hingePitch),1.03f);
            }
            for(int i=0;i<2;i++) {
                int side=i==0?-1:1;
                Location pos=around(base,front,right,side*(.30+.15*p),
                    1.57+.18*p,-.31);
                pos.setYaw(base.getYaw());
                pos.setPitch(0);
                ItemDisplay barrel=v.barrels.get(i);
                barrel.teleport(pos);
                // The twin long cannon barrels pitch together with crosshair
                // while their backpack mounts stay locked to body yaw.
                double deployedPitch=aiming?Math.max(-65,Math.min(65,base.getPitch())):-34;
                v.barrelPitch=Math.toRadians(-80*(1-p)+deployedPitch*p);
                float aim=(float)v.barrelPitch;
                pose(barrel,new Quaternionf().rotateX(aim),1.15f);
            }
            if(p>.92 && aiming
                    && Bukkit.getCurrentTick()%4==0) {
                for(ItemDisplay panel:v.reflectors) {
                    Location glow=panel.getLocation().add(0,.12,.04);
                    player.getWorld().spawnParticle(Particle.DUST,glow,6,
                        .19,.25,.07,0,new Particle.DustOptions(
                            Color.fromRGB(255,225,93),1.25f));
                }
                for(ItemDisplay barrel:v.barrels) {
                    Location focus=satelliteMuzzle(barrel,v.barrelPitch);
                    player.getWorld().spawnParticle(Particle.END_ROD,focus,4,
                        .11,.12,.11,.035);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onInteract(PlayerInteractEvent event) {
        if(event.getHand()!=EquipmentSlot.HAND)return;
        Action action=event.getAction();
        boolean right=action==Action.RIGHT_CLICK_AIR||action==Action.RIGHT_CLICK_BLOCK;
        boolean left=action==Action.LEFT_CLICK_AIR||action==Action.LEFT_CLICK_BLOCK;
        if(!right&&!left)return;
        Player p=event.getPlayer();
        String type=kind(p);
        if(type.isEmpty())return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        if(!enabled(type))return;
        if(ATOMIC.equals(type)&&right)begin(p,ATOMIC);
        else if(MACE.equals(type)&&right) {
            if(p.isSneaking())pileBunker(p);
            else slam(p);
        }
        else if(SATELLITE.equals(type)){
            if(left||p.isSneaking())toggle(p);
            else begin(p,SATELLITE);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onSwing(PlayerArmSwingEvent event) {
        if(event.getHand()!=EquipmentSlot.HAND)return;
        Player p=event.getPlayer();
        String type=kind(p);
        if(type.isEmpty()||!enabled(type))return;
        if(SATELLITE.equals(type))toggle(p);
        else if(MACE.equals(type) && Bukkit.getCurrentTick()%3==0) {
            Location tip=p.getEyeLocation().add(p.getEyeLocation()
                .getDirection().multiply(1.5));
            p.getWorld().spawnParticle(Particle.CRIT,tip,5,.32,.19,.32,.09);
        }
    }

    private void toggle(Player p) {
        long now=Bukkit.getCurrentTick();
        UUID id=p.getUniqueId();
        if(now<toggleDebounce.getOrDefault(id,0L))return;
        toggleDebounce.put(id,now+6);
        ensureVisuals(p,SATELLITE);
        Visuals v=visuals.get(id);
        v.opened=!v.opened;
        Charge c=charging.get(id);
        if(!v.opened && c!=null && SATELLITE.equals(c.kind))c.state.pause();
        p.getWorld().playSound(p.getLocation(),Sound.BLOCK_PISTON_EXTEND,.85f,
            v.opened?1.13f:.81f);
        p.sendActionBar(Component.text(v.opened
            ?"§eツインサテライトキャノン：リフレクター展開"
            :"§7ツインサテライトキャノン：収納・充填量保持"));
    }

    private int chargeDuration(String kind) {
        if(ATOMIC.equals(kind))return ticks("atomic.charge-ticks",55,10,160);
        // Migrate the old shipped 75-tick default even on existing servers.
        int configured=plugin.getConfig().getInt("heavy-weapons.satellite.charge-ticks",240);
        return configured==75?240:Math.max(100,Math.min(1200,configured));
    }

    private void begin(Player p,String kind) {
        UUID id=p.getUniqueId();
        long now=Bukkit.getCurrentTick();
        Long previousInput=lastRightInput.put(id,now);
        // The vanilla client repeats right-click while the button is held.
        // A gap prevents that stream from firing as soon as charging completes.
        boolean freshClick=previousInput==null||now-previousInput>5;
        Map<UUID,Long> cd=kind.equals(ATOMIC)?cooldownsAtomic:cooldownsSatellite;
        if(now<cd.getOrDefault(id,0L)){
            p.sendActionBar(Component.text("§c冷却中 "+((cd.get(id)-now+19)/20)+"秒"));
            return;
        }
        if(kind.equals(SATELLITE)){
            Visuals v=visuals.get(id);
            if(v==null||!v.opened||v.progress<.95){
                p.sendActionBar(Component.text("§7先に左クリックでリフレクターを展開してください"));
                return;
            }
        }
        String itemId=weaponSerial(p);
        if(itemId==null)return;
        Charge c=charging.get(id);
        if(c==null||!c.kind.equals(kind)||!c.itemId.equals(itemId)
                ||!c.world.equals(p.getWorld().getUID())){
            c=new Charge(kind,itemId,p.getWorld().getUID(),
                new HeavyChargeState(chargeDuration(kind),ATOMIC.equals(kind)?1:3));
            charging.put(id,c);
        }
        if(c.state.ready()){
            if(!freshClick||now<fireDebounce.getOrDefault(id,0L))return;
            if(SATELLITE.equals(kind)){
                Visuals v=visuals.get(id);
                if(v==null||v.barrels.size()!=2||v.barrels.stream().anyMatch(d->!d.isValid()))return;
                updateVisuals(p,v);
                fireSatellite(p);
            }else launchAtomic(p);
            fireDebounce.put(id,now+20);
            c.state.consume();
            if(c.state.ready()){
                p.sendActionBar(Component.text("§b照射完了：残り "+c.state.shots()+" / 3 発"));
            }else{
                charging.remove(id);
                cd.put(id,now+(ATOMIC.equals(kind)
                    ?ticks("atomic.cooldown-ticks",600,40,2400)
                    :ticks("satellite.cooldown-ticks",360,40,2400)));
            }
            return;
        }
        if(c.state.active()||!freshClick)return;
        c.state.resume();
        p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,1f,
            kind.equals(ATOMIC)?.60f:1.20f);
        p.sendActionBar(Component.text(kind.equals(ATOMIC)
            ?"§c核弾頭装填中":"§eツインサテライトキャノン：マイクロウェーブ充填"));
    }

    private boolean satelliteCharging(Player p) {
        Charge c=charging.get(p.getUniqueId());
        Visuals v=visuals.get(p.getUniqueId());
        return c!=null && SATELLITE.equals(c.kind) && c.state.active()
            && SATELLITE.equals(kind(p)) && c.itemId.equals(weaponSerial(p))
            && c.world.equals(p.getWorld().getUID()) && enabled(SATELLITE)
            && v!=null && v.opened;
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onChargeMove(PlayerMoveEvent e) {
        if(e instanceof PlayerTeleportEvent || !satelliteCharging(e.getPlayer()))return;
        Location to=e.getTo(),from=e.getFrom();
        if(to==null)return;
        if(from.getX()!=to.getX()||from.getY()!=to.getY()||from.getZ()!=to.getZ()){
            Location locked=from.clone();
            locked.setYaw(to.getYaw());locked.setPitch(to.getPitch());
            e.setTo(locked);
        }
    }

    private void tick() {
        long now=Bukkit.getCurrentTick();
        for(UUID id:new ArrayList<>(visuals.keySet())) {
            Player p=Bukkit.getPlayer(id);
            if(p==null||!p.isOnline()||p.isDead()
                ||!visuals.get(id).kind.equals(kind(p))
                ||!enabled(visuals.get(id).kind))clear(id);
        }
        for(Player p:Bukkit.getOnlinePlayers()) {
            String k=kind(p);
            if(!k.isEmpty()&&!p.isDead()&&enabled(k)&&!k.equals(MACE)) {
                ensureVisuals(p,k);
                updateVisuals(p,visuals.get(p.getUniqueId()));
            }
        }
        for(var entry:new ArrayList<>(charging.entrySet())) {
            UUID id=entry.getKey();
            Charge c=entry.getValue();
            Player p=Bukkit.getPlayer(id);
            if(p==null||!p.isOnline()||p.isDead()
                ||!c.kind.equals(kind(p))
                ||!c.world.equals(p.getWorld().getUID())
                ||!c.itemId.equals(weaponSerial(p))||!enabled(c.kind)){
                charging.remove(id);continue;
            }
            if(c.state.ready()){
                if(now%20==0)p.sendActionBar(Component.text(ATOMIC.equals(c.kind)
                    ?"§a核弾頭装填完了：右クリックで発射"
                    :"§a充填完了：右クリックで発射（残り "+c.state.shots()+" / 3 発）"));
                continue;
            }
            if(!c.state.active())continue;
            if(SATELLITE.equals(c.kind)){
                if(!satelliteCharging(p)){c.state.pause();continue;}
                // Slowness VII exceeds 100%; refresh short-lived effect only
                // during charging so no indefinite debuff leaks after folding.
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,3,6,true,false,false));
            }
            if(c.state.advance()){
                p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,1.1f,1.5f);
                p.sendActionBar(Component.text(ATOMIC.equals(c.kind)
                    ?"§a核弾頭装填完了：右クリックで発射"
                    :"§a充填完了：右クリックで発射（残り 3 / 3 発）"));
            }else if(now%5==0){
                p.sendActionBar(Component.text("§e"+(ATOMIC.equals(c.kind)
                    ?"核弾頭装填中":"サテライト充填")+" "+c.state.percent()+"%"));
                Location chargeAt=p.getEyeLocation().add(
                    p.getEyeLocation().getDirection().multiply(1.2));
                p.getWorld().spawnParticle(Particle.END_ROD,chargeAt,5,.22,.22,.22,.015);
            }
        }
    }

    private static boolean targetable(Entity entity,Player shooter) {
        return entity instanceof LivingEntity living && entity!=shooter
            && living.isValid()&&!living.isDead()
            && !(entity instanceof Player other&&other.getGameMode()==GameMode.SPECTATOR);
    }

    private static Location shotAim(Player shooter,double range) {
        Location eye=shooter.getEyeLocation();
        Vector direction=eye.getDirection().normalize();
        World w=shooter.getWorld();
        RayTraceResult wall=w.rayTraceBlocks(eye,direction,range,
            FluidCollisionMode.NEVER,true);
        double distance=wall==null?range:Math.min(range,
            eye.toVector().distance(wall.getHitPosition()));
        RayTraceResult entity=w.rayTraceEntities(eye,direction,distance,.24,
            e->targetable(e,shooter));
        return eye.clone().add(direction.multiply(entity==null?distance:
            Math.min(distance,eye.toVector().distance(entity.getHitPosition()))));
    }

    private void launchAtomic(Player p) {
        World w=p.getWorld();
        Location destination=shotAim(p,number("atomic.range",48,8,80));
        Location eye=p.getEyeLocation();
        Vector view=eye.getDirection().normalize();
        Vector right=new Vector(view.getZ(),0,-view.getX());
        Location start=eye.clone().add(view.clone().multiply(.60))
            .add(right.multiply(.24)).add(0,-.23,0);
        Vector dir=destination.toVector().subtract(start.toVector()).normalize();
        double length=destination.distance(start);
        w.playSound(start,Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,2.5f,.45f);
        w.playSound(start,Sound.ENTITY_GENERIC_EXPLODE,1.8f,.55f);
        w.playSound(start,Sound.BLOCK_PISTON_EXTEND,1.8f,.55f);
        w.spawnParticle(Particle.FLASH,start,2,.08,.08,.08,0,Color.fromRGB(255,239,190));
        w.spawnParticle(Particle.SMOKE,start,32,.28,.28,.28,.035);
        w.spawnParticle(Particle.CLOUD,start,24,.22,.22,.22,.04);
        w.spawnParticle(Particle.FLAME,start,18,.15,.15,.15,.035);
        Vector recoil=front(p.getLocation()).multiply(-.28);
        p.setVelocity(p.getVelocity().add(recoil));
        // The shell is a cosmetic projectile only. No TNT, world mutation or
        // real explosive entity is spawned; damage is applied server-side once.
        new org.bukkit.scheduler.BukkitRunnable() {
            double travelled=0;
            @Override public void run(){
                if(!p.isOnline()||!w.equals(p.getWorld())){cancel();return;}
                travelled=Math.min(length,travelled+1.9);
                Location at=start.clone().add(dir.clone().multiply(travelled));
                w.spawnParticle(Particle.FLAME,at,8,.13,.13,.13,.03);
                w.spawnParticle(Particle.SMOKE,at,16,.22,.22,.22,.025);
                w.spawnParticle(Particle.END_ROD,at,3,.07,.07,.07,.01);
                if(travelled>=length-.001){
                    cancel();
                    atomicBlast(p,destination);
                }
            }
        }.runTaskTimer(plugin,1L,1L);
    }

    private void atomicBlast(Player shooter,Location origin) {
        World w=origin.getWorld();
        double radius=number("atomic.radius",7.0,1.0,12.0);
        double baseDamage=number("atomic.damage",1.5,0,1.5);
        w.playSound(origin,Sound.ENTITY_GENERIC_EXPLODE,3f,.52f);
        w.playSound(origin,Sound.BLOCK_BEACON_DEACTIVATE,2.6f,.5f);
        w.spawnParticle(Particle.EXPLOSION,origin,14,1.15,1.15,1.15,.025);
        w.spawnParticle(Particle.FLASH,origin,4,.65,.65,.65,0,Color.fromRGB(255,245,210));
        w.spawnParticle(Particle.CLOUD,origin,60,1.4,.65,1.4,.06);
        w.spawnParticle(Particle.SMOKE,origin,55,1.1,.8,1.1,.05);
        w.spawnParticle(Particle.FLAME,origin,30,.85,.85,.85,.035);
        for(Entity e:w.getNearbyEntities(origin,radius,radius,radius)) {
            if(!targetable(e,shooter))continue;
            LivingEntity t=(LivingEntity)e;
            double distance=t.getLocation().add(0,t.getHeight()*.5,0).distance(origin);
            if(distance>radius)continue;
            Vector dir=t.getEyeLocation().toVector().subtract(origin.toVector());
            double len=dir.length();
            if(len>1e-3) {
                RayTraceResult block=w.rayTraceBlocks(origin.clone().add(0,.65,0),
                    dir.normalize(),len,FluidCollisionMode.NEVER,true);
                if(block!=null&&origin.toVector().distance(block.getHitPosition())+.3<len)
                    continue;
            }
            double amount=baseDamage;
            if(amount>0)t.damage(amount,shooter);
            Vector push=t.getLocation().toVector().subtract(origin.toVector());
            if(push.lengthSquared()>1e-5){
                push.normalize().multiply(Math.max(.1,1.2*(1-distance/radius)));
                push.setY(.35);t.setVelocity(t.getVelocity().add(push));
            }
        }
        // Bounded, short-lived animated mushroom cloud. No blocks are changed.
        new org.bukkit.scheduler.BukkitRunnable(){
            int frame=0;
            @Override public void run(){
                if(frame++>=22){cancel();return;}
                double ring=(frame/22.0)*radius*1.35;
                int count=34;
                for(int i=0;i<count;i++){
                    double theta=i*Math.PI*2/count;
                    Location ringPoint=origin.clone().add(Math.cos(theta)*ring,
                        .13+frame*.12,Math.sin(theta)*ring);
                    w.spawnParticle(frame<5?Particle.FLAME:Particle.SMOKE,
                        ringPoint,1,0,0,0,0);
                }
                Location stem=origin.clone().add(0,frame*.47,0);
                w.spawnParticle(Particle.CLOUD,stem,19,.32,.32,.32,.045);
                w.spawnParticle(Particle.SMOKE,stem,16,.60,.34,.60,.025);
                if(frame>=6){
                    Location crown=origin.clone().add(0,5.4,0);
                    w.spawnParticle(Particle.CLOUD,crown,40,
                        1.5+frame*.10,.48,1.5+frame*.10,.035);
                    w.spawnParticle(Particle.SMOKE,crown,20,1.4+frame*.08,.45,1.4+frame*.08,.03);
                }
            }
        }.runTaskTimer(plugin,0L,2L);
    }

    /**
     * Lupus Rex's spear point is a real, separately modeled pile bunker.
     * Sneak + right-click plays an extended weapon model and after 6 ticks
     * inflicts one short-range, block-occluded armor-piercing strike.
     * All modified items are restored on cancel, logout, death and disable.
     */
    private void pileBunker(Player p) {
        long now=Bukkit.getCurrentTick();
        UUID owner=p.getUniqueId();
        if(now<cooldownsMacePile.getOrDefault(owner,0L))return;
        int cooldown=ticks("mace.pile-cooldown-ticks",165,20,600);
        cooldownsMacePile.put(owner,now+cooldown);
        String serial=weaponSerial(p);
        if(serial==null)return;
        ItemStack held=p.getInventory().getItemInMainHand();
        ItemMeta meta=held.getItemMeta();
        meta.setItemModel(new NamespacedKey("funnel","barbatos_ultra_mace_pile"));
        held.setItemMeta(meta);
        p.getInventory().setItemInMainHand(held);
        World world=p.getWorld();
        UUID worldId=world.getUID();
        world.playSound(p.getLocation(),Sound.BLOCK_PISTON_EXTEND,1.4f,.52f);
        p.sendActionBar(Component.text("§7超大型メイス：パイルバンカー射出！"));
        Bukkit.getScheduler().runTaskLater(plugin,() -> {
            Player shooter=Bukkit.getPlayer(owner);
            if(shooter==null||!shooter.isOnline())return;
            if(shooter.isDead()||!worldId.equals(shooter.getWorld().getUID())
                ||!MACE.equals(kind(shooter))||!serial.equals(weaponSerial(shooter))) {
                restoreMace(shooter);
                return;
            }
            Location eye=shooter.getEyeLocation();
            Vector ray=eye.getDirection().normalize();
            double reach=number("mace.pile-range",4.5,2,8);
            RayTraceResult wall=world.rayTraceBlocks(eye,ray,reach,
                FluidCollisionMode.NEVER,true);
            double unobstructed=wall==null?reach:Math.min(reach,
                eye.toVector().distance(wall.getHitPosition()));
            RayTraceResult hit=world.rayTraceEntities(eye,ray,unobstructed,.45,
                entity->targetable(entity,shooter));
            if(hit!=null&&hit.getHitEntity() instanceof LivingEntity victim) {
                double damage=number("mace.pile-damage",1.5,0,1.5);
                slamDamage.add(owner);
                try {if(damage>0)victim.damage(damage,shooter);}
                finally {slamDamage.remove(owner);}
                Vector away=ray.clone().multiply(.66);away.setY(.38);
                if(victim.isValid())
                    victim.setVelocity(victim.getVelocity().add(away));
            }
            double visual=hit==null?unobstructed:Math.min(unobstructed,
                eye.toVector().distance(hit.getHitPosition()));
            for(double d=0;d<=visual;d+=.22) {
                Location at=eye.clone().add(ray.clone().multiply(d));
                world.spawnParticle(Particle.CRIT,at,2,.08,.08,.08,.08);
            }
            world.playSound(shooter.getLocation(),Sound.ENTITY_IRON_GOLEM_ATTACK,
                1.22f,1.35f);
            restoreMace(shooter);
        },6L);
    }

    private void restoreMace(Player player) {
        if(player==null)return;
        var inv=player.getInventory();
        ItemStack[] contents=inv.getContents();
        for(int slot=0;slot<contents.length;slot++){
            ItemStack stack=contents[slot];
            if(stack==null||!stack.hasItemMeta())continue;
            ItemMeta meta=stack.getItemMeta();
            if(!MACE.equals(meta.getPersistentDataContainer()
                .get(kindKey,PersistentDataType.STRING)))continue;
            NamespacedKey itemModel=meta.getItemModel();
            if(itemModel!=null && itemModel.equals(
                new NamespacedKey("funnel","barbatos_ultra_mace_pile"))) {
                meta.setItemModel(new NamespacedKey("funnel","barbatos_ultra_mace"));
                stack.setItemMeta(meta);
                inv.setItem(slot,stack);
            }
        }
    }

    private void slam(Player p) {
        long now=Bukkit.getCurrentTick();
        UUID id=p.getUniqueId();
        if(now<cooldownsMaceSlam.getOrDefault(id,0L))return;
        cooldownsMaceSlam.put(id,now+ticks("mace.slam-cooldown-ticks",110,15,400));
        Vector horizontal=front(p.getLocation());
        Location center=p.getLocation().add(horizontal.multiply(1.8));
        center.setY(Math.max(center.getY(),p.getLocation().getY())+.2);
        World w=p.getWorld();
        double radius=number("mace.slam-radius",4.0,1,8);
        double damage=number("mace.slam-damage",1.5,0,1.5);
        w.playSound(center,Sound.ENTITY_IRON_GOLEM_ATTACK,1.4f,.60f);
        w.playSound(center,Sound.ENTITY_GENERIC_EXPLODE,.75f,.91f);
        for(int wave=0;wave<3;wave++){
            double ring=(wave+1)*radius/3;
            for(int j=0;j<42;j++){
                double angle=2*Math.PI*j/42;
                Location pt=center.clone().add(Math.cos(angle)*ring,.15,Math.sin(angle)*ring);
                w.spawnParticle(Particle.CRIT,pt,1,0,0,0,.05);
                w.spawnParticle(Particle.DUST,pt,1,0,0,0,0,
                    new Particle.DustOptions(Color.fromRGB(117,125,137),1.3f));
            }
        }
        slamDamage.add(id);
        try {
            for(Entity entity:w.getNearbyEntities(center,radius,2.7,radius)) {
                if(!targetable(entity,p))continue;
                LivingEntity t=(LivingEntity)entity;
                double distance=t.getLocation().distance(center);
                if(distance>radius+.2)continue;
                t.damage(damage,p);
                Vector away=t.getLocation().toVector().subtract(center.toVector());
                if(away.lengthSquared()>1e-6){
                    away.normalize().multiply(.65);away.setY(.57);
                    t.setVelocity(t.getVelocity().add(away));
                }
            }
        }finally{slamDamage.remove(id);}
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onMaceHit(EntityDamageByEntityEvent event) {
        if(!(event.getDamager() instanceof Player p))return;
        if(!MACE.equals(kind(p))||!enabled(MACE))return;
        UUID id=p.getUniqueId();
        if(slamDamage.contains(id))return;
        long now=Bukkit.getCurrentTick();
        if(now<cooldownsMaceHit.getOrDefault(id,0L)){
            event.setCancelled(true);return;
        }
        cooldownsMaceHit.put(id,now+ticks("mace.hit-cooldown-ticks",11,2,40));
        event.setDamage(number("mace.hit-damage",1.5,0,1.5));
        if(event.getEntity() instanceof LivingEntity target){
            Vector dir=target.getLocation().toVector().subtract(p.getLocation().toVector());
            if(dir.lengthSquared()>1e-6){
                dir.normalize().multiply(.52);dir.setY(.24);
                Bukkit.getScheduler().runTask(plugin,
                    () -> {if(target.isValid())target.setVelocity(target.getVelocity().add(dir));});
            }
            target.getWorld().spawnParticle(Particle.CRIT,
                target.getLocation().add(0,target.getHeight()*.5,0),
                16,.33,.32,.33,.12);
        }
    }

    // Matches the 31.688 model-space muzzle at scale 1.15, pivot 8.
    private static Location satelliteMuzzle(ItemDisplay barrel,double pitch) {
        Vector axis=front(barrel.getLocation()).multiply(Math.cos(pitch)).setY(-Math.sin(pitch));
        return barrel.getLocation().add(axis.multiply(DX_MUZZLE_DISTANCE));
    }

    private void fireSatellite(Player p) {
        Visuals v=visuals.get(p.getUniqueId());
        if(v==null||!v.opened||v.progress<.95||v.barrels.size()!=2)return;
        World w=p.getWorld();
        double range=number("satellite.range",56,8,90);
        Location aimed=shotAim(p,range);
        double perBeam=number("satellite.beam-damage",1.5,0,1.5);
        Map<UUID,LivingEntity> hit=new HashMap<>();
        Location[] start=new Location[2];
        Vector[] direction=new Vector[2];
        double[] length=new double[2];
        for(int i=0;i<2;i++){
            ItemDisplay barrel=v.barrels.get(i);
            if(!barrel.isValid())continue;
            Location muzzle=satelliteMuzzle(barrel,v.barrelPitch);
            start[i]=muzzle;
            direction[i]=aimed.toVector().subtract(muzzle.toVector()).normalize();
            RayTraceResult wall=w.rayTraceBlocks(muzzle,direction[i],range,
                FluidCollisionMode.NEVER,true);
            double visible=wall==null?range:Math.min(range,
                muzzle.toVector().distance(wall.getHitPosition()));
            RayTraceResult trace=w.rayTraceEntities(muzzle,direction[i],visible,.38,
                e->targetable(e,p));
            length[i]=trace==null?visible:Math.min(visible,
                muzzle.toVector().distance(trace.getHitPosition()));
            if(trace!=null&&trace.getHitEntity() instanceof LivingEntity living){
                hit.put(living.getUniqueId(),living);
            }
        }
        for(var entry:hit.entrySet()){
            LivingEntity target=entry.getValue();
            if(target.isValid()&&!target.isDead())
                target.damage(perBeam,p);
        }
        w.playSound(p.getLocation(),Sound.ENTITY_GUARDIAN_ATTACK,2f,.48f);
        new org.bukkit.scheduler.BukkitRunnable(){
            int t=0;
            @Override public void run(){
                if(t++>=15){cancel();return;}
                for(int i=0;i<2;i++){
                    if(start[i]==null)continue;
                    for(double step=0;step<length[i]&&step<90;step+=.50) {
                        Location at=start[i].clone().add(
                            direction[i].clone().multiply(step));
                        w.spawnParticle(Particle.DUST,at,2,.025,.025,.025,0,
                            new Particle.DustOptions(Color.fromRGB(220,250,255),2.7f));
                        w.spawnParticle(Particle.DUST,at,4,.16,.16,.16,0,
                            new Particle.DustOptions(Color.fromRGB(145,237,255),3.2f));
                        w.spawnParticle(Particle.DUST,at,3,.29,.29,.29,0,
                            new Particle.DustOptions(Color.fromRGB(248,226,118),2.5f));
                    }
                    w.spawnParticle(Particle.END_ROD,start[i],8,.28,.23,.28,.04);
                }
            }
        }.runTaskTimer(plugin,0L,1L);
        p.sendActionBar(Component.text("§bツインサテライトキャノン：照射完了"));
    }

    private void clear(UUID playerId) {
        charging.remove(playerId);
        fireDebounce.remove(playerId);
        lastRightInput.remove(playerId);
        Visuals v=visuals.remove(playerId);
        if(v==null)return;
        if(v.shield!=null&&v.shield.isValid())v.shield.remove();
        for(ItemDisplay d:v.reflectors)if(d.isValid())d.remove();
        for(ItemDisplay d:v.barrels)if(d.isValid())d.remove();
    }

    boolean command(CommandSender sender,String[] args) {
        if(args.length==2&&args[1].equalsIgnoreCase("status")){
            if(!(sender instanceof Player p)){
                sender.sendMessage("§7ゲーム内で確認してください");return true;
            }
            String kind=kind(p);
            long now=Bukkit.getCurrentTick();
            long until=ATOMIC.equals(kind)?cooldownsAtomic.getOrDefault(p.getUniqueId(),0L)
                :SATELLITE.equals(kind)?cooldownsSatellite.getOrDefault(p.getUniqueId(),0L)
                :cooldownsMaceSlam.getOrDefault(p.getUniqueId(),0L);
            sender.sendMessage("§b新兵装 §7/ §f"+kind+
                " §7/ クールダウン: §f"+Math.max(0,(until-now+19)/20)+"秒");
            return true;
        }
        sender.sendMessage("§7配布: /funnel give <atomic|mace|satellite> [player]"
            +" §7/ 状態: /funnel <atomic|mace|satellite> status");
        return true;
    }

    @EventHandler public void onQuit(PlayerQuitEvent e){restoreMace(e.getPlayer());clear(e.getPlayer().getUniqueId());}
    @EventHandler public void onDeath(PlayerDeathEvent e){restoreMace(e.getEntity());clear(e.getEntity().getUniqueId());}
    @EventHandler public void onWorld(PlayerChangedWorldEvent e){restoreMace(e.getPlayer());clear(e.getPlayer().getUniqueId());}
    @EventHandler public void onHeld(PlayerItemHeldEvent e){restoreMace(e.getPlayer());clear(e.getPlayer().getUniqueId());}
}
