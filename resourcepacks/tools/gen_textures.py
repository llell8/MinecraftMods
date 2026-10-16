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
  recoloured orange; flickering fires orbit the pearl and wind charge items
- particle/critical_hit, enchanted_hit, damage: thin fiery crit X, thin blue
  soul-fire enchanted sparkle, plain dark orange hearts when you hit something
- block/fire_0, fire_1: low fire (squashed frames)
- models/item: smaller first-person totem and shield
- potions: amber glass bottles with netherite stoppers
- diamond tools, spear and armor icons: same flames, handles and gem as netherite
- elytra (item, broken item, worn wings): phoenix wings on the fire palette
- bow, crossbow (all states), arrows: blaze rod wood, glowing string/metal/fletching
- shield: charred wood, netherite rim, flame emblem (plain shield)
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
    # diamond tools' stick
    (73, 54, 21): (179, 107, 25), (104, 78, 30): (216, 174, 38), (137, 103, 39): (255, 243, 45),
    (40, 30, 11): (174, 60, 0),
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
# Orange items that burn: recoloured, then two flickering fires slowly orbiting
# them a pixel away from their edge, so the fire never touches the item. Their own hotter shades (lemon-yellow
# head burning down to deep crimson) set them apart from the item's orange.
# Head to tail:
ORBIT_FIRE = [(255, 250, 190), (255, 222, 60), (255, 140, 20), (215, 40, 20), (140, 10, 20)]
# (vanilla file, pack path, ramp start, ramp end, shrink-to size or None)
BURNING_RECOLOURS = [
    ("ender_pearl.png", "item/ender_pearl.png", 0.15, 1.0, 11),  # shrunk to leave room for the ring
    ("wind_charge.png", "item/wind_charge.png", 0.1, 0.95, None),
]
ORBIT_FRAMES = 16    # half a turn per loop
ORBIT_FRAMETIME = 3  # ticks per frame: 2.4 s per half turn
# Wind charge burst particles: their own textures, mapped by absolute
# brightness (frames are only one or two greys each).
GUST_PARTICLES = [f"gust_{i}" for i in range(12)] + [f"small_gust_{i}" for i in range(7)]
# Hit particles (8x8). The game tints these: crit and damage-indicator
# particles are dimmed to 60-90%, so they are drawn extra bright; enchanted-hit
# particles are multiplied by (0.3, 0.8, 1.0) on top of that, which removes
# most red, so they are drawn in greys and come out as blue soul fire.
PARTICLE_COLOURS = {
    "W": (255, 255, 235), "y": (255, 220, 110), "o": (255, 150, 50), "r": (225, 70, 25),
    "w": (255, 255, 255), "l": (215, 215, 215), "g": (160, 160, 160),
    "H": (200, 90, 15), "h": (135, 50, 8),  # plain dark orange heart
}
PARTICLE_SPRITES = {
    "critical_hit": [  # thin fiery X spark
        "o.....o.",
        ".y...y..",
        "..y.y...",
        "...W....",
        "..y.y...",
        ".y...y..",
        "o.....o.",
        "........",
    ],
    "enchanted_hit": [  # thin soul-fire sparkle (greys; the game tints it blue)
        "...w....",
        "...l....",
        "........",
        "wl.w.lw.",
        "........",
        "...l....",
        "...w....",
        "........",
    ],
    "damage": [  # plain dark orange heart
        ".hh.hh..",
        "hHHhHHh.",
        "hHHHHHh.",
        "hHHHHHh.",
        ".hHHHh..",
        "..hHh...",
        "...h....",
        "........",
    ],
}
# Low fire: the on-screen burning overlay uses the fire block textures, so each
# frame is squashed to LOW_FIRE_HEIGHT of its height, anchored at the bottom.
# (Fire blocks in the world get shorter too, as in most PvP low-fire packs.)
LOW_FIRE_HEIGHT = 0.6
FIRE_TEXTURES = ["fire_0", "fire_1"]
# Smaller, lower first-person totem and shield (display transforms only).
TOTEM_FIRST_PERSON = {"rotation": [0, -90, 25], "translation": [1.13, 1.2, 1.13], "scale": [0.45, 0.45, 0.45]}
SHIELD_SCALE = 0.85      # vanilla first person is 1.25
SHIELD_DROP = 5          # pixels lower on screen (vanilla y + this many down)
SHIELD_BLOCKING_DROP = 2  # a little lower while blocking, so it stays visible
# Potions: warm amber glass with a dark netherite stopper. The liquid keeps its
# effect colour.
BOTTLE_COLOURS = {
    (212, 229, 247): (255, 236, 200), (179, 207, 236): (232, 182, 128),  # glass highlights
    (139, 173, 208): (200, 128, 78), (93, 143, 194): (130, 56, 28),      # glass shade, edge
}
CORK_COLOURS = {  # cork -> netherite stopper (same tones as the tools)
    (212, 109, 73): (134, 123, 134), (204, 175, 165): (112, 103, 112),
    (169, 71, 37): (79, 60, 62), (151, 55, 22): (50, 39, 39),
}
BOTTLES = ["potion", "splash_potion", "lingering_potion", "glass_bottle"]
# Diamond set: same treatment as netherite (blaze rod handles, flames, gem).
DIAMOND_TOOLS = {name.replace("netherite", "diamond"): spec for name, spec in TOOLS.items()}
DIAMOND_ARMOR = ["diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots"]
# Elytra: phoenix wings on the fire palette (vanilla feather shading kept);
# the item icon also gets flame tongues.
ELYTRA = {"elytra.png": "item/elytra.png", "elytra_broken.png": "item/elytra_broken.png",
          "elytra_wings.png": "entity/equipment/wings/elytra.png"}
# Bow, crossbow, arrows: warm browns (wood) go onto the copper blaze rod ramp,
# neutral greys/whites (string, metal, arrowheads, fletching) onto the fire
# ramp, by brightness; anything else (firework colours) is left alone.
ROD_RAMP = [(110, 34, 0), (174, 60, 0), (179, 107, 25), (216, 174, 38), (255, 243, 45)]
RANGED = {
    "bow": "item/bow.png", "bow_pulling_0": "item/bow_pulling_0.png",
    "bow_pulling_1": "item/bow_pulling_1.png", "bow_pulling_2": "item/bow_pulling_2.png",
    "crossbow_standby": "item/crossbow_standby.png", "crossbow_pulling_0": "item/crossbow_pulling_0.png",
    "crossbow_pulling_1": "item/crossbow_pulling_1.png", "crossbow_pulling_2": "item/crossbow_pulling_2.png",
    "crossbow_arrow": "item/crossbow_arrow.png", "crossbow_firework": "item/crossbow_firework.png",
    "arrow": "item/arrow.png", "arrow_entity": "entity/projectiles/arrow.png",
}
# Shield: charred wood, netherite rim, and a glowing flame on the front face
# (the plain shield only; the banner base keeps its white face for patterns).
SHIELD_RIM = {
    (87, 87, 94): (50, 39, 39), (101, 102, 109): (79, 60, 62), (110, 112, 119): (93, 86, 93),
    (120, 121, 129): (112, 103, 112), (145, 147, 155): (134, 123, 134), (163, 164, 170): (150, 140, 150),
}
CHARRED = [(26, 12, 9), (48, 20, 12), (74, 30, 14), (104, 42, 16)]
SHIELD_FLAME = [  # 6x9, centred on the 12x22 front face at (1, 1)
    "..y...",
    "..yy..",
    ".yyo..",
    ".yoWy.",
    "yoWWoy",
    "yoWWoy",
    "ooyyoo",
    ".orro.",
    "..rr..",
]
SHIELDS = {"shield_base_nopattern": True, "shield_base": False}  # name: draw the emblem
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


def fire(frame, base, columns, flame=LAVA):
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
                c = flame[3]
            elif frac < 0.67:
                c = flame[2]
            else:
                c = flame[1]
            px[(x, y)] = rgba(c)
    # Embers rise up-left off the head and loop every FRAMES.
    rng = random.Random(1337)
    for _ in range(4):
        sx = rng.choice(list(columns))
        sy = next(y for y in range(base.height) if base.getpixel((sx, y))[3]) - rng.randint(1, 2)
        t = (frame + rng.randint(0, FRAMES - 1)) % FRAMES
        p = (sx - t // 3, sy - t)
        if t < FRAMES - 2 and p[0] >= 0 and p[1] >= 0 and p not in px and base.getpixel(p)[3] == 0:
            px[p] = rgba(flame[4] if t < 3 else flame[2])
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


def engulf_frame(frame, base, crown=None, flame=LAVA):
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
            img.putpixel((x, y), rgba((flame[3], flame[2], flame[1])[k]))
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


def ember_image(name, lo, hi, absolute=False):
    img = Image.open(os.path.join(VANILLA, name)).convert("RGBA")
    for p, t in ember_levels(img, lo, hi, absolute).items():
        img.putpixel(p, ember_colour(t) + (img.getpixel(p)[3],))
    return img


def ember_recolour(name, dest, lo, hi, absolute=False):
    save_texture(ember_image(name, lo, hi, absolute), dest)


def save_animation(frames, dest):
    w, h = frames[0].size
    strip = Image.new("RGBA", (w, h * len(frames)))
    for i, f in enumerate(frames):
        strip.paste(f, (0, i * h))
    save_texture(strip, dest)
    with open(os.path.join(TEX, dest) + ".mcmeta", "w") as fh:
        json.dump({"animation": {"frametime": 2}}, fh, indent=2)
        fh.write("\n")


def shrink(img, size):
    """Scale the item's opaque area down to size x size, centred on the canvas."""
    item = img.crop(img.getbbox()).resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    offset = ((img.width - size) // 2, (img.height - size) // 2)
    for y in range(size):
        for x in range(size):
            r, g, b, a = item.getpixel((x, y))
            if a >= 128:
                out.putpixel((x + offset[0], y + offset[1]), (r, g, b, 255))
    return out


def burning_recolour(name, dest, lo, hi, size):
    base = Image.open(os.path.join(VANILLA, name)).convert("RGBA")
    for p, t in ember_levels(base, lo, hi).items():
        base.putpixel(p, ember_colour(t) + (base.getpixel(p)[3],))
    if size:
        base = shrink(base, size)
    strip = Image.new("RGBA", (base.width, base.height * ORBIT_FRAMES))
    for f in range(ORBIT_FRAMES):
        strip.paste(orbit_frame(f, base), (0, f * base.height))
    save_texture(strip, dest)
    with open(os.path.join(TEX, dest) + ".mcmeta", "w") as fh:
        json.dump({"animation": {"frametime": ORBIT_FRAMETIME}}, fh, indent=2)
        fh.write("\n")


def orbit_frame(frame, base):
    """Two flickering fires circling the item one pixel away from its edge,
    with flame tongues reaching outward near each head. Half a turn per loop;
    the fires are half a turn apart, so the loop is seamless."""
    img = base.copy()
    w, h = base.size
    solid = {(x, y) for y in range(h) for x in range(w) if base.getpixel((x, y))[3]}
    cx = sum(x for x, _ in solid) / len(solid)
    cy = sum(y for _, y in solid) / len(solid)
    head = frame / ORBIT_FRAMES * math.pi
    for y in range(h):
        for x in range(w):
            d = min(max(abs(x - sx), abs(y - sy)) for sx, sy in solid)
            if d not in (2, 3):  # d == 1 is the gap that keeps the fire off the item
                continue
            angle = math.atan2(y - cy, x - cx)
            for fire in (head, head + math.pi):
                behind = (fire - angle) % (2 * math.pi)  # how far behind the head
                reach = 2.4 if d == 2 else 0.9          # outer tongues only near the head
                if behind >= reach:
                    continue
                i = min(len(ORBIT_FIRE) - 1, int(behind / reach * len(ORBIT_FIRE)) + (d - 2))
                flicker = math.sin(frame * 2.1 + x * 3.7 + y * 5.3)
                if flicker > 0.6:
                    i = max(0, i - 1)
                elif flicker < -0.7:
                    i = min(len(ORBIT_FIRE) - 1, i + 1)
                if d == 3 and flicker < -0.2:
                    continue  # ragged outer edge
                img.putpixel((x, y), rgba(ORBIT_FIRE[i]))
    return img


def low_fire(name):
    src = Image.open(os.path.join(VANILLA, name + ".png")).convert("RGBA")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    size = src.width
    short = round(size * LOW_FIRE_HEIGHT)
    for top in range(0, src.height, size):
        frame = src.crop((0, top, size, top + size)).resize((size, short), Image.LANCZOS)
        out.paste(frame, (0, top + size - short))
    save_texture(out, "block/" + name + ".png")
    with open(os.path.join(VANILLA, name + ".png.mcmeta")) as fh:
        meta = fh.read()
    with open(os.path.join(TEX, "block", name + ".png.mcmeta"), "w") as fh:
        fh.write(meta)


def write_model(name, model):
    path = os.path.join(ROOT, "assets", "minecraft", "models", "item", name + ".json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(model, fh, indent=2)
        fh.write("\n")


def small_offhand_models():
    write_model("totem_of_undying", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "minecraft:item/totem_of_undying"},
        "display": {"firstperson_righthand": TOTEM_FIRST_PERSON, "firstperson_lefthand": TOTEM_FIRST_PERSON},
    })
    for name, drop in (("shield", SHIELD_DROP), ("shield_blocking", SHIELD_BLOCKING_DROP)):
        with open(os.path.join(VANILLA, "model_" + name + ".json")) as fh:
            model = json.load(fh)
        for hand in ("firstperson_righthand", "firstperson_lefthand"):
            view = model["display"][hand]
            view["scale"] = [SHIELD_SCALE] * 3
            view["translation"][1] -= drop
        write_model(name, model)


def potions():
    for name in BOTTLES:
        img = Image.open(os.path.join(VANILLA, name + ".png")).convert("RGBA")
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = img.getpixel((x, y))
                if a and (r, g, b) in BOTTLE_COLOURS:
                    img.putpixel((x, y), rgba(BOTTLE_COLOURS[(r, g, b)], a))
                elif a and (r, g, b) in CORK_COLOURS:
                    img.putpixel((x, y), rgba(CORK_COLOURS[(r, g, b)], a))
        save_texture(img, "item/" + name + ".png")


def ramp(stops, t):
    t = max(0.0, min(1.0, t))
    i = min(len(stops) - 2, int(t * (len(stops) - 1)))
    f = t * (len(stops) - 1) - i
    return tuple(round(a + (b - a) * f) for a, b in zip(stops[i], stops[i + 1]))


def ranged(name, dest):
    img = Image.open(os.path.join(VANILLA, "ranged", name + ".png")).convert("RGBA")
    rod = [tone(c, *ROD_TONE) for c in ROD_RAMP]
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if not a:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            if max(r, g, b) - min(r, g, b) < 12:          # grey/white: glowing string, metal, fletching
                img.putpixel((x, y), rgba(ramp(LAVA, lum * 1.15), a))
            elif r > g > b and r - b > 25:                  # warm brown: wood -> blaze rod
                img.putpixel((x, y), rgba(ramp(rod, lum * 2.3), a))
    save_texture(img, dest)


def shield(name, emblem):
    img = Image.open(os.path.join(VANILLA, "ranged", name + ".png")).convert("RGBA")
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if not a:
                continue
            if (r, g, b) in SHIELD_RIM:
                img.putpixel((x, y), rgba(SHIELD_RIM[(r, g, b)], a))
            elif r > g > b:                                 # wood -> charred
                img.putpixel((x, y), rgba(ramp(CHARRED, (0.3 * r + 0.59 * g + 0.11 * b) / 140), a))
    if emblem:
        colours = {"W": LAVA[4], "y": LAVA[3], "o": LAVA[2], "r": LAVA[1]}
        for y, row in enumerate(SHIELD_FLAME):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((1 + 3 + x, 1 + 7 + y), rgba(colours[ch]))
    save_texture(img, "entity/" + name + ".png")


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


def render_frame(frame, base, columns, gem, flame=LAVA):
    img = base.copy()
    flames = Image.new("RGBA", base.size, (0, 0, 0, 0))
    for p, c in fire(frame, base, columns, flame).items():
        flames.putpixel(p, c)
    img.alpha_composite(flames)
    if gem:
        pulse = 0.5 + 0.5 * math.sin(frame / FRAMES * 2 * math.pi)
        img.putpixel(gem, rgba(flame[3] if pulse > 0.5 else flame[2]))
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

    for name, (columns, gem) in DIAMOND_TOOLS.items():
        base = load_tool(name)
        save_animation([render_frame(f, base, columns, gem) for f in range(FRAMES)], "item/" + name + ".png")
    for name in DIAMOND_ARMOR:
        base = Image.open(os.path.join(VANILLA, name + ".png")).convert("RGBA")
        save_animation([engulf_frame(f, base) for f in range(FRAMES)], "item/" + name + ".png")

    elytra = ember_image("elytra.png", 0.1, 1.0)
    save_animation([engulf_frame(f, elytra) for f in range(FRAMES)], ELYTRA["elytra.png"])
    ember_recolour("elytra_broken.png", ELYTRA["elytra_broken.png"], 0.05, 0.8)
    ember_recolour("elytra_wings.png", ELYTRA["elytra_wings.png"], 0.05, 1.0)

    for name, dest in RANGED.items():
        ranged(name, dest)
    for name, emblem in SHIELDS.items():
        shield(name, emblem)

    mace = load_mace()
    strip = Image.new("RGBA", (16, 16 * FRAMES))
    for f in range(FRAMES):
        strip.paste(render_frame(f, mace, *MACE), (0, f * 16))
    strip.save(os.path.join(item_dir, "mace.png"))
    with open(os.path.join(item_dir, "mace.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2}}, fh, indent=2)
        fh.write("\n")

    for name in FIRE_TEXTURES:
        low_fire(name)
    small_offhand_models()
    potions()

    for name, dest, lo, hi in ORANGE_RECOLOURS:
        ember_recolour(name, dest, lo, hi)
    for name, dest, lo, hi, size in BURNING_RECOLOURS:
        burning_recolour(name, dest, lo, hi, size)
    for name, rows in PARTICLE_SPRITES.items():
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x, y), rgba(PARTICLE_COLOURS[ch]))
        save_texture(img, "particle/" + name + ".png")
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
