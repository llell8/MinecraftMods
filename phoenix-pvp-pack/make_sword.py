#!/usr/bin/env python3
"""Phoenix netherite sword: the vanilla netherite sword with its colours warmed toward fire.

Shape and shading are untouched; each vanilla shade is swapped for a warmer one.
Usage: python3 make_sword.py   (reads .vanilla/netherite_sword.png, fetching it if missing)
"""
import json
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
VANILLA = ROOT / ".vanilla" / "netherite_sword.png"
URL = "https://raw.githubusercontent.com/misode/mcmeta/1.21.11-assets/assets/minecraft/textures/item/netherite_sword.png"
PACK = ROOT / "pack"

# vanilla netherite -> phoenix netherite. The greys only warm up a little so it still reads as
# netherite; the purple trim becomes an ember edge and the blade's light edge a dull glow.
SWAP = {
    (22, 8, 20): (24, 8, 8),          # outline
    (35, 16, 18): (38, 14, 12),       # handle/blade shadow
    (59, 57, 59): (62, 54, 52),       # blade dark
    (74, 41, 64): (122, 34, 18),      # purple trim -> ember trim
    (96, 52, 50): (124, 58, 30),      # grip
    (79, 60, 62): (84, 60, 56),       # blade core
    (81, 68, 78): (88, 70, 64),       # blade mid
    (115, 69, 67): (170, 92, 36),     # grip highlight
    (93, 86, 93): (102, 88, 80),      # blade
    (112, 103, 112): (122, 104, 92),  # blade light
    (134, 123, 134): (182, 124, 84),  # edge glint
}


def main():
    if not VANILLA.exists():
        VANILLA.parent.mkdir(parents=True, exist_ok=True)
        VANILLA.write_bytes(urllib.request.urlopen(URL).read())
    sword = Image.open(VANILLA).convert("RGBA")
    px = sword.load()
    for y in range(sword.height):
        for x in range(sword.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) in SWAP:
                px[x, y] = SWAP[r, g, b] + (a,)
    out = PACK / "assets/minecraft/textures/item/netherite_sword.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sword.save(out, optimize=True)

    # Pack icon: the sword on a fire glow.
    icon = Image.new("RGBA", (128, 128))
    ipx = icon.load()
    for y in range(128):
        for x in range(128):
            d = min(((x - 64) ** 2 + (y - 64) ** 2) ** 0.5 / 90, 1)
            ipx[x, y] = (round(255 - 200 * d), round(120 - 110 * d), round(20 - 16 * d), 255)
    icon.alpha_composite(sword.resize((112, 112), Image.NEAREST), (8, 8))
    icon.save(PACK / "pack.png", optimize=True)

    meta = {"pack": {"description": "§6§lPhoenix PvP§r §c— rise from the ashes",
                     "pack_format": 75, "min_format": 75, "max_format": 75}}
    (PACK / "pack.mcmeta").write_text(json.dumps(meta, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
