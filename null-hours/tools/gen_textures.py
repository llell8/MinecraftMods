"""Generates the Null Hours textures (entity skins, jumpscare faces and the mod icon).

Run from the null-hours folder: python3 tools/gen_textures.py
Needs Pillow. Output goes straight into src/main/resources/assets/nullhours/.
"""
import os
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nullhours")
ENTITY = os.path.join(ROOT, "textures", "entity")
FACES = os.path.join(ROOT, "textures", "gui", "jumpscare")

# Boxes of the standard 64x64 humanoid layout, as (x, y, w, d, h) of the unfolded cube.
# Left limbs are painted both at their own spot and over the right limbs, since the
# zombie mesh mirrors the right-limb UVs.
HEAD = (0, 0, 8, 8, 8)
HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 4, 12)
RIGHT_ARM = (40, 16, 4, 4, 12)
LEFT_ARM = (32, 48, 4, 4, 12)
RIGHT_LEG = (0, 16, 4, 4, 12)
LEFT_LEG = (16, 48, 4, 4, 12)


def noisy(color, rng, amount):
    return tuple(max(0, min(255, c + rng.randint(-amount, amount))) for c in color[:3]) + (255,)


def paint_box(img, box, color, rng, amount=8):
    x, y, w, d, h = box
    for px in range(x, x + 2 * (w + d)):
        for py in range(y, y + d + h):
            # The two top-row corners of the unfolded cube are unused
            if py < y + d and (px < x + d or px >= x + d + 2 * w):
                continue
            img.putpixel((px, py), noisy(color, rng, amount))


def paint_rows(img, box, rows, rng, amount=6):
    """Paints the side faces of a box in horizontal bands: rows is [(height, color), ...]."""
    x, y, w, d, h = box
    top = y + d
    for band_h, color in rows:
        for py in range(top, min(top + band_h, y + d + h)):
            for px in range(x, x + 2 * (w + d)):
                img.putpixel((px, py), noisy(color, rng, amount))
        top += band_h


def face(box):
    """Top-left of the front face of a head-sized box."""
    x, y, w, d, h = box
    return x + d, y + d


def skin_hollow():
    rng = random.Random(1)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    black = (12, 10, 16)
    for box in (HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG):
        paint_box(img, box, black, rng, 5)
    fx, fy = face(HEAD)
    for ex in (1, 2, 5, 6):
        img.putpixel((fx + ex, fy + 4), (255, 255, 255, 255))
    for ex in (1, 6):
        img.putpixel((fx + ex, fy + 3), (90, 90, 100, 255))
    return img


def skin_echo():
    rng = random.Random(2)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = (176, 122, 92)
    hair = (52, 34, 20)
    shirt = (0, 158, 160)
    pants = (60, 52, 150)
    shoes = (70, 70, 70)
    paint_box(img, HEAD, skin, rng, 6)
    # Hair over the top, back and upper sides of the head
    x, y, w, d, h = HEAD
    for px in range(x + d, x + d + w):
        for py in range(y, y + d):
            img.putpixel((px, py), noisy(hair, rng, 6))
    for px in range(x, x + 2 * (w + d)):
        for py in range(y + d, y + d + 2):
            img.putpixel((px, py), noisy(hair, rng, 6))
    for px in range(x + 2 * d + w, x + 2 * (w + d)):
        for py in range(y + d, y + d + h):
            img.putpixel((px, py), noisy(hair, rng, 6))
    fx, fy = face(HEAD)
    # Hollow black eyes with a dark streak running down from each
    for ex in (1, 2, 5, 6):
        img.putpixel((fx + ex, fy + 4), (0, 0, 0, 255))
    for ex, length in ((2, 3), (5, 2)):
        for dy in range(1, length + 1):
            img.putpixel((fx + ex, fy + 4 + dy), (70, 8, 8, 255))
    for mx in range(3, 5):
        img.putpixel((fx + mx, fy + 6), (92, 56, 40, 255))
    paint_box(img, BODY, shirt, rng, 6)
    for box in (RIGHT_ARM, LEFT_ARM):
        paint_box(img, box, skin, rng, 6)
        paint_rows(img, box, [(4, shirt)], rng)
    for box in (RIGHT_LEG, LEFT_LEG):
        paint_box(img, box, pants, rng, 6)
        paint_rows(img, box, [(10, pants), (2, shoes)], rng)
    return img


def skin_grinner():
    rng = random.Random(3)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pale = (214, 210, 200)
    cloth = (24, 22, 22)
    paint_box(img, HEAD, pale, rng, 8)
    fx, fy = face(HEAD)
    # Big black eyes with a white pinprick
    for ex, ey in ((1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3)):
        img.putpixel((fx + ex, fy + ey), (0, 0, 0, 255))
    img.putpixel((fx + 2, fy + 2), (255, 255, 255, 255))
    img.putpixel((fx + 5, fy + 2), (255, 255, 255, 255))
    # Grin from edge to edge: a black mouth with teeth
    for mx in range(0, 8):
        img.putpixel((fx + mx, fy + 6), (255, 255, 250, 255) if mx % 2 == 0 else (10, 0, 0, 255))
    for mx in range(1, 7):
        img.putpixel((fx + mx, fy + 5), (10, 0, 0, 255))
    img.putpixel((fx + 0, fy + 5), (120, 20, 20, 255))
    img.putpixel((fx + 7, fy + 5), (120, 20, 20, 255))
    paint_box(img, BODY, cloth, rng, 5)
    for box in (RIGHT_ARM, LEFT_ARM):
        paint_box(img, box, pale, rng, 8)
        paint_rows(img, box, [(5, cloth)], rng)
    for box in (RIGHT_LEG, LEFT_LEG):
        paint_box(img, box, cloth, rng, 5)
    return img


def grain(img, rng, amount):
    px = img.load()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            n = rng.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), a)
    return img


def vignette(img, strength):
    w, h = img.size
    px = img.load()
    for x in range(w):
        for y in range(h):
            dx = (x - w / 2 + 0.5) / (w / 2)
            dy = (y - h / 2 + 0.5) / (h / 2)
            f = max(0.0, 1.0 - strength * (dx * dx + dy * dy))
            r, g, b, a = px[x, y]
            px[x, y] = (int(r * f), int(g * f), int(b * f), a)
    return img


def face_hollow():
    rng = random.Random(10)
    img = Image.new("RGBA", (64, 64), (8, 6, 12, 255))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for cx in (20, 44):
        gd.ellipse((cx - 9, 22, cx + 9, 36), fill=(170, 170, 200, 255))
    glow = glow.filter(ImageFilter.GaussianBlur(4))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)
    for cx in (20, 44):
        d.ellipse((cx - 6, 25, cx + 6, 33), fill=(255, 255, 255, 255))
    # A long, thin mouth that is barely visible
    d.line((24, 50, 40, 51), fill=(30, 26, 34, 255), width=1)
    return vignette(grain(img, rng, 10), 0.9)


def face_echo():
    rng = random.Random(11)
    # Drawn on an 16x16 grid, then scaled up so it keeps the blocky look
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    skin = (176, 122, 92)
    hair = (52, 34, 20)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), noisy(hair if y < 3 or (y < 5 and x in (0, 15)) else skin, rng, 10))
    for x in (3, 4, 5, 10, 11, 12):
        for y in (7, 8):
            img.putpixel((x, y), (0, 0, 0, 255))
    for x, length in ((4, 6), (11, 4), (5, 2)):
        for y in range(9, 9 + length):
            img.putpixel((x, min(15, y)), (rng.randint(80, 110), 6, 6, 255))
    for x in range(6, 10):
        img.putpixel((x, 12), (20, 6, 6, 255))
        img.putpixel((x, 13), (20, 6, 6, 255) if x in (7, 8) else (92, 56, 40, 255))
    img = img.resize((64, 64), Image.NEAREST)
    return vignette(grain(img, rng, 14), 0.8)


def face_grinner():
    rng = random.Random(12)
    img = Image.new("RGBA", (64, 64), (210, 206, 196, 255))
    d = ImageDraw.Draw(img)
    for cx in (19, 45):
        d.ellipse((cx - 9, 12, cx + 9, 30), fill=(0, 0, 0, 255))
        d.rectangle((cx - 1, 18, cx, 19), fill=(255, 255, 255, 255))
    # Grin that splits the face
    d.chord((2, 30, 62, 60), 0, 180, fill=(14, 0, 0, 255))
    for i in range(12):
        x = 5 + i * 5
        d.polygon([(x, 45), (x + 4, 45), (x + 2, 52)], fill=(250, 248, 236, 255))
        d.polygon([(x + 1, 58), (x + 4, 58), (x + 2, 52)], fill=(236, 232, 220, 255))
    d.line((2, 45, 62, 45), fill=(120, 14, 14, 255), width=1)
    return vignette(grain(img, rng, 12), 0.75)


def main():
    os.makedirs(ENTITY, exist_ok=True)
    os.makedirs(FACES, exist_ok=True)
    skin_hollow().save(os.path.join(ENTITY, "hollow.png"))
    skin_echo().save(os.path.join(ENTITY, "echo.png"))
    skin_grinner().save(os.path.join(ENTITY, "grinner.png"))
    face_hollow().save(os.path.join(FACES, "hollow.png"))
    face_echo().save(os.path.join(FACES, "echo.png"))
    face_grinner().save(os.path.join(FACES, "grinner.png"))
    face_hollow().save(os.path.join(ROOT, "icon.png"))


if __name__ == "__main__":
    main()
