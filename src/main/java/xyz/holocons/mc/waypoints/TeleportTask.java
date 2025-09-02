package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TeleportTask extends BukkitRunnable {

    public enum Type {
        CAMP,
        HOME,
        WAYPOINT,
    }

    private final Player player;
    private final Traveler traveler;
    private final int cost;
    private final Location destination;
    private final double initialHealth;
    private final Vector initialPosition;
    private final NamespacedKey key;
    private final WaypointsPlugin plugin;
    private final String destinationName;

    public TeleportTask(final WaypointsPlugin plugin, final Player player, final Type type,
            final Location destination) {
        plugin.getTravelerMap().registerTask(player, this);
        final var teleportWaitTime = plugin.getTeleportWaitTime();
        final var period = teleportWaitTime / 20;
        final var taskId = runTaskTimer(plugin, period, period).getTaskId();
        this.plugin = plugin;
        this.player = player;
        this.traveler = plugin.getTravelerMap().getOrCreateTraveler(player);
        this.cost = switch (type) {
            case CAMP -> 0;  // Free camp teleport
            case HOME -> 0;  // Free home teleport
            case WAYPOINT -> {
                // Check if this is an inter-world teleport
                final boolean isInterWorld = !player.getWorld().equals(destination.getWorld());
                yield isInterWorld ? plugin.getInterWorldTeleportCost() : plugin.getWaypointTeleportCost();
            }
        };
        this.destination = toXZCenterLocation(destination);
        this.destinationName = getDestinationName(destination, type);
        this.initialHealth = player.getHealth();
        this.initialPosition = player.getLocation().toVector();
        this.key = new NamespacedKey(plugin, Integer.toString(taskId));

        // Log teleportation attempt
        final boolean isInterWorld = !player.getWorld().equals(destination.getWorld());
        final String teleportInfo = type == Type.WAYPOINT && isInterWorld 
            ? String.format("%s teleport (INTER-WORLD: %s → %s)", type.name().toLowerCase(), 
                           player.getWorld().getName(), destination.getWorld().getName())
            : type.name().toLowerCase() + " teleport";
            
        plugin.getLogger().info(String.format(
                "[TELEPORT] Player %s (%s) initiated %s to %s (World: %s, Coords: %.1f, %.1f, %.1f) - Cost: %d charges",
                player.getName(), player.getUniqueId(), teleportInfo, destinationName,
                destination.getWorld().getName(), destination.getX(), destination.getY(), destination.getZ(), cost));

        final var bossBar = Bukkit.createBossBar(key, "Teleporting...", BarColor.GREEN, BarStyle.SEGMENTED_20);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            removeBossBar(key);
        }, teleportWaitTime + 2);
    }

    @Override
    public void run() {
        if (playerTookDamage() || playerMoved()) {
            cancel();
            removeBossBar(key);

            // Log teleportation failure due to movement or damage
            String failureReason = playerTookDamage() ? "player took damage" : "player moved";
            plugin.getLogger().info(String.format("[TELEPORT] Player %s (%s) teleportation to %s FAILED - Reason: %s",
                    player.getName(), player.getUniqueId(), destinationName, failureReason));

            player.sendMessage("Teleportation failed...");
            return;
        }

        final var bossBar = Bukkit.getBossBar(key);
        if (bossBar == null) {
            // Log unexpected teleportation failure
            plugin.getLogger()
                    .warning(String.format(
                            "[TELEPORT] Player %s (%s) teleportation to %s FAILED - Reason: boss bar unexpectedly null",
                            player.getName(), player.getUniqueId(), destinationName));
            cancel();
            return;
        }

        final var newProgress = Math.min(bossBar.getProgress() + 0.05, 1.0);
        bossBar.setProgress(newProgress);
        if (newProgress == 1.0) {
            cancel();
            final var charges = traveler.getCharges();
            if (charges >= cost) {
                traveler.setCharges(charges - cost);
                destination.setDirection(player.getLocation().getDirection());

                // Log successful teleportation
                plugin.getLogger().info(String.format(
                        "[TELEPORT] Player %s (%s) teleportation to %s SUCCESS - Charges used: %d, Remaining: %d",
                        player.getName(), player.getUniqueId(), destinationName, cost, charges - cost));

                player.teleport(destination);
            } else {
                // Log teleportation failure due to insufficient charges
                plugin.getLogger().info(String.format(
                        "[TELEPORT] Player %s (%s) teleportation to %s FAILED - Reason: insufficient charges (has %d, needs %d)",
                        player.getName(), player.getUniqueId(), destinationName, charges, cost));

                player.sendMessage(net.kyori.adventure.text.Component.text(
                        "Teleportation failed - not enough charges! You have " + charges + " but need " + cost + ".",
                        net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        // Log manual cancellation (if not already completed)
        final var bossBar = Bukkit.getBossBar(key);
        if (bossBar != null && bossBar.getProgress() < 1.0) {
            plugin.getLogger().info(String.format(
                    "[TELEPORT] Player %s (%s) teleportation to %s CANCELLED - Manual cancellation or disconnect",
                    player.getName(), player.getUniqueId(), destinationName));
        }

        super.cancel();
    }

    private void removeBossBar(final NamespacedKey key) {
        final var bossBar = Bukkit.getBossBar(key);
        if (bossBar != null) {
            bossBar.removePlayer(player);
            Bukkit.removeBossBar(key);
        }
    }

    private boolean playerTookDamage() {
        return player.getHealth() < initialHealth;
    }

    private boolean playerMoved() {
        final var currentPosition = player.getLocation().toVector();
        final var distanceX = currentPosition.getX() - initialPosition.getX();
        final var distanceZ = currentPosition.getZ() - initialPosition.getZ();
        return distanceX * distanceX + distanceZ * distanceZ > 1.0;
    }

    private static Location toXZCenterLocation(final Location location) {
        final var newLocation = location.clone();
        newLocation.setX(location.getBlockX() + 0.5);
        newLocation.setZ(location.getBlockZ() + 0.5);
        return newLocation;
    }

    private String getDestinationName(final Location destination, final Type type) {
        switch (type) {
            case CAMP -> {
                return "Camp";
            }
            case HOME -> {
                return "Home";
            }
            case WAYPOINT -> {
                // Try to find the waypoint name from the destination
                final var waypointMap = plugin.getWaypointMap();
                final var waypoint = waypointMap.getNearbyWaypoint(destination);
                if (waypoint != null && waypoint.hasName()) {
                    return "Waypoint '" + waypoint.getName() + "'";
                } else {
                    return "Waypoint";
                }
            }
            default -> {
                return "Unknown";
            }
        }
    }
}
