package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class EventListener implements Listener {

    private final WaypointsPlugin plugin;
    private final HologramMap hologramMap;
    private final TravelerMap travelerMap;
    private final WaypointMap waypointMap;

    public EventListener(final WaypointsPlugin plugin) {
        this.plugin = plugin;
        this.hologramMap = plugin.getHologramMap();
        this.travelerMap = plugin.getTravelerMap();
        this.waypointMap = plugin.getWaypointMap();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Tag.ITEMS_BANNERS.isTagged(event.getItemInHand().getType())) {
            return;
        }

        final var blockPlaced = event.getBlockPlaced();
        final var player = event.getPlayer();
        final var task = travelerMap.getTask(player, TravelerTask.class);

        if (task == null) {
            if (waypointMap.isWaypoint(blockPlaced)) {
                final var waypoint = waypointMap.getNearbyWaypoint(blockPlaced);
                hologramMap.updateTrackedPlayers(waypoint, player);
            }
            return;
        }

        if (!isValidWaypointPlacement(blockPlaced, event.getBlockAgainst())) {
            return;
        }

        switch (task.getType()) {
            case CREATE -> {
                final var waypoint = waypointMap.createWaypoint(blockPlaced);

                if (waypoint == null) {
                    player.sendMessage(Component.text("There is already a waypoint nearby!", NamedTextColor.RED));
                    return;
                }

                hologramMap.showTrackedPlayers(waypoint, player);
            }
            default -> {
                return;
            }
        }
        travelerMap.unregisterTask(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final var block = event.getBlock();
        final var player = event.getPlayer();
        final var location = block.getLocation();

        // Check if this block is a camp banner (higher priority protection)
        if (plugin.getCampBannerMap().isCampBanner(location)) {
            event.setCancelled(true);
            final var owner = plugin.getCampBannerMap().getCampBannerOwner(location);
            final var ownerName = Bukkit.getOfflinePlayer(owner).getName();
            player.sendMessage(
                    Component.text("This camp banner belongs to " + ownerName + " and cannot be broken!",
                            NamedTextColor.RED));
            return;
        }

        // Check if this block is a waypoint banner
        if (!waypointMap.isWaypoint(block)) {
            return;
        }

        final var waypoint = waypointMap.getNearbyWaypoint(block);

        // Only allow staff to break waypoint banners
        if (!player.hasPermission("waypoints.staff")) {
            event.setCancelled(true);
            player.sendMessage(
                    Component.text("You cannot break waypoint banners! Use /editwaypoints delete to remove waypoints.",
                            NamedTextColor.RED));
            return;
        }

        // If staff breaks a waypoint banner, remove the waypoint data
        waypointMap.removeWaypoint(waypoint);
        travelerMap.removeWaypoint(waypoint);

        // Refund tokens to contributors
        final var maxTokens = plugin.getMaxTokens();
        for (final var uniqueId : waypoint.getContributors()) {
            final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
            traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
        }

        hologramMap.remove(waypoint);
        player.sendMessage(
                Component.text("Waypoint removed and tokens refunded to contributors.", NamedTextColor.GREEN));
    }

    private boolean isValidWaypointPlacement(Block blockPlaced, Block blockAgainst) {
        return blockAgainst.getFace(blockPlaced) == BlockFace.UP
                && plugin.getWaypointWorlds().contains(blockPlaced.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!event.getAction().equals(InventoryAction.PICKUP_ALL)
                || !(event.getClickedInventory().getHolder() instanceof Menu)) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                () -> menu.handleClick(event.getCurrentItem(), event.getSlot()));
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        final var chunk = event.getChunk();
        final var player = event.getPlayer();

        // Handle regular waypoint holograms
        final var waypoint = waypointMap.getWaypoint(chunk.getChunkKey());
        if (waypoint != null && waypoint.getLocation().getWorld() == event.getWorld()) {
            hologramMap.show(waypoint, player);
        }

        // Handle camp banner holograms - check all tracked camp banners in this chunk
        final var campBannerMap = plugin.getCampBannerMap();
        for (var entry : campBannerMap.getAllCampBanners().entrySet()) {
            final var location = entry.getKey();
            final var ownerId = entry.getValue();

            // Check if this camp banner is in the loaded chunk
            if (location.getChunk().equals(chunk)) {
                final var ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
                final var tempWaypoint = new CampWaypoint(location, ownerName + "'s Camp");
                hologramMap.show(tempWaypoint, player);
            }
        }
    }

    @EventHandler
    public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
        final var waypoint = waypointMap.getWaypoint(event.getChunk().getChunkKey());

        if (waypoint == null || waypoint.getLocation().getWorld() != event.getWorld()) {
            return;
        }

        hologramMap.hide(waypoint, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final var player = event.getPlayer();
        final var task = travelerMap.getTask(player, TravelerTask.class);
        
        // Handle REPLACEBANNER mode with highest priority
        if (task != null && task.getType() == TravelerTask.Type.REPLACEBANNER && 
            event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            
            final var clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && waypointMap.isWaypoint(clickedBlock)) {
                // Cancel the event immediately to prevent any block placement
                event.setCancelled(true);
                handleBannerReplacement(player, clickedBlock, event);
                travelerMap.unregisterTask(player);
                return;
            }
        }

        if (event.isBlockInHand() || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final var clickedBlock = event.getClickedBlock();
        final var isHoldingToken = plugin.isToken(player.getInventory().getItemInMainHand());

        if (!waypointMap.isWaypoint(clickedBlock)) {
            final var destinationBlock = clickedBlock.isPassable() ? clickedBlock
                    : clickedBlock.getRelative(BlockFace.UP);
            if (task == null || !destinationBlock.isPassable()) {
                return;
            }
            switch (task.getType()) {
                case SETCAMP -> {
                    if (plugin.getCampWorlds().contains(destinationBlock.getWorld().getName())) {
                        travelerMap.getOrCreateTraveler(player).setCamp(destinationBlock.getLocation());
                        player.sendMessage(Component.text("You assigned your camp!", NamedTextColor.GREEN));
                    }
                }
                case SETHOME -> {
                    if (plugin.getHomeWorlds().contains(destinationBlock.getWorld().getName())) {
                        travelerMap.getOrCreateTraveler(player).setHome(destinationBlock.getLocation());
                        player.sendMessage(Component.text("You assigned your home!", NamedTextColor.GREEN));
                    }
                }
                default -> {
                    return;
                }
            }
            travelerMap.unregisterTask(player);
            return;
        }

        final var waypoint = waypointMap.getNearbyWaypoint(clickedBlock);

        if (task == null) {
            // Handle direct token consumption when right-clicking waypoints
            if (isHoldingToken && !waypoint.isActive()) {
                event.setCancelled(true);

                final var tokenRequirement = plugin.getWaypointActivateCost();
                final var contributors = waypoint.getContributors();
                final var playerInventory = player.getInventory();

                // Consume the physical token
                player.playEffect(EntityEffect.BREAK_EQUIPMENT_MAIN_HAND);
                playerInventory.setItemInMainHand(playerInventory.getItemInMainHand().subtract());

                // Add contribution to waypoint
                contributors.add(player.getUniqueId());
                player.sendMessage(Component.text("You added a token!", NamedTextColor.BLUE));

                // Check if waypoint should activate
                if (contributors.size() >= tokenRequirement) {
                    waypoint.activate();
                    hologramMap.updateTrackedPlayers(waypoint, player);
                }

                sendActionBar(player, contributors.size(), tokenRequirement);
                return;
            }

            if (waypoint.isActive()) {
                final var traveler = travelerMap.getOrCreateTraveler(player);
                if (!traveler.hasWaypoint(waypoint)) {
                    traveler.registerWaypoint(waypoint);
                    player.sendMessage(Component.text("You registered a waypoint!", NamedTextColor.GOLD));
                }
            } else {
                final var tokenRequirement = plugin.getWaypointActivateCost();
                final var contributors = waypoint.getContributorNames();
                final var contributorNamesBuilder = Component.text()
                        .color(NamedTextColor.GOLD);
                if (contributors.isEmpty()) {
                    contributorNamesBuilder.append(Component.text("Nobody has contributed to this waypoint!"));
                } else {
                    contributorNamesBuilder.append(Component.text("Contributors: " + String.join(", ", contributors)));
                }
                player.sendMessage(contributorNamesBuilder.build());
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            return;
        }

        switch (task.getType()) {
            case ACTIVATE -> {
                waypoint.activate();
                hologramMap.updateTrackedPlayers(waypoint, player);
            }
            case CREATE -> {
                if (waypoint.isActive()) {
                    return;
                }
                waypointMap.removeWaypoint(waypoint);
                final var maxTokens = plugin.getMaxTokens();
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                }
                hologramMap.remove(waypoint);
            }
            case DELETE -> {
                waypointMap.removeWaypoint(waypoint);
                travelerMap.removeWaypoint(waypoint);
                final var maxTokens = plugin.getMaxTokens();
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                }
                hologramMap.remove(waypoint);
            }
            case REMOVETOKEN -> {
                if (waypoint.isActive()) {
                    return;
                }
                final var uniqueId = player.getUniqueId();
                final var contributors = waypoint.getContributors();
                if (contributors.contains(uniqueId)) {
                    player.sendMessage(Component.text("You removed a token!", NamedTextColor.BLUE));
                    final var maxTokens = plugin.getMaxTokens();
                    final var traveler = travelerMap.getOrCreateTraveler(player);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                    contributors.remove(uniqueId);
                }
                final var tokenRequirement = plugin.getWaypointActivateCost();
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            default -> {
                return;
            }
        }
        travelerMap.unregisterTask(player);
    }
    
    private void handleBannerReplacement(Player player, org.bukkit.block.Block clickedBlock, PlayerInteractEvent event) {
        // Check if player is holding a banner
        final var heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.getType().name().endsWith("_BANNER")) {
            player.sendMessage(Component.text("You must be holding a banner to replace the waypoint banner!",
                    NamedTextColor.RED));
            return;
        }

        final var block = clickedBlock;
        if (!block.getType().name().endsWith("_BANNER")) {
            player.sendMessage(Component.text("That block is not a banner.", NamedTextColor.RED));
            return;
        }

        // Store the original banner properties before replacement
        final var originalState = block.getState();
        final var originalMaterial = block.getType();
        final var originalPatterns = originalState instanceof org.bukkit.block.Banner originalBanner
                ? originalBanner.getPatterns()
                : java.util.List.<org.bukkit.block.banner.Pattern>of();
        final var originalCustomName = originalState instanceof org.bukkit.block.Banner originalBanner
                ? originalBanner.customName()
                : null;

        // Replace with the held banner
        block.setType(heldItem.getType());
        final var newState = block.getState();

        // Apply patterns from held banner if it has any
        if (newState instanceof org.bukkit.block.Banner newBanner) {
            // Clear existing patterns first
            newBanner.getPatterns().clear();
            
            if (heldItem.hasItemMeta() && heldItem.getItemMeta() instanceof org.bukkit.inventory.meta.BannerMeta bannerMeta) {
                for (final var pattern : bannerMeta.getPatterns()) {
                    newBanner.addPattern(pattern);
                }
            }
            
            // Preserve the original waypoint name
            if (originalCustomName != null) {
                newBanner.customName(originalCustomName);
            }
            
            // Update the banner state
            newBanner.update();
        }

        // Set facing direction toward player for wall banners
        if (newState instanceof org.bukkit.block.data.Directional directional) {
            final var playerFacing = player.getFacing();
            // For banners to face the player, we need the opposite direction
            final var bannerFacing = playerFacing.getOppositeFace();
            directional.setFacing(bannerFacing);
            newState.setBlockData(directional);
            newState.update();
        }

        // Give player the original banner
        final var originalBanner = new org.bukkit.inventory.ItemStack(originalMaterial);
        if (!originalPatterns.isEmpty()
                && originalBanner.getItemMeta() instanceof org.bukkit.inventory.meta.BannerMeta originalMeta) {
            for (final var pattern : originalPatterns) {
                originalMeta.addPattern(pattern);
            }
            originalBanner.setItemMeta(originalMeta);
        }

        // Remove held banner and give original
        heldItem.setAmount(heldItem.getAmount() - 1);
        final var leftover = player.getInventory().addItem(originalBanner);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), originalBanner);
        }

        player.sendMessage(Component.text("Waypoint banner swapped!", NamedTextColor.GREEN));
    }

    private static void sendActionBar(Player player, int contributorsSize, int tokenRequirement) {
        player.sendActionBar(Component.text(String.format("%d / %d", contributorsSize, tokenRequirement)));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        travelerMap.getOrCreateTraveler(player).startRegenCharge(plugin);

        // Show holograms for all camp banners in loaded chunks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            final var campBannerMap = plugin.getCampBannerMap();
            for (var entry : campBannerMap.getAllCampBanners().entrySet()) {
                final var location = entry.getKey();
                final var ownerId = entry.getValue();

                // Check if the chunk is loaded
                if (location.getChunk().isLoaded()) {
                    final var ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
                    final var tempWaypoint = new CampWaypoint(location, ownerName + "'s Camp");
                    hologramMap.show(tempWaypoint, player);
                }
            }
        }, 1L); // Small delay to ensure player is fully loaded
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        travelerMap.getOrCreateTraveler(player).stopRegenCharge();
        travelerMap.unregisterTask(player);
        hologramMap.remove(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Tokens are now paper-based and not throwable, but handle any edge cases
        if (!(event.getEntity().getShooter() instanceof Player)
                || !plugin.isToken(event.getEntity())) {
            return;
        }

        // Cancel any accidental token projectile launches (shouldn't happen with paper)
        event.setCancelled(true);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        plugin.loadData();
    }
}
