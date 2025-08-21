package xyz.holocons.mc.waypoints;

import org.bukkit.Location;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * A simple wrapper class to create camp holograms using the existing waypoint
 * hologram system
 */
public class CampWaypoint extends Waypoint {
    private final String campName;

    public CampWaypoint(Location location, String name) {
        super(-1, location, null, true); // Use -1 as ID for camp waypoints
        this.campName = name;
    }

    @Override
    public Component getDisplayName() {
        return Component.text(campName, NamedTextColor.YELLOW);
    }

    @Override
    public boolean hasName() {
        return true;
    }
}
