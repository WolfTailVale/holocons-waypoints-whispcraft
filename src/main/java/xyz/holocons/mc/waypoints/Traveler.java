package xyz.holocons.mc.waypoints;

import java.util.BitSet;

import org.bukkit.Location;

public class Traveler {

    private int charges;
    private int tokens;
    private Location home;
    private Location camp;
    private BitSet waypoints;

    public Traveler(int charges, int tokens, Location home, Location camp, BitSet waypoints) {
        this.charges = charges;
        this.tokens = tokens;
        this.home = home;
        this.camp = camp;
        this.waypoints = waypoints != null ? waypoints : new BitSet();
    }

    public int getCharges() {
        return charges;
    }

    public int getTokens() {
        return tokens;
    }

    public Location getHome() {
        return home;
    }

    public Location getCamp() {
        return camp;
    }

    public BitSet getWaypoints() {
        return waypoints;
    }

    public boolean hasWaypoint(Waypoint waypoint) {
        return waypoint != null && waypoints.get(waypoint.getId());
    }

    public void registerWaypoint(Waypoint waypoint) {
        if (waypoint == null) {
            return;
        }
        waypoints.set(waypoint.getId());
    }

    public void unregisterWaypoint(Waypoint waypoint) {
        if (waypoint == null) {
            return;
        }
        waypoints.clear(waypoint.getId());
    }

    public void setCharges(int charges) {
        this.charges = charges;
    }

    public void addCharges(int amount) {
        if (amount <= 0)
            return;
        this.charges += amount;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public void setCamp(Location camp) {
        this.camp = camp;
    }
}
