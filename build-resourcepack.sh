#!/bin/bash

# WhispWaypoints Resource Pack Builder
# Creates a zip file ready for distribution

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building WhispWaypoints Resource Pack...${NC}"

# Check if texture exists
texture_path="resourcepack/assets/minecraft/textures/item/whispwaypoints_token.png"
if [ ! -f "$texture_path" ]; then
    echo -e "${YELLOW}⚠️  Warning: Custom texture not found!${NC}"
    echo -e "${YELLOW}   Expected: $texture_path${NC}"
    echo -e "${YELLOW}   The pack will work but will use missing texture.${NC}"
    echo ""
fi

# Create output directory
output_dir="build/resourcepack"
mkdir -p "$output_dir"

# Get version for filename
version="unknown"
if [ -f "version.properties" ]; then
    minecraft=$(grep "^minecraft=" version.properties | cut -d'=' -f2)
    plugin_major=$(grep "^plugin_major=" version.properties | cut -d'=' -f2)
    plugin_minor=$(grep "^plugin_minor=" version.properties | cut -d'=' -f2)
    plugin_patch=$(grep "^plugin_patch=" version.properties | cut -d'=' -f2)
    version="${minecraft}_${plugin_major}.${plugin_minor}.${plugin_patch}"
fi

pack_name="WhispWaypoints-ResourcePack-$version.zip"
output_path="$output_dir/$pack_name"

# Remove old pack if exists
if [ -f "$output_path" ]; then
    rm -f "$output_path"
fi

# Create zip file
if command -v zip >/dev/null 2>&1; then
    # Use zip command if available
    cd resourcepack
    if zip -r "../$output_path" . >/dev/null 2>&1; then
        cd ..
        echo -e "${GREEN}✅ Resource pack created successfully!${NC}"
        echo -e "${CYAN}   File: $output_path${NC}"
        
        # Calculate file size
        size_kb=$(du -k "$output_path" | cut -f1)
        echo -e "${CYAN}   Size: ${size_kb} KB${NC}"
        
        echo ""
        echo -e "${YELLOW}📋 Next Steps:${NC}"
        echo -e "${WHITE}   1. Add your custom texture: $texture_path${NC}"
        echo -e "${WHITE}   2. Test in Minecraft with the built pack${NC}"
        echo -e "${WHITE}   3. Upload to server for automatic distribution${NC}"
    else
        cd ..
        echo -e "${RED}❌ Failed to create resource pack${NC}"
        exit 1
    fi
elif command -v python3 >/dev/null 2>&1; then
    # Fallback to Python if zip is not available
    python3 << EOF
import zipfile
import os
from pathlib import Path

def create_zip():
    try:
        source_dir = Path('resourcepack')
        output_path = Path('$output_path')
        
        with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
            for file_path in source_dir.rglob('*'):
                if file_path.is_file():
                    arcname = file_path.relative_to(source_dir)
                    zipf.write(file_path, arcname)
        
        print("Resource pack created successfully!")
        return True
    except Exception as e:
        print(f"Failed to create resource pack: {e}")
        return False

if create_zip():
    exit(0)
else:
    exit(1)
EOF

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Resource pack created successfully!${NC}"
        echo -e "${CYAN}   File: $output_path${NC}"
        
        # Calculate file size
        size_kb=$(du -k "$output_path" | cut -f1)
        echo -e "${CYAN}   Size: ${size_kb} KB${NC}"
        
        echo ""
        echo -e "${YELLOW}📋 Next Steps:${NC}"
        echo -e "${WHITE}   1. Add your custom texture: $texture_path${NC}"
        echo -e "${WHITE}   2. Test in Minecraft with the built pack${NC}"
        echo -e "${WHITE}   3. Upload to server for automatic distribution${NC}"
    else
        echo -e "${RED}❌ Failed to create resource pack${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ Neither 'zip' command nor 'python3' found. Please install one of them.${NC}"
    exit 1
fi

echo ""
echo -e "${MAGENTA}🎨 Resource Pack Features:${NC}"
echo -e "${WHITE}   • Custom model data: 1001${NC}"
echo -e "${WHITE}   • Pack format: 48 + supported_formats (MC 1.20.5+ through 1.21.x)${NC}"
echo -e "${WHITE}   • Auto-detects Waypoint Tokens${NC}"
