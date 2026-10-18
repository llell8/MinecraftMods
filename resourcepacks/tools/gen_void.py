"""Generates the Void PvP pack: built on gen_textures.py (the Phoenix
generator) with purple/white/black palettes, then adds its own pieces.

Run: python3 resourcepacks/tools/gen_void.py

- netherite sword, axe, pickaxe, shovel, hoe and spears: galaxy heads
  (drifting nebula, twinkling stars, bright edge) and a pulsing star gem
- diamond tools: void handles and a star gem; armor icons stay vanilla
- mace: faceted void crystal head with a sweeping glint
- sword/axe/mace renamed "Crushing Swedes": Danish-flag versions
- bow/crossbow: purple wood, white string that charges green/yellow/red
- shield: obsidian-purple planks, violet rim, black hole emblem (normal size)
- hunger: purple meat-on-bone (AppleSkin outline recoloured); vanilla hearts
- totem: an angel (golden halo, white wings, purple gem)
- pearls/wind charges: accretion disk; elytra: plain violet recolour
- no golden apple textures (left vanilla)
"""
import json
import math
import os
import shutil

from PIL import Image

import gen_textures as g

g.ROOT = os.path.join(g.HERE, "..", "VoidPvP")
g.TEX = os.path.join(g.ROOT, "assets", "minecraft", "textures")

# Void energy, dark to bright (same five steps as the fire palette). Mutated in
# place so every default argument that captured the fire palette follows it.
VOID = [(48, 16, 84), (92, 36, 156), (150, 80, 230), (205, 160, 255), (250, 245, 255)]
g.LAVA[:] = VOID
# Recolours run black -> purple -> white.
g.EMBER_RAMP = [(8, 4, 14), (30, 10, 50)] + VOID

# Handles: dark void rods instead of blaze rods (no extra toning).
ROD_DARK, ROD_MID, ROD_LIGHT = (28, 14, 40), (70, 40, 110), (150, 110, 210)
g.BLAZE_ROD = {
    (47, 33, 34): ROD_DARK, (96, 52, 50): ROD_MID, (115, 69, 67): ROD_LIGHT,
    (59, 57, 59): ROD_MID, (67, 64, 67): ROD_LIGHT,
    (73, 54, 21): ROD_DARK, (104, 78, 30): ROD_MID, (137, 103, 39): ROD_LIGHT, (40, 30, 11): (16, 8, 24),
}
g.ROD_TONE = (1.0, 1.0, 0.0)
g.ROD_EDGE = (16, 8, 24)
g.ROD_END = (8, 4, 14)
g.MACE_ROD = {(35, 66, 105): (16, 8, 24), (80, 99, 144): ROD_DARK,
              (122, 130, 196): ROD_MID, (181, 165, 202): ROD_LIGHT}
g.MACE_COLOURS[(191, 206, 231)] = VOID[3]  # the crack glows violet
# The Danish easter egg is done here (sword, axe and mace), not by gen_textures.

# Accretion disk orbiting the pearl and wind charge, head to tail.
g.ORBIT_FIRE = [(255, 255, 255), (225, 200, 255), (170, 110, 245), (110, 50, 190), (55, 20, 100)]

# Hit particles: white/violet crit X, dark purple hit hearts. (Enchanted crits
# are tinted blue by the game whatever we draw.)
g.PARTICLE_COLOURS.update({
    "W": (255, 255, 255), "y": (230, 205, 255), "o": (180, 125, 250), "r": (130, 70, 210),
    "H": (120, 60, 200), "h": (70, 28, 124),
})

# Potions: smoky violet glass with an obsidian stopper.
g.BOTTLE_COLOURS = {
    (212, 229, 247): (240, 230, 255), (179, 207, 236): (190, 160, 235),
    (139, 173, 208): (120, 80, 180), (93, 143, 194): (60, 30, 100),
}
g.CORK_COLOURS = {
    (212, 109, 73): (90, 70, 130), (204, 175, 165): (70, 52, 104),
    (169, 71, 37): (40, 24, 60), (151, 55, 22): (20, 12, 30),
}

# Bow: violet-tinted wood, white-violet string. Crossbow string/metal and arrow
# shafts use a violet-to-white ramp instead of blaze rod.
g.BOW_STRING_YELLOW = (232, 214, 255)
g.BOW_WOOD_TONE = (0.9, 0.85, 0.68)
g.BLAZE_RAMP = [(60, 24, 110), (110, 50, 190), (150, 80, 230), (205, 160, 255), (240, 225, 255), (255, 255, 255)]

# Hearts: lilac at the top fading to near-black at the tip, white highlight.
g.HEART_ROWS = {1: (225, 200, 255), 2: (195, 150, 255), 3: (155, 90, 235), 4: (115, 55, 195),
                5: (82, 32, 145), 6: (56, 20, 102), 7: (40, 14, 72)}
g.HEART_COLOURS = {(255, 200, 200): (255, 255, 255), (187, 19, 19): (24, 8, 40), (96, 7, 7): (16, 6, 28)}
_rows = [g.HEART_ROWS[y] for y in range(7, 0, -1)]  # dark -> bright
g.HEART_HOTTER = {c: _rows[i + 1] for i, c in enumerate(_rows[:-1])}
g.HEART_HOTTER[_rows[-1]] = (255, 255, 255)

# Hunger: a small black hole (dark core, violet rings, white glint) in place
# of the feather. Keys reuse the feather colour slots.
g.FEATHER = [
    ".........",
    "...ooo...",
    "..oyyyo..",
    ".oywrryo.",
    ".oyrqryo.",
    ".oyrrryo.",
    "..oyyyo..",
    "...ooo...",
    ".........",
]
g.FEATHER_COLOURS = {"w": (255, 255, 255), "y": (205, 160, 255), "o": (130, 65, 210),
                     "r": (70, 30, 120), "q": (8, 4, 14)}
g.FEATHER_HUNGER_TINT = (90, 120, 60)
g.SATURATION_GOLD = (240, 225, 255)

# Golden apples: the gap goes pale lilac/white, the egap deep violet-black, so
# they stay easy to tell apart. Built from the vanilla colours' brightness.
GAP_RAMP = [(70, 40, 110), (150, 110, 210), (220, 200, 255), (255, 255, 255)]
EGAP_RAMP = [(16, 6, 28), (60, 24, 110), (120, 60, 200), (200, 160, 255)]
_apple_colours = list(g.GAP_COLOURS)
_lums = {c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2] for c in _apple_colours}
_lo, _hi = min(_lums.values()), max(_lums.values())
for _ramp, _dest in ((GAP_RAMP, g.GAP_COLOURS), (EGAP_RAMP, g.EGAP_COLOURS)):
    for c in _apple_colours:
        _dest[c] = g.ramp(_ramp, (_lums[c] - _lo) / (_hi - _lo))


# Totem: an animated black hole. A dark core with a white photon ring, a faint
# lensed halo, and a tilted accretion disk crossing in front of the core whose
# bright side swings round as it spins.
HOLE_CORE = (6, 3, 10)


def black_hole_frame(frame, _grid=None):
    size = 16
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = cy = 7.5
    phase = frame / g.FRAMES * 2 * math.pi
    for y in range(size):
        for x in range(size):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            colour = None
            if d < 2.7:
                colour = HOLE_CORE
            elif d < 3.5:
                colour = (245, 235, 255)                       # photon ring
            elif d < 4.6:
                colour = (120, 60, 200) if (x + y) % 2 else (95, 40, 165)  # lensed glow
            in_disk = (dx / 7.6) ** 2 + (dy / 1.3) ** 2 <= 1
            if in_disk and (dy >= 0.5 or d >= 4.6):            # disk passes in front of the core, below centre
                angle = math.atan2(dy * 4, dx)
                bright = 0.55 + 0.45 * math.cos(angle - phase)  # bright side spins round
                fade = 1 - abs(dx) / 8.5                        # dimmer toward the disk's ends
                colour = g.ramp(VOID, max(0.0, min(1.0, bright * fade + 0.15)))
            if colour:
                img.putpixel((x, y), g.rgba(colour))
    return img


g.draw_phoenix = lambda: None
g.phoenix_frame = black_hole_frame


# Instead of flames: a void pull. Dotted rings of particles ripple inward
# toward the item (dim purple far out, white-violet right at its edge), with
# a few white stars twinkling nearby. Only empty pixels are used, so the item
# itself is never covered.
PULL_COLOURS = {3: VOID[1], 2: VOID[2], 1: VOID[3]}
STAR = (255, 255, 255)


def _void_pull(frame, base, near):
    """{(x, y): rgba} for one frame; near(x, y) limits where the effect goes."""
    w, h = base.size
    solid = {(x, y) for y in range(h) for x in range(w) if base.getpixel((x, y))[3]}
    wave = 3 - frame % 4          # 3 -> 2 -> 1 -> (rest), twice per loop
    px = {}
    for y in range(h):
        for x in range(w):
            if (x, y) in solid or not near(x, y):
                continue
            d = min(max(abs(x - sx), abs(y - sy)) for sx, sy in solid)
            if d > 3:
                continue
            if d == wave and (x * 3 + y * 5 + frame) % 3:
                px[(x, y)] = g.rgba(PULL_COLOURS[d])
            elif d == 2 and (x * 7 + y * 11) % 13 == frame % 13 % 8:
                px[(x, y)] = g.rgba(STAR)   # twinkle
    return px


def void_fire(frame, base, columns, flame=None):
    """Replaces the flames on tools/apples: the pull, kept to the head area."""
    cols = list(columns)
    lo, hi = min(cols) - 2, max(cols) + 2
    tops = [next(y for y in range(base.height) if base.getpixel((x, y))[3]) for x in cols]
    bottom = max(tops) + 3
    return _void_pull(frame, base, lambda x, y: lo <= x <= hi and y <= bottom)


def void_engulf(frame, base, crown=None, flame=None):
    """Replaces the flame tongues on armor icons and the elytra item."""
    img = base.copy()
    for p, c in _void_pull(frame, base, lambda x, y: True).items():
        img.putpixel(p, c)
    return img


g.fire = void_fire
g.engulf_frame = void_engulf
g.HEART_FLAME_COLUMNS = []  # hearts keep their shimmer but lose the flame tongues


# The netherite sword and axe get a galaxy head: the inside of the blade is deep
# space with a purple nebula drifting along it and white stars twinkling,
# a bright violet edge and a white-hot tip. Instead of the void pull it has a
# light sprinkle of stardust twinkling around the blade, and a star for a gem.
SPACE = [(10, 4, 20), (40, 14, 76), (110, 40, 190), (190, 120, 250)]
BLADE_EDGE = (225, 205, 255)
SWORD_OUTLINE = {(74, 41, 64), (35, 16, 18), (22, 8, 20)}


ROD_COLOURS = {ROD_DARK, ROD_MID, ROD_LIGHT, (16, 8, 24), (8, 4, 14)}
# Which part of each item becomes galaxy: (x range, max y) around the head;
# handle, outline and gem pixels are always left alone.
GALAXY_ITEMS = {  # items whose head is recoloured as a galaxy (hand-set bounds)
    "netherite_sword": (range(6, 16), 9),
    "netherite_axe": (range(6, 15), 8),
}
# These get the galaxy too; their head is found by colour.
GALAXY_BY_COLOUR = ["netherite_pickaxe", "netherite_shovel", "netherite_hoe",
                    "netherite_spear", "netherite_spear_in_hand"]


# Diamond tools keep their look (void handle, star gem), with no animation.
# Armor icons stay vanilla.


def _galaxy_base(name):
    """(base image, gem pixel or None) for an item that gets a galaxy head."""
    if name == "mace":
        return g.load_mace(), g.MACE[1]
    if name in g.TOOLS:
        return g.load_tool(name), g.TOOLS[name][1]
    if name in g.DIAMOND_TOOLS:
        return g.load_tool(name), g.DIAMOND_TOOLS[name][1]
    return Image.open(os.path.join(g.VANILLA, name + ".png")).convert("RGBA"), None  # armor icon


def galaxy_region(base, name, gem):
    if name in GALAXY_ITEMS:  # hand-set bounds (keeps the sword's guard out)
        xs, max_y = GALAXY_ITEMS[name]
        return [(x, y) for y in range(max_y + 1) for x in xs
                if (x, y) != gem and base.getpixel((x, y))[3]
                and base.getpixel((x, y))[:3] not in SWORD_OUTLINE | ROD_COLOURS
                and (name != "netherite_sword" or x + y >= 12)]
    # By colour: metal/diamond pixels; not the handle, the dark outline, or
    # warm leather wraps.
    region = []
    for y in range(base.height):
        for x in range(base.width):
            r, gr, b, a = base.getpixel((x, y))
            lum = 0.3 * r + 0.59 * gr + 0.11 * b
            if (a and (x, y) != gem and (r, gr, b) not in ROD_COLOURS | SWORD_OUTLINE and lum >= 45
                    and not (r > gr > b and r - b > 60)):
                region.append((x, y))
    return region


def galaxy_blade(base, frame, region):
    img = base.copy()
    phase = frame / g.FRAMES * 2 * math.pi
    for x, y in region:
        u = x - y                                    # position along the head
        nebula = 0.5 + 0.5 * math.sin(u * 0.8 - phase)
        colour = g.ramp(SPACE, 0.15 + 0.65 * nebula * (0.6 + 0.4 * math.sin(x * 1.7 + y)))
        if (x * 5 + y * 3) % 7 == 0:                 # star positions
            twinkle = math.sin(phase * 2 + x * 2.3 + y)
            if twinkle > 0.2:
                colour = (255, 255, 255) if twinkle > 0.7 else (210, 190, 255)
        img.putpixel((x, y), g.rgba(colour))
    # bright edge along the upper/left side, white tip at the top
    inside = set(region)
    for x, y in region:
        if not any((x + dx, y + dy) in inside for dx, dy in ((-1, 0), (0, -1))):
            img.putpixel((x, y), g.rgba(BLADE_EDGE))
    tip = min(region, key=lambda p: p[1] - p[0])
    img.putpixel(tip, g.rgba((255, 255, 255)))
    return img


def _void_trim(base):
    """Off-theme leftovers go void: diamond's dark teal outline -> dark purple,
    warm leather wraps (spears) -> the void rod colours."""
    for y in range(base.height):
        for x in range(base.width):
            r, gr, b, a = base.getpixel((x, y))
            if not a:
                continue
            lum = 0.3 * r + 0.59 * gr + 0.11 * b
            if gr > r and lum < 45:
                base.putpixel((x, y), g.rgba((20, 8, 32), a))
            elif r > gr > b and r - b > 60:
                base.putpixel((x, y), g.rgba(g.ramp([ROD_DARK, ROD_MID, ROD_LIGHT], lum / 130), a))
    return base


def galaxy_item(name):
    """Galaxy head with its pulsing star gem; no particles around it."""
    base, gem = _galaxy_base(name)
    base = _void_trim(base)
    region = galaxy_region(base, name, gem)
    frames = []
    for f in range(g.FRAMES):
        img = galaxy_blade(base, f, region)
        pulse = 0.5 + 0.5 * math.sin(f / g.FRAMES * 2 * math.pi)
        img.putpixel(gem, g.rgba((255, 255, 255) if pulse > 0.5 else VOID[3]))
        frames.append(img)
    g.save_animation(frames, "item/" + name + ".png")


def static_tool(name):
    """Diamond tools: void handle and a white star gem, no animation."""
    base, gem = _galaxy_base(name)
    base.putpixel(gem, g.rgba((255, 255, 255)))
    g.save_texture(base, "item/" + name + ".png")


# The mace head is a faceted void crystal: diagonal facets from deep violet to
# pale lilac with white facet edges, a dark outline, and a light glint
# sweeping across it. Handle and pulsing star gem stay.
CRYSTAL = [(34, 12, 62), (70, 28, 124), (120, 60, 200), (175, 120, 245), (225, 200, 255)]
CRYSTAL_EDGE = (20, 6, 36)


def mace_head(base, gem):
    return {(x, y) for y in range(11) for x in range(5, 16) if (x, y) != gem
            and base.getpixel((x, y))[3] and base.getpixel((x, y))[:3] not in ROD_COLOURS}


def crystal_mace():
    base, gem = g.load_mace(), g.MACE[1]
    head = mace_head(base, gem)
    frames = []
    for f in range(g.FRAMES):
        img = base.copy()
        sweep = f / g.FRAMES * 22 - 4                     # glint band position along x + y
        for x, y in head:
            if any((x + dx, y + dy) not in head and not (0 <= x + dx < 16 and 0 <= y + dy < 16
                   and base.getpixel((x + dx, y + dy))[3]) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), g.rgba(CRYSTAL_EDGE))
                continue
            facet = ((x - y) // 3) % 3                    # diagonal facets
            t = 0.25 + 0.25 * facet + 0.15 * ((x + y) % 2)
            if (x - y) % 3 == 0:
                t = 0.95                                  # bright facet edge
            if abs((x + y) - sweep) < 1.5:
                t = 1.0                                   # sweeping glint
            img.putpixel((x, y), g.rgba(g.ramp(CRYSTAL, t)))
        pulse = 0.5 + 0.5 * math.sin(f / g.FRAMES * 2 * math.pi)
        img.putpixel(gem, g.rgba((255, 255, 255) if pulse > 0.5 else VOID[3]))
        frames.append(img)
    g.save_animation(frames, "item/mace.png")


# Easter egg: the netherite sword, axe and mace renamed "Crushing Swedes" show
# the Danish flag (item definitions select on custom_name).
DANISH_ITEMS = {"netherite_sword": "minecraft:item/handheld", "netherite_axe": "minecraft:item/handheld",
                "mace": "minecraft:item/handheld_mace"}


def danish_variant(name, parent):
    if name == "mace":
        base, gem = g.load_mace(), g.MACE[1]
        region = mace_head(base, gem)
    else:
        base, gem = _galaxy_base(name)
        region = set(galaxy_region(base, name, gem))
    xs = [x for x, _ in region]
    ys = [y for _, y in region]
    if name == "netherite_sword":   # the blade runs diagonally: cross near its middle
        cross = lambda x, y: x + y in (15,) or x - y in (2,)
    else:
        cx = min(xs) + round((max(xs) - min(xs)) * 0.36)
        cy = (min(ys) + max(ys)) // 2
        cross = lambda x, y: x == cx or y == cy
    img = base.copy()
    for x, y in region:
        lum = sum(img.getpixel((x, y))[:3]) / 765
        edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3])
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and name != "netherite_sword":
            img.putpixel((x, y), g.rgba(g.DANNEBROG_EDGE))
            continue
        shades = g.DANNEBROG_WHITE if cross(x, y) else g.DANNEBROG_RED
        img.putpixel((x, y), g.rgba(shades[1 if lum > 0.25 else 0]))
    g.save_texture(img, "item/" + name + "_danish.png")
    g.write_model(name + "_danish", {"parent": parent, "textures": {"layer0": "minecraft:item/" + name + "_danish"}})
    items_dir = os.path.join(g.ROOT, "assets", "minecraft", "items")
    os.makedirs(items_dir, exist_ok=True)
    with open(os.path.join(items_dir, name + ".json"), "w") as fh:
        json.dump({"model": {
            "type": "minecraft:select", "property": "minecraft:component", "component": "minecraft:custom_name",
            "cases": [{"when": g.DANISH_NAMES,
                       "model": {"type": "minecraft:model", "model": "minecraft:item/" + name + "_danish"}}],
            "fallback": {"type": "minecraft:model", "model": "minecraft:item/" + name},
        }}, fh, indent=2)
        fh.write("\n")


# Bow and crossbow: purple wood, a white string at rest, and the string
# charging green -> yellow -> red through the pull stages (a loaded crossbow
# stays red). Crossbow strings use three greys; their shading is kept.
STRING_COLOURS = {"rest": (240, 238, 255), 0: (70, 210, 80), 1: (245, 215, 50), 2: (235, 50, 45)}
BOW_STAGES = {"bow": "rest", "bow_pulling_0": 0, "bow_pulling_1": 1, "bow_pulling_2": 2}
CROSSBOW_STAGES = {"crossbow_standby": "rest", "crossbow_pulling_0": 0, "crossbow_pulling_1": 1,
                   "crossbow_pulling_2": 2, "crossbow_arrow": 2, "crossbow_firework": 2}
CROSSBOW_STRING_SHADE = {(68, 68, 68): 0.7, (109, 109, 109): 0.85, (150, 150, 150): 1.0, (151, 151, 151): 1.0}


def charging_bows():
    for name, stage in list(BOW_STAGES.items()) + list(CROSSBOW_STAGES.items()):
        img = Image.open(os.path.join(g.VANILLA, "ranged", name + ".png")).convert("RGBA")
        colour = STRING_COLOURS[stage]
        crossbow = name.startswith("crossbow")
        for y in range(img.height):
            for x in range(img.width):
                r, gr, b, a = img.getpixel((x, y))
                if not a:
                    continue
                if crossbow and (r, gr, b) in CROSSBOW_STRING_SHADE:
                    k = CROSSBOW_STRING_SHADE[(r, gr, b)]
                    img.putpixel((x, y), g.rgba(tuple(int(c * k) for c in colour), a))
                elif not crossbow and (r, gr, b) == g.BOW_STRING:
                    img.putpixel((x, y), g.rgba(colour, a))
                elif r > gr > b and r - b > 25:          # wood -> purple
                    img.putpixel((x, y), g.rgba(g.tone((r, gr, b), *g.BOW_WOOD_TONE), a))
        g.save_texture(img, "item/" + name + ".png")


# Shield: dark obsidian-purple planks, a dark violet metal rim, and a small
# black hole emblem on the plain shield's front face (the banner base keeps
# its white face so patterns still show).
SHIELD_RIM = {(87, 87, 94): (34, 20, 48), (101, 102, 109): (52, 32, 74), (110, 112, 119): (66, 42, 92),
              (120, 121, 129): (82, 54, 112), (145, 147, 155): (104, 72, 140), (163, 164, 170): (124, 90, 164)}
SHIELD_WOOD = [(14, 6, 22), (28, 12, 44), (44, 20, 70), (60, 28, 94)]


def void_shield(name, emblem):
    img = Image.open(os.path.join(g.VANILLA, "ranged", name + ".png")).convert("RGBA")
    for y in range(img.height):
        for x in range(img.width):
            r, gr, b, a = img.getpixel((x, y))
            if not a:
                continue
            if (r, gr, b) in SHIELD_RIM:
                img.putpixel((x, y), g.rgba(SHIELD_RIM[(r, gr, b)], a))
            elif r > gr > b:
                img.putpixel((x, y), g.rgba(g.ramp(SHIELD_WOOD, (0.3 * r + 0.59 * gr + 0.11 * b) / 140), a))
    if emblem:  # small black hole centred on the 12x22 front face at (1, 1)
        cx, cy = 1 + 5.5, 1 + 10.5
        for y in range(1, 23):
            for x in range(1, 13):
                d = math.hypot(x - cx, (y - cy) * 0.9)
                if d < 1.6:
                    img.putpixel((x, y), g.rgba(HOLE_CORE))
                elif d < 2.5:
                    img.putpixel((x, y), g.rgba((245, 235, 255)))
                elif d < 3.3:
                    img.putpixel((x, y), g.rgba(VOID[2]))
                elif abs(y - cy) < 0.6 and abs(x - cx) < 5.5:
                    img.putpixel((x, y), g.rgba(VOID[3]))   # accretion disk
    g.save_texture(img, "entity/" + name + ".png")


# Hunger: the vanilla meat-on-bone, with purple meat and a white bone. The
# AppleSkin saturation outline keeps its drumstick shape, recoloured.
MEAT = [(30, 10, 54), (70, 28, 124), (120, 60, 200), (175, 120, 245)]
MEAT_HUNGER = [(24, 34, 16), (52, 78, 36), (90, 120, 60), (140, 170, 100)]
BONE = [(200, 190, 225), (250, 248, 255)]


def void_drumsticks():
    hud = os.path.join(g.TEX, "gui", "sprites", "hud")
    for name in ["food_full", "food_half", "food_empty", "food_full_hunger", "food_half_hunger", "food_empty_hunger"]:
        img = Image.open(os.path.join(g.VANILLA, "hud", name + ".png")).convert("RGBA")
        hunger = name.endswith("hunger")
        for y in range(img.height):
            for x in range(img.width):
                r, gr, b, a = img.getpixel((x, y))
                if not a:
                    continue
                lum = (0.3 * r + 0.59 * gr + 0.11 * b) / 255
                if name.startswith("food_empty"):
                    if (r, gr, b) != (0, 0, 0):
                        img.putpixel((x, y), g.rgba((40, 20, 60) if not hunger else (30, 40, 22), a))
                elif lum > 0.7 and abs(r - b) < 70:                 # bone
                    img.putpixel((x, y), g.rgba(BONE[1 if lum > 0.85 else 0], a))
                else:                                                # meat
                    img.putpixel((x, y), g.rgba(g.ramp(MEAT_HUNGER if hunger else MEAT, lum * 1.5), a))
        img.save(os.path.join(hud, name + ".png"))
    icons = Image.open(os.path.join(g.VANILLA, "appleskin", "icons.png")).convert("RGBA")
    for y in range(9):
        for x in range(36):
            r, gr, b, a = icons.getpixel((x, y))
            if a:
                icons.putpixel((x, y), g.rgba(g.SATURATION_GOLD, a))
    icons.save(os.path.join(g.ROOT, "assets", "appleskin", "textures", "icons.png"))


# Totem: an angel (not void-themed): a golden halo, white wings and robe, and
# a small purple heart-gem. The halo has a glint running round it and the
# gem pulses.
ANGEL = [
    "................",
    ".....gGGGGg.....",  # halo
    "....G......G....",
    ".....gGGGGg.....",
    ".......WW.......",  # head
    "WW....WWWW....WW",  # wing tips
    "WWW...swws...WWW",
    "sWWW.WWppWW.WWWs",  # shoulders, gem
    ".sWWWWWWWWWWWWs.",
    "..sWWwWWWWwWWs..",
    "...ss.WWWW.ss...",  # feather ends
    "......WwwW......",  # robe
    ".....WWwwWW.....",
    ".....WwwwwW.....",
    "....sWWWWWWs....",
    "................",
]
ANGEL_COLOURS = {"W": (250, 250, 255), "w": (215, 210, 235), "s": (160, 150, 195),
                 "g": (225, 175, 55), "G": (255, 232, 140), "p": (150, 80, 230)}


def angel_frame(frame, _grid=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    halo = [(x, y) for y in range(1, 4) for x, ch in enumerate(ANGEL[y]) if ch in "gG"]
    hx = sum(x for x, _ in halo) / len(halo)
    glint = frame / g.FRAMES * 2 * math.pi
    for y, row in enumerate(ANGEL):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            colour = ANGEL_COLOURS[ch]
            if ch in "gG":
                angle = math.atan2(y - 2, x - hx)
                if math.cos(angle - glint) > 0.85:
                    colour = (255, 255, 230)
            if ch == "p" and math.sin(glint) > 0:
                colour = (200, 150, 255)
            img.putpixel((x, y), g.rgba(colour))
    return img


g.phoenix_frame = angel_frame


def violet_glint(name):
    src = Image.open(os.path.join(g.VANILLA, name)).convert("RGB")
    out = Image.new("RGB", src.size)
    for y in range(src.height):
        for x in range(src.width):
            v = max(src.getpixel((x, y)))
            out.putpixel((x, y), (int(v * 0.68), int(v * 0.32), v))
    os.makedirs(os.path.join(g.TEX, "misc"), exist_ok=True)
    out.save(os.path.join(g.TEX, "misc", name))


g.orange_glint = violet_glint


def _remove(*paths):
    for path in paths:
        full = os.path.join(g.ROOT, "assets", "minecraft", path)
        if os.path.isdir(full):
            shutil.rmtree(full)
        elif os.path.exists(full):
            os.remove(full)


def main():
    shutil.rmtree(os.path.join(g.ROOT, "assets"), ignore_errors=True)  # no stale files
    g.main()
    # Removed from this pack: golden apples, hearts, the small shield, armor icons.
    _remove("textures/item/golden_apple.png", "textures/item/golden_apple.png.mcmeta",
            "textures/item/enchanted_golden_apple.png", "textures/item/enchanted_golden_apple.png.mcmeta",
            "models/item/enchanted_golden_apple.json", "textures/gui/sprites/hud/heart",
            "models/item/shield.json", "models/item/shield_blocking.json")
    for name in g.ARMOR_ITEMS + g.DIAMOND_ARMOR:
        _remove("textures/item/" + name + ".png", "textures/item/" + name + ".png.mcmeta")
    # Gear.
    for name in list(GALAXY_ITEMS) + GALAXY_BY_COLOUR:
        galaxy_item(name)
    for name in g.DIAMOND_TOOLS:
        _remove("textures/item/" + name + ".png.mcmeta")
        static_tool(name)
    crystal_mace()
    for name, parent in DANISH_ITEMS.items():
        danish_variant(name, parent)
    charging_bows()
    void_shield("shield_base_nopattern", True)
    void_shield("shield_base", False)
    void_drumsticks()
    # Elytra item: plain recolour, no particles.
    _remove("textures/item/elytra.png.mcmeta")
    g.save_texture(g.ember_image("elytra.png", 0.1, 1.0), g.ELYTRA["elytra.png"])
    # pack.png: the black hole on a deep violet gradient; pack.mcmeta.
    icon = Image.new("RGBA", (64, 64))
    for y in range(64):
        t = y / 63
        for x in range(64):
            icon.putpixel((x, y), (int(10 + 40 * t), int(4 + 10 * t), int(20 + 70 * t), 255))
    icon.alpha_composite(black_hole_frame(0).resize((64, 64), Image.NEAREST))
    icon.save(os.path.join(g.ROOT, "pack.png"))
    with open(os.path.join(g.ROOT, "pack.mcmeta"), "w") as fh:
        fh.write('{\n  "pack": {\n    "description": "\\u00a75Void PvP \\u00a77- \\u00a7fblack hole textures",\n'
                 '    "min_format": 75,\n    "max_format": 75\n  }\n}\n')


if __name__ == "__main__":
    main()
