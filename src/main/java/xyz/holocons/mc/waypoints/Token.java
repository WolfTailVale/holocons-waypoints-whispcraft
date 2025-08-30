package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public class Token {

    public static final int MODEL_DATA = 1001; // Central constant for resource pack mapping
    private final NamespacedKey key;

    public Token(WaypointsPlugin plugin) {
        this.key = new NamespacedKey(plugin, "token");
        Bukkit.addRecipe(getRecipe());
        plugin.getLogger().info("Registering waypoint token recipe with custom model data: " + MODEL_DATA);
    }

    private ShapedRecipe getRecipe() {
        return new ShapedRecipe(key, getItemStack())
                .shape("aea", "ebe", "aea")
                .setIngredient('a', Material.ENDER_EYE)
                .setIngredient('e', Material.ECHO_SHARD)
                .setIngredient('b', Material.WHITE_BANNER);
    }

    private ItemStack getItemStack() {
        return createTokenItem();
    }

    private ItemStack createTokenItem() {
        final var itemStack = new ItemStack(Material.PAPER);
        final var meta = itemStack.getItemMeta(); // get copy
        applyTokenMeta(meta); // modify copy
        itemStack.setItemMeta(meta); // apply modified copy
        return itemStack;
    }

    public ItemStack createToken(int amount) {
        final var stack = createTokenItem();
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        return stack;
    }

    private void applyTokenMeta(org.bukkit.inventory.meta.ItemMeta itemMeta) {
        itemMeta.displayName(Component.text("Waypoint Token").decoration(TextDecoration.ITALIC, false));
        // Always re-apply custom model data (in case recipe cloning stripped it)
        itemMeta.setCustomModelData(MODEL_DATA);
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0x0);
    }

    /**
     * Repairs a token ItemStack in place (adds custom model data & PDC marker).
     * Returns true if modified.
     */
    public boolean repairToken(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER)
            return false;
        final var meta = stack.getItemMeta();
        final boolean wasMissing = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE) == null
                || !meta.hasCustomModelData();
        applyTokenMeta(meta);
        stack.setItemMeta(meta);
        return wasMissing;
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
                && itemStack.getType() == Material.PAPER
                && itemStack.hasItemMeta()
                && itemStack.getItemMeta().getPersistentDataContainer().has(key);
    }

    public Integer getCustomModelData(ItemStack stack) {
        return stack != null && stack.hasItemMeta() ? stack.getItemMeta().getCustomModelData() : null;
    }
}
