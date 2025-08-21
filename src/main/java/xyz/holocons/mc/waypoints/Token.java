package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public class Token {

    private final NamespacedKey key;

    public Token(WaypointsPlugin plugin) {
        this.key = new NamespacedKey(plugin, "token");
        Bukkit.addRecipe(getRecipe());
    }

    private ShapedRecipe getRecipe() {
        return new ShapedRecipe(key, getItemStack())
                .shape("aea", "ebe", "aea")
                .setIngredient('a', Material.ENDER_EYE)
                .setIngredient('e', Material.ECHO_SHARD)
                .setIngredient('b', Material.WHITE_BANNER);
    }

    private ItemStack getItemStack() {
        final var itemStack = new ItemStack(Material.ENDER_PEARL);
        final var itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Waypoint Token").decoration(TextDecoration.ITALIC, false));
        
        // Set custom model data for resource pack support
        itemMeta.setCustomModelData(470001); // Unique ID for WhispWaypoints token
        
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0x0);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isToken(Object obj) {
        return isTokenThrowableProjectile(obj) || isTokenItemStack(obj);
    }

    private boolean isTokenThrowableProjectile(Object obj) {
        return obj instanceof ThrowableProjectile throwableProjectile
                && isTokenItemStack(throwableProjectile.getItem());
    }

    private boolean isTokenItemStack(Object obj) {
        return obj instanceof ItemStack itemStack
                && itemStack.getType() == Material.ENDER_PEARL
                && itemStack.getItemMeta().getPersistentDataContainer().has(key);
    }
}
