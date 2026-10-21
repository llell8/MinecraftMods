#!/usr/bin/env python3
"""Phoenix diamond sword: the vanilla diamond sword with each blade colour swapped for a fire colour.

Shape, shading and the stick handle are untouched; only the seven teal shades change.
Usage: python3 make_sword.py   (reads .vanilla/diamond_sword.png, fetching it if missing)
"""
import json
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
VANILLA = ROOT / ".vanilla" / "diamond_sword.png"
URL = "https://raw.githubusercontent.com/misode/mcmeta/1.21.11-assets/assets/minecraft/textures/item/diamond_sword.png"
PACK = ROOT / "pack"

# vanilla teal -> phoenix fire, darkest to brightest
SWAP = {
    (8, 37, 32): (44, 6, 4),        # outline
    (14, 63, 54): (104, 14, 8),     # inner outline
    (21, 99, 85): (156, 26, 10),    # guard shadow
    (30, 138, 119): (206, 56, 14),  # blade shadow
    (43, 199, 172): (242, 112, 22), # blade
    (51, 235, 203): (255, 172, 46), # blade light
    (164, 253, 240): (255, 238, 160),  # edge glint
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
    out = PACK / "assets/minecraft/textures/item/diamond_sword.png"
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
