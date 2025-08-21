package xyz.holocons.mc.waypoints;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;

public class CampBannerMap {

    private final Map<Location, UUID> campBanners = new HashMap<>();
    private final Map<UUID, BannerDesign> playerBannerDesigns = new HashMap<>();

    public CampBannerMap(WaypointsPlugin plugin) {
        // Constructor for future extensibility
    }

    /**
     * Checks if a location has a camp banner
     */
    public boolean isCampBanner(Location location) {
        return campBanners.containsKey(location);
    }

    /**
     * Gets the owner of a camp banner at the given location
     */
    public UUID getCampBannerOwner(Location location) {
        return campBanners.get(location);
    }

    /**
     * Registers a camp banner at the given location
     */
    public void addCampBanner(Location location, UUID owner) {
        campBanners.put(location, owner);
    }

    /**
     * Removes a camp banner from tracking
     */
    public void removeCampBanner(Location location) {
        campBanners.remove(location);
    }

    /**
     * Registers a custom banner design for a player
     */
    public void registerBannerDesign(UUID playerId, BannerDesign design) {
        playerBannerDesigns.put(playerId, design);
    }

    /**
     * Gets a player's registered banner design, or default if none
     */
    public BannerDesign getPlayerBannerDesign(UUID playerId) {
        return playerBannerDesigns.getOrDefault(playerId, getDefaultBannerDesign());
    }

    /**
     * Gets the default banner design (white banner)
     */
    public BannerDesign getDefaultBannerDesign() {
        return new BannerDesign(Material.WHITE_BANNER, new Pattern[0]);
    }

    /**
     * Creates a banner design from a player's held banner
     */
    public BannerDesign createBannerDesignFromHeldItem(Player player) {
        var item = player.getInventory().getItemInMainHand();

        if (!item.getType().name().endsWith("_BANNER")) {
            return null;
        }

        var meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.BannerMeta bannerMeta) {
            return new BannerDesign(item.getType(), bannerMeta.getPatterns().toArray(new Pattern[0]));
        }

        return new BannerDesign(item.getType(), new Pattern[0]);
    }

    /**
     * Clears all camp banner data (for reloading)
     */
    public void clear() {
        campBanners.clear();
        playerBannerDesigns.clear();
    }

    /**
     * Gets all camp banner locations
     */
    public Map<Location, UUID> getAllCampBanners() {
        return new HashMap<>(campBanners);
    }

    /**
     * Gets all player banner designs
     */
    public Map<UUID, BannerDesign> getAllBannerDesigns() {
        return new HashMap<>(playerBannerDesigns);
    }

    /**
     * Represents a banner design with material and patterns
     */
    public static class BannerDesign {
        private final Material material;
        private final Pattern[] patterns;

        public BannerDesign(Material material, Pattern[] patterns) {
            this.material = material;
            this.patterns = patterns.clone();
        }

        public Material getMaterial() {
            return material;
        }

        public Pattern[] getPatterns() {
            return patterns.clone();
        }

        /**
         * Applies this design to a banner block
         */
        public void applyToBanner(Banner banner) {
            for (Pattern pattern : patterns) {
                banner.addPattern(pattern);
            }
            banner.update();
        }
    }
}
