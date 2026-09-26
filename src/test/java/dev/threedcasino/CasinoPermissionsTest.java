package dev.threedcasino;

import static org.junit.jupiter.api.Assertions.*;

import dev.threedcasino.integration.CasinoPermissions;

import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

class CasinoPermissionsTest {
    private Permissible player(Map<String, Boolean> explicit) {
        return (Permissible)
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] {Permissible.class},
                        (proxy, method, args) ->
                                switch (method.getName()) {
                                    case "isPermissionSet" -> explicit.containsKey(args[0]);
                                    case "hasPermission" ->
                                            explicit.getOrDefault(
                                                    args[0], args[0].equals("3dcasino.use"));
                                    default ->
                                            throw new UnsupportedOperationException(
                                                    method.getName());
                                });
    }

    @Test
    void onlyCurrentPermissionsControlAccess() {
        assertTrue(CasinoPermissions.allowed(player(Map.of("servermines.use", false)), "use"));
        assertFalse(CasinoPermissions.allowed(player(Map.of("servermines.machine", true)), "machine"));
        assertTrue(CasinoPermissions.allowed(player(Map.of("3dcasino.machine", true)), "machine"));
        assertFalse(CasinoPermissions.allowed(player(Map.of("3dcasino.use", false)), "use"));
        assertTrue(CasinoPermissions.allowed(player(Map.of()), "use"));
        assertFalse(CasinoPermissions.allowed(player(Map.of()), "machine"));
    }
}
