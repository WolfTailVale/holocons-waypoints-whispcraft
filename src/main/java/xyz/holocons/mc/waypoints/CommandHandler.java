package xyz.holocons.mc.waypoints;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CommandHandler implements TabExecutor {

    private final WaypointsPlugin plugin;
    private final TravelerMap travelerMap;
    private final WaypointMap waypointMap;
    private final CampBannerMap campBannerMap;

    public CommandHandler(final WaypointsPlugin plugin) {
        this.plugin = plugin;
        this.travelerMap = plugin.getTravelerMap();
        this.waypointMap = plugin.getWaypointMap();
        this.campBannerMap = plugin.getCampBannerMap();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            switch (command.getName().toUpperCase()) {
                case "WAYPOINTS" -> {
                    if (args.length == 0) {
                        new Menu(plugin, player, Menu.Type.TELEPORT);
                        return true;
                    }
                    final var subcommand = args[0].toUpperCase();
                    switch (subcommand) {
                        case "CREATE", "REMOVETOKEN", "SETCAMP", "SETHOME" -> {
                            new TravelerTask(plugin, player, TravelerTask.Type.valueOf(subcommand));
                        }
                        case "CANCEL" -> {
                            travelerMap.unregisterTask(player);
                        }
                        case "TELEPORT" -> {
                            teleport(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                        }
                        case "TOKENDEBUG" -> {
                            debugToken(player);
                        }
                        case "TOKENREPAIR" -> {
                            repairHeldToken(player);
                        }
                        default -> {
                            return false;
                        }
                    }
                }
                case "EDITWAYPOINTS" -> {
                    if (args.length == 0) {
                        new Menu(plugin, player, Menu.Type.EDIT);
                        return true;
                    }
                    final var subcommand = args[0].toUpperCase();
                    switch (subcommand) {
                        case "ACTIVATE", "DELETE" -> {
                            new TravelerTask(plugin, player, TravelerTask.Type.valueOf(subcommand));
                        }
                        case "REPLACEBANNER" -> {
                            new TravelerTask(plugin, player, TravelerTask.Type.REPLACEBANNER);
                        }
                        case "MENU" -> {
                            new Menu(plugin, player, Menu.Type.EDIT);
                        }
                        case "UNSETCAMPS" -> {
                            travelerMap.removeCamps();
                        }
                        case "UNSETHOMES" -> {
                            travelerMap.removeHomes();
                        }
                        default -> {
                            return false;
                        }
                    }
                }
                case "SETCAMP" -> {
                    setCamp(player);
                }
                case "CAMP" -> {
                    teleport(player, "camp");
                }
                case "UNSETCAMP" -> {
                    unsetCamp(player);
                }
                case "RELOADWAYPOINTS" -> {
                    if (!player.hasPermission("waypoints.staff")) {
                        player.sendMessage(
                                Component.text("You don't have permission to reload waypoints!", NamedTextColor.RED));
                        return true;
                    }
                    plugin.loadConfig();
                    player.sendMessage(Component.text("Waypoints configuration reloaded!", NamedTextColor.GREEN));
                }
                case "REGISTERBANNER" -> {
                    registerBanner(player);
                }
                case "GIVEWPTOKEN" -> {
                    if (!player.hasPermission("waypoints.staff")) {
                        player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                        return true;
                    }
                    int amount = 1;
                    if (args.length > 0) {
                        try {
                            amount = Integer.parseInt(args[0]);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    amount = Math.max(1, Math.min(64, amount));
                    final var tokenItem = plugin.getToken().createToken(amount);
                    final var leftover = player.getInventory().addItem(tokenItem);
                    if (!leftover.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), tokenItem);
                    }
                    player.sendMessage(Component.text("Given " + amount + " waypoint token" + (amount == 1 ? "" : "s"),
                            NamedTextColor.GREEN));
                }
            }
        } else {
            if (args.length == 0) {
                return false;
            }
            final var subcommand = args[0].toUpperCase();
            switch (subcommand) {
                case "BACKUP" -> {
                    plugin.backupData();
                }
                case "LOAD" -> {
                    plugin.loadData();
                }
                case "RELOAD" -> {
                    plugin.loadConfig();
                }
                case "SAVE" -> {
                    plugin.saveData();
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            return switch (command.getName().toUpperCase()) {
                case "WAYPOINTS" -> {
                    yield switch (args.length) {
                        case 1 -> {
                            yield List.of("create", "removetoken", "setcamp", "sethome", "teleport", "tokendebug",
                                    "tokenrepair");
                        }
                        case 2 -> {
                            if (args[0].equalsIgnoreCase("teleport")) {
                                final var traveler = travelerMap.getOrCreateTraveler(player);
                                final var waypoints = waypointMap.getNamedWaypoints().filter(traveler::hasWaypoint);
                                var names = waypoints.map(Waypoint::getName);
                                if (traveler.getCamp() != null) {
                                    names = Stream.concat(names, Stream.of("camp"));
                                }
                                if (traveler.getHome() != null) {
                                    names = Stream.concat(names, Stream.of("home"));
                                }
                                yield names.toList();
                            } else {
                                yield List.of();
                            }
                        }
                        default -> List.of();
                    };
                }
                case "EDITWAYPOINTS" -> {
                    yield switch (args.length) {
                        case 1 -> {
                            yield List.of("activate", "delete", "menu", "unsetcamps", "unsethomes", "replacebanner");
                        }
                        default -> List.of();
                    };
                }
                case "RELOADWAYPOINTS" -> {
                    yield List.of();
                }
                case "REGISTERBANNER" -> {
                    yield List.of();
                }
                case "GIVEWPTOKEN" -> {
                    yield List.of("1", "2", "4", "8", "16", "32", "64");
                }
                default -> List.of();
            };
        } else {
            return List.of();
        }
    }

    private void teleport(Player player, String destination) {
        final var traveler = travelerMap.getOrCreateTraveler(player);
        final TeleportTask.Type type;
        final Location location;
        if (destination.equalsIgnoreCase("camp")) {
            type = TeleportTask.Type.CAMP;
            location = traveler.getCamp();
            if (location == null) {
                player.sendMessage(Component.text("You don't have a camp!"));
            }
        } else if (destination.equalsIgnoreCase("home")) {
            type = TeleportTask.Type.HOME;
            location = traveler.getHome();
            if (location == null) {
                player.sendMessage(Component.text("You don't have a home!"));
            }
        } else {
            type = TeleportTask.Type.WAYPOINT;
            Predicate<Waypoint> matchesDestination = waypoint -> waypoint.getName().equalsIgnoreCase(destination);
            final var waypoint = waypointMap.getNamedWaypoints().filter(matchesDestination).findAny().orElse(null);
            location = traveler.hasWaypoint(waypoint) ? waypoint.getLocation() : null;
        }
        if (location == null) {
            new Menu(plugin, player, Menu.Type.TELEPORT);
        } else {
            new TeleportTask(plugin, player, type, location);
        }
    }

    private void debugToken(Player player) {
        final var item = player.getInventory().getItemInMainHand();
        final boolean isToken = plugin.isToken(item);
        Integer cmd = null;
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            cmd = item.getItemMeta().getCustomModelData();
        }
        player.sendMessage(Component.text("Token Debug -> isToken=" + isToken + ", customModelData=" + cmd +
                (cmd == null ? " (no CMD)" : cmd == Token.MODEL_DATA ? " (expected)" : " (unexpected)"),
                NamedTextColor.AQUA));
    }

    private void repairHeldToken(Player player) {
        final var item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.PAPER) {
            player.sendMessage(Component.text("Hold a potential token (paper) first.", NamedTextColor.RED));
            return;
        }
        final boolean repaired = plugin.getToken().repairToken(item);
        player.sendMessage(Component.text(repaired ? "Token repaired." : "No changes needed.",
                repaired ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
    }

    private void setCamp(Player player) {
        // Check if the world allows camps
        String currentWorld = player.getWorld().getName();
        List<String> allowedWorlds = plugin.getCampWorlds();

        if (!allowedWorlds.contains(currentWorld)) {
            player.sendMessage(Component.text("You cannot set a camp in this world!", NamedTextColor.RED));
            player.sendMessage(Component.text("Current world: " + currentWorld, NamedTextColor.YELLOW));
            player.sendMessage(
                    Component.text("Allowed worlds: " + String.join(", ", allowedWorlds), NamedTextColor.YELLOW));
            return;
        }

        final var traveler = travelerMap.getOrCreateTraveler(player);
        final var location = player.getLocation();

        // Remove existing camp banner if it exists
        final var oldCamp = traveler.getCamp();
        if (oldCamp != null) {
            removeCampBanner(oldCamp);
        }

        // Find a suitable location for the banner (on ground or floating)
        final var bannerLocation = findSuitableBannerLocation(location);
        if (bannerLocation == null) {
            player.sendMessage(Component.text("Cannot place camp banner here!", NamedTextColor.RED));
            return;
        }

        // Create and place the banner
        if (placeCampBanner(bannerLocation, player)) {
            traveler.setCamp(bannerLocation);

            // Create a temporary waypoint for hologram display
            createCampHologram(bannerLocation, player);

            player.sendMessage(
                    Component.text("Camp set! A banner has been placed to mark your location.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Could not place camp banner!", NamedTextColor.RED));
        }
    }

    private void unsetCamp(Player player) {
        final var traveler = travelerMap.getOrCreateTraveler(player);
        final var camp = traveler.getCamp();

        if (camp == null) {
            player.sendMessage(Component.text("You don't have a camp to remove!", NamedTextColor.YELLOW));
            return;
        }

        // Remove the camp banner
        removeCampBanner(camp);

        // Remove camp hologram
        removeCampHologram(camp, player);

        // Clear the camp from traveler data
        traveler.setCamp(null);
        player.sendMessage(Component.text("Your camp has been removed!", NamedTextColor.GREEN));
    }

    private Location findSuitableBannerLocation(Location playerLoc) {
        final var world = playerLoc.getWorld();
        final var blockX = playerLoc.getBlockX();
        final var blockY = playerLoc.getBlockY();
        final var blockZ = playerLoc.getBlockZ();

        // Try to place on the ground first
        for (int y = blockY; y >= world.getMinHeight(); y--) {
            final var checkLoc = new Location(world, blockX, y, blockZ);
            final var block = checkLoc.getBlock();
            final var above = block.getRelative(BlockFace.UP);

            if (!block.getType().isAir() && above.getType().isAir()) {
                return above.getLocation();
            }
        }

        // If no ground found, place at player location
        final var airLoc = new Location(world, blockX, blockY, blockZ);
        if (airLoc.getBlock().getType().isAir()) {
            return airLoc;
        }

        return null;
    }

    private boolean placeCampBanner(Location location, Player player) {
        final var block = location.getBlock();
        if (!block.getType().isAir()) {
            return false;
        }

        // Get the player's custom banner design or use default
        final var bannerDesign = campBannerMap.getPlayerBannerDesign(player.getUniqueId());

        // Set the block to the player's banner type
        block.setType(bannerDesign.getMaterial());

        // Get the banner's block state and apply custom design
        final var banner = (org.bukkit.block.Banner) block.getState();
        banner.customName(Component.text(player.getName() + "'s Camp", NamedTextColor.WHITE));

        // Apply the custom patterns
        bannerDesign.applyToBanner(banner);

        // Track this as a camp banner
        campBannerMap.addCampBanner(location, player.getUniqueId());

        return true;
    }

    private void removeCampBanner(Location location) {
        // Check if this is a tracked camp banner
        if (campBannerMap.isCampBanner(location)) {
            final var block = location.getBlock();
            // Remove any banner type (not just white banners)
            if (block.getType().name().endsWith("_BANNER")) {
                block.setType(Material.AIR);
                campBannerMap.removeCampBanner(location);
            }
        }
    }

    private void createCampHologram(Location location, Player player) {
        // Create a temporary waypoint object for the hologram system
        final var tempWaypoint = new CampWaypoint(location, player.getName() + "'s Camp");
        final var hologramMap = plugin.getHologramMap();
        hologramMap.show(tempWaypoint, player);
    }

    private void removeCampHologram(Location location, Player player) {
        // Create a temporary waypoint object for the hologram system
        final var tempWaypoint = new CampWaypoint(location, player.getName() + "'s Camp");
        final var hologramMap = plugin.getHologramMap();
        hologramMap.hide(tempWaypoint, player);
    }

    private void registerBanner(Player player) {
        final var bannerDesign = campBannerMap.createBannerDesignFromHeldItem(player);

        if (bannerDesign == null) {
            player.sendMessage(Component.text("You must be holding a banner to register it!", NamedTextColor.RED));
            return;
        }

        campBannerMap.registerBannerDesign(player.getUniqueId(), bannerDesign);
        player.sendMessage(Component.text("Banner design registered! This will now be used for your camps.",
                NamedTextColor.GREEN));

        // Show them what material type they registered
        final var materialName = bannerDesign.getMaterial().name().toLowerCase().replace("_", " ");
        player.sendMessage(Component.text(
                "Registered: " + materialName + " with " + bannerDesign.getPatterns().length + " patterns",
                NamedTextColor.YELLOW));
    }
}
