package dev.threedcasino.model;

import org.bukkit.Material;

/** Visible built-in items for servers that do not distribute a resource pack. */
public final class VanillaModels {
    public static Material material(String reference) {
        String name = reference.substring(reference.indexOf(':') + 1);
        if (name.startsWith("card_")) return Material.PAPER;
        if (name.startsWith("cabinet_button_") || name.startsWith("showcase_button_"))
            return Material.STONE_BUTTON;
        if (name.startsWith("showcase_duck_")) return Material.EGG;
        return switch (name) {
            case "mine_hidden" -> Material.POLISHED_DEEPSLATE;
            case "mine_gem" -> Material.EMERALD_BLOCK;
            case "mine_bomb" -> Material.TNT;
            case "plinko_ball" -> Material.SLIME_BALL;
            case "cabinet_rocket" -> Material.FIREWORK_ROCKET;
            case "showcase_tile" -> Material.BROWN_CONCRETE;
            case "showcase_tile_selected" -> Material.ORANGE_CONCRETE;
            case "showcase_wheel_fortune", "showcase_wheel_money" -> Material.CLOCK;
            case "showcase_pointer" -> Material.ARROW;
            case "showcase_penguin" -> Material.BLACK_CONCRETE;
            case "showcase_hilo_panel" -> Material.GRAY_CONCRETE;
            case "showcase_slider" -> Material.REDSTONE_TORCH;
            default -> Material.PAPER;
        };
    }

    private VanillaModels() {}
}
