#!/usr/bin/env bash
# Reproduce Android assets from the user's original PNG, without redrawing the logo.
# Requires ImageMagick 6 (convert / identify). No network or AI generation.
set -euo pipefail
cd "$(dirname "$0")/.."
source_image=20260912_111618.png
res=app/src/main/res
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Only the connected black area outside the supplied rounded white tile is removed.
# Preserve the supplied artwork, gradients and tile inside the app.
convert "$source_image" -alpha on -fuzz 8% -fill none \
  -draw 'matte 0,0 floodfill' \
  -draw 'matte 1499,0 floodfill' \
  -draw 'matte 0,1499 floodfill' \
  -draw 'matte 1499,1499 floodfill' \
  -resize 512x512 -strip "PNG32:$res/drawable-nodpi/lifemate_brand.png"

# Separate the saturated blue/green/yellow artwork from the pale tile. The mask
# is also used for Android's single-color themed and notification icon rendering.
# The complete emblem is inside this rectangle; exclude colored edge pixels
# around the outer tile, which must not become specks in a monochrome icon.
convert "$source_image" -crop 1140x1100+190+220 +repage "$work/artwork.png"
convert "$work/artwork.png" \
  -fx 'max(0,min(1,(max(r,max(g,b))-min(r,min(g,b))-0.24)/0.20))' \
  -colorspace Gray "$work/mask.png"
convert "$work/artwork.png" "$work/mask.png" -alpha off -compose CopyOpacity -composite \
  -trim +repage -resize 265x265 "$work/symbol.png"
# Padding keeps the complete person, leaves and rays inside the adaptive safe zone.
convert "$work/symbol.png" -gravity center -background none -extent 512x512 \
  -strip "PNG32:$res/drawable-nodpi/lifemate_launcher_foreground.png"
convert "$res/drawable-nodpi/lifemate_launcher_foreground.png" \
  -channel RGB -evaluate set 100% +channel -strip \
  "PNG32:$res/drawable-nodpi/lifemate_launcher_monochrome.png"

for spec in mdpi:24 hdpi:36 xhdpi:48 xxhdpi:72 xxxhdpi:96; do
  density=${spec%:*}; size=${spec#*:}; inset=$((size * 5 / 6))
  convert "$work/symbol.png" -channel RGB -evaluate set 100% +channel \
    -resize "${inset}x${inset}" -gravity center -background none -extent "${size}x${size}" \
    -strip "PNG32:$res/drawable-$density/ic_notification.png"
done
