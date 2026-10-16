"""Generates the animated fiery netherite sword for the Phoenix PvP pack.

Run: python3 resourcepacks/tools/gen_fiery_netherite_sword.py
Writes netherite_sword.png (16x16 frame strip) + .mcmeta, and pack.png.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "PhoenixPvP")
ITEM_DIR = os.path.join(ROOT, "assets", "minecraft", "textures", "item")
FRAMES = 8
S = 16

OUTLINE = (22, 14, 16, 255)
NETH_DARK = (49, 40, 44, 255)
NETH_MID = (74, 62, 66, 255)
NETH_LIGHT = (104, 90, 92, 255)
LAVA = [(140, 24, 6), (196, 52, 8), (240, 110, 16), (255, 170, 40), (255, 228, 120)]
GRIP = (70, 26, 18, 255)
GRIP_LIGHT = (110, 40, 22, 255)


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def sword_pixels(frame):
    """Returns {(x, y): colour} for the sword body (no outline, no fire)."""
    px = {}
    pulse = 0.5 + 0.5 * math.sin(frame / FRAMES * 2 * math.pi)
    # Molten cutting edge along x+y=14, hotter toward the tip.
    for x in range(6, 14):
        t = (x - 6) / 7
        heat = min(len(LAVA) - 1, int(t * 3 + pulse * 0.9))
        px[(x, 14 - x)] = rgba(LAVA[heat])
    # Netherite body: light bevel on x+y=15, dark on x+y=16, with a
    # glowing crack that wanders along the dark side.
    for x in range(6, 15):
        px[(x, 15 - x)] = NETH_LIGHT
        crack = x == 8 + (frame // 2) % 2 * 3 and pulse > 0.3
        px[(x, 16 - x)] = rgba(LAVA[1]) if crack else NETH_DARK
    px[(14, 1)] = rgba(LAVA[3])
    # Cross-guard along x-y=-5, gem in the middle.
    for x, y in [(3, 8), (4, 9), (6, 11), (7, 12)]:
        px[(x, y)] = NETH_LIGHT if x < 5 else NETH_MID
    px[(5, 10)] = rgba(LAVA[3 + (1 if pulse > 0.6 else 0)])
    # Grip.
    px[(4, 11)] = GRIP_LIGHT
    px[(3, 12)] = GRIP
    # Ember pommel.
    px[(2, 13)] = rgba(LAVA[2])
    px[(1, 13)] = rgba(LAVA[1])
    px[(2, 14)] = rgba(LAVA[1])
    px[(1, 14)] = rgba(LAVA[0])
    return px


def outline(body):
    out = set()
    for (x, y) in body:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if p not in body and 0 <= p[0] < S and 0 <= p[1] < S:
                out.add(p)
    return out


def fire(frame, taken, rng_seed=1337):
    """Flames licking up off the blade's upper edge, plus drifting embers."""
    px = {}
    phase = frame / FRAMES * 2 * math.pi
    for x in range(5, 15):
        top = 12 - x  # first empty pixel above the outline at x+y=13
        h = 1.6 + 1.2 * math.sin(phase + x * 1.9) + 0.6 * math.sin(phase * 2 + x * 0.7)
        h = max(0, int(round(h)))
        for k in range(h):
            p = (x, top - k)
            if p in taken or p[1] < 0:
                continue
            frac = k / max(1, h - 1) if h > 1 else 0
            if frac < 0.34:
                c, a = LAVA[3], 235
            elif frac < 0.67:
                c, a = LAVA[2], 200
            else:
                c, a = LAVA[1], 150
            px[p] = rgba(c, a)
    # Embers rise diagonally and loop every FRAMES.
    rng = random.Random(rng_seed)
    for _ in range(4):
        sx, sy = rng.randint(6, 13), rng.randint(3, 9)
        off = rng.randint(0, FRAMES - 1)
        t = (frame + off) % FRAMES
        p = (sx - t // 3, sy - t)
        if p[1] >= 0 and p[0] >= 0 and p not in taken and p not in px and t < FRAMES - 2:
            px[p] = rgba(LAVA[4] if t < 3 else LAVA[2], 230 - t * 25)
    return px


def render_frame(frame):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    body = sword_pixels(frame)
    edge = outline(body)
    taken = set(body) | edge
    for p, c in fire(frame, taken).items():
        img.putpixel(p, c)
    for p in edge:
        img.putpixel(p, OUTLINE)
    for p, c in body.items():
        img.putpixel(p, c)
    return img


def main():
    os.makedirs(ITEM_DIR, exist_ok=True)
    strip = Image.new("RGBA", (S, S * FRAMES))
    for f in range(FRAMES):
        strip.paste(render_frame(f), (0, f * S))
    strip.save(os.path.join(ITEM_DIR, "netherite_sword.png"))
    with open(os.path.join(ITEM_DIR, "netherite_sword.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2}}, fh, indent=2)
        fh.write("\n")

    # pack.png: sword on an ember gradient.
    icon = Image.new("RGBA", (64, 64))
    for y in range(64):
        for x in range(64):
            t = y / 63
            icon.putpixel((x, y), (int(30 + 90 * t), int(8 + 20 * t), 10, 255))
    big = render_frame(0).resize((64, 64), Image.NEAREST)
    icon.alpha_composite(big)
    icon.save(os.path.join(ROOT, "pack.png"))


if __name__ == "__main__":
    main()
