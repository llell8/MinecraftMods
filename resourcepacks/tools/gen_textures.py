"""Generates the Phoenix PvP textures from the vanilla 1.21.11 ones in tools/vanilla.

Run: python3 resourcepacks/tools/gen_textures.py
- netherite tools and weapons: vanilla texture with a blaze rod handle,
  animated fire on the head and an orange gem where the head meets the handle
- totem_of_undying.png: dragon-rune style phoenix, serpent body with spread wings
- enchanted_glint_item.png / enchanted_glint_armor.png: orange glint
- pack.png
"""
import colorsys
import json
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(__file__)
VANILLA = os.path.join(HERE, "vanilla")
ROOT = os.path.join(HERE, "..", "PhoenixPvP")
TEX = os.path.join(ROOT, "assets", "minecraft", "textures")
FRAMES = 8

LAVA = [(140, 24, 6), (196, 52, 8), (240, 110, 16), (255, 170, 40), (255, 228, 120)]
# texture name -> (columns the flames rise from, gem pixel where head meets handle)
TOOLS = {
    "netherite_sword": (range(7, 14), (5, 9)),
    "netherite_axe": (range(6, 13), (9, 7)),
    "netherite_pickaxe": (range(5, 14), (10, 5)),
    "netherite_shovel": (range(8, 15), (10, 6)),
    "netherite_hoe": (range(6, 14), (12, 4)),
    "netherite_spear": (range(7, 14), (8, 7)),
    "netherite_spear_in_hand": (range(0, 9), (7, 7)),
}
# Vanilla stick colours on netherite tools -> blaze rod colours.
BLAZE_ROD = {
    (47, 33, 34): (179, 107, 25),
    (96, 52, 50): (216, 174, 38),
    (115, 69, 67): (255, 243, 45),
    # netherite grip wrap on the lower handle
    (59, 57, 59): (216, 174, 38),
    (67, 64, 67): (255, 243, 45),
}
# Blaze rod toned down to a dim copper: (saturation, brightness, hue shift).
# The hue shift pulls the yellow toward orange so it doesn't go olive.
ROD_TONE = (0.55, 0.62, -0.035)
# The black outline around the handle becomes the blaze rod's dark edge,
# and the end cap on the bottom row its darker tip.
ROD_EDGE = (174, 60, 0)
ROD_END = (149, 51, 0)
OUTLINE = {(35, 16, 18), (27, 20, 21), (22, 8, 20)}
MAX_OVERLAP = 2  # deepest the flames start inside the head


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def fire(frame, base, columns):
    """Flames licking up off the head's top edge, plus drifting embers."""
    px = {}
    phase = frame / FRAMES * 2 * math.pi
    for x in columns:
        top = next(y for y in range(base.height) if base.getpixel((x, y))[3])
        h = 1.6 + 1.2 * math.sin(phase + x * 1.9) + 0.6 * math.sin(phase * 2 + x * 0.7)
        # Where each flame starts wanders between the head's edge and
        # MAX_OVERLAP pixels inside it, so the base shifts frame to frame.
        start = 1.2 + 0.9 * math.sin(phase * 2 + x * 2.7 + 1.0)
        overlap = max(0, min(MAX_OVERLAP, int(round(start))))
        h = max(0, int(round(h))) + overlap
        for k in range(h):
            y = top + overlap - 1 - k
            if y < 0:
                break
            frac = k / (h - 1) if h > 1 else 0
            if frac < 0.34:
                c, a = LAVA[3], 200
            elif frac < 0.67:
                c, a = LAVA[2], 190
            else:
                c, a = LAVA[1], 150
            px[(x, y)] = rgba(c, a)
    # Embers rise up-left off the head and loop every FRAMES.
    rng = random.Random(1337)
    for _ in range(4):
        sx = rng.choice(list(columns))
        sy = next(y for y in range(base.height) if base.getpixel((sx, y))[3]) - rng.randint(1, 2)
        t = (frame + rng.randint(0, FRAMES - 1)) % FRAMES
        p = (sx - t // 3, sy - t)
        if t < FRAMES - 2 and p[0] >= 0 and p[1] >= 0 and p not in px and base.getpixel(p)[3] == 0:
            px[p] = rgba(LAVA[4] if t < 3 else LAVA[2], 230 - t * 25)
    return px


def tone(c, saturation, brightness, hue_shift):
    h, l, s = colorsys.rgb_to_hls(*(v / 255 for v in c))
    h = (h + hue_shift) % 1.0
    return tuple(round(v * 255) for v in colorsys.hls_to_rgb(h, l * brightness, s * saturation))


def load_tool(name):
    """Vanilla texture with the wooden stick swapped for a blaze rod."""
    img = Image.open(os.path.join(VANILLA, name + ".png")).convert("RGBA")
    handle = set()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a and (r, g, b) in BLAZE_ROD:
                handle.add((x, y))
                img.putpixel((x, y), rgba(tone(BLAZE_ROD[(r, g, b)], *ROD_TONE), a))
    # Outline pixels that only border the handle (not the head) take the rod's
    # edge colour, or its tip colour on the bottom row.
    bottom = max(y for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3])
    edges = {}
    # Second pass picks up corner pixels that only touch the new edge.
    for _ in range(2):
        for y in range(bottom + 1):
            for x in range(img.width):
                if (x, y) in edges or img.getpixel((x, y))[:3] not in OUTLINE or not img.getpixel((x, y))[3]:
                    continue
                near = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                        if 0 <= x + dx < img.width and 0 <= y + dy < img.height]
                touches_head = any(
                    img.getpixel(p)[3] and p not in handle and img.getpixel(p)[:3] not in OUTLINE
                    for p in near)
                if any(p in handle or p in edges for p in near) and not touches_head:
                    edges[(x, y)] = ROD_END if y == bottom else ROD_EDGE
    for p, c in edges.items():
        img.putpixel(p, rgba(tone(c, *ROD_TONE)))
    return img


# Phoenix totem, drawn from scratch in a dragon-rune style: a serpent body
# winding down the middle with bat-like wings spread wide.
PHOENIX_COLOURS = {
    "R": (150, 26, 10), "r": (205, 60, 16), "o": (240, 120, 20),
    "y": (255, 190, 50), "w": (255, 240, 170), "m": (175, 38, 12),
}
MEMBRANE_ALPHA = 230
# Left wing (cols 0-7), mirrored onto the right: bright leading edge arching
# up to a peak, membrane below, bony fingers hanging into jagged points.
PHOENIX_WING = [
    "........",
    "........",
    "........",
    ".yy.....",  # wing peak
    "yyyoo...",
    "yymmyoo.",
    "ymmmmmoo",
    "ymymmmmm",
    "ymmymmmm",
    "ym.ymmym",
    "y..yy.yy",  # hanging points
    "...y....",
]
# Serpent body drawn over the wings, head at the top facing right.
PHOENIX_BODY = [
    "........yyo.....",  # crest + head
    "........rooooy..",  # snout
    "........ro......",
    ".......ro.......",
    "......ro........",
    "......ro........",
    "......ro........",
    ".......ro.......",
    "........ro......",
    "........rro.....",  # tail thickens
    "........rro.....",
    ".......rro......",
    "......rro.......",
    ".....rro........",
    ".....ro.........",
    ".....yy.........",  # tail flame
]
# Heat shimmer: a band runs down the dragon, briefly heating hot colours.
SHIMMER = {"o": "y", "y": "w"}


def phoenix_frame(frame):
    grid = {}
    for y, row in enumerate(PHOENIX_WING):
        for x, ch in enumerate(row):
            if ch != ".":
                grid[(x, y)] = grid[(15 - x, y)] = ch
    for y, row in enumerate(PHOENIX_BODY):
        for x, ch in enumerate(row):
            if ch != ".":
                grid[(x, y)] = ch
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    phase = frame / FRAMES * 2 * math.pi
    for (x, y), ch in grid.items():
        if ch in SHIMMER and math.sin(phase - y * 0.5) > 0.85:
            ch = SHIMMER[ch]
        img.putpixel((x, y), rgba(PHOENIX_COLOURS[ch], MEMBRANE_ALPHA if ch == "m" else 255))
    # Embers rising off the wing peaks.
    rng = random.Random(7)
    for _ in range(3):
        sx = rng.choice([1, 2, 13, 14])
        t = (frame + rng.randint(0, FRAMES - 1)) % FRAMES
        p = (sx, 2 - t)
        if t < 3 and p[1] >= 0:
            img.putpixel(p, rgba(LAVA[4] if t < 2 else LAVA[3], 240 - t * 30))
    return img


def render_frame(frame, base, columns, gem):
    img = base.copy()
    flames = Image.new("RGBA", base.size, (0, 0, 0, 0))
    for p, c in fire(frame, base, columns).items():
        flames.putpixel(p, c)
    img.alpha_composite(flames)
    if gem:
        pulse = 0.5 + 0.5 * math.sin(frame / FRAMES * 2 * math.pi)
        img.putpixel(gem, rgba(LAVA[3] if pulse > 0.5 else LAVA[2]))
    return img


def orange_glint(name):
    src = Image.open(os.path.join(VANILLA, name)).convert("RGB")
    out = Image.new("RGB", src.size)
    for y in range(src.height):
        for x in range(src.width):
            v = max(src.getpixel((x, y)))
            out.putpixel((x, y), (v, int(v * 0.42), int(v * 0.06)))
    os.makedirs(os.path.join(TEX, "misc"), exist_ok=True)
    out.save(os.path.join(TEX, "misc", name))


def main():
    item_dir = os.path.join(TEX, "item")
    os.makedirs(item_dir, exist_ok=True)
    for name, (columns, gem) in TOOLS.items():
        base = load_tool(name)
        w, h = base.size
        strip = Image.new("RGBA", (w, h * FRAMES))
        for f in range(FRAMES):
            strip.paste(render_frame(f, base, columns, gem), (0, f * h))
        strip.save(os.path.join(item_dir, name + ".png"))
        with open(os.path.join(item_dir, name + ".png.mcmeta"), "w") as fh:
            json.dump({"animation": {"frametime": 2}}, fh, indent=2)
            fh.write("\n")

    strip = Image.new("RGBA", (16, 16 * FRAMES))
    for f in range(FRAMES):
        strip.paste(phoenix_frame(f), (0, f * 16))
    strip.save(os.path.join(item_dir, "totem_of_undying.png"))
    with open(os.path.join(item_dir, "totem_of_undying.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2}}, fh, indent=2)
        fh.write("\n")

    orange_glint("enchanted_glint_item.png")
    orange_glint("enchanted_glint_armor.png")

    # pack.png: sword on an ember gradient.
    icon = Image.new("RGBA", (64, 64))
    for y in range(64):
        t = y / 63
        for x in range(64):
            icon.putpixel((x, y), (int(30 + 90 * t), int(8 + 20 * t), 10, 255))
    frame = render_frame(0, load_tool("netherite_sword"), *TOOLS["netherite_sword"])
    icon.alpha_composite(frame.resize((64, 64), Image.NEAREST))
    icon.save(os.path.join(ROOT, "pack.png"))


if __name__ == "__main__":
    main()
