package xyz.holocons.mc.waypoints;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Stream;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class WaypointMap {

    public static final String FILENAME = "waypoint.json";

    private final HashMap<Long, Waypoint> waypoints;

    public WaypointMap() {
        this.waypoints = new HashMap<>();
    }

    public void loadWaypoints(WaypointsPlugin plugin) throws IOException {
        final var file = new File(plugin.getDataFolder(), FILENAME);

        if (!file.exists()) {
            return;
        }

        final var reader = new GsonReader(file);
        final var waypointMap = reader.readWaypointMap();
        reader.close();

        clearWaypoints();

        if (waypointMap != null) {
            waypoints.putAll(waypointMap);
        }
    }

    public void saveWaypoints(WaypointsPlugin plugin) throws IOException {
        if (waypoints.isEmpty()) {
            return;
        }

        final var file = new File(plugin.getDataFolder(), FILENAME);

        final var writer = new GsonWriter(file);
        writer.writeWaypointMap(waypoints);
        writer.close();
    }

    public void clearWaypoints() {
        waypoints.clear();
    }

    public Waypoint getWaypoint(long chunkKey) {
        return waypoints.get(chunkKey);
    }

    public Waypoint getNearbyWaypoint(Location location) {
        return getWaypoint(Chunk.getChunkKey(location));
    }

    public Waypoint getNearbyWaypoint(Block block) {
        return getNearbyWaypoint(block.getLocation());
    }

    public Waypoint createWaypoint(Location location) {
        if (getNearbyWaypoint(location) != null) {
            return null;
        }
        var waypoint = new Waypoint(getAvailableId(), location, null, false);
        var chunkKey = waypoint.getChunkKey();
        waypoints.put(chunkKey, waypoint);
        return waypoint;
    }

    public Waypoint createWaypoint(Block block) {
        return createWaypoint(block.getLocation());
    }

    private int getAvailableId() {
        return waypoints.values().stream().mapToInt(Waypoint::getId).max().orElse(-1) + 1;
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint.getChunkKey());
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.put(waypoint.getChunkKey(), waypoint);
    }

    public boolean isWaypoint(Location location) {
        var waypoint = getNearbyWaypoint(location);
        return waypoint != null && waypoint.getLocation().equals(location);
    }

    public boolean isWaypoint(Block block) {
        return isWaypoint(block.getLocation());
    }

    public Stream<Waypoint> getAllWaypoints() {
        return waypoints.values().stream();
    }

    public Stream<Waypoint> getActiveWaypoints() {
        return getAllWaypoints().filter(Waypoint::isActive);
    }

    public Stream<Waypoint> getNamedWaypoints() {
        return getActiveWaypoints().filter(Waypoint::hasName);
    }
}
