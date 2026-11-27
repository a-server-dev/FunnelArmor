package dev.emerald.funnel;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

final class GundamAdvancedWeapons implements Listener {
    static final List<String> TYPES=List.of("epyon","heavyarms","freedom");
    static final int ROD_LINKS=14, ROTORS=4, WINGS=10;
    static final int HEAT_LIMIT=100;
    private final JavaPlugin plugin;
    private final EpyonOffhand epyonOffhand;
    private final NamespacedKey epyonSwordModel=new NamespacedKey("funnel","epyon_beam_sword");
    private final NamespacedKey kindKey, serialKey, marker;
    private final Map<UUID,State> active=new HashMap<>();
    private final Map<String,ItemStack> models=new HashMap<>();
    private final Set<UUID> applyingDamage=new HashSet<>();
    private BukkitTask task;

    private static final class State {
        final String kind, serial;
        final UUID world;
        final List<ItemDisplay> parts=new ArrayList<>();
        final Set<UUID> hit=new HashSet<>();
        boolean opened,firing,pull,overheated;
        double progress,spin,spinAngle,heat;
        FreedomMode freedomMode=FreedomMode.CLOSED;
        double cannonProgress;
        final List<Casing> casings=new ArrayList<>();
        long lastRight=-100;
        int gatlingPhase;
        long nextInput,nextToggle,nextShot,attackStart=-1;
        State(String kind,String serial,UUID world){this.kind=kind;this.serial=serial;this.world=world;}
    }

    private record Casing(Location location,Vector velocity,int life) {}

    GundamAdvancedWeapons(JavaPlugin plugin){
        this.plugin=plugin;
        this.epyonOffhand=new EpyonOffhand(plugin);
        kindKey=new NamespacedKey(plugin,"advanced_weapon");
        serialKey=new NamespacedKey(plugin,"advanced_weapon_serial");
        marker=new NamespacedKey(plugin,"advanced_weapon_display");
    }
    private ItemStack model(String id){
        return models.computeIfAbsent(id,key->{
            ItemStack item=new ItemStack(Material.LEATHER_HORSE_ARMOR);
            ItemMeta m=item.getItemMeta();
            m.setItemModel(new NamespacedKey("funnel",key));m.setUnbreakable(true);
            item.setItemMeta(m);return item;
        });
    }
    ItemStack giftItem(String type){
        if(!TYPES.contains(type))throw new IllegalArgumentException(type);
        ItemStack item=model(switch(type){
            case "epyon"->"epyon_beam_sword";
            case "heavyarms"->"heavyarms_gatling_body";
            default->"freedom_lupus_rifle";
        }).clone();
        ItemMeta m=item.getItemMeta();
        m.displayName(Component.text(switch(type){
            case "epyon"->"§cガンダムエピオン ビームソード";
            case "heavyarms"->"§9ヘビーアームズ改EW ダブルガトリング";
            default->"§bフリーダム ハイマットフルバースト";
        }));
        m.lore(switch(type){
            case "epyon"->epyonLore();
            case "heavyarms"->heavyarmsLore();
            default->List.of(Component.text("§7左クリック: 収納 / ハイマット"),Component.text("§bしゃがみ左クリック: ハイマット / フルバースト"),Component.text("§e右クリック: ライフル / フルバースト時5門同時射撃"),Component.text("§bフルバーストでしゃがみ右クリック: マルチロックオン"));
        });
        m.getPersistentDataContainer().set(kindKey,PersistentDataType.STRING,type);
        m.getPersistentDataContainer().set(serialKey,PersistentDataType.STRING,UUID.randomUUID().toString());
        item.setItemMeta(m);
        if(type.equals("heavyarms"))prepareHeavyarms(item);
        return item;
    }
    private static List<Component> epyonLore(){
        return List.of(Component.text("§a左クリック: ビームソード攻撃"),
            Component.text("§eしゃがみ右クリック: 盾装着 / 収納"),
            Component.text("§7盾装着中の右クリック: ヒートロッド攻撃"));
    }
    private void migrateEpyon(Player p){
        ItemStack item=p.getInventory().getItemInMainHand();
        ItemMeta meta=item.getItemMeta();
        if(epyonSwordModel.equals(meta.getItemModel()))return;
        meta.setItemModel(epyonSwordModel);
        meta.displayName(Component.text("§cガンダムエピオン ビームソード"));
        meta.lore(epyonLore());item.setItemMeta(meta);
        p.getInventory().setItemInMainHand(item);
    }
    private static List<Component> heavyarmsLore(){
        return List.of(Component.text("§e右クリック長押し: 連射 / 離すと停止"),
            Component.text("§7過熱時は冷却"));
    }
    private void prepareHeavyarms(ItemStack item){
        ItemMeta m=item.getItemMeta();m.lore(heavyarmsLore());item.setItemMeta(m);
        item.setData(DataComponentTypes.CONSUMABLE,Consumable.consumable()
            .consumeSeconds(3600f).animation(ItemUseAnimation.NONE).hasConsumeParticles(false)
            .sound(net.kyori.adventure.key.Key.key("minecraft:intentionally_empty")));
        item.setData(DataComponentTypes.USE_EFFECTS,UseEffects.useEffects()
            .speedMultiplier(1f).canSprint(true).interactVibrations(false));
    }
    private void migrateHeavyarms(Player p){
        ItemStack item=p.getInventory().getItemInMainHand();
        if(!item.hasData(DataComponentTypes.CONSUMABLE)||!item.hasData(DataComponentTypes.USE_EFFECTS)){
            prepareHeavyarms(item);p.getInventory().setItemInMainHand(item);
        }
    }
    private boolean holdingTrigger(Player p,State s){
        if(!p.hasActiveItem()||p.getActiveItemHand()!=EquipmentSlot.HAND)return false;
        ItemStack item=p.getActiveItem();
        if(!item.hasItemMeta())return false;
        return s.serial.equals(item.getItemMeta().getPersistentDataContainer()
            .get(serialKey,PersistentDataType.STRING));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onConsumeTrigger(PlayerItemConsumeEvent e){
        ItemStack item=e.getItem();
        if(item.hasItemMeta()&&"heavyarms".equals(item.getItemMeta()
            .getPersistentDataContainer().get(kindKey,PersistentDataType.STRING)))e.setCancelled(true);
    }
    private String value(Player p,NamespacedKey key){
        ItemStack i=p.getInventory().getItemInMainHand();
        if(!i.hasItemMeta())return "";
        String s=i.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
        return s==null?"":s;
    }
    private String kind(Player p){String s=value(p,kindKey);return TYPES.contains(s)?s:"";}
    private boolean enabled(String k){return plugin.getConfig().getBoolean("advanced-weapons."+k+".enabled",true);}
    private double damage(String k,double cap){
        double d=plugin.getConfig().getDouble("advanced-weapons."+k+".damage",cap);
        return Double.isFinite(d)?Math.max(0,Math.min(cap,d)):cap;
    }
    void start(){
        for(World w:Bukkit.getWorlds())for(ItemDisplay d:w.getEntitiesByClass(ItemDisplay.class))
            if(d.getPersistentDataContainer().has(marker,PersistentDataType.BYTE))d.remove();
        Bukkit.getPluginManager().registerEvents(this,plugin);
        Bukkit.getPluginManager().registerEvents(epyonOffhand,plugin);
        for(Player p:Bukkit.getOnlinePlayers())epyonOffhand.restore(p);
        task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
    }
    void stop(){if(task!=null)task.cancel();for(UUID id:new ArrayList<>(active.keySet()))clear(id);for(Player p:Bukkit.getOnlinePlayers())epyonOffhand.restore(p);applyingDamage.clear();}
    private void clear(UUID id){
        State s=active.remove(id);
        if(s!=null){
            for(ItemDisplay d:s.parts)if(d.isValid())d.remove();
            if(s.kind.equals("epyon")){Player p=Bukkit.getPlayer(id);if(p!=null)epyonOffhand.restore(p);}
        }
    }
    private ItemDisplay spawn(Player p,String id){
        return p.getWorld().spawn(p.getLocation(),ItemDisplay.class,d->{
            d.setItemStack(model(id));d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.FIXED);d.setPersistent(false);d.setGravity(false);
            d.setInvulnerable(true);d.setSilent(true);d.setTeleportDuration(0);
            d.setInterpolationDuration(1);d.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);
        });
    }
    private State ensure(Player p,String k){
        UUID id=p.getUniqueId();String serial=value(p,serialKey);
        State s=active.get(id);
        if(s!=null&&(!s.kind.equals(k)||!s.serial.equals(serial)||!s.world.equals(p.getWorld().getUID())
            ||s.parts.stream().anyMatch(d->!d.isValid()))){clear(id);s=null;}
        if(s!=null)return s;
        s=new State(k,serial,p.getWorld().getUID());active.put(id,s);
        switch(k){
            case "epyon"->{
                for(int i=0;i<ROD_LINKS;i++)s.parts.add(spawn(p,i==ROD_LINKS-1?"epyon_rod_tip":"epyon_rod_link"));
            }
            case "heavyarms"->{
                for(int i=0;i<2;i++)s.parts.add(spawn(p,"heavyarms_gatling_body"));
                for(int i=0;i<ROTORS;i++)s.parts.add(spawn(p,"heavyarms_gatling_rotor"));
            }
            case "freedom"->{
                s.parts.add(spawn(p,"freedom_backpack"));
                for(int i=0;i<WINGS;i++)s.parts.add(spawn(p,FreedomWingLayout.model(i%5)));
                for(int i=0;i<2;i++)s.parts.add(spawn(p,"freedom_balaena"));
                for(int i=0;i<2;i++)s.parts.add(spawn(p,"freedom_railgun"));
                s.parts.add(spawn(p,"freedom_lupus_rifle"));
            }
        }
        return s;
    }
    private static Vector front(Player p){return new Vector(-Math.sin(Math.toRadians(p.getLocation().getYaw())),0,Math.cos(Math.toRadians(p.getLocation().getYaw())));}
    private static Vector right(Vector f){return new Vector(f.getZ(),0,-f.getX());}
    private static Location at(Player p,Vector f,Vector r,double x,double y,double z){
        return p.getLocation().add(r.clone().multiply(x)).add(0,y,0).add(f.clone().multiply(z));
    }
    private static void place(ItemDisplay d,Location l,Quaternionf q,float scale){
        d.teleport(l);
        d.setTransformation(new Transformation(new Vector3f(),q,new Vector3f(scale,scale,scale),new Quaternionf()));
    }
    private static void aim(ItemDisplay d,Location l,Vector direction,double spin,float scale){
        l=l.clone();if(direction.lengthSquared()>1e-9)l.setDirection(direction);
        place(d,l,new Quaternionf().rotateZ((float)spin),scale);
    }
    private static double smooth(double p){return p*p*(3-2*p);}
    private static double approach(double current,double target,double step){return current<target?Math.min(target,current+step):Math.max(target,current-step);}
    private boolean targetable(Entity e,Player p){
        return e instanceof LivingEntity l && !l.isDead() && !e.equals(p) && !(e instanceof ArmorStand)
            && (!(e instanceof Player q)||(q.getGameMode()!=GameMode.CREATIVE&&q.getGameMode()!=GameMode.SPECTATOR));
    }
    private void hurt(Player p,LivingEntity target,double amount){
        if(amount<=0||!targetable(target,p))return;
        applyingDamage.add(p.getUniqueId());
        try{target.damage(amount,p);}finally{applyingDamage.remove(p.getUniqueId());}
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onInteract(PlayerInteractEvent e){
        Player p=e.getPlayer();String k=kind(p);if(k.isEmpty())return;
        Action a=e.getAction();boolean left=a==Action.LEFT_CLICK_AIR||a==Action.LEFT_CLICK_BLOCK;
        boolean right=a==Action.RIGHT_CLICK_AIR||a==Action.RIGHT_CLICK_BLOCK;
        if(e.getHand()!=EquipmentSlot.HAND){
            if(k.equals("epyon")&&right&&epyonOffhand.equipped(p)){
                e.setUseItemInHand(Event.Result.DENY);e.setUseInteractedBlock(Event.Result.DENY);e.setCancelled(true);
                if(enabled(k))use(p,k);
            }
            return;
        }
        if(!left&&!right)return;
        if(k.equals("heavyarms")&&right&&enabled(k)){
            // Allow the vanilla use-item state, not block interaction. The
            // actual release packet clears hasActiveItem on the server.
            migrateHeavyarms(p);
            e.setCancelled(false);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.ALLOW);
            return;
        }
        e.setUseItemInHand(Event.Result.DENY);e.setUseInteractedBlock(Event.Result.DENY);e.setCancelled(true);
        if(!enabled(k))return;
        if(left)left(p,k);else use(p,k);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onSwing(PlayerArmSwingEvent e){
        if(e.getHand()!=EquipmentSlot.HAND)return;
        String k=kind(e.getPlayer());if(!k.isEmpty()&&enabled(k))left(e.getPlayer(),k);
    }
    private void left(Player p,String k){
        if(k.equals("heavyarms"))return;
        State s=ensure(p,k);long now=Bukkit.getCurrentTick();
        if(now<s.nextToggle||now<s.nextInput)return;s.nextToggle=now+6;
        if(k.equals("epyon")){swordAttack(p);return;}
        if(k.equals("freedom")){
            s.freedomMode=s.freedomMode.next(p.isSneaking());
            p.getWorld().playSound(p.getLocation(),Sound.BLOCK_PISTON_EXTEND,.75f,
                s.freedomMode==FreedomMode.CLOSED?.75f:1.15f);
            p.sendActionBar(Component.text("§bフリーダム："+s.freedomMode.label()));
            return;
        }
    }
    private void swordAttack(Player p){
        Location eye=p.getEyeLocation();Vector d=eye.getDirection();
        Set<UUID> hit=new HashSet<>();
        // A modest arc of five rays, occluded by blocks. The same victim is
        // hit at most once, even when several rays intersect it.
        for(int i=-2;i<=2;i++){
            Vector ray=d.clone().rotateAroundY(Math.toRadians(i*12));
            RayTraceResult wall=p.getWorld().rayTraceBlocks(eye,ray,3.4,FluidCollisionMode.NEVER,true);
            double range=wall==null?3.4:eye.toVector().distance(wall.getHitPosition());
            if(range<=.01)continue;
            RayTraceResult contact=p.getWorld().rayTraceEntities(eye,ray,range,.25,e->targetable(e,p));
            if(contact!=null&&contact.getHitEntity() instanceof LivingEntity target&&hit.add(target.getUniqueId()))
                hurt(p,target,damage("epyon",1.5));
        }
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.9f,1.5f);
        Location effect=eye.clone().add(d.clone().multiply(1.6)).subtract(0,.35,0);
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,effect,1,0,0,0,0);
    }
    private void use(Player p,String k){
        State s=ensure(p,k);long now=Bukkit.getCurrentTick();
        long previous=s.lastRight;s.lastRight=now;
        if(now<s.nextInput||now-previous<=5)return;s.nextInput=now+6;s.nextToggle=now+6;
        switch(k){
            case "epyon"->{
                if(p.isSneaking()){
                    if(epyonOffhand.equipped(p)){
                        epyonOffhand.restore(p);s.attackStart=-1;s.hit.clear();
                        p.sendActionBar(Component.text("§7エピオンシールド収納"));
                    }else if(epyonOffhand.equip(p)){
                        p.sendActionBar(Component.text("§cエピオンシールド装着"));
                    }
                    return;
                }
                if(!epyonOffhand.equipped(p)){p.sendActionBar(Component.text("§7しゃがみ右クリックで盾を装着してください"));return;}
                if(now<s.nextShot)return;s.nextShot=now+26;s.attackStart=now;s.hit.clear();s.pull=false;
                p.getWorld().playSound(p.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.2f,.65f);
            }
            case "freedom"->{
                if(now<s.nextShot)return;
                boolean full=s.freedomMode.fullBurst();
                if(full&&(s.progress<.98||s.cannonProgress<.98)){
                    p.sendActionBar(Component.text("§7フルバースト展開中"));return;
                }
                s.nextShot=now+(full?80:12);
                freedomFire(p,s,p.isSneaking()&&full);
            }
        }
    }
    private void tick(){
        long now=Bukkit.getCurrentTick();
        for(UUID id:new ArrayList<>(active.keySet())){
            Player p=Bukkit.getPlayer(id);State s=active.get(id);
            if(p==null||!p.isOnline()||p.isDead()||!s.kind.equals(kind(p))||!enabled(s.kind)
                ||!s.world.equals(p.getWorld().getUID())||!s.serial.equals(value(p,serialKey)))clear(id);
        }
        for(Player p:Bukkit.getOnlinePlayers()){
            String k=kind(p);
            if(!k.equals("epyon")||!enabled(k)||!epyonOffhand.equipped(p))epyonOffhand.recoverIfNeeded(p);
            if(k.isEmpty()||p.isDead()||!enabled(k))continue;
            if(k.equals("heavyarms"))migrateHeavyarms(p);
            if(k.equals("epyon"))migrateEpyon(p);
            State s=ensure(p,k);
            if(k.equals("freedom")){
                s.progress=approach(s.progress,s.freedomMode.wingsOpen()?1:0,.07);
                s.cannonProgress=approach(s.cannonProgress,
                    s.freedomMode.fullBurst()&&s.progress>=.98?1:0,.085);
            }else s.progress=approach(s.progress,s.opened?1:0,.07);
            switch(k){case "epyon"->rod(p,s,now);case "heavyarms"->gatling(p,s,now);case "freedom"->freedom(p,s);}
        }
    }
    private void rod(Player p,State s,long now){
        Vector f=front(p),r=right(f);
        // The shield is now a real offhand item. No duplicate shield display.
        double side=p.getMainHand()==org.bukkit.inventory.MainHand.RIGHT?1:-1;
        Location joint=at(p,f,r,side*.48,.39,.20);
        boolean equipped=epyonOffhand.equipped(p);
        long elapsed=s.attackStart<0?999:now-s.attackStart;
        boolean attacking=equipped&&elapsed>=0&&elapsed<18;
        if(!equipped||elapsed>=24)s.attackStart=-1;
        double t=attacking?elapsed/17.0:1;
        Vector look=p.getEyeLocation().getDirection();
        Vector aimRight=right(f);
        double deployed=equipped&&elapsed<24?smooth(Math.min(1,Math.min((elapsed+1)/4.0,(24-elapsed)/6.0))):0;
        boolean blocked=false;
        for(int i=0;i<ROD_LINKS;i++){
            double u=(i+.5)/ROD_LINKS;
            // Each physical segment is attached to the end of the previous one.
            // Bend travels down the chain; the resting chain hangs under the shield.
            double angle=attacking?(1.0-2*t)*1.25+Math.sin(u*3.0-t*5.5)*.42:Math.sin(u*2.7+now*.09)*.45;
            Vector tangent=attacking?look.clone().multiply(Math.cos(angle)).add(aimRight.clone().multiply(Math.sin(angle))).add(new Vector(0,-.11*u,0))
                :f.clone().multiply(.65+.20*u).add(r.clone().multiply(.45*Math.sin(u*4.2))).add(new Vector(0,-.09+.20*u,0));
            tangent.normalize();
            double len=.40*deployed;
            Location end=joint.clone().add(tangent.clone().multiply(len));
            if(!blocked&&len>.015){
                RayTraceResult wall=p.getWorld().rayTraceBlocks(joint,tangent,len,FluidCollisionMode.NEVER,true);
                if(wall!=null){end=wall.getHitPosition().toLocation(p.getWorld());blocked=true;}
                if(attacking){
                    double travel=joint.distance(end);
                    if(travel>.01){
                        RayTraceResult hit=p.getWorld().rayTraceEntities(joint,tangent,travel,.24,e->targetable(e,p));
                        if(hit!=null&&hit.getHitEntity() instanceof LivingEntity target&&s.hit.add(target.getUniqueId())){
                            hurt(p,target,damage("epyon",1.5));
                            if(s.pull){Vector pull=p.getLocation().toVector().subtract(target.getLocation().toVector());
                                if(pull.lengthSquared()>.01)target.setVelocity(pull.normalize().multiply(.42).setY(.12));}
                            p.getWorld().spawnParticle(Particle.CRIT,target.getLocation().add(0,.8,0),8,.15,.2,.15,.06);
                        }
                    }
                }
            }
            Location mid=joint.clone().add(end.toVector().subtract(joint.toVector()).multiply(.5));
            float scale=blocked||deployed<.025?(float).001:(float)deployed;
            aim(s.parts.get(i),mid,tangent,0,scale);
            if(attacking&&!blocked&&i%4==0&&now%2==0)p.getWorld().spawnParticle(Particle.DUST,mid,1,0,0,0,0,new Particle.DustOptions(Color.fromRGB(240,95,42),.55f));
            joint=end;
        }
    }
    private void gatling(Player p,State s,long now){
        Vector f=front(p),r=right(f),look=p.getEyeLocation().getDirection();
        boolean held=holdingTrigger(p,s);
        if(held&&p.getActiveItemRemainingTime()<200)p.setActiveItemRemainingTime(72000);
        boolean wasFiring=s.firing;
        s.firing=held&&!s.overheated;
        if(s.firing&&!wasFiring){
            s.gatlingPhase=0;
            p.getWorld().playSound(p.getLocation(),Sound.BLOCK_PISTON_EXTEND,.6f,1.5f);
        }
        s.spin=approach(s.spin,s.firing&&!s.overheated?1:0,.10);
        s.spinAngle=(s.spinAngle+.78*s.spin)%(Math.PI*2);
        for(ListIterator<Casing> it=s.casings.listIterator();it.hasNext();){
            Casing c=it.next();if(c.life()<=0){it.remove();continue;}
            Location l=c.location().clone().add(c.velocity());
            p.getWorld().spawnParticle(Particle.DUST,l,1,0,0,0,0,new Particle.DustOptions(Color.fromRGB(193,145,62),.65f));
            it.set(new Casing(l,c.velocity().clone().add(new Vector(0,-.045,0)),c.life()-1));
        }
        if(!s.firing||s.overheated)s.heat=Math.max(0,s.heat-1.6);
        if(s.overheated&&s.heat<=25){s.overheated=false;p.sendActionBar(Component.text("§a冷却完了"));}
        Location[] muzzles=new Location[4];
        float scale=1.18f;
        Vector up=look.clone().crossProduct(r).normalize();
        for(int i=0;i<2;i++){
            int side=i==0?-1:1;Location body=at(p,f,r,side*.67,1.15,.24);
            aim(s.parts.get(i),body,look,0,scale);
            for(int j=0;j<2;j++){
                int n=i*2+j;
                Vector vertical=up.clone().multiply((j==0?-1:1)*2.6*scale/16);
                Location root=body.clone().add(vertical).add(look.clone().multiply(3.0*scale/16));
                aim(s.parts.get(2+n),root,look,s.spinAngle,scale);
                muzzles[n]=root.clone().add(look.clone().multiply(18.92*scale/16));
            }
        }
        if(s.firing&&!s.overheated&&s.spin>=.95&&now>=s.nextShot){
            s.nextShot=now+HeavyarmsCadence.interval(s.gatlingPhase++);
            s.heat=Math.min(HEAT_LIMIT,s.heat+2);
            Set<UUID> hits=new HashSet<>();
            Location endpoint=aimPoint(p,36);
            for(int i=0;i<2;i++){
                int side=i==0?-1:1;
                Location port=at(p,f,r,side*.86,1.2,.18);
                s.casings.add(new Casing(port,r.clone().multiply(side*.18).add(new Vector(0,.13,0)),8));
            }
            for(Location muzzle:muzzles)
                beam(p,muzzle,endpoint,36,Color.fromRGB(255,208,90),.62f,false,
                    hits,damage("heavyarms",.5),true);
            p.getWorld().playSound(p.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_BLAST,.65f,1.35f);
            if(s.heat>=HEAT_LIMIT){s.overheated=true;s.firing=false;p.getWorld().playSound(p.getLocation(),Sound.BLOCK_FIRE_EXTINGUISH,.9f,.8f);}
        }
        if(s.spin>.15&&now%4==0)for(Location muzzle:muzzles)p.getWorld().spawnParticle(Particle.SMOKE,muzzle,2,.04,.04,.04,.01);
        if(now%10==0)p.sendActionBar(Component.text((s.overheated?"§c過熱 ":"§eHEAT ")+(int)s.heat+"%"+(s.firing?" §a連射中":"")));
    }
    private static final float FREEDOM_SHOULDER_SCALE=1.16f, FREEDOM_HIP_SCALE=1.12f, FREEDOM_RIFLE_SCALE=.95f;
    private void freedom(Player p,State s){
        Vector f=front(p),r=right(f),look=p.getEyeLocation().getDirection();
        double q=smooth(s.progress),c=smooth(s.cannonProgress);
        Location back=at(p,f,r,0,1.36,-.36);back.setPitch(0);
        place(s.parts.get(0),back,new Quaternionf(),.86f);
        for(int i=0;i<WINGS;i++){
            int side=i<5?-1:1,row=i%5;
            FreedomWingLayout.Pose pose=FreedomWingLayout.pose(row,q,c);
            Location root=at(p,f,r,side*pose.x(),pose.y(),pose.z());root.setPitch(0);
            float roll=(float)Math.toRadians(-side*pose.roll());
            float sweep=(float)Math.toRadians(side*pose.sweep());
            place(s.parts.get(1+i),root,new Quaternionf().rotateY(sweep).rotateZ(roll),(float)pose.scale());
        }
        // HiMAT keeps the heavy guns folded; full burst visibly clears the
        // shoulders and hips. The actual muzzle/scale are shared with firing.
        for(int i=0;i<2;i++){
            int side=i==0?-1:1;
            Location shoulder=at(p,f,r,side*(.38+.32*c),1.57+.49*c,-.57+.17*c);
            Vector shoulderDirection=new Vector(0,-1,0).multiply(1-c).add(look.clone().add(new Vector(0,.22,0)).normalize().multiply(c));
            if(shoulderDirection.lengthSquared()<1e-8)shoulderDirection=f.clone();
            aim(s.parts.get(11+i),shoulder,shoulderDirection,0,(float)(.84+(FREEDOM_SHOULDER_SCALE-.84)*c));
            Location hip=at(p,f,r,side*(.44+.19*c),.78+.05*c,-.20+.49*c);
            Vector hipDirection=f.clone().multiply(-(1-c)).add(new Vector(0,-.26*(1-c),0)).add(look.clone().multiply(c));
            if(hipDirection.lengthSquared()<1e-8)hipDirection=new Vector(0,-1,0);
            aim(s.parts.get(13+i),hip,hipDirection,0,(float)(.53+(FREEDOM_HIP_SCALE-.53)*c));
        }
        // Keep the rifle grip at the right hand, not floating at shoulder height.
        double handSide=p.getMainHand()==org.bukkit.inventory.MainHand.RIGHT?-1:1;
        Location hand=at(p,f,r,handSide*.43,.96,.34);
        Vector rifleDirection=look.clone().add(new Vector(0,-.38*(1-c),0));
        if(rifleDirection.lengthSquared()<1e-8)rifleDirection=f.clone();
        aim(s.parts.get(15),hand,rifleDirection,0,FREEDOM_RIFLE_SCALE);
    }
    private Location aimPoint(Player p,double range){
        Location eye=p.getEyeLocation();Vector d=eye.getDirection();
        RayTraceResult wall=p.getWorld().rayTraceBlocks(eye,d,range,FluidCollisionMode.NEVER,true);
        double reach=wall==null?range:eye.toVector().distance(wall.getHitPosition());
        RayTraceResult hit=reach>.001?p.getWorld().rayTraceEntities(eye,d,reach,.2,e->targetable(e,p)):null;
        return hit!=null?hit.getHitPosition().toLocation(p.getWorld()):eye.add(d.multiply(reach));
    }
    private void freedomFire(Player p,State s,boolean multi){
        freedom(p,s);Vector look=p.getEyeLocation().getDirection();
        Location focus=aimPoint(p,48);
        List<LivingEntity> locks=new ArrayList<>();
        if(multi){
            Location eye=p.getEyeLocation();
            for(Entity e:p.getWorld().getNearbyEntities(eye,36,24,36)){
                if(!targetable(e,p))continue;
                LivingEntity l=(LivingEntity)e;Vector delta=l.getEyeLocation().toVector().subtract(eye.toVector());
                if(delta.lengthSquared()<.01||delta.length()>36||delta.clone().normalize().dot(look)<.70||!p.hasLineOfSight(l))continue;
                locks.add(l);
            }
            locks.sort(Comparator.comparingDouble(l->l.getLocation().distanceSquared(p.getLocation())));
        }
        Set<UUID> hit=new HashSet<>();
        boolean full=s.freedomMode.fullBurst();
        int[] guns=full?new int[]{11,12,13,14,15}:new int[]{15};
        for(int i=0;i<guns.length;i++){
            int index=guns[i];ItemDisplay gun=s.parts.get(index);
            float gunScale=index<=12?FREEDOM_SHOULDER_SCALE:index<=14?FREEDOM_HIP_SCALE:FREEDOM_RIFLE_SCALE;
            double reach=(index<=12?21:index<=14?20.5:18.5)*gunScale/16;
            Location muzzle=gun.getLocation().add(look.clone().multiply(reach));
            Location end=locks.isEmpty()?focus:locks.get(i%locks.size()).getEyeLocation();
            // Aim each articulated gun at its selected target before firing.
            Vector d=end.toVector().subtract(gun.getLocation().toVector());
            if(d.lengthSquared()>.001){d.normalize();aim(gun,gun.getLocation(),d,0,gunScale);
                muzzle=gun.getLocation().add(d.clone().multiply(reach));}
            Color color=index<=12?Color.fromRGB(255,90,175):index<=14?Color.fromRGB(255,221,85):Color.fromRGB(110,255,125);
            beam(p,muzzle,end,48,color,full?1.8f:1.1f,true,hit,damage("freedom",1.5),true);
        }
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_GUARDIAN_ATTACK,1.4f,full?.55f:1.15f);
        if(multi)p.sendActionBar(Component.text("§bマルチロックオン："+Math.min(5,locks.size())+"目標"));
    }
    private void beam(Player p,Location muzzle,Location endpoint,double range,Color color,float size,boolean glow,Set<UUID> hit,double damage,boolean render){
        World w=p.getWorld();Vector d=endpoint.toVector().subtract(muzzle.toVector());
        if(d.lengthSquared()<.001)d=p.getEyeLocation().getDirection();d.normalize();
        RayTraceResult wall=w.rayTraceBlocks(muzzle,d,range,FluidCollisionMode.NEVER,true);
        double length=wall==null?range:muzzle.toVector().distance(wall.getHitPosition());
        if(length>.001){
            RayTraceResult contact=w.rayTraceEntities(muzzle,d,length,.19,e->targetable(e,p));
            if(contact!=null){length=muzzle.toVector().distance(contact.getHitPosition());
                if(contact.getHitEntity() instanceof LivingEntity l&&hit.add(l.getUniqueId()))hurt(p,l,damage);}
        }
        // Rendering is independent of collision and damage.
        if(!render)return;
        Particle.DustOptions dust=new Particle.DustOptions(color,size);
        if(!glow){
            // Restore half the original tracer density (2.30 vs 1.15 block
            // spacing). Use small size-controlled bright dust for the muzzle
            // flash; vanilla FLASH itself has no size setting.
            for(double x=0;x<=length;x+=2.30)
                w.spawnParticle(Particle.DUST,muzzle.clone().add(d.clone().multiply(x)),1,.035,.035,.035,0,dust);
            w.spawnParticle(Particle.DUST,muzzle,2,.025,.025,.025,0,
                new Particle.DustOptions(Color.fromRGB(255,248,196),.75f));
            return;
        }
        for(double x=0;x<=length;x+=.48){
            Location point=muzzle.clone().add(d.clone().multiply(x));
            w.spawnParticle(Particle.DUST,point,glow?3:1,.035,.035,.035,0,dust);
            if(glow)w.spawnParticle(Particle.DUST,point,1,0,0,0,0,new Particle.DustOptions(Color.fromRGB(245,250,255),size*.6f));
        }
        w.spawnParticle(Particle.FLASH,muzzle,1,0,0,0,0,color);
        w.spawnParticle(Particle.ELECTRIC_SPARK,muzzle.clone().add(d.multiply(length)),5,.10,.10,.10,.02);
    }
    boolean command(CommandSender sender,String[] args){
        String k=args[0].toLowerCase(Locale.ROOT);
        sender.sendMessage("§b/funnel give "+k+" [player] §7| "+(enabled(k)?"有効":"無効"));
        return true;
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onVanillaHit(EntityDamageByEntityEvent e){
        if(e.getDamager() instanceof Player p&&!kind(p).isEmpty()&&!applyingDamage.contains(p.getUniqueId())){
            e.setCancelled(true);
            if(kind(p).equals("epyon")&&enabled("epyon"))left(p,"epyon");
        }
    }
    @EventHandler public void onQuit(PlayerQuitEvent e){epyonOffhand.restore(e.getPlayer());clear(e.getPlayer().getUniqueId());}
    @EventHandler public void onDeath(PlayerDeathEvent e){epyonOffhand.onDeath(e);clear(e.getEntity().getUniqueId());}
    @EventHandler public void onWorld(PlayerChangedWorldEvent e){clear(e.getPlayer().getUniqueId());}
    @EventHandler public void onHeld(PlayerItemHeldEvent e){clear(e.getPlayer().getUniqueId());}
}
