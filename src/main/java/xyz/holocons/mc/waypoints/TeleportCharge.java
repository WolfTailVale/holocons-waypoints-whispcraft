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

    private final WaypointsPlugin plugin;
    private final NamespacedKey key;

    public TeleportCharge(WaypointsPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "teleport_charge");
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
        final var model = getCustomModelData();
        if (model > 0) {
            meta.setCustomModelData(model);
        }
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

    private int getCustomModelData() {
        final var cfg = plugin.getConfig();
        return cfg.getInt("charge.item.model-data", cfg.getInt("teleport-charge-item.model-data", 0));
    }
}
