package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class TravelerTask extends BukkitRunnable {

    public enum Type {
        ACTIVATE,
        CREATE,
        DELETE,
        REMOVETOKEN,
        REPLACEBANNER,
        REPOSITION,
        SETCAMP,
        SETHOME,
    }

    private final Player player;
    private final Type type;
    private final int expiration;
    private Waypoint repositionWaypoint; // Holds waypoint data during reposition
    private org.bukkit.Material repositionBannerMaterial; // Holds banner material
    private java.util.List<org.bukkit.block.banner.Pattern> repositionBannerPatterns; // Holds banner patterns
    private net.kyori.adventure.text.Component repositionBannerName; // Holds banner name

    public TravelerTask(final WaypointsPlugin plugin, final Player player, final Type type) {
        plugin.getTravelerMap().registerTask(player, this);
        final var period = 40;
        runTaskTimer(plugin, 0, period);
        this.player = player;
        this.type = type;
        this.expiration = Bukkit.getCurrentTick() + 600;
        final var messageComponent = Component.text()
                .clickEvent(ClickEvent.runCommand("/waypoints cancel"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to cancel early!")))
                .append(Component.text("You entered WAYPOINT " + type.toString() + " mode for 30 seconds!"))
                .build();
        player.sendMessage(messageComponent);
    }

    @Override
    public void run() {
        player.sendActionBar(Component.text("WAYPOINT " + type.toString(), NamedTextColor.GREEN));
        if (Bukkit.getCurrentTick() >= expiration) {
            cancel();
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        // Handle cleanup for REPOSITION mode if waypoint is picked up
        if (type == Type.REPOSITION && repositionWaypoint != null) {
            // Restore the waypoint to its original location
            final var originalLocation = repositionWaypoint.getLocation();
            final var originalBlock = originalLocation.getBlock();

            // Only restore if the block is still air (hasn't been replaced by something
            // else)
            if (originalBlock.getType().isAir()) {
                // Use the stored banner material or fallback to white banner
                final var bannerMaterial = repositionBannerMaterial != null ? repositionBannerMaterial
                        : org.bukkit.Material.WHITE_BANNER;
                originalBlock.setType(bannerMaterial);

                final var bannerState = originalBlock.getState();
                if (bannerState instanceof org.bukkit.block.Banner banner) {
                    // Apply stored patterns
                    if (repositionBannerPatterns != null) {
                        for (final var pattern : repositionBannerPatterns) {
                            banner.addPattern(pattern);
                        }
                    }

                    // Use stored name or fallback to waypoint name
                    if (repositionBannerName != null) {
                        banner.customName(repositionBannerName);
                    } else if (repositionWaypoint.hasName()) {
                        banner.customName(net.kyori.adventure.text.Component.text(repositionWaypoint.getName()));
                    }
                    banner.update();
                }
            }

            // Add waypoint back to the map at its original location
            final var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("WaypointsPlugin");
            if (plugin instanceof WaypointsPlugin waypointsPlugin) {
                waypointsPlugin.getWaypointMap().addWaypoint(repositionWaypoint);

                // Restore hologram if waypoint was active
                if (repositionWaypoint.isActive()) {
                    waypointsPlugin.getHologramMap().show(repositionWaypoint, player);
                }
            }

            player.sendMessage(net.kyori.adventure.text.Component.text(
                    "Reposition mode expired. Waypoint restored to original location.",
                    net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        }

        super.cancel();
    }

    public Type getType() {
        return type;
    }

    public Waypoint getRepositionWaypoint() {
        return repositionWaypoint;
    }

    public void setRepositionWaypoint(Waypoint waypoint) {
        this.repositionWaypoint = waypoint;
    }

    public org.bukkit.Material getRepositionBannerMaterial() {
        return repositionBannerMaterial;
    }

    public void setRepositionBannerMaterial(org.bukkit.Material material) {
        this.repositionBannerMaterial = material;
    }

    public java.util.List<org.bukkit.block.banner.Pattern> getRepositionBannerPatterns() {
        return repositionBannerPatterns;
    }

    public void setRepositionBannerPatterns(java.util.List<org.bukkit.block.banner.Pattern> patterns) {
        this.repositionBannerPatterns = patterns;
    }

    public net.kyori.adventure.text.Component getRepositionBannerName() {
        return repositionBannerName;
    }

    public void setRepositionBannerName(net.kyori.adventure.text.Component name) {
        this.repositionBannerName = name;
    }
}
