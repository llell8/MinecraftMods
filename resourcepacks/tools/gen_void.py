"""Generates the Void PvP pack: same pieces as Phoenix PvP, re-themed purple,
white and black around a black hole / void look.

Run: python3 resourcepacks/tools/gen_void.py

It reuses gen_textures.py (the Phoenix generator) and swaps its palettes and a
few drawing functions before running it with VoidPvP as the output folder:
- flames -> a "void pull": dotted particle rings rippling inward toward tools,
  armor icons, apples and the elytra, with twinkling white stars
- blaze rod handles -> dark void rods
- orbiting fire -> a white-to-purple accretion disk around pearls/wind charges
- phoenix totem -> an animated black hole
- feathers (hunger) -> small black-hole orbs, with the AppleSkin outline traced around them
- orange glint -> violet glint
- easter egg: a mace renamed "Crushing Swedes" gets a Danish-flag head
- netherite sword and axe: galaxy head (drifting nebula, twinkling stars, bright edge)
- mace: black hole head (void core, pulsing photon ring, orbiting accretion comets)
- all other gear (netherite/diamond tools, spears, armor icons): stardust twinkling
  around the head and a pulsing star gem, instead of the void pull
"""
import math
import os

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
g.DANISH_MACE = True  # rename a mace to "Crushing Swedes" for a Danish-flag head

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
GALAXY_ITEMS = {  # items whose head is recoloured as a galaxy
    "netherite_sword": (range(6, 16), 9),
    "netherite_axe": (range(6, 15), 8),
}


# All other gear keeps its normal look but gets the same animation: stardust
# twinkling around the head and (tools/weapons) a pulsing star gem. Its head
# area is found by colour (see galaxy_region). Pearls and wind charges keep
# their accretion disk.
GALAXY_GEAR = (list(g.TOOLS) + list(g.DIAMOND_TOOLS)
               + g.ARMOR_ITEMS + g.DIAMOND_ARMOR)


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
    base, gem = _galaxy_base(name)
    galaxy = name in GALAXY_ITEMS
    if galaxy:
        base = _void_trim(base)
    region = galaxy_region(base, name, gem)
    w, h = base.size
    solid = {(x, y) for y in range(h) for x in range(w) if base.getpixel((x, y))[3]}
    head_x = range(max(0, min(x for x, _ in region) - 2), w)
    head_y = min(h - 1, max(y for _, y in region) + 2)
    near_region = {(x + dx, y + dy) for x, y in region for dx in range(-3, 4) for dy in range(-3, 4)}
    dust = [(x, y) for y in range(head_y + 1) for x in head_x if (x, y) not in solid
            and (x, y) in near_region
            and 1 <= min(max(abs(x - sx), abs(y - sy)) for sx, sy in solid) <= 2
            and (x * 7 + y * 13) % 5 == 0]
    frames = []
    for f in range(g.FRAMES):
        img = galaxy_blade(base, f, region) if galaxy else base.copy()
        for i, (x, y) in enumerate(dust):
            twinkle = math.sin(f / g.FRAMES * 2 * math.pi * 2 + i * 2.1)
            if twinkle > 0.5:
                img.putpixel((x, y), g.rgba((255, 255, 255) if twinkle > 0.85 else VOID[3]))
        if gem:
            pulse = 0.5 + 0.5 * math.sin(f / g.FRAMES * 2 * math.pi)
            img.putpixel(gem, g.rgba((255, 255, 255) if pulse > 0.5 else VOID[3]))
        frames.append(img)
    g.save_animation(frames, "item/" + name + ".png")


# The mace is its own thing: its head is a small black hole (a void core
# shading to deep purple, rimmed by a pulsing white photon ring) with two
# accretion comets orbiting the head. Handle and gem stay.
MACE_FRAMES = 16


def black_hole_mace():
    base, gem = g.load_mace(), g.MACE[1]
    # the whole head as one solid shape: every non-handle pixel up top
    head = {(x, y) for y in range(11) for x in range(5, 16) if (x, y) != gem
            and base.getpixel((x, y))[3] and base.getpixel((x, y))[:3] not in ROD_COLOURS}
    cx = sum(x for x, _ in head) / len(head)
    cy = sum(y for _, y in head) / len(head)
    rim = {p for p in head if any((p[0] + dx, p[1] + dy) not in head
                                  for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    solid = {(x, y) for y in range(16) for x in range(16) if base.getpixel((x, y))[3]}
    orbit = [(x, y) for y in range(16) for x in range(16) if (x, y) not in solid
             and min(max(abs(x - hx), abs(y - hy)) for hx, hy in head) == 1]
    frames = []
    for f in range(MACE_FRAMES):
        phase = f / MACE_FRAMES * 2 * math.pi
        img = base.copy()
        for x, y in head:
            d = math.hypot(x - cx, y - cy)
            img.putpixel((x, y), g.rgba(g.ramp([HOLE_CORE, (30, 10, 56), (70, 28, 124)], d / 5)))
        for x, y in rim:
            glow = 0.5 + 0.5 * math.sin(phase * 2 + math.atan2(y - cy, x - cx) * 2)
            img.putpixel((x, y), g.rgba(g.ramp([VOID[3], (255, 255, 255)], glow)))
        head_angle = f / MACE_FRAMES * math.pi       # half a turn per loop, two comets
        for x, y in orbit:
            angle = math.atan2(y - cy, x - cx)
            for comet in (head_angle, head_angle + math.pi):
                behind = (comet - angle) % (2 * math.pi)
                if behind < 1.6:
                    img.putpixel((x, y), g.rgba(g.ORBIT_FIRE[min(4, int(behind / 1.6 * 5))]))
        pulse = 0.5 + 0.5 * math.sin(phase)
        img.putpixel(gem, g.rgba((255, 255, 255) if pulse > 0.5 else VOID[3]))
        frames.append(img)
    w, h = frames[0].size
    strip = Image.new("RGBA", (w, h * MACE_FRAMES))
    for i, fr in enumerate(frames):
        strip.paste(fr, (0, i * h))
    g.save_texture(strip, "item/mace.png")
    with open(os.path.join(g.TEX, "item", "mace.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 2\n  }\n}\n')


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


def main():
    g.main()
    for name in dict.fromkeys(list(GALAXY_ITEMS) + GALAXY_GEAR):
        galaxy_item(name)
    black_hole_mace()
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
