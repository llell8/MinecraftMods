#!/usr/bin/env python3
"""Builds the Phoenix PvP resource pack from vanilla Minecraft textures.

Every texture keeps its vanilla shape and shading: the generator only
gradient-maps the vanilla pixels onto phoenix fire palettes, so the pack
reads like vanilla at a glance, just burning.

Usage:  python3 generate.py [--zip]
Vanilla textures are fetched once from a mirror of the 1.21.11 assets and
cached in .vanilla-cache/ (not committed).
"""
import colorsys
import json
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

from PIL import Image

MC_VERSION = "1.21.11"
PACK_FORMAT = 75
MIRROR = f"https://raw.githubusercontent.com/misode/mcmeta/{MC_VERSION}-assets/assets/minecraft/textures/"

ROOT = Path(__file__).resolve().parent
CACHE = ROOT / ".vanilla-cache" / MC_VERSION
OUT = ROOT / "pack"
TEX = OUT / "assets" / "minecraft" / "textures"

# Palettes, dark to light. Each tier gets its own fire so gear stays readable mid-fight.
PHOENIX = [(38, 4, 6), (112, 10, 10), (190, 34, 12), (240, 96, 18), (255, 170, 44), (255, 236, 150)]
ASHEN = [(14, 10, 12), (32, 24, 26), (54, 42, 42), (84, 58, 50), (150, 62, 30), (236, 128, 40)]
SOLAR = [(64, 26, 2), (140, 62, 4), (220, 128, 10), (255, 196, 36), (255, 246, 170)]
WHITE_FLAME = [(58, 30, 22), (132, 92, 66), (210, 170, 128), (252, 224, 180), (255, 252, 236)]
EMBER_STONE = [(26, 16, 14), (62, 34, 26), (108, 54, 32), (162, 86, 40), (204, 128, 62)]
CHARRED = [(18, 8, 4), (44, 18, 8), (88, 34, 12), (150, 62, 18), (196, 104, 34)]
RUST = [(30, 12, 8), (78, 34, 18), (140, 70, 34), (200, 120, 60), (240, 180, 110)]
CRIMSON = [(48, 2, 6), (120, 6, 14), (196, 22, 20), (250, 80, 40), (255, 190, 120)]
# Light, warm GUI tone; keeps dark labels readable on container backgrounds.
GUI_WARM = [(20, 6, 4), (70, 26, 14), (140, 70, 40), (200, 140, 100), (236, 196, 156), (255, 240, 220)]
GUI_DARK = [(14, 4, 4), (40, 10, 10), (86, 22, 14), (170, 64, 20), (255, 160, 50)]

STICK = {(40, 30, 11), (73, 54, 21), (104, 78, 30), (137, 103, 39)}


def fetch(path):
    dest = CACHE / path
    if not dest.exists():
        dest.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(MIRROR + path) as r:
            dest.write_bytes(r.read())
    return dest


def load(path):
    return Image.open(fetch(path)).convert("RGBA")


def save(img, path):
    dest = TEX / path
    dest.parent.mkdir(parents=True, exist_ok=True)
    img.save(dest, optimize=True)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def sat(c):
    return colorsys.rgb_to_hsv(*(v / 255 for v in c[:3]))[1]


def sample(grad, t):
    t = min(max(t, 0.0), 1.0) * (len(grad) - 1)
    i = min(int(t), len(grad) - 2)
    f = t - i
    return tuple(round(a + (b - a) * f) for a, b in zip(grad[i], grad[i + 1]))


def gradient_map(img, grad, select=lambda c: True, absolute=False, lo=0.0, hi=1.0):
    """Recolours the selected pixels along grad by brightness.

    By default brightness is taken relative to the selected pixels (half rank, half
    linear) so every vanilla shade stays distinct; absolute=True maps raw luminance
    instead, which keeps GUI contrast exactly as vanilla has it.
    """
    px = img.load()
    w, h = img.size
    picked = {px[x, y][:3] for y in range(h) for x in range(w) if px[x, y][3] and select(px[x, y][:3])}
    if not picked:
        return img
    if absolute:
        pos = {c: lum(c) / 255 for c in picked}
    else:
        order = sorted(picked, key=lum)
        lmin, lmax = lum(order[0]), lum(order[-1])
        span = (lmax - lmin) or 1
        n = max(len(order) - 1, 1)
        pos = {c: 0.5 * (i / n) + 0.5 * (lum(c) - lmin) / span for i, c in enumerate(order)}
    for y in range(h):
        for x in range(w):
            p = px[x, y]
            if p[3] and p[:3] in pos:
                px[x, y] = sample(grad, lo + (hi - lo) * pos[p[:3]]) + (p[3],)
    return img


def not_stick(c):
    return c not in STICK


def recolor(path, grad, out=None, **kw):
    save(gradient_map(load(path), grad, **kw), out or path)


def copy_meta(path):
    dest = TEX / (path + ".mcmeta")
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(fetch(path + ".mcmeta"), dest)


# --- weapons & armour -------------------------------------------------------------

TIERS = {
    # item prefix: equipment model name, palette, where on the palette the shades start
    "wooden": (None, CHARRED, 0.0),
    "stone": (None, EMBER_STONE, 0.0),
    "iron": ("iron", WHITE_FLAME, 0.0),
    "golden": ("gold", SOLAR, 0.0),
    "diamond": ("diamond", PHOENIX, 0.2),
    "netherite": ("netherite", ASHEN, 0.0),
    "chainmail": ("chainmail", RUST, 0.0),
}


def gear():
    for tier, (model, grad, lo) in TIERS.items():
        if tier != "chainmail":
            # Handles keep their vanilla stick colours; netherite has its own dark handle.
            sel = (lambda c: True) if tier == "netherite" else not_stick
            for tool in ("sword", "axe"):
                recolor(f"item/{tier}_{tool}.png", grad, lo=lo, select=sel)
        if tier not in ("wooden", "stone"):
            for piece in ("helmet", "chestplate", "leggings", "boots"):
                recolor(f"item/{tier}_{piece}.png", grad, lo=lo)
        if model:
            recolor(f"entity/equipment/humanoid/{model}.png", grad, lo=lo)
            recolor(f"entity/equipment/humanoid_leggings/{model}.png", grad, lo=lo)

    # Mace: phoenix head on the vanilla breeze-rod grip.
    recolor("item/mace.png", SOLAR)

    # Shield: charred planks, gold rim.
    shield = load("entity/shield_base_nopattern.png")
    gradient_map(shield, CHARRED, select=lambda c: sat(c) >= 0.25, lo=0.2)
    gradient_map(shield, SOLAR, select=lambda c: sat(c) < 0.25)
    save(shield, "entity/shield_base_nopattern.png")

    # Bows and crossbows: charred wood, glowing string.
    for name in ("bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2",
                 "crossbow_standby", "crossbow_pulling_0", "crossbow_pulling_1",
                 "crossbow_pulling_2", "crossbow_arrow", "crossbow_firework"):
        img = load(f"item/{name}.png")
        gradient_map(img, CHARRED, select=lambda c: sat(c) >= 0.3 and c[0] > c[2])
        gradient_map(img, SOLAR, select=lambda c: sat(c) < 0.3 and lum(c) > 90, lo=0.55)
        save(img, f"item/{name}.png")

    # Arrows: flame fletching, vanilla shaft and head.
    for path in ("item/arrow.png", "entity/projectiles/arrow.png"):
        recolor(path, PHOENIX, select=lambda c: sat(c) < 0.15 and lum(c) > 150, lo=0.5)


# --- consumables & utility ----------------------------------------------------------

def utility():
    recolor("item/totem_of_undying.png", PHOENIX)  # the phoenix itself: rebirth on death
    recolor("item/ender_pearl.png", CRIMSON)
    recolor("item/golden_apple.png", SOLAR)  # enchanted gapple reuses this with glint
    recolor("item/elytra.png", PHOENIX)
    recolor("entity/equipment/wings/elytra.png", PHOENIX)
    recolor("item/end_crystal.png", PHOENIX)
    recolor("entity/end_crystal/end_crystal.png", PHOENIX)
    recolor("item/experience_bottle.png", SOLAR, select=lambda c: sat(c) > 0.3)


# --- HUD, GUI & effects ---------------------------------------------------------------

def hud():
    recolor("gui/sprites/hud/hotbar.png", GUI_DARK)
    recolor("gui/sprites/hud/hotbar_selection.png", SOLAR, lo=0.3)
    for heart in ("full", "half", "full_blinking", "half_blinking", "hardcore_full", "hardcore_half"):
        recolor(f"gui/sprites/hud/heart/{heart}.png", CRIMSON, select=lambda c: sat(c) > 0.2 or lum(c) > 200, lo=0.15)
    recolor("gui/sprites/hud/experience_bar_progress.png", SOLAR)
    recolor("gui/sprites/hud/experience_bar_background.png", GUI_DARK)
    for gui in ("inventory", "generic_54", "shulker_box"):
        recolor(f"gui/container/{gui}.png", GUI_WARM, absolute=True)
    recolor("gui/sprites/widget/button.png", GUI_DARK, absolute=True, hi=0.8)
    recolor("gui/sprites/widget/button_highlighted.png", PHOENIX, absolute=True, lo=0.1, hi=0.9)
    recolor("gui/sprites/widget/button_disabled.png", ASHEN, absolute=True, hi=0.6)


def glint():
    # Orange flame glint instead of purple; the glint is additive, so brightness is all that matters.
    for path in ("misc/enchanted_glint_item.png", "misc/enchanted_glint_armor.png"):
        img = load(path)
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                v = max(r, g, b)
                px[x, y] = (v, round(v * 0.5), round(v * 0.12), a)
        save(img, path)


def low_fire():
    # The on-screen burning overlay draws fire_1; drop each frame's flames 6px so they stay out of view.
    img = load("block/fire_1.png")
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    for top in range(0, img.height, 16):
        frame = img.crop((0, top, 16, top + 10))
        out.paste(frame, (0, top + 6))
    save(out, "block/fire_1.png")
    copy_meta("block/fire_1.png")


# --- pack metadata ----------------------------------------------------------------

def pack_icon():
    size = 128
    icon = Image.new("RGBA", (size, size))
    px = icon.load()
    for y in range(size):
        for x in range(size):
            d = ((x - size / 2) ** 2 + (y - size * 0.6) ** 2) ** 0.5 / (size * 0.75)
            px[x, y] = sample(PHOENIX, 1 - d) + (255,)
    totem = Image.open(TEX / "item/totem_of_undying.png").resize((96, 96), Image.NEAREST)
    icon.alpha_composite(totem, (16, 16))
    icon.save(OUT / "pack.png", optimize=True)


def pack_meta():
    meta = {
        "pack": {
            "description": "§6§lPhoenix PvP§r §c— rise from the ashes",
            "pack_format": PACK_FORMAT,
            "min_format": PACK_FORMAT,
            "max_format": PACK_FORMAT,
        }
    }
    (OUT / "pack.mcmeta").write_text(json.dumps(meta, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    TEX.mkdir(parents=True)
    gear()
    utility()
    hud()
    glint()
    low_fire()
    pack_icon()
    pack_meta()
    if "--zip" in sys.argv:
        dist = ROOT / "build"
        dist.mkdir(exist_ok=True)
        zpath = dist / "Phoenix-PvP.zip"
        with zipfile.ZipFile(zpath, "w", zipfile.ZIP_DEFLATED) as z:
            for f in sorted(OUT.rglob("*")):
                if f.is_file():
                    z.write(f, f.relative_to(OUT).as_posix())
        print(f"wrote {zpath}")
    print(f"generated {sum(1 for f in OUT.rglob('*') if f.is_file())} files in {OUT}")


if __name__ == "__main__":
    main()
