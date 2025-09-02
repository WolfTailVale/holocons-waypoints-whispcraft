# WhispWaypoints

A banner-based waypoint system for Minecraft Paper 1.21+ servers. Players can create, activate, and teleport to custom waypoints using banners and a token-based activation system.

Originally developed for the Hololive Construction Minecraft server by Derek Lee (@dlee13), updated for Paper 1.21+ by wolftailvale and renamed to WhispWaypoints.

## Features

- **Banner-based waypoints**: Create waypoints by placing named banners
- **Token activation system**: Collaborative waypoint activation using craftable tokens
- **Teleportation with delays**: Configurable teleport delays with progress bars
- **Charge system**: Collect charges from mob drops to fuel teleportation
- **Home & camp locations**: Set personal teleport points with convenient shortcuts
- **Visual holograms**: Floating text displays above waypoints
- **Chunk-based storage**: One waypoint per chunk with persistent data
- **Staff management tools**: Admin commands for waypoint management, banner replacement, and repositioning
- **Custom textures**: Resource pack with custom token and charge item appearances
- **Wallet system**: Right-click items to store charges and tokens in your personal wallet
- **Smart teleport costs**: Different costs for same-world vs inter-world teleports
- **Anti-farming**: Prevents excessive charge farming with per-chunk limits
- **Rare token drops**: Small chance for waypoint tokens to drop from any mob

## Installation

1. Download the latest release JAR file
2. Place `WhispWaypoints-x.x.x.jar` in your server's `plugins/` folder
3. Download and install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (required dependency)
4. **Optional**: Install the included resource pack for custom token and charge textures
5. Restart your server
6. Configure `plugins/WhispWaypoints/config.yml` as needed

### Resource Pack Installation

The plugin includes a custom resource pack that adds unique textures for waypoint tokens and teleport charges:

- **Waypoint Tokens**: Custom paper texture (custom model data 1001)
- **Teleport Charges**: Custom amethyst shard texture (custom model data 1001)

To install:
1. Extract `WhispWaypoints-ResourcePack-x.x.x.zip` from the release
2. Upload to your server's resource pack hosting or use it client-side
3. Players will see custom textures instead of default paper/amethyst shard items

## Usage

### Creating Waypoints

1. **Enter create mode**: `/waypoints create`
2. **Place a banner** on top of any block (within 30 seconds)
   - Banner must be in an allowed world (see config)
   - Chunk must not already contain a waypoint
   - Rename the banner in an anvil first to set the waypoint name

### Activating Waypoints

Newly created waypoints start inactive and need tokens to activate:

1. **Get tokens**: 
   - **Craft**: 8 Echo Shards surrounding 1 Ender Pearl
   - **Mob drops**: 0.005% chance from any mob kill (very rare!)
   - **Right-click to store**: Right-click tokens to add them to your wallet
2. **Contribute tokens**: Right-click inactive waypoints to spend wallet tokens (1 per click)
3. **Check progress**: Hover over waypoints to see activation progress
4. **Auto-activation**: Waypoint activates when enough tokens are contributed (default: 3 tokens)

### Getting Teleport Charges

Teleportation requires charges that are obtained from mob drops:

1. **Kill mobs**: 5% base chance + 2% per looting level to drop charges
2. **Boss mobs**: Guaranteed drops with higher amounts (Ender Dragon, Warden, Wither)
3. **Right-click charges**: Right-click amethyst shard charges to add them to your wallet
4. **Anti-farming**: Max 15 drops per chunk per hour to prevent exploitation
5. **Check wallet**: Use `/waypoints wallet` or `/wallet` to see your current charges and tokens

### Using Waypoints

1. **Register**: Right-click any active waypoint to add it to your personal list
2. **Teleport**:
   - Open GUI: `/waypoints`
   - Direct command: `/waypoints teleport <name>`
   - **Costs**: 1 charge for same-world, 2 charges for inter-world teleports
3. **Set locations**:
   - Home: `/waypoints sethome` or `/sethome` then right-click a location
   - Camp: `/setcamp` (automatically places banner at your current location)

### Quick Commands

Essential shortcuts for fast navigation:

**Home Commands:**
- `/sethome` - Set your home location (same as `/waypoints sethome`)
- `/home` - Teleport to your home location (free teleport)

**Camp Commands:**  
- `/setcamp` - Set your camp location and place a banner
- `/camp` - Teleport to your camp location (free teleport)
- `/unsetcamp` - Remove your camp location and banner

**Wallet Commands:**
- `/waypoints wallet` - View your current charges and tokens
- `/waypoints removetoken` - Extract tokens from waypoints back to your wallet

**Camp Features:**
- **Automatic banner placement**: `/setcamp` places a white banner with "[PlayerName]'s Camp" 
- **Smart positioning**: Banner is placed on the ground or at your location if suitable
- **Hologram display**: Shows camp name above the banner (visible to you)
- **Auto-cleanup**: `/unsetcamp` removes both the banner and hologram
- **Replaces existing**: Setting a new camp removes any previous camp banner

### Staff Commands

Players with `waypoints.staff` permission (ops by default):

- `/editwaypoints activate` - Instantly activate any waypoint
- `/editwaypoints delete` - Remove waypoints and refund tokens
- `/editwaypoints menu` - Open staff management GUI
- `/editwaypoints replacebanner` - Replace waypoint banners while preserving waypoint data
- `/editwaypoints reposition` - Move waypoints to new locations
- `/givewptoken [amount]` - Give waypoint tokens to yourself (1-64)
- `/givewpcharge [amount]` - Give teleport charges to yourself (1-64)

#### Staff Management Details

**Banner Replacement Mode** (`/editwaypoints replacebanner`):
- Enter replacement mode for 30 seconds
- Hold the new banner type in your hand
- Right-click any waypoint banner to swap it
- Preserves waypoint name, activation status, and contributors
- Automatically faces the banner toward you

**Waypoint Repositioning Mode** (`/editwaypoints reposition`):
- Enter reposition mode for 30 seconds
- Crouch + right-click with empty hand on waypoint banner to pick it up
- Right-click any solid block to place the waypoint at new location
- Preserves all waypoint data, banner patterns, and activation status
- Validates distance from other waypoints and world restrictions
- Automatically restores waypoint if mode expires while holding one

### Console Commands

- `/waypoints backup` - Create backup ZIP file
- `/waypoints save` - Force save data to disk
- `/waypoints load` - Reload data from disk
- `/waypoints reload` - Reload configuration

## Configuration

Edit `plugins/WhispWaypoints/config.yml`:

```yaml
charge:
  mob-blacklist: []                       # Mobs that don't drop charges
  drop-percentage: 5                      # Base % chance for charge drops
  looting-bonus-percentage: 2             # Additional % per looting level
  boss-mob-list:                          # Mobs that guarantee charge drops
    - ender_dragon
    - warden  
    - wither
  boss-drop-percentage: 100               # % chance for boss drops
  boss-drop-amount: 10                    # Charges dropped by bosses
  player-damage-req: true                 # Require player damage for drops
  anti-farm: true                         # Enable anti-farming limits
  drops-per-chunk-per-hour: 15            # Max drops per chunk per hour
  worlds:                                 # Worlds where charges drop
    - world
    - world_nether
    - world_the_end
  item:
    material: AMETHYST_SHARD              # Material for charge items
    name: "Teleport Charge"               # Display name

token:
  activate-waypoint-cost: 3               # Tokens needed to activate waypoint
  drop-percentage: 0.005                  # % chance for token drops from mobs

teleport:
  wait-time: 120                          # Teleport delay in ticks (6 seconds)
  particle-effect: null                   # Particle effect during teleport
  teleport-waypoint-cost: 1               # Charges for same-world teleport
  inter-world-cost: 2                     # Charges for inter-world teleport

world:
  home: [world]                           # Worlds where homes can be set
  camp: [resource_world, resource_nether] # Worlds where camps can be set
  waypoint: [world, world_nether, world_the_end] # Worlds where waypoints can be created
```

## Permissions

- `waypoints.player` (default: true) - Access to basic waypoint commands
- `waypoints.staff` (default: op) - Access to administrative commands

## Data Storage

- **Waypoints**: `plugins/WhispWaypoints/waypoint.json`
- **Players**: `plugins/WhispWaypoints/traveler.json`  
- **Backups**: `plugins/WhispWaypoints/backup-[timestamp].zip`

## Building from Source

### Prerequisites
- Java 21+
- Gradle (wrapper included)

### Clone and Build
```bash
git clone https://github.com/wolftailvale/holocons-waypoints-whispcraft.git
cd holocons-waypoints-whispcraft

# Build plugin JAR (Linux/macOS)
./build.sh

# Build resource pack (Linux/macOS)  
./build-resourcepack.sh

# Or build both
./build.sh && ./build-resourcepack.sh
```

**Windows:**
```powershell
# Build plugin JAR
.\build.ps1

# Build resource pack
.\build-resourcepack.ps1

# Or build both
.\build.ps1; .\build-resourcepack.ps1
```

**Manual Gradle:**
```bash
./gradlew clean build
# Note: Resource pack must be built separately with the build script
./build-resourcepack.sh
```

### Output Files
- Plugin JAR: `build/libs/WhispWaypoints-x.x.x.jar`
- Resource pack: `WhispWaypoints_ResourcePack.zip` (root directory)

## Changelog

### Version 1.10.0 (Latest)

- ✨ **Major Feature**: Custom textures for items via resource pack
  - Custom waypoint token texture (paper with model data 1001)
  - Custom teleport charge texture (amethyst shard with model data 1001)
  - Automatic resource pack generation in build process
- 🎯 **New Feature**: Wallet system for charges and tokens
  - Right-click items to store in personal wallet (unlimited capacity)
  - `/waypoints wallet` command to view current totals
  - Chat feedback when tokens are returned to wallets
- 🗡️ **New Feature**: Mob drop system for charges and tokens
  - Charges: 5% base + 2% per looting level from mob kills
  - Tokens: 0.005% chance from any mob (very rare)
  - Boss guarantees: Ender Dragon, Warden, Wither drop 10 charges
  - Anti-farming: Max 15 drops per chunk per hour
- 💰 **New Feature**: Inter-world teleport costs
  - Same world: 1 charge, Inter-world: 2 charges (configurable)
  - Encourages using portals before relying on waypoints
- 🏠 **New Feature**: Home command shortcuts
  - `/sethome` - Set home location
  - `/home` - Teleport to home
- ⚡ **New Feature**: Staff give commands
  - `/givewptoken [amount]` - Give tokens for testing
  - `/givewpcharge [amount]` - Give charges for testing  
- 🧹 **Improvement**: Simplified token activation
  - Right-click waypoints to spend wallet tokens (no mode required)
  - Reduced default activation cost from 8 to 3 tokens
  - `/waypoints removetoken` to extract tokens back
- 📢 **Improvement**: Better teleport failure feedback
  - Clear messages when teleports fail due to insufficient charges
  - Shows current vs required charge amounts
- 🗑️ **Cleanup**: Removed debugging commands
  - Removed `/waypoints tokendebug` and `/waypoints tokenrepair`
  - Cleaner production-ready command set

### Version 1.8.2

- 📊 **New Feature**: Comprehensive teleportation logging
  - Logs all teleportation attempts with player details, destination, and coordinates
  - Tracks teleportation success/failure with specific failure reasons
  - Logs charge consumption and remaining charges after successful teleports
  - Monitors player actions that cause teleport failures (movement, damage)
  - Includes both command-based and GUI-based teleportation tracking
  - Logs teleportation cancellations and disconnects during teleport

### Version 1.8.1
- 🐛 **Fixed**: Waypoint repositioning issues
  - Fixed banner material and patterns not being preserved during reposition
  - Fixed waypoint names showing as "Unnamed Waypoint" after repositioning
  - Fixed cleanup logic running after successful placement, causing duplicate banners
  - Fixed conflicting success/failure messages during placement
  - Enhanced banner data storage during pickup for proper preservation

### Version 1.8.0
- ✨ **New Feature**: Waypoint repositioning system (`/editwaypoints reposition`)
  - Crouch + right-click with empty hand to pick up waypoints
  - Right-click solid blocks to place waypoints at new locations
  - Preserves all waypoint data, banner patterns, and activation status
  - Smart validation for distance and world restrictions
- 🔧 Enhanced administrative tools for better waypoint management

### Version 1.7.3
- ✨ **New Feature**: Banner replacement system (`/editwaypoints replacebanner`)
  - Replace waypoint banners while preserving all waypoint data
  - Automatic banner orientation toward admin
  - Custom name preservation during replacement
- 🐛 Fixed YAML formatting issues in plugin.yml
- 🐛 Fixed banner orientation for better visibility

### Version 1.7.0
- 🔄 Updated for Minecraft 1.21.8 compatibility
- 🛠️ Improved code structure and organization
- 📦 Enhanced build system with proper resource pack generation

## Credits

- **Original Author**: Derek Lee (@dlee13)
- **Updated for 1.21**: wolftailvale
- **License**: MIT License

## Dependencies

- [Paper 1.21+](https://papermc.io/) - Server software
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) - Packet manipulation library
