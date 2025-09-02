package xyz.holocons.mc.waypoints;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class WaypointsPlugin extends JavaPlugin {

    private FileConfiguration config;
    private HologramMap hologramMap;
    private TravelerMap travelerMap;
    private WaypointMap waypointMap;
    private CampBannerMap campBannerMap;
    private Token token;
    private TeleportCharge teleportCharge;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        loadConfig();
        hologramMap = new HologramMap();
        travelerMap = new TravelerMap();
        waypointMap = new WaypointMap();
        campBannerMap = new CampBannerMap(this);
        token = new Token(this);
        teleportCharge = new TeleportCharge(this);
    }

    @Override
    public void onEnable() {
        final var commandHandler = new CommandHandler(this);
        getCommand("waypoints").setExecutor(commandHandler);
        getCommand("editwaypoints").setExecutor(commandHandler);
        getCommand("setcamp").setExecutor(commandHandler);
        getCommand("camp").setExecutor(commandHandler);
        getCommand("sethome").setExecutor(commandHandler);
        getCommand("home").setExecutor(commandHandler);
        getCommand("unsetcamp").setExecutor(commandHandler);
        getCommand("reloadwaypoints").setExecutor(commandHandler);
        getCommand("registerbanner").setExecutor(commandHandler);
        getCommand("givewptoken").setExecutor(commandHandler);
        getCommand("givewpcharge").setExecutor(commandHandler);
        final var eventListener = new EventListener(this);
        Bukkit.getPluginManager().registerEvents(eventListener, this);
    }

    @Override
    public void onDisable() {
        saveData();
    }

    public void loadConfig() {
        reloadConfig();
        config = getConfig();
        getLogger().info("Config loaded");
    }

    public void backupData() {
        final var zipFile = new File(getDataFolder(), "backup-" + Instant.now().toString() + ".zip");
        final var travelerFile = new File(getDataFolder(), TravelerMap.FILENAME);
        final var waypointFile = new File(getDataFolder(), WaypointMap.FILENAME);
        try {
            final var writer = new ZipWriter(zipFile);
            writer.addFile(travelerFile, waypointFile);
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().info("Backup written");
    }

    public void loadData() {
        try {
            travelerMap.loadTravelers(this);
            waypointMap.loadWaypoints(this);
            loadCampBannerData();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().info("Data loaded");
    }

    public void saveData() {
        try {
            waypointMap.saveWaypoints(this);
            travelerMap.saveTravelers(this);
            saveCampBannerData();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().info("Data saved");
    }

    public int getMaxCharges() {
        return config.getInt("charge.capacity");
    }

    public int getHomeTeleportCost() {
        return config.getInt("charge.teleport-home-cost");
    }

    public int getCampTeleportCost() {
        return config.getInt("charge.teleport-camp-cost");
    }

    public int getWaypointTeleportCost() {
        return config.getInt("teleport.teleport-waypoint-cost", 1);
    }

    public int getInterWorldTeleportCost() {
        return config.getInt("teleport.inter-world-cost", 2);
    }

    public int getRegenerateChargeTime() {
        return config.getInt("charge.regenerate-time");
    }

    public String getRegenerateChargeParticle() {
        return config.getString("charge.particle-effect");
    }

    public int getMaxTokens() {
        return config.getInt("token.capacity");
    }

    public int getWaypointActivateCost() {
        return config.getInt("token.activate-waypoint-cost");
    }

    public String getRegenerateTokenParticle() {
        return config.getString("token.particle-effect");
    }

    public int getTeleportWaitTime() {
        return config.getInt("teleport.wait-time");
    }

    public String getTeleportParticle() {
        return config.getString("teleport.particle-effect");
    }

    public List<String> getHomeWorlds() {
        return config.getStringList("world.home");
    }

    public List<String> getCampWorlds() {
        return config.getStringList("world.camp");
    }

    public List<String> getWaypointWorlds() {
        return config.getStringList("world.waypoint");
    }

    public HologramMap getHologramMap() {
        return hologramMap;
    }

    public TravelerMap getTravelerMap() {
        return travelerMap;
    }

    public WaypointMap getWaypointMap() {
        return waypointMap;
    }

    public CampBannerMap getCampBannerMap() {
        return campBannerMap;
    }

    public boolean isToken(Object obj) {
        return token.isToken(obj);
    }

    public Token getToken() {
        return token;
    }

    public TeleportCharge getTeleportCharge() {
        return teleportCharge;
    }

    private void loadCampBannerData() throws IOException {
        campBannerMap.loadCampBanners(this);
        getLogger().info("Camp banner data loaded");
    }

    private void saveCampBannerData() throws IOException {
        campBannerMap.saveCampBanners(this);
        getLogger().info("Camp banner data saved");
    }
}
