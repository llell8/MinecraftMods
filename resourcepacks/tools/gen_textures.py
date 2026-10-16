"""Generates the Phoenix PvP textures from the vanilla 1.21.11 ones in tools/vanilla.

Run: python3 resourcepacks/tools/gen_textures.py
- netherite tools and weapons: vanilla texture with a blaze rod handle,
  animated fire on the head and an orange gem where the head meets the handle
- totem_of_undying.png: 32x32 dragon-rune style phoenix, serpent body with spread wings
- enchanted_glint_item.png / enchanted_glint_armor.png: orange glint
- pack.png
"""
import colorsys
import json
import math
import os
import random
from collections import Counter

from PIL import Image, ImageDraw

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


# Phoenix totem, dragon-rune style: a serpent body winding down the middle
# with bat-like wings spread wide. Drawn as smooth shapes at PHOENIX_SCALE x
# resolution, then reduced to 32x32 by majority colour per pixel.
PHOENIX_SIZE = 32
PHOENIX_SCALE = 12
# Same fire palette as the flames on the tools, plus a near-black ember for
# the legs.
PHOENIX_COLOURS = {
    "M": (110, 16, 6), "m": LAVA[0], "r": LAVA[1], "o": LAVA[2], "y": LAVA[3], "w": LAVA[4],
    "L": (60, 12, 8),
}
MEMBRANE_ALPHA = 230
# Heat shimmer: a band runs down the dragon, briefly heating hot colours.
SHIMMER = {"o": "y", "y": "w"}


def _bezier(p0, p1, p2, p3, n=60):
    pts = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        pts.append(tuple(u**3 * a + 3 * u * u * t * b + 3 * u * t * t * c + t**3 * e
                         for a, b, c, e in zip(p0, p1, p2, p3)))
    return pts


def draw_phoenix():
    """Returns {(x, y): colour key} for the 32x32 phoenix."""
    k = PHOENIX_SCALE
    keys = list(PHOENIX_COLOURS)
    layer = Image.new("L", (PHOENIX_SIZE * k, PHOENIX_SIZE * k), 0)
    d = ImageDraw.Draw(layer)

    def fill(key):
        return keys.index(key) + 1

    def stroke(pts, w0, w1, key):
        for i, (x, y) in enumerate(pts):
            w = (w0 + (w1 - w0) * i / (len(pts) - 1)) * k
            d.ellipse([x * k - w, y * k - w, x * k + w, y * k + w], fill=fill(key))

    def wing(mirror):
        def at(x, y):
            return ((PHOENIX_SIZE - 0.01 - x) * k if mirror else x * k, y * k)
        shoulder, wrist, peak, outer = (14.5, 9.5), (8, 6), (3, 3.5), (0.8, 6.5)
        root = (15, 15.5)  # where the wing's trailing edge meets the body
        points = [(0.8, 18), (5.5, 20.5), (10.5, 18.5)]  # hanging points
        # Solid membrane, its trailing edge scalloped gently between points.
        poly = [shoulder, wrist, peak, outer]
        prev = outer
        for tip in points + [root]:
            mx, my = (prev[0] + tip[0]) / 2, (prev[1] + tip[1]) / 2
            cx, cy = mx + (wrist[0] - mx) * 0.18, my + (wrist[1] - my) * 0.18
            for i in range(1, 11):
                t = i / 10
                poly.append(((1 - t)**2 * prev[0] + 2 * (1 - t) * t * cx + t * t * tip[0],
                             (1 - t)**2 * prev[1] + 2 * (1 - t) * t * cy + t * t * tip[1]))
            prev = tip
        d.polygon([at(*q) for q in poly], fill=fill("m"))
        d.polygon([at(*q) for q in [shoulder, (10, 8.5), (9, 16), root]], fill=fill("M"))
        # Bright leading edge, and finger bones out to the other points.
        for a, b, w in [(shoulder, wrist, 1.5), (wrist, peak, 1.3), (peak, outer, 1.1), (outer, points[0], 0.9)]:
            d.line([at(*a), at(*b)], fill=fill("y"), width=int(w * k))
        for tip in points[1:]:
            d.line([at(*wrist), at(*tip)], fill=fill("o"), width=k)

    wing(False)
    wing(True)
    # Serpent body: an S from the head down to the tail, belly highlight inside.
    s1 = _bezier((18, 4.5), (14, 6), (11.5, 8.5), (14.5, 11))
    s2 = _bezier((14.5, 11), (18, 13.5), (20, 14.5), (17, 17.5))
    s3 = _bezier((17, 17.5), (15.8, 18.6), (15, 18.6), (15, 19.2))
    stroke(s1, 1.6, 2.0, "r")
    stroke(s2, 2.0, 2.2, "r")
    stroke(s3, 2.2, 1.6, "r")
    stroke([(x + 0.6, y - 0.4) for x, y in s1], 0.7, 0.9, "o")
    stroke([(x + 0.6, y - 0.2) for x, y in s2], 0.9, 1.0, "o")
    stroke([(x + 0.5, y) for x, y in s3], 1.0, 0.7, "o")
    # Tail feathers fanning out from the end of the tail: long two-tone vanes
    # either side of a bright quill, with notches cut into the edges where
    # the barbs split.
    base = (15, 19)
    for tip, half_width in [((3.5, 28.5), 1.7), ((8.5, 31.5), 1.9), ((15, 32.5), 2.0),
                            ((21.5, 31.5), 1.9), ((26.5, 28.5), 1.7)]:
        dx, dy = tip[0] - base[0], tip[1] - base[1]
        length = math.hypot(dx, dy)
        ux, uy = dx / length, dy / length
        nx, ny = -uy, ux

        def at(t, side):
            # narrow quill near the body, widest about two thirds of the way out
            w = half_width * math.sin(math.pi * min(1, t ** 1.6)) * side
            return ((base[0] + dx * t + nx * w) * k, (base[1] + dy * t + ny * w) * k)

        ts = [i / 20 for i in range(21)]
        spine = [((base[0] + dx * t) * k, (base[1] + dy * t) * k) for t in ts]
        d.polygon(spine + [at(t, 1) for t in reversed(ts)], fill=fill("o"))
        d.polygon(spine + [at(t, -1) for t in reversed(ts)], fill=fill("r"))
        for t, side in [(0.55, 1), (0.7, -1)]:
            edge = at(t, side)
            inner = at(t + 0.08, side * 0.35)
            d.polygon([edge, inner, at(t + 0.12, side)], fill=0)
        d.line([spine[3], spine[-2]], fill=fill("y"), width=int(0.5 * k))
        d.line([spine[-6], spine[-1]], fill=fill("w"), width=int(0.6 * k))
    # Two dark legs hanging below the body over the tail, each ending in
    # three talons.
    for hip, knee, foot, toes in [((14, 16), (12, 19), (11, 22.5), [(9.2, 24), (10.6, 24.8), (12.2, 24.4)]),
                                  ((17, 16), (19, 19), (20, 22.5), [(18.8, 24.4), (20.4, 24.8), (21.8, 24)])]:
        d.line([(hip[0] * k, hip[1] * k), (knee[0] * k, knee[1] * k), (foot[0] * k, foot[1] * k)],
               fill=fill("L"), width=int(1.3 * k), joint="curve")
        for toe in toes:
            d.line([(foot[0] * k, foot[1] * k), (toe[0] * k, toe[1] * k)], fill=fill("L"), width=int(0.9 * k))
    # Head, snout, crest flames streaming back.
    d.ellipse([16 * k, 1.5 * k, 21.5 * k, 6 * k], fill=fill("o"))
    d.polygon([(20.5 * k, 2.4 * k), (25.5 * k, 3.6 * k), (20.5 * k, 5.4 * k)], fill=fill("o"))
    d.polygon([(17 * k, 3 * k), (13 * k, 0), (16.5 * k, 0.6 * k), (19.5 * k, 2 * k)], fill=fill("y"))
    d.polygon([(18 * k, 2 * k), (17.5 * k, 0), (20 * k, 1.6 * k)], fill=fill("w"))

    grid = {}
    for y in range(PHOENIX_SIZE):
        for x in range(PHOENIX_SIZE):
            cell = Counter(layer.getpixel((x * k + i, y * k + j)) for i in range(k) for j in range(k))
            if k * k - cell[0] < k * k * 0.45:
                continue
            del cell[0]
            grid[(x, y)] = keys[cell.most_common(1)[0][0] - 1]
    return grid


def phoenix_frame(frame, grid):
    img = Image.new("RGBA", (PHOENIX_SIZE, PHOENIX_SIZE), (0, 0, 0, 0))
    phase = frame / FRAMES * 2 * math.pi
    for (x, y), key in grid.items():
        if key in SHIMMER and math.sin(phase - y * 0.25) > 0.85:
            key = SHIMMER[key]
        img.putpixel((x, y), rgba(PHOENIX_COLOURS[key], MEMBRANE_ALPHA if key in "mM" else 255))
    # Embers rising off the wing peaks.
    rng = random.Random(7)
    for _ in range(4):
        sx = rng.choice([2, 3, 4, 27, 28, 29])
        t = (frame + rng.randint(0, FRAMES - 1)) % FRAMES
        p = (sx, 2 - t)
        if t < 3 and p[1] >= 0 and img.getpixel(p)[3] == 0:
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

    phoenix = draw_phoenix()
    size = PHOENIX_SIZE
    strip = Image.new("RGBA", (size, size * FRAMES))
    for f in range(FRAMES):
        strip.paste(phoenix_frame(f, phoenix), (0, f * size))
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
