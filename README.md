# Waypoints Plugin

A banner-based waypoint system for Minecraft Paper 1.21+ servers. Players can create, activate, and teleport to custom waypoints using banners and a token-based activation system.

Originally developed for the Hololive Construction Minecraft server by Derek Lee (@dlee13), updated for Paper 1.21+ by wolftailvale.

## Features

- **Banner-based waypoints**: Create waypoints by placing named banners
- **Token activation system**: Collaborative waypoint activation using craftable tokens
- **Teleportation with delays**: Configurable teleport delays with progress bars
- **Charge system**: Teleports consume charges that regenerate over time
- **Home & camp locations**: Set personal teleport points
- **Visual holograms**: Floating text displays above waypoints
- **Chunk-based storage**: One waypoint per chunk with persistent data

## Installation

1. Download the latest release JAR file
2. Place `waypoints-x.x.x.jar` in your server's `plugins/` folder
3. Download and install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (required dependency)
4. Restart your server
5. Configure `plugins/waypoints/config.yml` as needed

## Usage

### Creating Waypoints

1. **Enter create mode**: `/waypoints create`
2. **Place a banner** on top of any block (within 30 seconds)
   - Banner must be in an allowed world (see config)
   - Chunk must not already contain a waypoint
   - Rename the banner in an anvil first to set the waypoint name

### Activating Waypoints

Newly created waypoints start inactive and need tokens to activate:

1. **Craft tokens**: 8 Echo Shards surrounding 1 Ender Pearl
2. **Enter token mode**: `/waypoints addtoken`  
3. **Right-click the waypoint** to contribute a token
4. **Repeat** until enough tokens are contributed (default: 8 tokens required)
5. Waypoint automatically activates when threshold is reached

### Using Waypoints

1. **Register**: Right-click any active waypoint to add it to your personal list
2. **Teleport**: 
   - Open GUI: `/waypoints`
   - Direct command: `/waypoints teleport <name>`
3. **Set locations**:
   - Home: `/waypoints sethome` then right-click a location
   - Camp: `/waypoints setcamp` then right-click a location

### Staff Commands

Players with `waypoints.staff` permission (ops by default):

- `/editwaypoints activate` - Instantly activate any waypoint
- `/editwaypoints delete` - Remove waypoints and refund tokens
- `/editwaypoints menu` - Open staff management GUI

### Console Commands

- `/waypoints backup` - Create backup ZIP file
- `/waypoints save` - Force save data to disk
- `/waypoints load` - Reload data from disk
- `/waypoints reload` - Reload configuration

## Configuration

Edit `plugins/waypoints/config.yml`:

```yaml
charge:
  capacity: 9                    # Maximum charges per player
  teleport-home-cost: 0          # Charges for home teleport
  teleport-camp-cost: 0          # Charges for camp teleport  
  teleport-waypoint-cost: 1      # Charges for waypoint teleport
  regenerate-time: 24000         # Ticks between charge regeneration (20 min)

token:
  capacity: 1                    # Maximum tokens per player
  activate-waypoint-cost: 8      # Tokens needed to activate waypoint

teleport:
  wait-time: 140                 # Teleport delay in ticks (7 seconds)

world:
  home: [world]                  # Worlds where homes can be set
  camp: [world_nether, world_the_end]  # Worlds where camps can be set
  waypoint: [world, world_nether, world_the_end]  # Worlds where waypoints can be created
```

## Permissions

- `waypoints.player` (default: true) - Access to basic waypoint commands
- `waypoints.staff` (default: op) - Access to administrative commands

## Data Storage

- **Waypoints**: `plugins/waypoints/waypoint.json`
- **Players**: `plugins/waypoints/traveler.json`  
- **Backups**: `plugins/waypoints/backup-[timestamp].zip`

## Building from Source

Requirements:
- Java 21+
- Gradle (wrapper included)

```bash
git clone https://github.com/wolftailvale/waypoints.git
cd waypoints
./gradlew clean build
```

Built JAR will be in `build/libs/`

## Credits

- **Original Author**: Derek Lee (@dlee13)
- **Updated for 1.21**: wolftailvale
- **License**: MIT License

## Dependencies

- [Paper 1.21+](https://papermc.io/) - Server software
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) - Packet manipulation library
