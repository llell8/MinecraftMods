#!/usr/bin/env python3
"""Generates the Black Hole PvP resource pack.

Every texture is drawn from scratch here (no vanilla assets), in a black, white
and purple palette. Run `python3 generate.py` to rebuild `pack/` and
`BlackHolePvP.zip`.
"""
import json
import math
import random
import shutil
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent
PACK = ROOT / "pack"
TEX = PACK / "assets" / "minecraft" / "textures"

# Palette
VOID = (6, 3, 12, 255)
ABYSS = (18, 9, 32, 255)
DUSK = (42, 18, 72, 255)
PURPLE = (88, 33, 170, 255)
VIOLET = (139, 92, 246, 255)
LAVENDER = (196, 181, 253, 255)
WHITE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)
OUTLINE = (4, 2, 8, 255)

rng = random.Random(1337)


def save(img, rel):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def mcmeta(rel, data):
    path = TEX / (rel + ".mcmeta")
    path.write_text(json.dumps(data, indent=2) + "\n")


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def ramp(t, stops=(VOID, DUSK, PURPLE, VIOLET, LAVENDER, WHITE)):
    """Maps 0..1 onto the black -> purple -> white gradient."""
    t = max(0.0, min(1.0, t)) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    return lerp(stops[i], stops[i + 1], t - i)


def outline(img, color=OUTLINE):
    """Adds a 1px outline around every opaque pixel (4-neighbourhood)."""
    w, h = img.size
    px = img.load()
    solid = {(x, y) for y in range(h) for x in range(w) if px[x, y][3] > 0}
    for x, y in solid:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in solid:
                px[nx, ny] = color
    return img


def new(w=16, h=16):
    return Image.new("RGBA", (w, h), CLEAR)


# ---------------------------------------------------------------- weapons

def sword(edge, mid, back, guard, grip):
    """Diagonal sword; s = x + y runs across the blade, d = x - y along it."""
    img = new()
    px = img.load()
    for x in range(16):
        for y in range(16):
            s, d = x + y, x - y
            if -3 <= d <= 11 and s in (14, 15, 16):
                along = (d + 3) / 14
                if s == 14:
                    px[x, y] = edge
                elif s == 15:
                    px[x, y] = lerp(mid, edge, along * 0.6)
                else:
                    px[x, y] = back
            elif d == 13 and s == 15:
                px[x, y] = edge
            elif d == -5 and 11 <= s <= 19:
                px[x, y] = guard
            elif s == 15 and -11 <= d <= -7:
                px[x, y] = grip
    for p in ((1, 14), (0, 14), (1, 15)):
        px[p] = guard
    return outline(img)


def axe(edge, light, mid, dark, grip):
    img = new()
    px = img.load()
    shades = {7: edge, 8: edge, 9: light, 10: light, 11: mid, 12: mid, 13: dark, 14: dark}
    for x in range(16):
        for y in range(16):
            s, d = x + y, x - y
            if s == 15 and -11 <= d <= 11:
                px[x, y] = grip
            elif 9 <= s <= 14 and 1 <= d <= 11:
                px[x, y] = shades[s]
            elif 7 <= s <= 8 and 3 <= d <= 9:
                px[x, y] = shades[s]
    return outline(img)


# ---------------------------------------------------------------- orbs

def black_hole(size, ring=1.0, disk=True, glow=True, ss=8):
    """Anti-aliased black hole: event horizon, photon ring and accretion disk."""
    n = size * ss
    ys, xs = np.mgrid[0:n, 0:n]
    cx = cy = n / 2
    x = (xs + 0.5 - cx) / (n / 2)
    y = (ys + 0.5 - cy) / (n / 2)
    r = np.sqrt(x * x + y * y)
    rgba = np.zeros((n, n, 4), dtype=np.float64)

    def over(color, alpha):
        a = np.clip(alpha, 0, 1)[..., None]
        c = np.array(color, dtype=np.float64)
        rgba[..., :3] = rgba[..., :3] * (1 - a) + c[:3] * a
        rgba[..., 3:] = rgba[..., 3:] * (1 - a) + 255 * a

    horizon = 0.42 * ring
    if glow:
        over(VIOLET, np.clip(1 - (r - horizon) / 0.55, 0, 1) ** 2 * 0.55 * (r < 1))
    if disk:
        # Tilted accretion disk: an ellipse band, brighter on one side.
        ey = y / 0.28
        er = np.sqrt(x * x + ey * ey)
        band = np.clip(1 - np.abs(er - 0.72) / 0.22, 0, 1)
        side = 0.65 + 0.35 * x
        back = (y < 0) & (r < horizon * 1.15)
        band = np.where(back, 0, band)
        over(PURPLE, band * 0.9)
        over(LAVENDER, np.clip(band - 0.5, 0, 1) * 2 * side)
    over(WHITE, np.clip(1 - np.abs(r - horizon * 1.12) / 0.07, 0, 1))
    over(VOID, (r < horizon).astype(float))
    if disk:
        # Front half of the disk passes in front of the horizon.
        ey = y / 0.28
        er = np.sqrt(x * x + ey * ey)
        front = np.clip(1 - np.abs(er - 0.72) / 0.22, 0, 1) * (y > 0)
        over(VIOLET, front * 0.85)
        over(WHITE, np.clip(front - 0.6, 0, 1) * 2.5)
    img = Image.fromarray(rgba.astype(np.uint8), "RGBA")
    return img.resize((size, size), Image.LANCZOS)


def pearl():
    img = new()
    px = img.load()
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r <= 6.2:
                if r <= 3.0:
                    px[x, y] = VOID
                elif r <= 4.2:
                    px[x, y] = ABYSS
                else:
                    px[x, y] = lerp(DUSK, VIOLET, (r - 4.2) / 2)
    for p in ((5, 4), (4, 5), (5, 5)):
        px[p] = WHITE
    px[10, 10] = LAVENDER
    px[6, 6] = LAVENDER
    return outline(img)


def apple(body_dark, body, body_light, shine):
    img = new()
    px = img.load()
    for x in range(16):
        for y in range(16):
            # Two overlapping lobes make the apple shape.
            r1 = math.hypot(x - 5.6, y - 8.8)
            r2 = math.hypot(x - 9.4, y - 8.8)
            if (r1 <= 4.9 or r2 <= 4.9) and 3 <= y <= 14 and not (y == 4 and 7 <= x <= 8):
                light = 1 - (math.hypot(x - 5, y - 6) / 9)
                px[x, y] = lerp(body_dark, body_light, light) if light > 0.45 else lerp(body_dark, body, light * 2)
    for p in ((7, 1), (7, 2), (8, 3)):
        px[p] = ABYSS
    for p in ((9, 1), (10, 1), (9, 2), (11, 0)):
        px[p] = VIOLET
    for p in ((4, 6), (4, 7), (5, 5)):
        px[p] = shine
    return outline(img)


def totem():
    img = black_hole(16, ring=1.15, glow=False)
    px = img.load()
    for x in range(16):
        for y in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= 110 else CLEAR
    return outline(img)


# ---------------------------------------------------------------- bow / arrow

BOW_C = (1.5, 14.5)
BOW_R = 12.5


def bow(pull=None):
    img = new()
    px = img.load()
    for x in range(16):
        for y in range(16):
            dx, dy = x - BOW_C[0], BOW_C[1] - y
            r = math.hypot(dx, dy)
            if dx >= 0 and dy >= 0 and abs(r - BOW_R) <= 0.75:
                t = math.atan2(dy, dx) / (math.pi / 2)
                px[x, y] = VIOLET if 0.35 < t < 0.65 else lerp(DUSK, PURPLE, 1 - abs(t - 0.5) * 2)
    for p in ((10, 4), (11, 5), (4, 3), (13, 10)):
        px[p] = LAVENDER
    ends = ((1, 2), (14, 15))
    if pull is None:
        for i in range(1, 13):
            px[1 + i, 2 + i] = WHITE
    else:
        # String pulled back toward the bow's centre, arrow nocked.
        ax, ay = 7.5 - pull, 8.5 + pull
        for a, b in ((ends[0], (ax, ay)), ((ax, ay), ends[1])):
            steps = 24
            for k in range(steps + 1):
                t = k / steps
                px[int(round(a[0] + (b[0] - a[0]) * t)), int(round(a[1] + (b[1] - a[1]) * t))] = WHITE
        sx, sy = int(round(ax)), int(round(ay))
        for k in range(1, 8 + pull):
            x, y = sx + k, sy - k
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = LAVENDER if k < 6 + pull else WHITE
        px[sx, sy] = VIOLET
    return outline(img)


def arrow():
    img = new()
    px = img.load()
    for i in range(3, 12):
        px[i, 15 - i] = LAVENDER
    for p in ((12, 3), (13, 2), (12, 2), (13, 3), (11, 2), (13, 4)):
        px[p] = WHITE
    for p in ((2, 12), (3, 13), (2, 11), (4, 13), (1, 12), (3, 14), (2, 13)):
        px[p] = VIOLET
    px[1, 14] = PURPLE
    return outline(img)


# ---------------------------------------------------------------- blocks

def obsidian():
    img = new()
    px = img.load()
    for x in range(16):
        for y in range(16):
            v = rng.random()
            px[x, y] = VOID if v < 0.55 else ABYSS if v < 0.85 else DUSK
    # A few violet veins / specks.
    for _ in range(4):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randrange(2, 5)):
            px[x % 16, y % 16] = PURPLE
            x += rng.choice((-1, 0, 1))
            y += rng.choice((-1, 0, 1))
    for _ in range(3):
        px[rng.randrange(16), rng.randrange(16)] = VIOLET
    px[rng.randrange(16), rng.randrange(16)] = LAVENDER
    return img


def crying_obsidian():
    img = obsidian()
    px = img.load()
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        for k in range(rng.randrange(1, 4)):
            px[x, (y + k) % 16] = VIOLET if k else LAVENDER
    return img


def cobweb():
    img = new()
    px = img.load()
    c = 7.5
    for x in range(16):
        for y in range(16):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            spoke = abs(dx) < 0.6 or abs(dy) < 0.6 or abs(abs(dx) - abs(dy)) < 0.6
            ring = any(abs(r - k) < 0.45 for k in (2.5, 5, 7.3))
            if r <= 8 and (spoke or ring):
                px[x, y] = (255, 255, 255, 200) if spoke else (196, 181, 253, 170)
    return img


def fire(frames=16):
    """Low, purple 'void fire': only the bottom few rows burn."""
    img = new(16, 16 * frames)
    px = img.load()
    for f in range(frames):
        phase = f / frames * math.tau
        for x in range(16):
            h = 4.5 + 1.8 * math.sin(x * 0.9 + phase) + 1.2 * math.sin(x * 2.3 - phase * 2)
            for y in range(16):
                depth = 15 - y
                if depth < h:
                    t = depth / max(h, 1)
                    flick = 0.1 * math.sin(x * 3 + y * 2 + phase * 3)
                    c = ramp(0.95 - t * 0.8 + flick, (DUSK, PURPLE, VIOLET, LAVENDER, WHITE))
                    if t > 0.7:
                        c = c[:3] + (170,)
                    px[x, f * 16 + y] = c
    return img


# ---------------------------------------------------------------- entities

def face_box(img, u, v, w, h, d, paint):
    """Paints each face of a model box laid out with the standard cube UV."""
    faces = {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }
    for name, (x, y, fw, fh) in faces.items():
        paint(img, name, x, y, fw, fh)


def plate(img, name, x, y, w, h, base=ABYSS, trim=PURPLE, hi=VIOLET):
    px = img.load()
    for i in range(w):
        for j in range(h):
            edge = i == 0 or j == 0 or i == w - 1 or j == h - 1
            noise = rng.random()
            c = trim if edge else (DUSK if noise < 0.18 else base)
            if edge and (i + j) % 5 == 0:
                c = hi
            px[x + i, y + j] = c


def armor_layer(leggings=False):
    img = new(64, 32)
    if not leggings:
        face_box(img, 0, 0, 8, 8, 8, plate)
        # Helmet visor: a white-rimmed black slit.
        px = img.load()
        for i in range(1, 7):
            px[8 + i, 8 + 3] = WHITE
            px[8 + i, 8 + 4] = VOID
        face_box(img, 16, 16, 8, 12, 4, plate)
        # Chest emblem: small black hole on the front plate.
        emblem = black_hole(6, disk=False, glow=False)
        img.alpha_composite(emblem, (21, 22))
        face_box(img, 40, 16, 4, 12, 4, plate)
        face_box(img, 0, 16, 4, 12, 4, plate)  # boots
    else:
        face_box(img, 16, 16, 8, 12, 4, plate)
        face_box(img, 0, 16, 4, 12, 4, plate)
    return img


def end_crystal():
    img = new(64, 32)

    def glass(img, name, x, y, w, h):
        px = img.load()
        for i in range(w):
            for j in range(h):
                if i in (0, w - 1) or j in (0, h - 1):
                    px[x + i, y + j] = LAVENDER if (i + j) % 2 else WHITE
                elif i in (1, w - 2) or j in (1, h - 2):
                    px[x + i, y + j] = VIOLET

    def core(img, name, x, y, w, h):
        hole = black_hole(w, disk=False, ss=6)
        tile = Image.new("RGBA", (w, h), ABYSS)
        tile.alpha_composite(hole)
        img.paste(tile, (x, y))

    def base(img, name, x, y, w, h):
        plate(img, name, x, y, w, h, base=VOID, trim=DUSK, hi=PURPLE)

    face_box(img, 0, 0, 8, 8, 8, glass)
    face_box(img, 32, 0, 8, 8, 8, core)
    face_box(img, 0, 16, 12, 4, 12, base)
    return img


def shield():
    img = new(64, 64)
    px = img.load()
    # Plate box (texOffs 0,0; 12x22x1) and handle (texOffs 26,0; 2x6x6).
    face_box(img, 0, 0, 12, 22, 1, lambda im, n, x, y, w, h: plate(im, n, x, y, w, h, base=VOID, trim=PURPLE))
    face_box(img, 26, 0, 2, 6, 6, lambda im, n, x, y, w, h: plate(im, n, x, y, w, h, base=DUSK, trim=ABYSS))
    hole = black_hole(10, ss=8)
    img.alpha_composite(hole, (2, 7))
    for i in range(1, 13):
        px[i, 1] = WHITE
        px[i, 22] = LAVENDER
    return img


# ---------------------------------------------------------------- environment

def sun():
    # The sun is drawn additively, so black is invisible: what shows is the
    # glowing photon ring and disk around an empty dark centre.
    return black_hole(64, ring=0.9)


def end_sky():
    img = Image.new("RGBA", (128, 128), (14, 6, 26, 255))
    px = img.load()
    for _ in range(140):
        x, y = rng.randrange(128), rng.randrange(128)
        px[x, y] = rng.choice((LAVENDER, WHITE, VIOLET, PURPLE))
    for _ in range(600):
        x, y = rng.randrange(128), rng.randrange(128)
        px[x, y] = rng.choice((DUSK, (30, 12, 52, 255)))
    return img


def glint():
    n = 128
    ys, xs = np.mgrid[0:n, 0:n]
    v = (np.sin((xs + ys) / n * math.tau * 2) + 1) / 2
    v = v ** 3 * 0.8 + ((np.sin(xs / n * math.tau * 3 - ys / n * math.tau) + 1) / 2) ** 6 * 0.4
    v = np.clip(v, 0, 1)
    rgb = np.stack([110 + 145 * v, 40 + 160 * v ** 1.5, 200 + 55 * v], -1) * v[..., None]
    a = np.full((n, n, 1), 255.0)
    return Image.fromarray(np.concatenate([rgb, a], -1).astype(np.uint8), "RGBA")


# ---------------------------------------------------------------- gui

def heart(fill, half=False, blink=False):
    shape = [
        ".XX.X.XX.",
        "XooXoXooX",
        "XowoooooX",
        "XoooooooX",
        ".XoooooX.",
        "..XoooX..",
        "...XoX...",
        "....X....",
        ".........",
    ]
    img = new(9, 9)
    px = img.load()
    edge = WHITE if blink else OUTLINE
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == "X":
                if fill is not None and half and x > 4:
                    continue
                px[x, y] = edge
            elif ch in "ow":
                if fill is None:
                    px[x, y] = (24, 12, 40, 255) if not blink else (60, 30, 90, 255)
                elif half and x > 4:
                    continue
                else:
                    px[x, y] = WHITE if ch == "w" else lerp(fill, VOID, y / 14)
    return img


def crosshair():
    img = new(15, 15)
    px = img.load()
    for x in range(15):
        for y in range(15):
            r = math.hypot(x - 7, y - 7)
            if abs(r - 4.2) < 0.55:
                px[x, y] = WHITE
    px[7, 7] = WHITE
    return img


def hotbar():
    img = Image.new("RGBA", (182, 22), (8, 4, 16, 200))
    px = img.load()
    for x in range(182):
        px[x, 0] = PURPLE
        px[x, 21] = PURPLE
    for y in range(22):
        px[0, y] = PURPLE
        px[181, y] = PURPLE
    for i in range(9):
        x0 = 1 + i * 20
        for x in range(x0 + 1, x0 + 19):
            px[x, 1] = DUSK
            px[x, 20] = DUSK
        for y in range(1, 21):
            px[x0 + 1, y] = DUSK
            px[x0 + 18, y] = DUSK
        if i:
            for y in range(1, 21):
                px[x0, y] = (20, 10, 34, 255)
    return img


def hotbar_selection():
    img = new(24, 23)
    px = img.load()
    for x in range(24):
        for y in range(23):
            border = x in (0, 23) or y in (0, 22)
            inner = x in (1, 22) or y in (1, 21)
            if border:
                px[x, y] = WHITE
            elif inner:
                px[x, y] = VIOLET
    return img


def pack_icon():
    img = Image.new("RGBA", (128, 128), VOID)
    sky = end_sky().resize((128, 128))
    img.alpha_composite(sky)
    img.alpha_composite(black_hole(128, ss=4))
    return img


# ---------------------------------------------------------------- build

def build():
    if PACK.exists():
        shutil.rmtree(PACK)
    PACK.mkdir()

    (PACK / "pack.mcmeta").write_text(json.dumps({
        "pack": {
            "description": "§5§lBlack Hole PvP§r\n§7Black, white & purple",
            "pack_format": 46,
            "supported_formats": {"min_inclusive": 46, "max_inclusive": 99},
            "min_format": 46,
            "max_format": 99,
        }
    }, indent=2) + "\n")
    pack_icon().save(PACK / "pack.png")

    # Swords and axes: diamond = purple, netherite = black, iron = white.
    save(sword(WHITE, VIOLET, PURPLE, LAVENDER, DUSK), "item/diamond_sword.png")
    save(sword(VIOLET, ABYSS, VOID, PURPLE, DUSK), "item/netherite_sword.png")
    save(sword(WHITE, LAVENDER, (150, 140, 170, 255), VIOLET, DUSK), "item/iron_sword.png")
    save(axe(WHITE, LAVENDER, VIOLET, PURPLE, DUSK), "item/diamond_axe.png")
    save(axe(VIOLET, DUSK, ABYSS, VOID, PURPLE), "item/netherite_axe.png")
    save(axe(WHITE, LAVENDER, (150, 140, 170, 255), DUSK, ABYSS), "item/iron_axe.png")

    save(pearl(), "item/ender_pearl.png")
    save(apple(DUSK, PURPLE, VIOLET, WHITE), "item/golden_apple.png")
    save(apple(PURPLE, VIOLET, LAVENDER, WHITE), "item/enchanted_golden_apple.png")
    save(totem(), "item/totem_of_undying.png")
    save(bow(), "item/bow.png")
    for i in range(3):
        save(bow(pull=i + 1), f"item/bow_pulling_{i}.png")
    save(arrow(), "item/arrow.png")

    save(obsidian(), "block/obsidian.png")
    save(crying_obsidian(), "block/crying_obsidian.png")
    save(cobweb(), "block/cobweb.png")
    save(cobweb(), "item/cobweb.png")
    for i in range(2):
        save(fire(), f"block/fire_{i}.png")
        mcmeta(f"block/fire_{i}.png", {"animation": {"frametime": 2}})

    save(armor_layer(), "entity/equipment/humanoid/diamond.png")
    save(armor_layer(leggings=True), "entity/equipment/humanoid_leggings/diamond.png")
    save(end_crystal(), "entity/end_crystal/end_crystal.png")
    save(shield(), "entity/shield_base_nopattern.png")

    save(sun(), "environment/sun.png")
    save(end_sky(), "environment/end_sky.png")
    save(glint(), "misc/enchanted_glint_item.png")
    save(glint(), "misc/enchanted_glint_armor.png")

    save(crosshair(), "gui/sprites/hud/crosshair.png")
    save(hotbar(), "gui/sprites/hud/hotbar.png")
    save(hotbar_selection(), "gui/sprites/hud/hotbar_selection.png")
    for prefix in ("", "hardcore_"):
        save(heart(VIOLET), f"gui/sprites/hud/heart/{prefix}full.png")
        save(heart(VIOLET, half=True), f"gui/sprites/hud/heart/{prefix}half.png")
        save(heart(LAVENDER, blink=True), f"gui/sprites/hud/heart/{prefix}full_blinking.png")
        save(heart(LAVENDER, half=True, blink=True), f"gui/sprites/hud/heart/{prefix}half_blinking.png")
    save(heart(None), "gui/sprites/hud/heart/container.png")
    save(heart(None, blink=True), "gui/sprites/hud/heart/container_blinking.png")

    zip_path = ROOT / "BlackHolePvP.zip"
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for f in sorted(PACK.rglob("*")):
            if f.is_file():
                zf.write(f, f.relative_to(PACK).as_posix())
    print(f"Wrote {zip_path}")


if __name__ == "__main__":
    build()
