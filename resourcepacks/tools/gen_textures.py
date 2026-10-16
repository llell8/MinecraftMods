"""Generates the Phoenix PvP textures from the vanilla 1.21.11 ones in tools/vanilla.

Run: python3 resourcepacks/tools/gen_textures.py
- netherite tools and weapons: vanilla texture with animated fire on the head
  and an orange gem where the head meets the handle
- enchanted_glint_item.png / enchanted_glint_armor.png: orange glint
- pack.png
"""
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


def render_frame(frame, base, columns, gem):
    img = base.copy()
    flames = Image.new("RGBA", base.size, (0, 0, 0, 0))
    for p, c in fire(frame, base, columns).items():
        flames.putpixel(p, c)
    img.alpha_composite(flames)
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
        base = Image.open(os.path.join(VANILLA, name + ".png")).convert("RGBA")
        w, h = base.size
        strip = Image.new("RGBA", (w, h * FRAMES))
        for f in range(FRAMES):
            strip.paste(render_frame(f, base, columns, gem), (0, f * h))
        strip.save(os.path.join(item_dir, name + ".png"))
        with open(os.path.join(item_dir, name + ".png.mcmeta"), "w") as fh:
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
    sword = Image.open(os.path.join(VANILLA, "netherite_sword.png")).convert("RGBA")
    frame = render_frame(0, sword, *TOOLS["netherite_sword"])
    icon.alpha_composite(frame.resize((64, 64), Image.NEAREST))
    icon.save(os.path.join(ROOT, "pack.png"))


if __name__ == "__main__":
    main()
