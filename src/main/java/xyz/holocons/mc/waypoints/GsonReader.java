package xyz.holocons.mc.waypoints;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class GsonReader extends JsonReader {

    public GsonReader(final File file) throws FileNotFoundException {
        super(new BufferedReader(new FileReader(file)));
    }

    public HashMap<Long, Waypoint> readWaypointMap() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        HashMap<Long, Waypoint> map = new HashMap<>();

        beginArray();
        while (hasNext()) {
            var waypoint = readWaypoint();
            var chunkKey = waypoint.getChunkKey();
            map.put(chunkKey, waypoint);
        }
        endArray();

        return map.isEmpty() ? null : map;
    }

    public HashMap<UUID, Traveler> readTravelerMap() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        HashMap<UUID, Traveler> map = new HashMap<>();

        beginObject();
        while (hasNext()) {
            var uniqueIdString = nextName();
            UUID uniqueId;
            try {
                uniqueId = UUID.fromString(uniqueIdString);
            } catch (IllegalArgumentException e) {
                throw new IOException("Unrecognized UUID: " + uniqueIdString);
            }
            var traveler = readTraveler();
            map.put(uniqueId, traveler);
        }
        endObject();

        return map.isEmpty() ? null : map;
    }

    public Waypoint readWaypoint() throws IOException {
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
                case "id" -> id = nextInt();
                case "location" -> location = readLocation();
                case "contributors" -> contributors = readArrayListUUID();
                case "active" -> active = nextBoolean();
                default -> throw new IOException("Unrecognized property name");
            }
        }
        endObject();

        return id == -1 ? null : new Waypoint(id, location, contributors, active);
    }

    public Traveler readTraveler() throws IOException {
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
                case "charges" -> charges = nextInt();
                case "tokens" -> tokens = nextInt();
                case "home" -> home = readLocation();
                case "camp" -> camp = readLocation();
                case "waypoints" -> waypoints = readBitSet();
                default -> throw new IOException("Unrecognized property name");
            }
        }
        endObject();

        return new Traveler(charges, tokens, home, camp, waypoints);
    }

    public Location readLocation() throws IOException {
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

    public BitSet readBitSet() throws IOException {
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

    public ArrayList<UUID> readArrayListUUID() throws IOException {
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

        return list.isEmpty() ? null : list;
    }

    public UUID readUUID() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        var uniqueIdString = nextString();
        try {
            return UUID.fromString(uniqueIdString);
        } catch (IllegalArgumentException e) {
            throw new IOException("Unrecognized UUID: " + uniqueIdString);
        }
    }

    public CampBannerMap.BannerDesign readBannerDesign() throws IOException {
        if (peek() == JsonToken.NULL) {
            nextNull();
            return null;
        }

        beginObject();

        org.bukkit.Material material = org.bukkit.Material.WHITE_BANNER;
        java.util.List<org.bukkit.block.banner.Pattern> patterns = new java.util.ArrayList<>();

        while (hasNext()) {
            switch (nextName()) {
                case "material" -> {
                    try {
                        material = org.bukkit.Material.valueOf(nextString());
                    } catch (IllegalArgumentException e) {
                        material = org.bukkit.Material.WHITE_BANNER; // fallback
                    }
                }
                case "patterns" -> {
                    beginArray();
                    while (hasNext()) {
                        beginObject();

                        org.bukkit.DyeColor color = org.bukkit.DyeColor.WHITE;
                        org.bukkit.block.banner.PatternType patternType = org.bukkit.block.banner.PatternType.STRIPE_MIDDLE;

                        while (hasNext()) {
                            switch (nextName()) {
                                case "color" -> {
                                    try {
                                        color = org.bukkit.DyeColor.valueOf(nextString());
                                    } catch (IllegalArgumentException e) {
                                        color = org.bukkit.DyeColor.WHITE; // fallback
                                    }
                                }
                                case "pattern" -> {
                                    try {
                                        String patternName = nextString();
                                        // Try to parse using valueOf (deprecated but still works)
                                        patternType = org.bukkit.block.banner.PatternType.valueOf(patternName);
                                    } catch (IllegalArgumentException e) {
                                        patternType = org.bukkit.block.banner.PatternType.STRIPE_MIDDLE; // fallback
                                    }
                                }
                                default -> skipValue();
                            }
                        }

                        patterns.add(new org.bukkit.block.banner.Pattern(color, patternType));
                        endObject();
                    }
                    endArray();
                }
                default -> skipValue();
            }
        }

        endObject();

        return new CampBannerMap.BannerDesign(material, patterns.toArray(new org.bukkit.block.banner.Pattern[0]));
    }
}
