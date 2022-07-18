package xyz.holocons.mc.waypoints;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.UUID;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class GsonReader extends JsonReader {

    public GsonReader(final File file) throws FileNotFoundException {
        super(new BufferedReader(new FileReader(file)));
    }

    public Waypoint nextWaypoint() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        int id = -1;
        Location location = null;
        ArrayList<UUID> contributors = null;
        boolean active = false;

        beginObject();
        while (hasNext()) {
            switch (nextName()) {
                case "id"           -> id = nextInt();
                case "location"     -> location = nextLocation();
                case "contributors" -> contributors = nextArrayListUUID();
                case "active"       -> active = nextBoolean();
                default -> throw new IOException("Unrecognized property name");
            }
        }
        endObject();

        return id != -1 ? new Waypoint(id, location, contributors, active) : null;
    }

    public Traveler nextTraveler() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        int charges = 0;
        int tokens = 0;
        Location home = null;
        Location camp = null;
        BitSet waypoints = null;

        beginObject();
        while (hasNext()) {
            switch (nextName()) {
                case "charges"   -> charges = nextInt();
                case "tokens"    -> tokens = nextInt();
                case "home"      -> home = nextLocation();
                case "camp"      -> camp = nextLocation();
                case "waypoints" -> waypoints = nextBitSet();
                default -> throw new IOException("Unrecognized property name");
            }
        }
        endObject();

        return new Traveler(charges, tokens, home, camp, waypoints);
    }

    public Location nextLocation() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        var location = nextString().split(",");
        var world = Bukkit.getWorld(location[0]);
        double x, y, z;
        try {
            if (world == null) {
                throw new NullPointerException();
            }
            x = Double.parseDouble(location[1]);
            y = Double.parseDouble(location[2]);
            z = Double.parseDouble(location[3]);
        } catch (NullPointerException | NumberFormatException e) {
            throw new IOException("Unrecognized Location: " + Arrays.toString(location));
        }

        return new Location(world, x, y, z);
    }

    public BitSet nextBitSet() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        var hexString = nextString();
        byte[] bytes;
        try {
            bytes = HexFormat.of().parseHex(hexString);
        } catch (IllegalArgumentException e) {
            throw new IOException("Unrecognized BitSet: " + hexString);
        }

        return BitSet.valueOf(bytes);
    }

    public ArrayList<UUID> nextArrayListUUID() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        ArrayList<UUID> list = new ArrayList<>();

        beginArray();
        while (hasNext()) {
            if (peek() == JsonToken.NULL) {
                nextNull();
                continue;
            }

            var uniqueIdString = nextString();
            UUID uniqueId;
            try {
                uniqueId = UUID.fromString(uniqueIdString);
            } catch (IllegalArgumentException e) {
                throw new IOException("Unrecognized UUID: " + uniqueIdString);
            }
            list.add(uniqueId);
        }
        endArray();

        return !list.isEmpty() ? list : null;
    }
}
