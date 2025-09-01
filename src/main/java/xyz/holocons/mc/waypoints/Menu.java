package xyz.holocons.mc.waypoints;

import java.util.Comparator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Menu implements InventoryHolder {

    public enum Type {
        EDIT,
        TELEPORT,
    }

    private static final int HOME_SLOT = 35;
    private static final int CAMP_SLOT = 44;
    private static final int INFO_SLOT = 53;

    private final Inventory inventory;
    private final NamespacedKey locationKey, pageKey;
    private final WaypointsPlugin plugin;
    private final Player player;
    private final Type type;

    public Menu(final WaypointsPlugin plugin, final Player player, final Type type) {
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Waypoints"));
        this.locationKey = new NamespacedKey(plugin, "location");
        this.pageKey = new NamespacedKey(plugin, "page");
        this.plugin = plugin;
        this.player = player;
        this.type = type;

        switch (type) {
            case EDIT -> {
                createEditPage(0);
            }
            case TELEPORT -> {
                createTeleportPage(0);
            }
            default -> {
            }
        }
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void createEditPage(int page) {
        final var waypoints = plugin.getWaypointMap()
                .getAllWaypoints()
                .sorted(Comparator.comparing(Waypoint::getName))
                .toList();
        final var pageCount = Math.max(waypoints.size() - 1, 0) / 48 + 1;
        final var currentPage = page % pageCount;
        final var fromIndex = currentPage * 48;
        final var toIndex = Math.min(fromIndex + 48, waypoints.size());

        inventory.clear();
        int slot = 0;
        for (final var waypoint : waypoints.subList(fromIndex, toIndex)) {
            final var item = waypoint.getDisplayItem();
            final var location = waypoint.getLocation();
            final var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(locationKey, DataType.LOCATION, location);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
            if ((slot + 1) % 9 == 0) {
                slot++;
            }
        }
        final var item = new ItemStack(Material.ENDER_PEARL);
        final var meta = item.getItemMeta();
        meta.displayName(Component.text(String.format("Page %d/%d", currentPage + 1, pageCount)));
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, currentPage);
        item.setItemMeta(meta);
        inventory.setItem(INFO_SLOT, item);
    }

    private void createTeleportPage(int page) {
        final var traveler = plugin.getTravelerMap().getOrCreateTraveler(player);
        final var waypoints = plugin.getWaypointMap()
                .getActiveWaypoints()
                .filter(traveler::hasWaypoint)
                .sorted(Comparator.comparing(Waypoint::getName))
                .toList();
        final var pageCount = Math.max(waypoints.size() - 1, 0) / 48 + 1;
        final var currentPage = page % pageCount;
        final var fromIndex = currentPage * 48;
        final var toIndex = Math.min(fromIndex + 48, waypoints.size());

        inventory.clear();
        int slot = 0;
        for (final var waypoint : waypoints.subList(fromIndex, toIndex)) {
            final var item = waypoint.getDisplayItem();
            final var location = waypoint.getLocation();
            final var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(locationKey, DataType.LOCATION, location);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
            if ((slot + 1) % 9 == 0) {
                slot++;
            }
        }
        final var home = traveler.getHome();
        if (home != null) {
            final var item = new ItemStack(Material.RED_BED);
            final var meta = item.getItemMeta();
            meta.displayName(Component.text("Home"));
            meta.getPersistentDataContainer().set(locationKey, DataType.LOCATION, home);
            item.setItemMeta(meta);
            inventory.setItem(HOME_SLOT, item);
        }
        final var camp = traveler.getCamp();
        if (camp != null) {
            final var item = new ItemStack(Material.CAMPFIRE);
            final var meta = item.getItemMeta();
            meta.displayName(Component.text("Camp"));
            meta.getPersistentDataContainer().set(locationKey, DataType.LOCATION, camp);
            item.setItemMeta(meta);
            inventory.setItem(CAMP_SLOT, item);
        }
        final var item = new ItemStack(Material.ENDER_PEARL);
        final var meta = item.getItemMeta();
        meta.displayName(Component.text(String.format("Page %d/%d", currentPage + 1, pageCount)));
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, currentPage);
        final var charges = Component.text(String.format("%d charges", traveler.getCharges()));
        final var tokens = Component.text(String.format("%d tokens", traveler.getTokens()));
        meta.lore(List.of(charges, tokens));
        item.setItemMeta(meta);
        inventory.setItem(INFO_SLOT, item);
    }

    public void handleClick(ItemStack clickedItem, int slot) {
        switch (type) {
            case EDIT -> {
                final var location = clickedItem.getItemMeta().getPersistentDataContainer()
                        .get(locationKey, DataType.LOCATION);
                if (location == null) {
                    final var currentPage = inventory.getItem(INFO_SLOT).getItemMeta().getPersistentDataContainer()
                            .get(pageKey, PersistentDataType.INTEGER);
                    createEditPage(currentPage + 1);
                    return;
                }
                player.teleport(location);
                inventory.close();
            }
            case TELEPORT -> {
                final var location = clickedItem.getItemMeta().getPersistentDataContainer()
                        .get(locationKey, DataType.LOCATION);
                if (location == null) {
                    final var currentPage = inventory.getItem(INFO_SLOT).getItemMeta().getPersistentDataContainer()
                            .get(pageKey, PersistentDataType.INTEGER);
                    createTeleportPage(currentPage + 1);
                    return;
                }
                final var teleportType = switch (slot) {
                    case HOME_SLOT -> TeleportTask.Type.HOME;
                    case CAMP_SLOT -> TeleportTask.Type.CAMP;
                    default -> TeleportTask.Type.WAYPOINT;
                };

                // Log GUI-initiated teleportation
                String destinationType = switch (teleportType) {
                    case HOME -> "Home";
                    case CAMP -> "Camp";
                    case WAYPOINT -> {
                        final var displayName = clickedItem.getItemMeta().displayName();
                        if (displayName != null) {
                            yield "Waypoint '" + PlainTextComponentSerializer.plainText().serialize(displayName) + "'";
                        } else {
                            yield "Waypoint";
                        }
                    }
                };
                plugin.getLogger().info(String.format("[TELEPORT] Player %s (%s) initiated %s teleport via GUI",
                        player.getName(), player.getUniqueId(), destinationType));

                new TeleportTask(plugin, player, teleportType, location);
                inventory.close();
            }
            default -> {
            }
        }
    }
}
