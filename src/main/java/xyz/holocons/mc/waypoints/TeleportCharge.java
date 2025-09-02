package xyz.holocons.mc.waypoints;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Represents the teleport charge item that players can obtain from mob drops
 * and right-click to convert into stored teleport charges (wallet).
 */
public final class TeleportCharge {

    public static final int MODEL_DATA = 1001; // Central constant for resource pack mapping
    private final WaypointsPlugin plugin;
    private final NamespacedKey key;

    public TeleportCharge(WaypointsPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "teleport_charge");
        plugin.getLogger().info("Registering teleport charge with custom model data: " + MODEL_DATA);
    }

    public ItemStack createCharge(int amount) {
        final var stack = createSingle();
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        return stack;
    }

    public boolean isTeleportCharge(Object obj) {
        return obj instanceof ItemStack itemStack && isTeleportCharge(itemStack);
    }

    private boolean isTeleportCharge(ItemStack stack) {
        if (stack == null)
            return false;
        if (stack.getType() != getMaterial())
            return false;
        if (!stack.hasItemMeta())
            return false;
        return stack.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private ItemStack createSingle() {
        final var mat = getMaterial();
        final var item = new ItemStack(mat);
        final var meta = item.getItemMeta();
        meta.displayName(Component.text(getDisplayName()).decoration(TextDecoration.ITALIC, false));
        meta.setCustomModelData(MODEL_DATA); // Always use the constant value
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0x1);
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterial() {
        final var cfg = plugin.getConfig();
        final var matName = cfg.getString("charge.item.material",
                cfg.getString("teleport-charge-item.material", "AMETHYST_SHARD"));
        try {
            return Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Material.AMETHYST_SHARD; // fallback
        }
    }

    private String getDisplayName() {
        final var cfg = plugin.getConfig();
        return cfg.getString("charge.item.name", cfg.getString("teleport-charge-item.name", "Teleport Charge"));
    }
}
