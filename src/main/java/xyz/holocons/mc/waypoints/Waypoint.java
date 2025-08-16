package xyz.holocons.mc.waypoints;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Waypoint {

    private static final Component INACTIVE_WAYPOINT_COMPONENT;
    private static final Component UNNAMED_WAYPOINT_COMPONENT;
    private static final ItemStack MISSING_BANNER_ITEMSTACK;
    static {
        INACTIVE_WAYPOINT_COMPONENT = Component.text("Inactive Waypoint");
        UNNAMED_WAYPOINT_COMPONENT = Component.text("Unnamed Waypoint");
        MISSING_BANNER_ITEMSTACK = new ItemStack(Material.WHITE_BANNER);
        var bannerMeta = (BannerMeta) MISSING_BANNER_ITEMSTACK.getItemMeta();
        bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
        // RHOMBUS_MIDDLE was renamed in newer versions (e.g., LOZENGE / DIAMOND).
        // Resolve dynamically.
        var rhombus = resolvePatternType("RHOMBUS_MIDDLE", "LOZENGE", "DIAMOND");
        bannerMeta.addPattern(new Pattern(DyeColor.WHITE, rhombus));
        bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
        // HALF_HORIZONTAL_MIRROR may be renamed (e.g., HALF_HORIZONTAL /
        // HALF_HORIZONTAL_TOP)
        var halfHorizontalMirror = resolvePatternType("HALF_HORIZONTAL_MIRROR", "HALF_HORIZONTAL_TOP",
                "HALF_HORIZONTAL");
        bannerMeta.addPattern(new Pattern(DyeColor.WHITE, halfHorizontalMirror));
        bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM));
        bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
        bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM));
        bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        bannerMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS);
        bannerMeta.displayName(UNNAMED_WAYPOINT_COMPONENT);
        MISSING_BANNER_ITEMSTACK.setItemMeta(bannerMeta);
    }

    private static PatternType resolvePatternType(String... candidates) {
        for (var name : candidates) {
            try {
                return PatternType.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return PatternType.STRIPE_MIDDLE; // Fallback pattern
    }

    private int id;
    private Location location;
    private ArrayList<UUID> contributors;
    private boolean active;

    public Waypoint(int id, Location location, ArrayList<UUID> contributors, boolean active) {
        this.id = id;
        this.location = location;
        this.contributors = contributors == null ? new ArrayList<>() : contributors;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public Location getLocation() {
        return location.clone();
    }

    public ArrayList<UUID> getContributors() {
        return contributors;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public long getChunkKey() {
        return Chunk.getChunkKey(location);
    }

    public List<String> getContributorNames() {
        return contributors.stream().map(uniqueId -> {
            final var player = Bukkit.getPlayer(uniqueId);
            return player != null ? player.getName() : uniqueId.toString();
        }).toList();
    }

    private static ItemStack getBannerItem(Location location) {
        for (var itemStack : location.getBlock().getDrops()) {
            if (Tag.ITEMS_BANNERS.isTagged(itemStack.getType())) {
                return itemStack;
            }
        }
        return MISSING_BANNER_ITEMSTACK.clone();
    }

    private static ItemStack getDisplayItem(Location location) {
        var itemStack = getBannerItem(location);
        var itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS);
        if (itemMeta.hasDisplayName()) {
            itemMeta.displayName(itemMeta.displayName().decoration(TextDecoration.ITALIC, false));
        }
        var vectorComponent = Component.text(
                String.format("%d %d %d", location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                NamedTextColor.GRAY);
        var worldComponent = Component.text(location.getWorld().getName(), NamedTextColor.GRAY);
        itemMeta.lore(List.of(vectorComponent, worldComponent));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getDisplayItem() {
        var itemStack = Waypoint.getDisplayItem(location);
        if (!active) {
            var itemMeta = itemStack.getItemMeta();
            itemMeta.displayName(INACTIVE_WAYPOINT_COMPONENT);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private static Component getDisplayName(Location location) {
        var itemStack = getBannerItem(location);
        var itemMeta = itemStack.getItemMeta();
        if (itemMeta.hasDisplayName()) {
            return itemMeta.displayName();
        }
        return UNNAMED_WAYPOINT_COMPONENT;
    }

    public Component getDisplayName() {
        if (!active) {
            return INACTIVE_WAYPOINT_COMPONENT;
        }
        return Waypoint.getDisplayName(location);
    }

    public String getName() {
        return PlainTextComponentSerializer.plainText().serialize(Waypoint.getDisplayName(location));
    }

    public boolean hasName() {
        return getName() != PlainTextComponentSerializer.plainText().serialize(UNNAMED_WAYPOINT_COMPONENT);
    }

    public boolean hasBannerItem(Waypoint waypoint) {
        return getBannerItem(location) != MISSING_BANNER_ITEMSTACK;
    }
}
