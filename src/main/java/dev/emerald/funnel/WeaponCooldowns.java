package dev.emerald.funnel;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/** Native item group overlay; authoritative ability timers remain player-owned. */
final class WeaponCooldowns {
    private WeaponCooldowns() {}
    static NamespacedKey key(String group){return new NamespacedKey("funnel","cooldown/"+group);}
    static String group(String model){
        if(model.startsWith("gn_sword_v_"))return "gn_v";
        if(model.startsWith("gn_sword_ii_"))return "gn_ii";
        if(model.startsWith("twin_buster_rifle_"))return "buster";
        if(model.startsWith("barbatos_ultra_mace"))return "mace";
        return switch(model){
            case "hyper_hammer_handle" -> "hammer";
            case "destiny_controller" -> "destiny";
            case "epyon_beam_sword","epyon_shield" -> "epyon";
            case "heavyarms_gatling_body" -> "heavyarms";
            case "freedom_lupus_rifle" -> "freedom";
            case "gp02_atomic_bazooka" -> "atomic";
            case "gx_twin_satellite_controller" -> "satellite";
            case "brau_bro_controller" -> "brau";
            case "aerial_v2_bit_01" -> "aerial";
            case "funnel_drone_1","funnel_drone_1_folded" -> "fin";
            case "funnel_drone_3" -> "qubeley";
            case "funnel_drone_4" -> "shield";
            default -> null;
        };
    }
    static void refreshOnline(){for(Player p:Bukkit.getOnlinePlayers())refresh(p);}
    static void refresh(Player p){
        PlayerInventory inv=p.getInventory();
        for(int slot=0;slot<inv.getSize();slot++){
            ItemStack item=inv.getItem(slot);
            if(item==null||!item.hasItemMeta())continue;
            ItemMeta meta=item.getItemMeta();
            NamespacedKey model=meta.getItemModel();
            if(model==null||!model.getNamespace().equals("funnel"))continue;
            String group=group(model.getKey());
            if(group==null)continue;
            var component=meta.getUseCooldown();
            NamespacedKey key=key(group);
            if(key.equals(component.getCooldownGroup()))continue;
            component.setCooldownSeconds(.05f);
            component.setCooldownGroup(key);
            meta.setUseCooldown(component);
            item.setItemMeta(meta);
            inv.setItem(slot,item);
        }
    }
    static void show(Player player,String group,long remaining){
        if(remaining<=0)return;
        refresh(player);
        NamespacedKey key=key(group);
        int ticks=(int)Math.min(Integer.MAX_VALUE,remaining);
        if(player.getCooldown(key)<ticks)player.setCooldown(key,ticks);
    }
}
