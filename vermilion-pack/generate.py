"""Generates the Vermilion PvP resource pack textures into ./pack and zips it.

Run: python3 vermilion-pack/generate.py   (needs Pillow)
"""
import json
import math
import random
import shutil
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
PACK = ROOT / "pack"
TEX = PACK / "assets" / "minecraft" / "textures"


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# Shared Vermilion Bird trim: gold guard and pommel, crimson wrapped grip, flame-red tassel
GOLD = hexc("f5c542")
GOLD_DARK = hexc("b07d12")
JADE = hexc("3fbf7f")
JADE_DARK = hexc("1f7a4f")
GRIP = hexc("7a1010")
GRIP_LIGHT = hexc("b22222")
TASSEL = hexc("e8341c")
TASSEL_LIGHT = hexc("ff6a3d")

# Blade colours per tier: (light edge, middle, dark edge, outline)
SWORDS = {
    "wooden_sword": ("d9734a", "a8422a", "6e2516", "2a0d08"),
    "stone_sword": ("bdb7b0", "8a8580", "5e5a56", "1f1d1c"),
    "copper_sword": ("f7b27a", "d4763f", "94491f", "2e140a"),
    "iron_sword": ("ffffff", "dcdcdc", "9a9a9a", "262626"),
    "golden_sword": ("fff3a0", "f2c230", "b8860b", "3d2a05"),
    "diamond_sword": ("b8f5d0", "3fbf7f", "1f7a4f", "08261a"),  # imperial jade
    "netherite_sword": ("5a4f54", "ff5a1f", "2a2226", "0d0a0b"),  # black blade, vermilion fire core
}


def sword(edge, mid, dark, outline, guard=GOLD, guard_dark=GOLD_DARK):
    """Short diagonal sword. s = x + y runs across the blade, d = x - y runs along it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            s, d = x + y, x - y
            off = abs(s - 15)
            # blade
            if s in (14, 15, 16) and -1 <= d <= 11 - off:
                px[x, y] = {14: edge, 15: mid, 16: dark}[s]
            # crossguard, perpendicular to the blade
            elif d in (-2, -3) and 11 <= s <= 19:
                px[x, y] = guard_dark if d == -3 or s in (11, 19) else guard
            # wrapped grip
            elif s in (15, 16) and -9 <= d <= -4:
                px[x, y] = GRIP_LIGHT if (d // 2) % 2 else GRIP
            # pommel
            elif -12 <= d <= -10 and 14 <= s <= 17:
                px[x, y] = guard if s == 15 else guard_dark
    # tassel hanging off the pommel
    for (x, y), c in {(4, 13): TASSEL_LIGHT, (4, 14): TASSEL, (5, 14): TASSEL_LIGHT, (4, 15): TASSEL, (5, 15): TASSEL}.items():
        if px[x, y][3] == 0:
            px[x, y] = c
    # 1px outline around everything except the tassel
    filled = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for y in range(16):
        for x in range(16):
            if (x, y) in filled or y >= 13 and x in (3, 4, 5, 6):
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = outline
    return img


def glint(size=128, seed=7):
    """Tileable orange glint. The game blends it additively, so brightness = glow strength."""
    rnd = random.Random(seed)
    waves = [(rnd.randint(1, 3), rnd.randint(-3, 3), rnd.random() * math.tau, rnd.uniform(0.6, 1.0)) for _ in range(6)]
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            u, v = x / size * math.tau, y / size * math.tau
            # diagonal streaks plus soft interference, all integer frequencies so it tiles
            streak = 0.5 + 0.5 * math.sin(3 * u + 3 * v + 1.3 * math.sin(2 * u - v))
            soft = sum(a * math.sin(fx * u + fy * v + p) for fx, fy, p, a in waves) / sum(w[3] for w in waves)
            i = max(0.0, min(1.0, 0.55 * streak ** 2 + 0.35 * (0.5 + 0.5 * soft) + 0.1))
            px[x, y] = (int(255 * i), int(115 * i ** 1.3), int(18 * i ** 2), 255)
    return img


FIRE_RAMP = [(0.0, hexc("8b0f0f", 0)), (0.12, hexc("8b0f0f", 200)), (0.35, hexc("e34234")),
             (0.6, hexc("ff7a1a")), (0.85, hexc("ffb238")), (1.0, hexc("ffe08a"))]


def ramp(t):
    t = max(0.0, min(1.0, t))
    for (t0, c0), (t1, c1) in zip(FIRE_RAMP, FIRE_RAMP[1:]):
        if t <= t1:
            k = (t - t0) / (t1 - t0)
            return tuple(int(a + (b - a) * k) for a, b in zip(c0, c1))
    return FIRE_RAMP[-1][1]


def low_fire(seed, frames=32):
    """Vermilion flames squashed into the bottom of each 16x16 frame, so on-screen fire stays low."""
    rnd = random.Random(seed)
    cols = [(rnd.random() * math.tau, rnd.random() * math.tau) for _ in range(16)]
    img = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
    px = img.load()
    for f in range(frames):
        t = f / frames * math.tau
        for x in range(16):
            p1, p2 = cols[x]
            height = 4.5 + 1.6 * math.sin(t + p1) + 1.0 * math.sin(2 * t + p2 + x * 0.9)
            for y in range(16):
                depth = 15.5 - y  # distance from the bottom edge
                if depth > height:
                    continue
                heat = 1 - depth / max(height, 0.1)
                heat *= 0.75 + 0.25 * math.sin(3 * t + x * 1.7 + y)
                px[x, f * 16 + y] = ramp(heat)
    return img


def pearl():
    """Phoenix-fire orb: vermilion core, gold rim, white-hot highlight."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx, cy, r = 7.5, 8.0, 5.6
    for y in range(16):
        for x in range(16):
            dist = math.hypot(x - cx, y - cy)
            if dist > r + 0.6:
                continue
            if dist > r - 0.4:
                px[x, y] = hexc("3a0a05")
            elif dist > r - 1.4:
                px[x, y] = GOLD if x + y < 16 else GOLD_DARK
            else:
                light = 1 - math.hypot(x - (cx - 1.5), y - (cy - 1.5)) / r
                px[x, y] = ramp(0.3 + 0.7 * light)
    px[5, 5] = px[6, 5] = px[5, 6] = hexc("fff6d6")
    return img


def icon(netherite):
    size = 128
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            dist = math.hypot(x - 64, y - 64) / 90
            px[x, y] = ramp(0.95 - dist)[:3] + (255,)
    big = netherite.resize((112, 112), Image.NEAREST)
    img.alpha_composite(big, (8, 8))
    return img


def main():
    if PACK.exists():
        shutil.rmtree(PACK)
    item, misc, block = TEX / "item", TEX / "misc", TEX / "block"
    for p in (item, misc, block):
        p.mkdir(parents=True)

    for name, cols in SWORDS.items():
        edge, mid, dark, outline = (hexc(c) for c in cols)
        guard = (JADE, JADE_DARK) if name == "golden_sword" else (GOLD, GOLD_DARK)
        sword(edge, mid, dark, outline, *guard).save(item / f"{name}.png")

    g = glint()
    # enchanted_glint_entity is the pre-1.21.2 name for the armor glint; harmless on newer versions
    for name in ("enchanted_glint_item", "enchanted_glint_armor", "enchanted_glint_entity"):
        g.save(misc / f"{name}.png")

    for i in (0, 1):
        low_fire(seed=i).save(block / f"fire_{i}.png")
        (block / f"fire_{i}.png.mcmeta").write_text(json.dumps({"animation": {}}) + "\n")

    pearl().save(item / "ender_pearl.png")
    icon(Image.open(item / "netherite_sword.png")).save(PACK / "pack.png")

    mcmeta = {"pack": {
        "description": "§cVermilion §6PvP §7• orange glint, short swords, low fire",
        # 1.21.9+ reads min/max_format; older 1.21.x reads pack_format/supported_formats
        "pack_format": 42,
        "supported_formats": [42, 99],
        "min_format": 42,
        "max_format": 99,
    }}
    (PACK / "pack.mcmeta").write_text(json.dumps(mcmeta, indent=2, ensure_ascii=False) + "\n")

    out = ROOT / "Vermilion-PvP.zip"
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(PACK.rglob("*")):
            if f.is_file():
                z.write(f, f.relative_to(PACK).as_posix())
    print("wrote", out)


if __name__ == "__main__":
    main()
