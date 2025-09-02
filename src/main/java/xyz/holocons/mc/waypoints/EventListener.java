package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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
    // Tracks last player damage timestamp for mobs (entity UUID -> epoch millis)
    private final java.util.Map<java.util.UUID, Long> recentDamages = new java.util.concurrent.ConcurrentHashMap<>();
    // Tracks charge drops per chunk with timestamps for sliding window anti-farm
    private final java.util.Map<Long, java.util.List<Long>> chunkDropTimestamps = new java.util.concurrent.ConcurrentHashMap<>();

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
        for (final var uniqueId : waypoint.getContributors()) {
            final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
            traveler.setTokens(traveler.getTokens() + 1);
            
            // Send feedback to online contributors
            final var contributorPlayer = plugin.getServer().getPlayer(uniqueId);
            if (contributorPlayer != null) {
                contributorPlayer.sendMessage(
                    Component.text("A waypoint token was returned to your wallet.", NamedTextColor.BLUE));
            }
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

        // Handle consuming teleport charge item (right-click air or block while holding it)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final var item = player.getInventory().getItemInMainHand();
            
            // Handle teleport charge consumption
            if (plugin.getTeleportCharge().isTeleportCharge(item)) {
                event.setCancelled(true);
                final var amount = item.getAmount();
                item.setAmount(0); // remove stack
                final var traveler = travelerMap.getOrCreateTraveler(player);
                final var before = traveler.getCharges();
                traveler.addCharges(amount);
                final var gained = traveler.getCharges() - before;
                player.sendMessage(
                        Component.text("Consumed " + gained + " teleport charge" + (gained == 1 ? "" : "s") + ".",
                                NamedTextColor.GREEN));
                final var cfg = plugin.getConfig();
                final var soundName = cfg.getString("charge.item.consume-sound",
                        cfg.getString("teleport-charge-item.consume-sound", "ENTITY_ITEM_BREAK"));
                try {
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1f, 1f);
                } catch (IllegalArgumentException ignored) {
                }
                return; // do not process further interactions when consuming
            }
            
            // Handle token consumption
            if (plugin.isToken(item)) {
                event.setCancelled(true);
                final var amount = item.getAmount();
                item.setAmount(0); // remove stack
                final var traveler = travelerMap.getOrCreateTraveler(player);
                traveler.setTokens(traveler.getTokens() + amount);
                player.sendMessage(
                        Component.text("Consumed " + amount + " waypoint token" + (amount == 1 ? "" : "s") + ".",
                                NamedTextColor.BLUE));
                try {
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                } catch (IllegalArgumentException ignored) {
                }
                return; // do not process further interactions when consuming
            }
        }

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

        // Handle REPOSITION mode
        if (task != null && task.getType() == TravelerTask.Type.REPOSITION && event.getHand() == EquipmentSlot.HAND) {
            final var clickedBlock = event.getClickedBlock();

            // Check for waypoint pickup (shift+right-click empty hand on waypoint banner)
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking() &&
                    player.getInventory().getItemInMainHand().getType() == Material.AIR &&
                    clickedBlock != null && waypointMap.isWaypoint(clickedBlock)) {

                event.setCancelled(true);
                handleWaypointPickup(player, clickedBlock, task);
                return;
            }

            // Check for waypoint placement (right-click solid block with picked up
            // waypoint)
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking() &&
                    task.getRepositionWaypoint() != null && clickedBlock != null && clickedBlock.isSolid()) {

                event.setCancelled(true);
                handleWaypointPlacement(player, clickedBlock, task);
                travelerMap.unregisterTask(player);
                return;
            }
        }

        if (event.isBlockInHand() || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final var clickedBlock = event.getClickedBlock();

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
            // Handle wallet token usage when right-clicking inactive waypoints
            if (!waypoint.isActive()) {
                event.setCancelled(true);
                
                final var traveler = travelerMap.getOrCreateTraveler(player);
                final var tokenRequirement = plugin.getWaypointActivateCost();
                final var contributors = waypoint.getContributors();
                
                // Check if player has tokens in wallet
                if (traveler.getTokens() <= 0) {
                    player.sendMessage(Component.text("You need tokens in your wallet to contribute! Use '/waypoints wallet' to check your balance.", NamedTextColor.RED));
                    return;
                }
                
                // Deduct token from wallet
                traveler.setTokens(traveler.getTokens() - 1);
                
                // Add contribution to waypoint
                contributors.add(player.getUniqueId());
                player.sendMessage(Component.text("You added a token! (" + traveler.getTokens() + " tokens remaining in wallet)", NamedTextColor.BLUE));
                
                // Check if waypoint should activate
                if (contributors.size() >= tokenRequirement) {
                    waypoint.activate();
                    hologramMap.updateTrackedPlayers(waypoint, player);
                    player.sendMessage(Component.text("Waypoint activated!", NamedTextColor.GREEN));
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
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(traveler.getTokens() + 1);
                }
                hologramMap.remove(waypoint);
            }
            case DELETE -> {
                waypointMap.removeWaypoint(waypoint);
                travelerMap.removeWaypoint(waypoint);
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(traveler.getTokens() + 1);
                    
                    // Send feedback to online contributors
                    final var contributorPlayer = plugin.getServer().getPlayer(uniqueId);
                    if (contributorPlayer != null) {
                        contributorPlayer.sendMessage(
                            Component.text("A waypoint token was returned to your wallet.", NamedTextColor.BLUE));
                    }
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
                    player.sendMessage(Component.text("A waypoint token was returned to your wallet!", NamedTextColor.BLUE));
                    final var traveler = travelerMap.getOrCreateTraveler(player);
                    traveler.setTokens(traveler.getTokens() + 1);
                    contributors.remove(uniqueId);
                }
                final var tokenRequirement = plugin.getWaypointActivateCost();
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            case REPOSITION -> {
                // This case is handled earlier in the method for both pickup and placement
                return;
            }
            default -> {
                return;
            }
        }
        travelerMap.unregisterTask(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        final var cfg = plugin.getConfig();
        final var killer = event.getEntity().getKiller();
        if (killer == null)
            return; // only player kills generate charges

        // Check if player damage is required
        if (cfg.getBoolean("charge.player-damage-req", true)) {
            final var entry = recentDamages.get(event.getEntity().getUniqueId());
            if (entry == null) {
                return; // no recent player damage tracked
            }
        }

        // Check world whitelist
        final var worldList = cfg.getStringList("charge.worlds");
        if (!worldList.isEmpty() && !worldList.contains(event.getEntity().getWorld().getName()))
            return;

        final var type = event.getEntityType();
        
        // Check mob blacklist
        final var blacklist = cfg.getStringList("charge.mob-blacklist");
        if (blacklist != null && blacklist.contains(type.name().toLowerCase()))
            return;

        // Handle boss drops
        final var bossList = cfg.getStringList("charge.boss-mob-list");
        if (bossList != null && bossList.contains(type.name().toLowerCase())) {
            final var bossDropChance = cfg.getInt("charge.boss-drop-percentage", 100);
            if (Math.random() * 100 <= bossDropChance) {
                final var amount = cfg.getInt("charge.boss-drop-amount", 10);
                dropTeleportCharges(event.getEntity().getLocation(), amount);
            }
            return;
        }

        // Check anti-farm limits if enabled
        if (cfg.getBoolean("charge.anti-farm", true)) {
            if (!canDropInChunk(event.getEntity().getLocation())) {
                return; // chunk limit reached
            }
        }

        // Standard drop calculation
        final double baseChance = cfg.getDouble("charge.drop-percentage", 5.0);
        final double lootingBonus = cfg.getDouble("charge.looting-bonus-percentage", 1.0);
        final int lootingLevel = killer.getInventory().getItemInMainHand()
                .getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOTING);
        final double totalChance = baseChance + (lootingBonus * lootingLevel);
        
        if (Math.random() * 100 <= totalChance) {
            dropTeleportCharges(event.getEntity().getLocation(), 1);
        }

        // Token drop calculation (separate roll, very rare)
        final double tokenDropChance = cfg.getDouble("token.drop-percentage", 0.005);
        if (Math.random() * 100 <= tokenDropChance) {
            dropWaypointToken(event.getEntity().getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        final var cfg = plugin.getConfig();
        if (!cfg.getBoolean("charge.player-damage-req", true))
            return;
        if (!(event.getDamager() instanceof Player))
            return;
        final var entity = event.getEntity();
        // Ignore players damaging players
        if (entity instanceof Player)
            return;
        // Track: mob UUID -> timestamp (simplified tracking)
        recentDamages.put(entity.getUniqueId(), System.currentTimeMillis());
    }

    private void dropTeleportCharges(org.bukkit.Location location, int amount) {
        if (amount <= 0)
            return;
        
        // Track the drop for anti-farm purposes
        if (plugin.getConfig().getBoolean("charge.anti-farm", true)) {
            trackChunkDrop(location);
        }
        
        final var stack = plugin.getTeleportCharge().createCharge(amount);
        location.getWorld().dropItemNaturally(location, stack);
    }

    private void dropWaypointToken(org.bukkit.Location location) {
        final var stack = plugin.getToken().createToken(1);
        location.getWorld().dropItemNaturally(location, stack);
    }

    /**
     * Check if a chunk can still have drops based on the sliding window limit
     */
    private boolean canDropInChunk(org.bukkit.Location location) {
        final var chunkKey = Chunk.getChunkKey(location);
        final var now = System.currentTimeMillis();
        final var windowMs = 60 * 60 * 1000L; // 1 hour in milliseconds
        final var maxDrops = plugin.getConfig().getInt("charge.drops-per-chunk-per-hour", 15);
        
        // Get existing timestamps for this chunk
        final var timestamps = chunkDropTimestamps.computeIfAbsent(chunkKey, k -> new java.util.ArrayList<>());
        
        // Remove old timestamps outside the sliding window
        timestamps.removeIf(timestamp -> (now - timestamp) > windowMs);
        
        // Check if we're under the limit
        return timestamps.size() < maxDrops;
    }

    /**
     * Track a drop in the chunk's timestamp list
     */
    private void trackChunkDrop(org.bukkit.Location location) {
        final var chunkKey = Chunk.getChunkKey(location);
        final var now = System.currentTimeMillis();
        
        final var timestamps = chunkDropTimestamps.computeIfAbsent(chunkKey, k -> new java.util.ArrayList<>());
        timestamps.add(now);
    }

    private void handleBannerReplacement(Player player, org.bukkit.block.Block clickedBlock,
            PlayerInteractEvent event) {
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

            if (heldItem.hasItemMeta()
                    && heldItem.getItemMeta() instanceof org.bukkit.inventory.meta.BannerMeta bannerMeta) {
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

    private void handleWaypointPickup(Player player, Block clickedBlock, TravelerTask task) {
        final var waypoint = waypointMap.getNearbyWaypoint(clickedBlock);

        // Capture banner data BEFORE removing the banner
        Material bannerMaterial = clickedBlock.getType();
        java.util.List<org.bukkit.block.banner.Pattern> bannerPatterns = java.util.List.of();
        net.kyori.adventure.text.Component bannerName = null;

        if (clickedBlock.getState() instanceof org.bukkit.block.Banner banner) {
            bannerPatterns = new java.util.ArrayList<>(banner.getPatterns());
            bannerName = banner.customName();
        }

        // Store the waypoint and banner data in the task for later placement
        task.setRepositionWaypoint(waypoint);
        task.setRepositionBannerMaterial(bannerMaterial);
        task.setRepositionBannerPatterns(bannerPatterns);
        task.setRepositionBannerName(bannerName);

        // Remove waypoint from the map temporarily
        waypointMap.removeWaypoint(waypoint);

        // Hide the hologram
        hologramMap.remove(waypoint);

        // Remove the banner block
        clickedBlock.setType(Material.AIR);

        player.sendMessage(Component.text("Waypoint picked up! Right-click a solid block to place it.",
                NamedTextColor.GREEN));
    }

    private void handleWaypointPlacement(Player player, Block clickedBlock, TravelerTask task) {
        final var waypoint = task.getRepositionWaypoint();
        if (waypoint == null) {
            return;
        }

        // Determine placement location (on top of clicked block or clicked block if
        // air)
        final var placementBlock = clickedBlock.getType() == Material.AIR ? clickedBlock
                : clickedBlock.getRelative(BlockFace.UP);

        // Check if placement location is valid
        if (!placementBlock.getType().isAir()) {
            player.sendMessage(Component.text("Cannot place waypoint here - location is not empty!",
                    NamedTextColor.RED));
            return;
        }

        // Check if we're in a valid waypoint world
        final var worldName = placementBlock.getWorld().getName();
        if (!plugin.getWaypointWorlds().contains(worldName)) {
            player.sendMessage(Component.text("Cannot place waypoints in this world!", NamedTextColor.RED));
            return;
        }

        // Try to create a new waypoint at the placement location
        // This will automatically check distance from existing waypoints
        final var newWaypoint = waypointMap.createWaypoint(placementBlock);
        if (newWaypoint == null) {
            player.sendMessage(Component.text("There is already a waypoint nearby!", NamedTextColor.RED));
            return;
        }

        // Remove the newly created waypoint since we want to reuse the old one's data
        waypointMap.removeWaypoint(newWaypoint);

        // Get stored banner data from the task
        final var bannerMaterial = task.getRepositionBannerMaterial();
        final var bannerPatterns = task.getRepositionBannerPatterns();
        final var bannerName = task.getRepositionBannerName();

        // Create a new waypoint with the old waypoint's data at the new location
        final var repositionedWaypoint = new Waypoint(waypoint.getId(), placementBlock.getLocation(),
                waypoint.getContributors(), waypoint.isActive());

        // Add the repositioned waypoint to the map
        waypointMap.addWaypoint(repositionedWaypoint);

        // Place the banner block with preserved data
        placementBlock.setType(bannerMaterial != null ? bannerMaterial : Material.WHITE_BANNER);
        final var bannerState = placementBlock.getState();

        if (bannerState instanceof org.bukkit.block.Banner banner) {
            // Apply preserved patterns
            if (bannerPatterns != null) {
                for (final var pattern : bannerPatterns) {
                    banner.addPattern(pattern);
                }
            }

            // Set waypoint name (preserve original banner name or use waypoint name)
            if (bannerName != null) {
                banner.customName(bannerName);
            } else if (repositionedWaypoint.hasName()) {
                banner.customName(Component.text(repositionedWaypoint.getName()));
            }
            banner.update();
        }

        // Show hologram again if active
        if (repositionedWaypoint.isActive()) {
            hologramMap.show(repositionedWaypoint, player);
        }

        // Clear the task data to prevent cleanup logic from running
        task.setRepositionWaypoint(null);
        task.setRepositionBannerMaterial(null);
        task.setRepositionBannerPatterns(null);
        task.setRepositionBannerName(null);

        player.sendMessage(Component.text("Waypoint placed successfully!", NamedTextColor.GREEN));
    }

    private static void sendActionBar(Player player, int contributorsSize, int tokenRequirement) {
        player.sendActionBar(Component.text(String.format("%d / %d", contributorsSize, tokenRequirement)));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();

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
