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
    private final Map<UUID,Motion> idle=new HashMap<>();
    private final Map<UUID,Integer> sequences=new HashMap<>();
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
        int attackMode;
        Motion(String kind,Player p,long start){this.kind=kind;world=p.getWorld().getUID();this.start=start;direction=p.getEyeLocation().getDirection().normalize();}
    }
    GundamMotionWeapons(JavaPlugin plugin){this.plugin=plugin;kind=new NamespacedKey(plugin,"motion_weapon");marker=new NamespacedKey(plugin,"motion_display");}
    private String held(Player p){ItemStack item=p.getInventory().getItemInMainHand();if(!item.hasItemMeta())return "";String k=item.getItemMeta().getPersistentDataContainer().get(kind,PersistentDataType.STRING);return k!=null&&TYPES.contains(k)?k:"";}
    private ItemStack model(String id){ItemStack i=new ItemStack(Material.LEATHER_HORSE_ARMOR);ItemMeta m=i.getItemMeta();m.setItemModel(new NamespacedKey("funnel",id));m.setUnbreakable(true);i.setItemMeta(m);return i;}
    ItemStack giftItem(String k){
        if(!TYPES.contains(k))throw new IllegalArgumentException(k);
        ItemStack i=model(k.equals("hammer")?"hyper_hammer_handle":"destiny_controller");ItemMeta m=i.getItemMeta();
        m.displayName(Component.text(k.equals("hammer")?"§7ガンダム ハイパーハンマー":"§bデスティニーガンダム 光の翼"));
        m.lore(List.of(Component.text(k.equals("hammer")?"§e右クリック: 回転投擲・横薙ぎ・叩きつけ・直線投擲":"§e右クリック: 光の翼展開・高速突進")));
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
    void stop(){if(task!=null)task.cancel();for(UUID id:new ArrayList<>(active.keySet()))clear(id);for(UUID id:new ArrayList<>(idle.keySet()))clearIdle(id);cooldowns.clear();input.clear();sequences.clear();}
    private void clearIdle(UUID id){Motion m=idle.remove(id);if(m!=null)for(ItemDisplay d:m.parts)if(d.isValid())d.remove();}
    private void clear(UUID id){Motion m=active.remove(id);if(m!=null)for(ItemDisplay d:m.parts)if(d.isValid())d.remove();}
    private ItemDisplay spawn(Player p,String id){return p.getWorld().spawn(p.getLocation(),ItemDisplay.class,d->{d.setItemStack(model(id));d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);d.setPersistent(false);d.setGravity(false);d.setInvulnerable(true);d.setTeleportDuration(1);d.setInterpolationDuration(1);d.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);});}
    private static void pose(ItemDisplay d,Location at,Quaternionf rotation,float x,float y,float z){d.teleport(at);d.setTransformation(new Transformation(new Vector3f(),rotation,new Vector3f(x,y,z),new Quaternionf()));}
    private static Vector forward(Player p){double a=Math.toRadians(p.getLocation().getYaw());return new Vector(-Math.sin(a),0,Math.cos(a));}
    private static Vector right(Vector f){return new Vector(-f.getZ(),0,f.getX());}
    private static double handSide(Player p){return p.getMainHand()==org.bukkit.inventory.MainHand.RIGHT?1:-1;}
    // Anchor at the forward end of the held handle, not the torso or opposite arm.
    private static Location hammerAnchor(Player p){Vector f=forward(p);double side=handSide(p);return p.getLocation().add(0,p.isSneaking()?.88:1.02,0).add(right(f).multiply(side*.36)).add(f.multiply(.46));}
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
        if(k.equals("hammer")){
            Motion resting=idle.remove(id);
            if(resting!=null&&resting.world.equals(m.world)&&resting.parts.stream().allMatch(ItemDisplay::isValid)){
                m.parts.addAll(resting.parts);m.ball=resting.ball.clone();
            }else{if(resting!=null)for(ItemDisplay d:resting.parts)d.remove();createHammer(p,m);}
            m.attackMode=(sequences.merge(id,1,Integer::sum)-1)&3;
        }else{
            m.lastPlayer=p.getLocation();
            for(int i=0;i<2;i++){m.parts.add(spawn(p,"destiny_wing_root"));for(int j=0;j<7;j++){ItemDisplay feather=spawn(p,j==0?"destiny_wing_outer":"destiny_wing_inner");feather.setBrightness(new Display.Brightness(15,15));m.parts.add(feather);}}
        }
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
    private void createHammer(Player p,Motion m){
        m.ball=restingBall(p);m.previous=m.ball.clone();
        m.parts.add(spawn(p,"hyper_hammer_ball"));
        for(int i=0;i<LINKS;i++)m.parts.add(spawn(p,"hyper_hammer_link"));
    }
    private Location restingBall(Player p){
        Location anchor=hammerAnchor(p);Vector f=forward(p),r=right(f);
        double side=handSide(p);
        return clip(anchor,anchor.clone().add(r.multiply(side*.13)).add(f.multiply(.20)).subtract(0,.60,0));
    }
    private void drawHammer(Player p,Motion m,int t,boolean moving){
        Location anchor=hammerAnchor(p),ball=m.ball;
        pose(m.parts.get(0),ball,new Quaternionf().rotateXYZ(moving?t*.24f:0,moving?t*.32f:0,0),.80f,.80f,.80f);
        Vector delta=ball.toVector().subtract(anchor.toVector());double sag=Math.min(.60,delta.length()*.09);
        Location prior=anchor;
        for(int i=0;i<LINKS;i++){
            double u=(i+1.0)/LINKS;
            Location end=anchor.clone().add(delta.clone().multiply(u)).subtract(0,Math.sin(Math.PI*u)*sag,0);
            end=clip(prior,end);Vector v=end.toVector().subtract(prior.toVector());
            Location mid=prior.clone().add(v.clone().multiply(.5));mid.setDirection(v.lengthSquared()>.00001?v:new Vector(0,0,1));
            float len=(float)Math.max(.001,v.length());
            float width=(float)Math.min(.52,Math.max(.06,len*2.2));
            pose(m.parts.get(i+1),mid,new Quaternionf().rotateZ((i%2)*(float)Math.PI/2),width,width,len/.25f);prior=end;
        }
    }
    private void idleHammer(Player p,long now){
        UUID id=p.getUniqueId();Motion m=idle.get(id);
        if(m!=null&&(!m.world.equals(p.getWorld().getUID())||m.parts.stream().anyMatch(d->!d.isValid()))){clearIdle(id);m=null;}
        if(m==null){m=new Motion("hammer",p,now);createHammer(p,m);idle.put(id,m);}
        Location goal=restingBall(p);if(m.ball.getWorld()!=goal.getWorld()||m.ball.distanceSquared(goal)>9)m.ball=goal;
        else m.ball=clip(hammerAnchor(p),m.ball.clone().add(goal.toVector().subtract(m.ball.toVector()).multiply(.55)));
        drawHammer(p,m,0,false);
    }
    private boolean hammer(Player p,Motion m,int t){
        Location anchor=hammerAnchor(p);Vector f=forward(p),r=right(f);Location old=m.ball.clone();
        double max=Math.max(.1,number("hammer","range",7,12));
        if(m.attackMode==1||m.attackMode==2){
            if(t<19){
                double progress=Math.min(1,t/18.0), envelope=Math.sin(Math.PI*progress);
                double reach=Math.min(max,.65+3.7*envelope);
                Location goal;
                if(m.attackMode==1){
                    double angle=handSide(p)*(1.65-3.30*progress);
                    goal=anchor.clone().add(f.clone().multiply(Math.cos(angle)*reach)).add(r.clone().multiply(Math.sin(angle)*reach)).add(0,.10+.35*envelope,0);
                }else{
                    double angle=1.48-2.0*progress;
                    goal=anchor.clone().add(f.clone().multiply(Math.cos(angle)*reach)).add(r.clone().multiply(handSide(p)*.65*(1-progress))).add(0,Math.max(-.95,Math.sin(angle)*reach),0);
                }
                m.ball=clip(anchor,clip(old,goal));
                if(t>=4){strike(p,m,old,m.ball,.60);if(t%2==0)p.getWorld().spawnParticle(Particle.CRIT,m.ball,2,.1,.1,.1,.01);}
                if(t==5)p.getWorld().playSound(anchor,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1f,.65f);
            }else m.returning=true;
        }else{
            int launch=m.attackMode==3?5:10;
            if(t<launch){
                double a=handSide(p)*(Math.PI/2-t*.82),reach=.45+t*.13;
                Location goal=m.attackMode==3?anchor.clone().subtract(f.clone().multiply(.45)).add(r.clone().multiply(handSide(p)*.35)).add(0,.35,0):anchor.clone().add(0,.95,0).add(f.clone().multiply(Math.cos(a)*reach)).add(r.clone().multiply(Math.sin(a)*reach));
                m.ball=clip(anchor,goal);
            }else{
                if(t==launch||m.velocity.lengthSquared()<.00001&&!m.returning){m.velocity=m.direction.clone().multiply(m.attackMode==3?1.65:1.35);p.getWorld().playSound(anchor,Sound.ENTITY_FIREWORK_ROCKET_BLAST,.9f,.7f);}
                if(t<21&&!m.returning){
                    Location desired=m.ball.clone().add(m.velocity),clipped=clip(m.ball,desired);
                    Vector offset=clipped.toVector().subtract(anchor.toVector());
                    if(offset.length()>max){clipped=anchor.clone().add(offset.normalize().multiply(max));m.returning=true;}
                    if(clipped.distanceSquared(desired)>.01){m.returning=true;p.getWorld().playSound(clipped,Sound.BLOCK_ANVIL_LAND,.6f,1.4f);}
                    m.ball=clip(anchor,clipped);m.velocity.multiply(.99).add(new Vector(0,-.035,0));strike(p,m,old,m.ball,.55);
                    p.getWorld().spawnParticle(Particle.FLAME,m.ball,4,.14,.14,.14,.025);
                }else m.returning=true;
            }
        }
        if(m.returning){
            Location goal=restingBall(p);Vector d=goal.toVector().subtract(m.ball.toVector());
            if(d.length()<.22)return false;
            m.ball=clip(m.ball,m.ball.clone().add(d.normalize().multiply(Math.min(1.25,m.ball.distance(goal)))));
        }
        drawHammer(p,m,t,true);return t<HAMMER_TICKS;
    }
    private static final Particle.DustOptions BLUE=new Particle.DustOptions(Color.fromRGB(235,105,200),.55f),PINK=new Particle.DustOptions(Color.fromRGB(255,125,215),.50f),WHITE=new Particle.DustOptions(Color.fromRGB(255,225,247),.40f);
    // Elevation from horizontal, longest feathers at the upper edge.
    private static final double[] FAN_ANGLE={70,54,38,22,6,-15,-38};
    private static final double[] FAN_LENGTH={2.65,2.95,2.85,2.55,2.30,1.95,1.65};
    private void wings(Player p,Motion m,int t,double open){
        Vector f=forward(p),r=right(f);
        Location base=p.getLocation().add(0,1.46,0).subtract(f.clone().multiply(.38));
        for(int i=0;i<2;i++){
            int side=i==0?-1:1;
            Location root=base.clone().add(r.clone().multiply(side*.24));
            root.setYaw(p.getLocation().getYaw());root.setPitch(0);
            pose(m.parts.get(i*8),root,new Quaternionf().rotateZ((float)(side*(.15+open*.40))),.48f,.62f,.48f);
            for(int j=0;j<7;j++){
                double angle=Math.toRadians(FAN_ANGLE[j]);
                double q=Math.max(.001,open);
                double length=FAN_LENGTH[j]*q;
                Vector direction=r.clone().multiply(side*Math.cos(angle)).add(new Vector(0,Math.sin(angle),0)).subtract(f.clone().multiply(.17+j*.025)).normalize();
                Location origin=root.clone().add(r.clone().multiply(side*.10)).add(0,.05-j*.045,0).subtract(f.clone().multiply(.12+j*.025));
                Location center=origin.clone().add(direction.clone().multiply(length*.5));
                // The feather mesh points along local +Y. Rotate it directly into the world-space fan direction.
                center.setYaw(0);center.setPitch(0);
                Quaternionf rotation=new Quaternionf().rotationTo(0,1,0,(float)direction.getX(),(float)direction.getY(),(float)direction.getZ());
                pose(m.parts.get(i*8+1+j),center,rotation,(float)(.82*q),(float)(length/1.5),(float)(.55*q));
                if(open>.4&&t%2==0){
                    Location tip=origin.clone().add(direction.clone().multiply(length));
                    p.getWorld().spawnParticle(Particle.DUST,tip,1,.04,.04,.04,0,j%2==0?PINK:WHITE);
                }
            }
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
        for(UUID id:new ArrayList<>(active.keySet())){Motion m=active.get(id);Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline()||p.isDead()||!p.getWorld().getUID().equals(m.world)||!held(p).equals(m.kind)||!enabled(m.kind)||m.parts.stream().anyMatch(d->!d.isValid())){clear(id);continue;}int t=(int)(now-m.start);if(!(m.kind.equals("hammer")?hammer(p,m,t):destiny(p,m,t))){
                if(m.kind.equals("hammer")){active.remove(id);idle.put(id,m);}else clear(id);
            }}
        for(UUID id:new ArrayList<>(idle.keySet())){Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline()||p.isDead()||!held(p).equals("hammer")||!enabled("hammer")||active.containsKey(id))clearIdle(id);}
        for(Player p:Bukkit.getOnlinePlayers())if(!p.isDead()&&held(p).equals("hammer")&&enabled("hammer")&&!active.containsKey(p.getUniqueId()))idleHammer(p,now);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void melee(EntityDamageByEntityEvent e){if(e.getDamager() instanceof Player p&&!held(p).isEmpty()&&!applying.contains(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler public void held(PlayerItemHeldEvent e){clear(e.getPlayer().getUniqueId());clearIdle(e.getPlayer().getUniqueId());}
    @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer().getUniqueId());clearIdle(e.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent e){clear(e.getEntity().getUniqueId());clearIdle(e.getEntity().getUniqueId());}
    @EventHandler public void world(PlayerChangedWorldEvent e){clear(e.getPlayer().getUniqueId());clearIdle(e.getPlayer().getUniqueId());}
    boolean command(CommandSender sender,String[] args){if(sender instanceof Player p){String k=args[0].toLowerCase(Locale.ROOT);long remaining=Math.max(0,cooldowns.getOrDefault(p.getUniqueId(),Map.of()).getOrDefault(k,0L)-Bukkit.getCurrentTick());sender.sendMessage("§b"+k+" §7/ CT "+((remaining+19)/20)+"秒");}else sender.sendMessage("§7/funnel give <hammer|destiny> [player]");return true;}
}
