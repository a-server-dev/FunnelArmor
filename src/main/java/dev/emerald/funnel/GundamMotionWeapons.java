package dev.emerald.funnel;

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
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

final class GundamMotionWeapons implements Listener {
    static final List<String> TYPES=List.of("hammer","destiny");
    private static final int LINKS=16, HAMMER_TICKS=32;
    private final JavaPlugin plugin;
    private final NamespacedKey kind, marker;
    private final Map<UUID,Motion> active=new HashMap<>();
    private final Map<UUID,Map<String,Long>> cooldowns=new HashMap<>();
    private final Map<UUID,Long> input=new HashMap<>();
    private final Set<UUID> applying=new HashSet<>();
    private BukkitTask task;
    private static final class Motion {
        final String kind; final UUID world; final long start; final Vector direction;
        final List<ItemDisplay> parts=new ArrayList<>();
        final Set<UUID> hit=new HashSet<>();
        Location ball, previous, lastPlayer;
        Vector velocity=new Vector();
        boolean returning;
        Motion(String kind,Player p,long start){this.kind=kind;world=p.getWorld().getUID();this.start=start;direction=p.getEyeLocation().getDirection().normalize();}
    }
    GundamMotionWeapons(JavaPlugin plugin){this.plugin=plugin;kind=new NamespacedKey(plugin,"motion_weapon");marker=new NamespacedKey(plugin,"motion_display");}
    private String held(Player p){ItemStack item=p.getInventory().getItemInMainHand();if(!item.hasItemMeta())return "";String k=item.getItemMeta().getPersistentDataContainer().get(kind,PersistentDataType.STRING);return k!=null&&TYPES.contains(k)?k:"";}
    private ItemStack model(String id){ItemStack i=new ItemStack(Material.LEATHER_HORSE_ARMOR);ItemMeta m=i.getItemMeta();m.setItemModel(new NamespacedKey("funnel",id));m.setUnbreakable(true);i.setItemMeta(m);return i;}
    ItemStack giftItem(String k){
        if(!TYPES.contains(k))throw new IllegalArgumentException(k);
        ItemStack i=model(k.equals("hammer")?"hyper_hammer_handle":"destiny_controller");ItemMeta m=i.getItemMeta();
        m.displayName(Component.text(k.equals("hammer")?"§7ガンダム ハイパーハンマー":"§bデスティニーガンダム 光の翼"));
        m.lore(List.of(Component.text(k.equals("hammer")?"§e右クリック: 回転・投擲・引き戻し":"§e右クリック: 光の翼展開・高速突進")));
        m.getPersistentDataContainer().set(kind,PersistentDataType.STRING,k);
        var cd=m.getUseCooldown();cd.setCooldownSeconds(.05f);cd.setCooldownGroup(WeaponCooldowns.key(k));m.setUseCooldown(cd);
        i.setItemMeta(m);return i;
    }
    private int ticks(String k,String path,int fallback,int max){return Math.max(1,Math.min(max,plugin.getConfig().getInt("motion-weapons."+k+"."+path,fallback)));}
    private double number(String k,String path,double fallback,double max){double d=plugin.getConfig().getDouble("motion-weapons."+k+"."+path,fallback);return Double.isFinite(d)?Math.max(0,Math.min(max,d)):fallback;}
    private boolean enabled(String k){return plugin.getConfig().getBoolean("motion-weapons."+k+".enabled",true);}
    void start(){
        for(World w:Bukkit.getWorlds())for(ItemDisplay d:w.getEntitiesByClass(ItemDisplay.class))if(d.getPersistentDataContainer().has(marker,PersistentDataType.BYTE))d.remove();
        Bukkit.getPluginManager().registerEvents(this,plugin);task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
    }
    void stop(){if(task!=null)task.cancel();for(UUID id:new ArrayList<>(active.keySet()))clear(id);cooldowns.clear();input.clear();}
    private void clear(UUID id){Motion m=active.remove(id);if(m!=null)for(ItemDisplay d:m.parts)if(d.isValid())d.remove();}
    private ItemDisplay spawn(Player p,String id){return p.getWorld().spawn(p.getLocation(),ItemDisplay.class,d->{d.setItemStack(model(id));d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);d.setPersistent(false);d.setGravity(false);d.setInvulnerable(true);d.setTeleportDuration(1);d.setInterpolationDuration(1);d.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);});}
    private static void pose(ItemDisplay d,Location at,Quaternionf rotation,float x,float y,float z){d.teleport(at);d.setTransformation(new Transformation(new Vector3f(),rotation,new Vector3f(x,y,z),new Quaternionf()));}
    private static Vector forward(Player p){double a=Math.toRadians(p.getLocation().getYaw());return new Vector(-Math.sin(a),0,Math.cos(a));}
    private static Vector right(Vector f){return new Vector(f.getZ(),0,-f.getX());}
    private static Location hand(Player p){Vector f=forward(p);return p.getLocation().add(0,1.18,0).add(right(f).multiply(p.getMainHand()==org.bukkit.inventory.MainHand.RIGHT?.40:-.40)).add(f.multiply(.25));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void interact(PlayerInteractEvent e){
        Player p=e.getPlayer();String k=held(p);if(k.isEmpty()||e.getHand()!=EquipmentSlot.HAND)return;
        if(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        e.setCancelled(true);e.setUseItemInHand(Event.Result.DENY);e.setUseInteractedBlock(Event.Result.DENY);
        if(!enabled(k)||p.isDead()||p.getGameMode()==GameMode.SPECTATOR)return;
        UUID id=p.getUniqueId();long now=Bukkit.getCurrentTick();Long prev=input.put(id,now);if(prev!=null&&now-prev<=5)return;
        if(active.containsKey(id))return;
        Map<String,Long> map=cooldowns.computeIfAbsent(id,key->new HashMap<>());
        long until=map.getOrDefault(k,0L);if(now<until){p.sendActionBar(Component.text("§c冷却中: "+((until-now+19)/20)+"秒"));return;}
        int cd=ticks(k,"cooldown-ticks",k.equals("hammer")?80:100,2400);
        cd=Math.max(cd,k.equals("hammer")?HAMMER_TICKS: ticks(k,"dash-ticks",12,30)+8);
        map.put(k,now+cd);WeaponCooldowns.show(p,k,cd);
        Motion m=new Motion(k,p,now);active.put(id,m);
        if(k.equals("hammer")){m.ball=hand(p);m.previous=m.ball.clone();m.parts.add(spawn(p,"hyper_hammer_ball"));for(int i=0;i<LINKS;i++)m.parts.add(spawn(p,"hyper_hammer_link"));}
        else {m.lastPlayer=p.getLocation();for(int i=0;i<2;i++)m.parts.add(spawn(p,"destiny_wing_root"));}
        p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.8f,1.5f);
    }
    private boolean target(Entity e,Player p){return e instanceof LivingEntity l&&!l.isDead()&&e!=p&&!(e instanceof ArmorStand)&&(!(e instanceof Player q)||(q.getGameMode()!=GameMode.CREATIVE&&q.getGameMode()!=GameMode.SPECTATOR));}
    private void strike(Player p,Motion m,Location from,Location to,double radius){
        Vector d=to.toVector().subtract(from.toVector());double length=d.length();if(length<.001)return;d.normalize();
        RayTraceResult wall=p.getWorld().rayTraceBlocks(from,d,length,FluidCollisionMode.NEVER,true);if(wall!=null)length=from.toVector().distance(wall.getHitPosition());
        if(length<.001)return;
        RayTraceResult hit=p.getWorld().rayTraceEntities(from,d,length,radius,e->target(e,p)&&!m.hit.contains(e.getUniqueId()));
        if(hit!=null&&hit.getHitEntity() instanceof LivingEntity l&&m.hit.add(l.getUniqueId())){
            applying.add(p.getUniqueId());try{l.damage(DamageSettings.read(plugin.getConfig(),"motion-weapons."+m.kind+".damage",1.5),p);}finally{applying.remove(p.getUniqueId());}
            p.getWorld().spawnParticle(Particle.CRIT,l.getLocation().add(0,.8,0),8,.2,.2,.2,.08);
        }
    }
    private Location clip(Location from,Location to){Vector d=to.toVector().subtract(from.toVector());double len=d.length();if(len<.001)return to;d.normalize();RayTraceResult hit=from.getWorld().rayTraceBlocks(from,d,len,FluidCollisionMode.NEVER,true);return hit==null?to:hit.getHitPosition().toLocation(from.getWorld()).subtract(d.multiply(.08));}
    private boolean hammer(Player p,Motion m,int t){
        Location anchor=hand(p);Vector f=forward(p),r=right(f);
        Location old=m.ball.clone();
        if(t<10){double a=t*.82, reach=.45+t*.13;Location desired=anchor.clone().add(0,.95,0).add(f.clone().multiply(Math.cos(a)*reach)).add(r.clone().multiply(Math.sin(a)*reach));m.ball=clip(anchor,desired);}
        else if(t==10){m.velocity=m.direction.clone().multiply(1.35);p.getWorld().playSound(p.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_BLAST,.9f,.7f);}
        if(t>=10&&t<21&&!m.returning){
            Location desired=m.ball.clone().add(m.velocity);Location clipped=clip(m.ball,desired);
            double max=number("hammer","range",7,12);Vector offset=clipped.toVector().subtract(anchor.toVector());
            if(offset.length()>max){clipped=anchor.clone().add(offset.normalize().multiply(max));m.returning=true;}
            if(clipped.distanceSquared(desired)>.01){m.returning=true;p.getWorld().playSound(clipped,Sound.BLOCK_ANVIL_LAND,.6f,1.4f);}
            m.ball=clip(anchor,clipped);m.velocity.multiply(.99).add(new Vector(0,-.035,0));
            strike(p,m,old,m.ball,.55);
            p.getWorld().spawnParticle(Particle.FLAME,m.ball,4,.14,.14,.14,.025);
        }else if(t>=21||m.returning){
            Vector d=anchor.toVector().subtract(m.ball.toVector());if(d.length()<.45)return false;
            m.ball=clip(m.ball,m.ball.clone().add(d.normalize().multiply(Math.min(1.25,m.ball.distance(anchor)))));
        }
        Location ball=m.ball;pose(m.parts.get(0),ball,new Quaternionf().rotateXYZ(t*.24f,t*.32f,0),1,1,1);
        Vector delta=ball.toVector().subtract(anchor.toVector());double sag=Math.min(.65,delta.length()*.10);
        Location prior=anchor;
        for(int i=0;i<LINKS;i++){double u=(i+1.0)/LINKS;Location end=anchor.clone().add(delta.clone().multiply(u)).subtract(0,Math.sin(Math.PI*u)*sag,0);end=clip(prior,end);Vector v=end.toVector().subtract(prior.toVector());Location mid=prior.clone().add(v.clone().multiply(.5));mid.setDirection(v.lengthSquared()>.00001?v:new Vector(0,0,1));float len=(float)Math.max(.001,v.length());pose(m.parts.get(i+1),mid,new Quaternionf().rotateZ((i%2)*(float)Math.PI/2),.65f,.65f,len/.25f);prior=end;}
        return t<HAMMER_TICKS;
    }
    private static final Particle.DustOptions BLUE=new Particle.DustOptions(Color.fromRGB(65,155,255),1.0f),PINK=new Particle.DustOptions(Color.fromRGB(240,105,255),.8f),WHITE=new Particle.DustOptions(Color.fromRGB(210,245,255),.6f);
    private void wings(Player p,Motion m,int t,double open){
        Vector f=forward(p),r=right(f);Location base=p.getLocation().add(0,1.43,0).subtract(f.clone().multiply(.27));
        for(int i=0;i<2;i++){int side=i==0?-1:1;Location root=base.clone().add(r.clone().multiply(side*.33));root.setYaw(p.getLocation().getYaw());root.setPitch(0);pose(m.parts.get(i),root,new Quaternionf().rotateZ((float)(-side*(.22+open*.70))),.8f,.8f,.8f);
            for(int feather=0;feather<5;feather++)for(int step=1;step<=10;step++){double u=step/10.0;double span=(2.8-feather*.36)*open;Location point=root.clone().add(r.clone().multiply(side*span*u)).add(0,(.38+feather*.20)*u-.45*u*u,0).subtract(f.clone().multiply((.3+feather*.21)*u+1.1*u*u));p.getWorld().spawnParticle(step%5==0?Particle.DUST:Particle.DUST,point,1,0,0,0,0,feather%2==0?BLUE:PINK);if(step%3==0)p.getWorld().spawnParticle(Particle.DUST,point.clone().add(0,.035,0),1,0,0,0,0,WHITE);}
        }
    }
    private boolean destiny(Player p,Motion m,int t){
        int duration=ticks("destiny","dash-ticks",12,30);
        double open=Math.max(0,Math.min(1,Math.min((t+1)/4.0,(duration+8-t)/5.0)));
        wings(p,m,t,open);
        Location now=p.getLocation();if(m.lastPlayer!=null&&m.lastPlayer.getWorld()==now.getWorld()&&m.lastPlayer.distanceSquared(now)<16){
            if(t>3&&t<=duration+3)strike(p,m,m.lastPlayer.clone().add(0,.9,0),now.clone().add(0,.9,0),.85);
            if(t%2==0&&t>3){Location ghost=m.lastPlayer.clone();for(int j=0;j<6;j++){ghost.add(0,.25,0);p.getWorld().spawnParticle(Particle.DUST,ghost,2,.22,.03,.15,0,BLUE);}}
        }
        m.lastPlayer=now;
        if(t>=4&&t<duration+4){
            Vector d=p.getEyeLocation().getDirection().normalize();double speed=number("destiny","dash-speed",1.05,1.5);Vector v=d.multiply(speed);v.setY(Math.max(-.35,Math.min(.45,v.getY())));
            Location center=now.clone().add(0,.9,0);if(clip(center,center.clone().add(v)).distanceSquared(center)<v.lengthSquared()*.75){p.setVelocity(new Vector(0,0,0));return false;}
            p.setVelocity(v);p.setFallDistance(0);
            if(t==4)p.getWorld().playSound(now,Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,1f,.65f);
        }
        if(t==duration+4)p.setVelocity(p.getVelocity().multiply(.35));
        return t<duration+8;
    }
    private void tick(){long now=Bukkit.getCurrentTick();cooldowns.values().forEach(m->m.values().removeIf(v->v<=now));cooldowns.values().removeIf(Map::isEmpty);input.values().removeIf(v->now-v>200);
        for(UUID id:new ArrayList<>(active.keySet())){Motion m=active.get(id);Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline()||p.isDead()||!p.getWorld().getUID().equals(m.world)||!held(p).equals(m.kind)||!enabled(m.kind)||m.parts.stream().anyMatch(d->!d.isValid())){clear(id);continue;}int t=(int)(now-m.start);if(!(m.kind.equals("hammer")?hammer(p,m,t):destiny(p,m,t)))clear(id);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void melee(EntityDamageByEntityEvent e){if(e.getDamager() instanceof Player p&&!held(p).isEmpty()&&!applying.contains(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler public void held(PlayerItemHeldEvent e){clear(e.getPlayer().getUniqueId());}
    @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent e){clear(e.getEntity().getUniqueId());}
    @EventHandler public void world(PlayerChangedWorldEvent e){clear(e.getPlayer().getUniqueId());}
    boolean command(CommandSender sender,String[] args){if(sender instanceof Player p){String k=args[0].toLowerCase(Locale.ROOT);long remaining=Math.max(0,cooldowns.getOrDefault(p.getUniqueId(),Map.of()).getOrDefault(k,0L)-Bukkit.getCurrentTick());sender.sendMessage("§b"+k+" §7/ CT "+((remaining+19)/20)+"秒");}else sender.sendMessage("§7/funnel give <hammer|destiny> [player]");return true;}
}
