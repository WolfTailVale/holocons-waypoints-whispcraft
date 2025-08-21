# WhispWaypoints Custom Token Resource Pack

This resource pack adds a custom appearance for the Waypoint Token item in WhispWaypoints.

## 🎨 How It Works

The plugin sets `custom_model_data: 470001` on Waypoint Tokens (which are Ender Pearls). This resource pack detects that custom model data and replaces the appearance with a custom texture.

## 📁 Structure

```
resourcepack/
├── pack.mcmeta                                    # Resource pack metadata
└── assets/
    └── minecraft/
        ├── models/item/
        │   ├── ender_pearl.json                   # Override ender pearl model
        │   └── whispwaypoints_token.json          # Custom token model
        └── textures/item/
            └── whispwaypoints_token.png           # Custom token texture (16x16)
```

## 🖼️ Creating the Texture

1. Create a 16x16 pixel PNG image for your token
2. Save it as `whispwaypoints_token.png` in `assets/minecraft/textures/item/`
3. Suggested design ideas:
   - Magical compass/waystone symbol
   - Glowing orb with runic patterns
   - Crystalline structure
   - Banner with waypoint symbol

## 🚀 Installation

### For Server Owners:
1. Zip the `resourcepack` folder contents (not the folder itself)
2. Upload to your server's resource pack hosting
3. Set in server.properties: `resource-pack=https://your-url/whispwaypoints-pack.zip`

### For Players:
1. Copy the `resourcepack` folder to `.minecraft/resourcepacks/`
2. Rename it to `WhispWaypoints`
3. Enable it in Options > Resource Packs

## 🔧 Customization

### Custom Model Data ID
The plugin uses `470001` as the custom model data. This number was chosen to avoid conflicts:
- 470000-470999: Reserved for WhispWaypoints items
- 470001: Waypoint Token

### Changing the Texture
Simply replace `whispwaypoints_token.png` with your own 16x16 texture.

### Advanced Models
For 3D models, replace `whispwaypoints_token.json` with a custom model file pointing to your model JSON.

## 🎭 Example Texture Ideas

1. **Mystical Orb**: Purple/blue glowing sphere with particle effects
2. **Compass Token**: Compass-like design with waypoint needle
3. **Runic Stone**: Stone tablet with glowing runes
4. **Crystal Shard**: Crystalline structure with internal glow
5. **Banner Symbol**: Miniature banner with waypoint symbol

## 📋 Pack Format

- **Pack Format 34**: Minecraft 1.21.x
- Update `pack_format` in `pack.mcmeta` if targeting different MC versions

## 🔗 Integration

The resource pack works automatically with WhispWaypoints v1.3.0+. No additional setup required - just install the pack and tokens will have the custom appearance!

## 🛠️ Development

To test changes:
1. Modify texture/model files
2. Press F3+T in Minecraft to reload resource packs
3. Craft or give yourself a Waypoint Token to see changes
