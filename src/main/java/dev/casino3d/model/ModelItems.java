package dev.casino3d.model;

import dev.casino3d.api.MachineModelResolver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ModelItems {
    public static ItemStack resolve(String reference) {
        if (reference.startsWith("material:"))
            return new ItemStack(Material.valueOf(reference.substring(9)));
        if (reference.startsWith("3dcasino:") && VanillaGeometry.get(reference.substring(9)) != null) {
            var item = new ItemStack(VanillaModels.material(reference));
            VanillaGeometry.mark(item, reference);
            return item;
        }
        var provider = Bukkit.getServicesManager().load(MachineModelResolver.class);
        if (provider != null) {
            var result = provider.resolve(reference);
            if (result != null) return result.clone();
        }
        return new ItemStack(VanillaModels.material(reference));
    }

    private ModelItems() {}
}
