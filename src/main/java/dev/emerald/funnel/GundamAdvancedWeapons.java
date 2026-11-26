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
            case "epyon"->"epyon_shield";
            case "heavyarms"->"heavyarms_gatling_body";
            default->"freedom_lupus_rifle";
        }).clone();
        ItemMeta m=item.getItemMeta();
        m.displayName(Component.text(switch(type){
            case "epyon"->"§cガンダムエピオン ヒートロッド";
            case "heavyarms"->"§9ヘビーアームズ改EW ダブルガトリング";
            default->"§bフリーダム ハイマットフルバースト";
        }));
        m.lore(switch(type){
            case "epyon"->List.of(Component.text("§7左クリック: 展開 / 収納"),Component.text("§e右クリック: 薙ぎ払い / しゃがみ右クリック: 引き寄せ"));
            case "heavyarms"->heavyarmsLore();
            default->List.of(Component.text("§7左クリック: 収納 / ハイマット"),Component.text("§bしゃがみ左クリック: ハイマット / フルバースト"),Component.text("§e右クリック: ライフル / フルバースト時5門同時射撃"),Component.text("§bフルバーストでしゃがみ右クリック: マルチロックオン"));
        });
        m.getPersistentDataContainer().set(kindKey,PersistentDataType.STRING,type);
        m.getPersistentDataContainer().set(serialKey,PersistentDataType.STRING,UUID.randomUUID().toString());
        item.setItemMeta(m);
        if(type.equals("heavyarms"))prepareHeavyarms(item);
        return item;
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
        task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
    }
    void stop(){if(task!=null)task.cancel();for(UUID id:new ArrayList<>(active.keySet()))clear(id);applyingDamage.clear();}
    private void clear(UUID id){
        State s=active.remove(id);
        if(s!=null)for(ItemDisplay d:s.parts)if(d.isValid())d.remove();
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
                s.parts.add(spawn(p,"epyon_shield"));
                for(int i=0;i<ROD_LINKS;i++)s.parts.add(spawn(p,i==ROD_LINKS-1?"epyon_rod_tip":"epyon_rod_link"));
            }
            case "heavyarms"->{
                for(int i=0;i<2;i++)s.parts.add(spawn(p,"heavyarms_gatling_body"));
                for(int i=0;i<ROTORS;i++)s.parts.add(spawn(p,"heavyarms_gatling_rotor"));
            }
            case "freedom"->{
                s.parts.add(spawn(p,"freedom_backpack"));
                for(int i=0;i<WINGS;i++)s.parts.add(spawn(p,i%5%2==0?"freedom_wing_navy":"freedom_wing_blue"));
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
        if(e.getHand()!=EquipmentSlot.HAND)return;
        Player p=e.getPlayer();String k=kind(p);if(k.isEmpty())return;
        Action a=e.getAction();boolean left=a==Action.LEFT_CLICK_AIR||a==Action.LEFT_CLICK_BLOCK;
        boolean right=a==Action.RIGHT_CLICK_AIR||a==Action.RIGHT_CLICK_BLOCK;
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
        if(k.equals("freedom")){
            s.freedomMode=s.freedomMode.next(p.isSneaking());
            p.getWorld().playSound(p.getLocation(),Sound.BLOCK_PISTON_EXTEND,.75f,
                s.freedomMode==FreedomMode.CLOSED?.75f:1.15f);
            p.sendActionBar(Component.text("§bフリーダム："+s.freedomMode.label()));
            return;
        }
        s.opened=!s.opened;s.attackStart=-1;s.hit.clear();
        p.getWorld().playSound(p.getLocation(),Sound.BLOCK_PISTON_EXTEND,.75f,s.opened?1.15f:.75f);
        p.sendActionBar(Component.text(s.opened?"§b兵装展開":"§7兵装収納"));
    }
    private void use(Player p,String k){
        State s=ensure(p,k);long now=Bukkit.getCurrentTick();
        long previous=s.lastRight;s.lastRight=now;
        if(now<s.nextInput||now-previous<=5)return;s.nextInput=now+6;s.nextToggle=now+6;
        switch(k){
            case "epyon"->{
                if(!s.opened||s.progress<.95){p.sendActionBar(Component.text("§7先にヒートロッドを展開してください"));return;}
                if(now<s.nextShot)return;s.nextShot=now+26;s.attackStart=now;s.hit.clear();s.pull=p.isSneaking();
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
            String k=kind(p);if(k.isEmpty()||p.isDead()||!enabled(k))continue;
            if(k.equals("heavyarms"))migrateHeavyarms(p);
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
        // The authored red face was facing the player's rear. Flip the shield
        // only, keeping its vertical socket at the bottom and the rod's world axes.
        Location shield=at(p,f,r,.55,1.10,.20);shield.setPitch(0);
        Quaternionf shieldRotation=new Quaternionf().rotateY((float)Math.PI);
        shield.setYaw(p.getLocation().getYaw()-12f);
        place(s.parts.get(0),shield,shieldRotation,.94f);
        // Socket center is x=z=8, its lower edge is model y=-5.3.
        Location joint=shield.clone().add(0,(-5.3-8)*.94/16,0);
        boolean attacking=s.attackStart>=0&&now-s.attackStart<18;
        if(!attacking)s.attackStart=-1;
        double t=attacking?(now-s.attackStart)/17.0:0;
        Vector look=p.getEyeLocation().getDirection();
        Vector aimRight=right(f);
        double deployed=smooth(s.progress);
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
            aim(s.parts.get(i+1),mid,tangent,0,scale);
            if(attacking&&!blocked&&i%2==0)p.getWorld().spawnParticle(Particle.DUST,mid,2,.03,.03,.03,0,new Particle.DustOptions(Color.fromRGB(240,95,42),.9f));
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
            for(Location muzzle:muzzles)beam(p,muzzle,endpoint,36,Color.fromRGB(255,208,90),.62f,false,hits,damage("heavyarms",.5));
            p.getWorld().playSound(p.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_BLAST,.65f,1.35f);
            if(s.heat>=HEAT_LIMIT){s.overheated=true;s.firing=false;p.getWorld().playSound(p.getLocation(),Sound.BLOCK_FIRE_EXTINGUISH,.9f,.8f);}
        }
        if(s.spin>.15&&now%4==0)for(Location muzzle:muzzles)p.getWorld().spawnParticle(Particle.SMOKE,muzzle,2,.04,.04,.04,.01);
        if(now%10==0)p.sendActionBar(Component.text((s.overheated?"§c過熱 ":"§eHEAT ")+(int)s.heat+"%"+(s.firing?" §a連射中":"")));
    }
    private void freedom(Player p,State s){
        Vector f=front(p),r=right(f),look=p.getEyeLocation().getDirection();
        double q=smooth(s.progress),c=smooth(s.cannonProgress);
        Location back=at(p,f,r,0,1.40,-.37);back.setPitch(0);
        place(s.parts.get(0),back,new Quaternionf(),.95f);
        // Five actual binders on each side, nested into a coherent wing rather
        // than a uniform radial fan. Outer wing panels step back in depth.
        double[] spread={23,42,61,94,122};
        double[] lengths={1.26,1.19,1.07,.88,.76};
        double[] closedAngles={165,169,173,177,181};
        for(int i=0;i<WINGS;i++){
            int side=i<5?-1:1,row=i%5;
            double x=.30+(.12+row*.026)*q;
            Location root=at(p,f,r,side*x,1.48-row*.026,-.44-row*.065);root.setPitch(0);
            double openAngle=spread[row]+(row<3?3:-4)*c;
            float roll=(float)Math.toRadians(-side*(closedAngles[row]+(openAngle-closedAngles[row])*q));
            float sweep=(float)Math.toRadians(side*(4+row*2.2)*q);
            place(s.parts.get(1+i),root,new Quaternionf().rotateY(sweep).rotateZ(roll),(float)lengths[row]);
        }
        // HiMAT opens the wings only. Four heavy guns stay in their stowed
        // position until FULL_BURST has been selected independently.
        for(int i=0;i<2;i++){
            int side=i==0?-1:1;
            Location shoulder=at(p,f,r,side*(.37+.14*c),1.53+.24*c,-.58+.05*c);
            Vector shoulderDirection=new Vector(0,-1,0).multiply(1-c).add(look.clone().multiply(c));
            if(shoulderDirection.lengthSquared()<1e-8)shoulderDirection=f.clone();
            aim(s.parts.get(11+i),shoulder,shoulderDirection,0,.94f);
            Location hip=at(p,f,r,side*(.44+.09*c),.79+.03*c,-.16);
            Vector hipDirection=f.clone().multiply(-(1-c)).add(new Vector(0,-.26*(1-c),0)).add(look.clone().multiply(c));
            if(hipDirection.lengthSquared()<1e-8)hipDirection=new Vector(0,-1,0);
            // A shorter folded presentation avoids a full-length railgun
            // protruding into the ground in the idle/HiMAT pose.
            aim(s.parts.get(13+i),hip,hipDirection,0,(float)(.53+.37*c));
        }
        Location hand=at(p,f,r,-.43,1.08,.25);
        aim(s.parts.get(15),hand,look,0,.95f);
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
            double reach=index<=12?21*.94/16:index<=14?20.5*.90/16:18.5*.95/16;
            Location muzzle=gun.getLocation().add(look.clone().multiply(reach));
            Location end=locks.isEmpty()?focus:locks.get(i%locks.size()).getEyeLocation();
            // Aim each articulated gun at its selected target before firing.
            Vector d=end.toVector().subtract(gun.getLocation().toVector());
            if(d.lengthSquared()>.001){d.normalize();aim(gun,gun.getLocation(),d,0,index<=12?.94f:index<=14?.90f:.95f);
                muzzle=gun.getLocation().add(d.clone().multiply(reach));}
            Color color=index<=12?Color.fromRGB(255,90,175):index<=14?Color.fromRGB(255,221,85):Color.fromRGB(110,255,125);
            beam(p,muzzle,end,48,color,full?1.8f:1.1f,true,hit,damage("freedom",1.5));
        }
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_GUARDIAN_ATTACK,1.4f,full?.55f:1.15f);
        if(multi)p.sendActionBar(Component.text("§bマルチロックオン："+Math.min(5,locks.size())+"目標"));
    }
    private void beam(Player p,Location muzzle,Location endpoint,double range,Color color,float size,boolean glow,Set<UUID> hit,double damage){
        World w=p.getWorld();Vector d=endpoint.toVector().subtract(muzzle.toVector());
        if(d.lengthSquared()<.001)d=p.getEyeLocation().getDirection();d.normalize();
        RayTraceResult wall=w.rayTraceBlocks(muzzle,d,range,FluidCollisionMode.NEVER,true);
        double length=wall==null?range:muzzle.toVector().distance(wall.getHitPosition());
        if(length>.001){
            RayTraceResult contact=w.rayTraceEntities(muzzle,d,length,.19,e->targetable(e,p));
            if(contact!=null){length=muzzle.toVector().distance(contact.getHitPosition());
                if(contact.getHitEntity() instanceof LivingEntity l&&hit.add(l.getUniqueId()))hurt(p,l,damage);}
        }
        Particle.DustOptions dust=new Particle.DustOptions(color,size);
        for(double x=0;x<=length;x+=glow?.48:1.15){
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
        if(e.getDamager() instanceof Player p&&!kind(p).isEmpty()&&!applyingDamage.contains(p.getUniqueId()))e.setCancelled(true);
    }
    @EventHandler public void onQuit(PlayerQuitEvent e){clear(e.getPlayer().getUniqueId());}
    @EventHandler public void onDeath(PlayerDeathEvent e){clear(e.getEntity().getUniqueId());}
    @EventHandler public void onWorld(PlayerChangedWorldEvent e){clear(e.getPlayer().getUniqueId());}
    @EventHandler public void onHeld(PlayerItemHeldEvent e){clear(e.getPlayer().getUniqueId());}
}
