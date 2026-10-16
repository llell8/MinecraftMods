"""Generates the Phoenix PvP textures from the vanilla 1.21.11 ones in tools/vanilla.

Run: python3 resourcepacks/tools/gen_textures.py
- netherite tools and weapons: vanilla texture with a blaze rod handle,
  animated fire on the head and an orange gem where the head meets the handle
- golden_apple.png / enchanted_golden_apple.png: molten gold gap with a stem
  flame, red-hot egap with a crown of flames (own model + texture)
- gui/sprites/hud: animated flaming hearts, phoenix feathers for the hunger bar
- appleskin/textures/icons.png: AppleSkin saturation outline traced around the feathers
- totem_of_undying.png: dragon-rune style phoenix, serpent body with spread wings
- mace.png: netherite head with a molten crack, blaze rod handle, flames
- ender pearl, cobweb, wind charge (item + projectile + burst particles):
  recoloured orange; blue soul-fire comets orbit the pearl and wind charge items
- netherite armor icons: flame tongues rising off the top, armor left visible
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
ROD_TONE = (0.85, 0.8, -0.02)
# The black outline around the handle becomes the blaze rod's dark edge,
# and the end cap on the bottom row its darker tip.
ROD_EDGE = (174, 60, 0)
ROD_END = (149, 51, 0)
OUTLINE = {(35, 16, 18), (27, 20, 21), (22, 8, 20)}
# Mace: steel head -> netherite with its bright highlight streak glowing as a
# molten crack; breeze rod handle -> the copper blaze rod.
MACE_COLOURS = {
    (30, 34, 41): (35, 16, 18), (46, 51, 59): (50, 39, 39), (66, 71, 82): (68, 52, 60),
    (85, 92, 105): (79, 60, 62), (102, 108, 122): (93, 86, 93), (124, 132, 145): (112, 103, 112),
    (153, 161, 174): (134, 123, 134), (191, 206, 231): LAVA[3],
}
MACE_ROD = {  # breeze rod colours -> blaze rod colours (toned like the other handles)
    (35, 66, 105): (174, 60, 0), (80, 99, 144): (179, 107, 25),
    (122, 130, 196): (216, 174, 38), (181, 165, 202): (255, 243, 45),
}
MACE = (range(5, 16), (8, 7))  # flame columns, gem where the handle meets the head
# Orange recolours by gradient map: each pixel's brightness (relative to the
# texture's own darkest/brightest) picks a colour along EMBER_RAMP, so the
# shading survives. (vanilla file, pack path, ramp start, ramp end)
EMBER_RAMP = [(50, 10, 4), LAVA[0], LAVA[1], LAVA[2], LAVA[3], LAVA[4]]
ORANGE_RECOLOURS = [
    ("cobweb.png", "block/cobweb.png", 0.55, 1.0),  # stays bright so webs are easy to spot
    ("wind_charge_entity.png", "entity/projectiles/wind_charge.png", 0.35, 1.0),
]
# Orange items that burn: recoloured, then two comets of blue soul fire
# orbiting them in the empty ring of pixels around their edge. The blue
# contrasts with the orange item, so the orbit reads clearly and the item
# stays visible. Head to tail:
SOUL_FIRE = [(225, 255, 255), (110, 225, 240), (45, 170, 205), (25, 105, 150)]
# (vanilla file, pack path, ramp start, ramp end)
BURNING_RECOLOURS = [
    ("ender_pearl.png", "item/ender_pearl.png", 0.15, 1.0),
    ("wind_charge.png", "item/wind_charge.png", 0.1, 0.95),
]
# Wind charge burst particles: their own textures, mapped by absolute
# brightness (frames are only one or two greys each).
GUST_PARTICLES = [f"gust_{i}" for i in range(12)] + [f"small_gust_{i}" for i in range(7)]
# Netherite armor icons get flame tongues rising off their top edge, never
# covering the armor.
ARMOR_ITEMS = ["netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"]
# HUD: fiery hearts (vanilla shape, fire gradient top to bottom, little flame
# licks above the lobes) and phoenix feathers instead of drumsticks.
HEART_ROWS = {1: LAVA[3], 2: LAVA[3], 3: (255, 150, 30), 4: LAVA[2], 5: (220, 80, 14), 6: LAVA[1], 7: LAVA[1]}
HEART_COLOURS = {
    (255, 200, 200): LAVA[4],   # highlight
    (187, 19, 19): LAVA[0],     # lower edge
    (96, 7, 7): (110, 16, 6),   # hardcore pattern
}
HEART_FLAME_COLUMNS = [2, 3, 5, 6]
# Brighter step for each heart colour, used by the heat shimmer.
HEART_HOTTER = {LAVA[1]: (220, 80, 14), (220, 80, 14): LAVA[2], LAVA[2]: (255, 150, 30),
                (255, 150, 30): LAVA[3], LAVA[3]: LAVA[4]}
HEARTS = ["full", "half", "hardcore_full", "hardcore_half"]
FEATHER = [
    ".........",
    ".wy......",
    ".yyo.....",
    ".oyyor...",
    "..oyyor..",
    "...ryyr..",
    "....rrq..",
    ".......q.",
    ".........",
]
FEATHER_COLOURS = {
    "w": LAVA[4], "y": LAVA[3], "o": LAVA[2], "r": LAVA[1], "q": (236, 200, 150),
}
FEATHER_HUNGER_TINT = (110, 140, 60)
# AppleSkin's saturation overlay (assets/appleskin/textures/icons.png, row 0:
# four 9x9 stages from <=25% to full) redrawn as a gold outline traced around
# the feather, growing from the quill end toward the tip.
SATURATION_GOLD = (255, 226, 110)
SATURATION_STAGES = [0.25, 0.5, 0.75, 1.0]
# Golden apple recolours (vanilla colour -> new). The gap goes molten gold with
# a small flame at the stem; the egap gets its own red-hot texture with a full
# crown of flames so the two are easy to tell apart.
APPLE_STEM = [(9, 1), (8, 2), (9, 2), (8, 3)]
GAP_COLOURS = {
    (117, 40, 2): (130, 36, 4), (178, 100, 17): (190, 90, 14), (84, 36, 9): (90, 24, 6),
    (219, 162, 19): (232, 150, 20), (254, 255, 230): (255, 250, 215), (236, 203, 69): (245, 180, 50),
    (234, 238, 87): (255, 214, 70), (246, 248, 146): (255, 236, 140), (84, 18, 9): (90, 16, 6),
}
EGAP_COLOURS = {
    (117, 40, 2): (110, 16, 6), (178, 100, 17): (160, 30, 6), (84, 36, 9): (80, 12, 6),
    (219, 162, 19): (214, 64, 10), (254, 255, 230): (255, 240, 170), (236, 203, 69): LAVA[2],
    (234, 238, 87): (255, 150, 30), (246, 248, 146): (255, 200, 80), (84, 18, 9): (70, 10, 6),
}
APPLES = {
    # texture name: (colours, flame columns)
    "golden_apple": (GAP_COLOURS, [8, 9]),
    "enchanted_golden_apple": (EGAP_COLOURS, range(3, 13)),
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
                c = LAVA[3]
            elif frac < 0.67:
                c = LAVA[2]
            else:
                c = LAVA[1]
            px[(x, y)] = rgba(c)
    # Embers rise up-left off the head and loop every FRAMES.
    rng = random.Random(1337)
    for _ in range(4):
        sx = rng.choice(list(columns))
        sy = next(y for y in range(base.height) if base.getpixel((sx, y))[3]) - rng.randint(1, 2)
        t = (frame + rng.randint(0, FRAMES - 1)) % FRAMES
        p = (sx - t // 3, sy - t)
        if t < FRAMES - 2 and p[0] >= 0 and p[1] >= 0 and p not in px and base.getpixel(p)[3] == 0:
            px[p] = rgba(LAVA[4] if t < 3 else LAVA[2])
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


# Phoenix totem, dragon-rune style, hand-placed at 16x16: a serpent body
# winding down the middle, bat-like wings spread wide with hanging points,
# two legs, and a fan of tail feathers. Same fire palette as the tools.
PHOENIX_SIZE = 16
PHOENIX_COLOURS = {
    "M": (110, 16, 6), "m": LAVA[0], "r": LAVA[1], "o": LAVA[2], "y": LAVA[3], "w": LAVA[4],
    "L": (60, 12, 8),
}
PHOENIX = [
    "......ywy.......",  # crest
    "........ooooy...",  # head + snout
    ".yw.....ro...wy.",  # wing peaks with white-hot highlights, neck
    "yrryyy.ro.yyyrry",  # leading edges, bright membrane beneath
    "yrroryro.yyrorry",
    "ymmomoMroMomommy",  # three bones per wing; membrane darkens downward
    "ymmomoMMroomommy",
    "yMmoMoMMMroMomMy",
    "yM.oMoMMrooMo.My",  # scallop notches
    "y..o.yLroLy.o..y",  # hanging points, legs
    "...y.LroyrL.y...",
    "....LLroyrLL....",  # feet
    "...ro.ooyo.or...",  # tail feathers
    "..ro.rooyor.or..",
    ".w..ro.oy.or..w.",
    "....y..ww..y....",
]
# Heat shimmer: a band runs down the dragon, briefly heating hot colours.
SHIMMER = {"o": "y", "y": "w"}


def draw_phoenix():
    """Returns {(x, y): colour key} for the phoenix."""
    return {(x, y): ch for y, row in enumerate(PHOENIX) for x, ch in enumerate(row) if ch != "."}


def phoenix_frame(frame, grid):
    img = Image.new("RGBA", (PHOENIX_SIZE, PHOENIX_SIZE), (0, 0, 0, 0))
    phase = frame / FRAMES * 2 * math.pi
    for (x, y), key in grid.items():
        if key in SHIMMER and math.sin(phase - y * 8 / PHOENIX_SIZE) > 0.85:
            key = SHIMMER[key]
        img.putpixel((x, y), rgba(PHOENIX_COLOURS[key]))
    # Embers rising off the wing peaks.
    rng = random.Random(7)
    for _ in range(4):
        sx = rng.choice([1, 2, PHOENIX_SIZE - 3, PHOENIX_SIZE - 2])
        t = (frame + rng.randint(0, FRAMES - 1)) % FRAMES
        p = (sx, 2 - t)
        if t < 3 and p[1] >= 0 and img.getpixel(p)[3] == 0:
            img.putpixel(p, rgba(LAVA[4] if t < 2 else LAVA[3]))
    return img


def engulf_frame(frame, base, crown=None):
    """A few flame tongues rising off the item's top edge without covering it:
    smooth waves so neighbouring columns form tongues instead of speckles.
    With crown=n, only columns whose top is within n rows of the highest point
    burn, so round items get a crown of fire rather than flames off their sides."""
    img = base.copy()
    phase = frame / FRAMES * 2 * math.pi
    highest = min(y for y in range(base.height) for x in range(base.width) if base.getpixel((x, y))[3])
    for x in range(base.width):
        tops = [y for y in range(base.height) if base.getpixel((x, y))[3]]
        if not tops or (crown is not None and tops[0] > highest + crown):
            continue
        h = 1.2 * math.sin(phase + x * 0.9) + 0.5 * math.sin(phase * 2 - x * 0.6) + 1.0
        for k in range(max(0, min(3, round(h)))):
            y = tops[0] - 1 - k
            if y < 0:
                break
            img.putpixel((x, y), rgba((LAVA[3], LAVA[2], LAVA[1])[k]))
    return img


def ember_colour(t):
    t = max(0.0, min(1.0, t))
    i = min(len(EMBER_RAMP) - 2, int(t * (len(EMBER_RAMP) - 1)))
    f = t * (len(EMBER_RAMP) - 1) - i
    return tuple(round(a + (b - a) * f) for a, b in zip(EMBER_RAMP[i], EMBER_RAMP[i + 1]))


def ember_levels(img, lo, hi, absolute=False):
    """{(x, y): position along EMBER_RAMP} from each pixel's brightness."""
    lum = {}
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                lum[(x, y)] = 0.3 * r + 0.59 * g + 0.11 * b
    darkest, brightest = (0, 255) if absolute else (min(lum.values()), max(lum.values()))
    return {p: lo + (hi - lo) * (l - darkest) / max(1, brightest - darkest) for p, l in lum.items()}


def save_texture(img, dest):
    path = os.path.join(TEX, dest)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def ember_recolour(name, dest, lo, hi, absolute=False):
    img = Image.open(os.path.join(VANILLA, name)).convert("RGBA")
    for p, t in ember_levels(img, lo, hi, absolute).items():
        img.putpixel(p, ember_colour(t) + (img.getpixel(p)[3],))
    save_texture(img, dest)


def burning_recolour(name, dest, lo, hi):
    base = Image.open(os.path.join(VANILLA, name)).convert("RGBA")
    for p, t in ember_levels(base, lo, hi).items():
        base.putpixel(p, ember_colour(t) + (base.getpixel(p)[3],))
    strip = Image.new("RGBA", (base.width, base.height * FRAMES))
    for f in range(FRAMES):
        strip.paste(orbit_frame(f, base), (0, f * base.height))
    save_texture(strip, dest)
    with open(os.path.join(TEX, dest) + ".mcmeta", "w") as fh:
        json.dump({"animation": {"frametime": 2}}, fh, indent=2)
        fh.write("\n")


def orbit_frame(frame, base):
    """Two soul-fire comets (bright head, fading tail) circling the item in the
    empty pixels touching its edge. Half a turn per loop; the comets are half
    a turn apart, so the animation loops seamlessly."""
    img = base.copy()
    w, h = base.size
    solid = {(x, y) for y in range(h) for x in range(w) if base.getpixel((x, y))[3]}
    cx = sum(x for x, _ in solid) / len(solid)
    cy = sum(y for _, y in solid) / len(solid)
    head = frame / FRAMES * math.pi
    for y in range(h):
        for x in range(w):
            if (x, y) in solid or not any((x + dx, y + dy) in solid for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                continue
            angle = math.atan2(y - cy, x - cx)
            for comet in (head, head + math.pi):
                behind = (comet - angle) % (2 * math.pi)  # how far behind the head
                if behind < 1.8:
                    colour = SOUL_FIRE[0 if behind < 0.3 else 1 if behind < 0.8 else 2 if behind < 1.3 else 3]
                    img.putpixel((x, y), rgba(colour))
    return img


def load_mace():
    img = Image.open(os.path.join(VANILLA, "mace.png")).convert("RGBA")
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a and (r, g, b) in MACE_ROD:
                img.putpixel((x, y), rgba(tone(MACE_ROD[(r, g, b)], *ROD_TONE), a))
            elif a and (r, g, b) in MACE_COLOURS:
                img.putpixel((x, y), rgba(MACE_COLOURS[(r, g, b)], a))
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


def load_apple(colours):
    img = Image.open(os.path.join(VANILLA, "golden_apple.png")).convert("RGBA")
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a and (r, g, b) in colours:
                img.putpixel((x, y), rgba(colours[(r, g, b)], a))
    for p in APPLE_STEM:  # the stem burns away into the flame
        img.putpixel(p, (0, 0, 0, 0))
    return img


def fiery_heart(name):
    img = Image.open(os.path.join(VANILLA, "hud", "heart_" + name + ".png")).convert("RGBA")
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if not a:
                continue
            if (r, g, b) in HEART_COLOURS:
                img.putpixel((x, y), rgba(HEART_COLOURS[(r, g, b)]))
            elif (r, g, b) == (255, 19, 19):
                img.putpixel((x, y), rgba(HEART_ROWS[y]))
    return img


def heart_frame(base, frame):
    """Flame tongues flickering off the top of the heart and a heat shimmer
    rising through it."""
    img = base.copy()
    phase = frame / FRAMES * 2 * math.pi
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a and (r, g, b) in HEART_HOTTER and math.sin(phase + y * 0.9 + x * 1.7) > 0.8:
                img.putpixel((x, y), rgba(HEART_HOTTER[(r, g, b)]))
    # One-pixel tongues above the lobes; the dip between them stays clear so
    # the heart keeps its shape.
    for x in HEART_FLAME_COLUMNS:
        tops = [y for y in range(base.height) if base.getpixel((x, y))[3]]
        if tops and tops[0] > 0 and math.sin(phase + x * 2.3) > -0.2:
            img.putpixel((x, tops[0] - 1), rgba(LAVA[3] if math.sin(phase * 2 + x) > 0 else LAVA[2]))
    return img


def blink(img):
    """Damage flash: the same sprite washed toward white."""
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                out.putpixel((x, y), (r + (255 - r) * 2 // 5, g + (255 - g) * 2 // 5, b + (255 - b) * 2 // 5, a))
    return out


def feather(kind):
    """kind: full, half, empty; hunger variants tint the feather sickly green."""
    shape = {(x, y): ch for y, row in enumerate(FEATHER) for x, ch in enumerate(row) if ch != "."}
    img = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    if kind.startswith("empty"):
        # dark silhouette with a black outline, like the vanilla empty slot
        inside = (40, 40, 40) if kind == "empty" else (38, 46, 26)
        for (x, y) in shape:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if 0 <= x + dx < 9 and 0 <= y + dy < 9 and (x + dx, y + dy) not in shape:
                        img.putpixel((x + dx, y + dy), (0, 0, 0, 255))
        for p in shape:
            img.putpixel(p, rgba(inside))
        return img
    for (x, y), ch in shape.items():
        if kind.startswith("half") and x + y < 8:
            continue  # half keeps the quill end
        c = FEATHER_COLOURS[ch]
        if kind.endswith("hunger"):
            t = FEATHER_HUNGER_TINT
            c = tuple((a + b * 2) // 3 for a, b in zip(c, t))
        img.putpixel((x, y), rgba(c))
    return img


def saturation_overlay():
    icons = Image.open(os.path.join(VANILLA, "appleskin", "icons.png")).convert("RGBA")
    shape = {(x, y) for y, row in enumerate(FEATHER) for x, ch in enumerate(row) if ch != "."}
    ring = sorted({(x + dx, y + dy) for x, y in shape for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                   if 0 <= x + dx < 9 and 0 <= y + dy < 9 and (x + dx, y + dy) not in shape},
                  key=lambda p: -(p[0] + p[1]))  # quill end first
    for i, fraction in enumerate(SATURATION_STAGES):
        for y in range(9):
            for x in range(9):
                icons.putpixel((i * 9 + x, y), (0, 0, 0, 0))
        for x, y in ring[:max(1, round(len(ring) * fraction))]:
            icons.putpixel((i * 9 + x, y), rgba(SATURATION_GOLD))
    path = os.path.join(ROOT, "assets", "appleskin", "textures", "icons.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    icons.save(path)


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

    mace = load_mace()
    strip = Image.new("RGBA", (16, 16 * FRAMES))
    for f in range(FRAMES):
        strip.paste(render_frame(f, mace, *MACE), (0, f * 16))
    strip.save(os.path.join(item_dir, "mace.png"))
    with open(os.path.join(item_dir, "mace.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2}}, fh, indent=2)
        fh.write("\n")

    for name, dest, lo, hi in ORANGE_RECOLOURS:
        ember_recolour(name, dest, lo, hi)
    for name, dest, lo, hi in BURNING_RECOLOURS:
        burning_recolour(name, dest, lo, hi)
    for name in GUST_PARTICLES:
        ember_recolour(os.path.join("particle", name + ".png"), "particle/" + name + ".png", 0.1, 0.9, absolute=True)

    for name in ARMOR_ITEMS:
        base = Image.open(os.path.join(VANILLA, name + ".png")).convert("RGBA")
        strip = Image.new("RGBA", (base.width, base.height * FRAMES))
        for f in range(FRAMES):
            strip.paste(engulf_frame(f, base), (0, f * base.height))
        strip.save(os.path.join(item_dir, name + ".png"))
        with open(os.path.join(item_dir, name + ".png.mcmeta"), "w") as fh:
            json.dump({"animation": {"frametime": 2}}, fh, indent=2)
            fh.write("\n")

    for name, (colours, columns) in APPLES.items():
        base = load_apple(colours)
        strip = Image.new("RGBA", (16, 16 * FRAMES))
        for f in range(FRAMES):
            strip.paste(render_frame(f, base, columns, None), (0, f * 16))
        strip.save(os.path.join(item_dir, name + ".png"))
        with open(os.path.join(item_dir, name + ".png.mcmeta"), "w") as fh:
            json.dump({"animation": {"frametime": 2}}, fh, indent=2)
            fh.write("\n")
    # Vanilla draws the egap with the gap texture; point it at its own.
    model_dir = os.path.join(ROOT, "assets", "minecraft", "models", "item")
    os.makedirs(model_dir, exist_ok=True)
    with open(os.path.join(model_dir, "enchanted_golden_apple.json"), "w") as fh:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": "minecraft:item/enchanted_golden_apple"}}, fh, indent=2)
        fh.write("\n")

    hud = os.path.join(TEX, "gui", "sprites", "hud")
    os.makedirs(os.path.join(hud, "heart"), exist_ok=True)
    for name in HEARTS:
        heart = fiery_heart(name)
        for suffix, look in (("", lambda im: im), ("_blinking", blink)):
            strip = Image.new("RGBA", (9, 9 * FRAMES))
            for f in range(FRAMES):
                strip.paste(look(heart_frame(heart, f)), (0, f * 9))
            path = os.path.join(hud, "heart", name + suffix + ".png")
            strip.save(path)
            with open(path + ".mcmeta", "w") as fh:
                json.dump({"animation": {"frametime": 2}}, fh, indent=2)
                fh.write("\n")
    for kind in ["full", "half", "empty"]:
        feather(kind).save(os.path.join(hud, "food_" + kind + ".png"))
        feather(kind + "_hunger").save(os.path.join(hud, "food_" + kind + "_hunger.png"))
    saturation_overlay()

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
