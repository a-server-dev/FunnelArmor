package dev.emerald.funnel;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * /funnel accepts only operator accounts, EmeraldREX91, and the server
 * console. LuckPerms funnel.admin by itself does not grant command access.
 */
final class FunnelCommandAccess {
    private FunnelCommandAccess() {}

    static boolean authorizedPlayer(String name, boolean op) {
        return op || "EmeraldREX91".equalsIgnoreCase(name);
    }

    static boolean allowed(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender instanceof Player p && authorizedPlayer(p.getName(), p.isOp());
    }
}
