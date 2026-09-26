package dev.casino3d.integration;

import org.bukkit.permissions.Permissible;

public final class CasinoPermissions {
    private CasinoPermissions() {}

    public static boolean allowed(Permissible player, String suffix) {
        return player.hasPermission("3dcasino." + suffix);
    }
}
