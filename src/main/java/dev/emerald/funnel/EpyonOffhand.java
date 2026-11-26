package dev.emerald.funnel;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Persistent escrow for the player's real offhand, not an in-memory-only swap. */
final class EpyonOffhand implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey backup, temporary;
    EpyonOffhand(JavaPlugin plugin){
        this.plugin=plugin;
        backup=new NamespacedKey(plugin,"epyon_offhand_backup");
        temporary=new NamespacedKey(plugin,"epyon_temporary_shield");
    }
    private boolean temporary(ItemStack item){
        return item!=null&&item.hasItemMeta()&&item.getItemMeta()
            .getPersistentDataContainer().has(temporary,PersistentDataType.BYTE);
    }
    boolean equipped(Player p){
        return temporary(p.getInventory().getItemInOffHand())&&p.getPersistentDataContainer().has(backup,PersistentDataType.BYTE_ARRAY);
    }
    private byte[] saved(Player p){return p.getPersistentDataContainer().get(backup,PersistentDataType.BYTE_ARRAY);}
    private ItemStack decode(byte[] bytes){return bytes.length==0?new ItemStack(Material.AIR):ItemStack.deserializeBytes(bytes);}
    boolean equip(Player p){
        if(equipped(p))return true;
        if(saved(p)!=null){
            restore(p);
            if(saved(p)!=null){p.sendActionBar(Component.text("§c退避アイテムの復元に空きが必要です"));return false;}
        }
        ItemStack original=p.getInventory().getItemInOffHand();
        if(temporary(original)){p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));original=new ItemStack(Material.AIR);}
        ItemStack shield=new ItemStack(Material.SHIELD);
        ItemMeta m=shield.getItemMeta();
        m.setItemModel(new NamespacedKey("funnel","epyon_shield"));
        m.displayName(Component.text("§cエピオンシールド"));
        m.setUnbreakable(true);
        m.getPersistentDataContainer().set(temporary,PersistentDataType.BYTE,(byte)1);
        shield.setItemMeta(m);
        p.getPersistentDataContainer().set(backup,PersistentDataType.BYTE_ARRAY,
            original.getType().isAir()?new byte[0]:original.serializeAsBytes());
        p.getInventory().setItemInOffHand(shield);
        return true;
    }
    void restore(Player p){
        byte[] bytes=saved(p);ItemStack current=p.getInventory().getItemInOffHand();
        if(bytes==null){
            if(temporary(current))p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            return;
        }
        ItemStack original;
        try{original=decode(bytes);}catch(RuntimeException ex){
            plugin.getLogger().warning("Cannot decode Epyon offhand backup for "+p.getUniqueId()+": "+ex.getMessage());
            return;
        }
        // Another plugin may have replaced the shield. Never overwrite that item.
        if(temporary(current)||current.getType().isAir())p.getInventory().setItemInOffHand(original);
        else if(!original.getType().isAir()){
            int slot=p.getInventory().firstEmpty();
            if(slot<0)return; // Retain escrow until storage becomes available.
            p.getInventory().setItem(slot,original);
        }
        p.getPersistentDataContainer().remove(backup);
    }
    void recoverIfNeeded(Player p){
        if(saved(p)!=null||temporary(p.getInventory().getItemInOffHand()))restore(p);
    }
    void onDeath(PlayerDeathEvent e){
        Player p=e.getEntity();
        e.getDrops().removeIf(this::temporary);
        if(e.getKeepInventory()){restore(p);return;}
        byte[] bytes=saved(p);
        if(bytes==null)return;
        try{
            ItemStack original=decode(bytes);
            if(!original.getType().isAir())e.getDrops().add(original);
            p.getPersistentDataContainer().remove(backup);
        }catch(RuntimeException ex){plugin.getLogger().warning("Epyon death backup retained for "+p.getUniqueId());}
    }
    @EventHandler public void join(PlayerJoinEvent e){restore(e.getPlayer());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void swap(PlayerSwapHandItemsEvent e){
        if(temporary(e.getMainHandItem())||temporary(e.getOffHandItem())||equipped(e.getPlayer()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drop(PlayerDropItemEvent e){if(temporary(e.getItemDrop().getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;
        if(temporary(e.getCurrentItem())||temporary(e.getCursor())
            ||(equipped(p)&&(e.getClick()==ClickType.SWAP_OFFHAND
            ||(e.getClickedInventory() instanceof PlayerInventory&&e.getSlot()==40))))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drag(InventoryDragEvent e){
        if(temporary(e.getOldCursor()))e.setCancelled(true);
        if(e.getWhoClicked() instanceof Player p&&equipped(p)){
            for(int raw:e.getRawSlots())
                if(e.getView().getInventory(raw) instanceof PlayerInventory&&e.getView().convertSlot(raw)==40){
                    e.setCancelled(true);return;
                }
        }
    }
}
