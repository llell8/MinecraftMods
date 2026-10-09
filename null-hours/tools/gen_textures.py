"""Generates the Null Hours textures: the flat entity sprites, the jumpscare faces and the icon.

Run from the null-hours folder: python3 tools/gen_textures.py
Needs Pillow. Output goes straight into src/main/resources/assets/nullhours/.

Everything is grayscale on purpose. Each sprite sheet is 128x128: the figure seen from
the front on the left half, and a mirrored copy on the right half for the back of the
flat model. Every entity has two frames (_0 and _1) that alternate while it walks.
"""
import os
import random

from PIL import Image, ImageDraw, ImageFilter, ImageOps

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nullhours")
ENTITY = os.path.join(ROOT, "textures", "entity")
FACES = os.path.join(ROOT, "textures", "gui", "jumpscare")

W, H = 64, 128  # one side of the flat sprite: one block wide, two blocks tall


def sheet(front):
    img = Image.new("RGBA", (W * 2, H), (0, 0, 0, 0))
    img.paste(front, (0, 0))
    img.paste(ImageOps.mirror(front), (W, 0))
    return img


def rough(img, rng, amount):
    """Grain on the visible pixels only."""
    px = img.load()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a:
                n = rng.randint(-amount, amount)
                v = max(0, min(255, r + n))
                px[x, y] = (v, v, v, a)
    return img


def legs(d, x_center, top, bottom, width, spread, fill):
    for side in (-1, 1):
        x = x_center + side * (spread + width // 2)
        d.rectangle((x - width // 2, top, x + width // 2, bottom), fill=fill)


def sprite_hollow(frame):
    rng = random.Random(10 + frame)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    black = (8, 8, 8, 255)
    # Small head on a long neck, narrow shoulders, arms hanging past the knees
    d.ellipse((25, 2, 39, 18), fill=black)
    d.rectangle((30, 16, 34, 24), fill=black)
    d.polygon([(20, 24), (44, 24), (40, 74), (24, 74)], fill=black)
    sway = 2 if frame else -2
    d.polygon([(20, 24), (24, 26), (18 + sway, 100), (14 + sway, 100)], fill=black)
    d.polygon([(44, 24), (40, 26), (46 - sway, 100), (50 - sway, 100)], fill=black)
    for i, x in enumerate((13, 15, 17)):
        d.line((x + sway, 100, x - 1 + sway, 108 + i), fill=black, width=1)
        d.line((51 - (x - 13) - sway, 100, 52 - (x - 13) - sway, 108 + i), fill=black, width=1)
    step = 3 if frame else 0
    d.polygon([(24, 72), (31, 72), (29 - step, 127), (25 - step, 127)], fill=black)
    d.polygon([(33, 72), (40, 72), (39 + step, 127), (35 + step, 127)], fill=black)
    # Eyes
    for x in (28, 34):
        d.rectangle((x, 9, x + 2, 10), fill=(255, 255, 255, 255))
    return rough(img, rng, 6)


def sprite_echo(frame):
    rng = random.Random(20 + frame)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    skin = (150, 150, 150, 255)
    hair = (45, 45, 45, 255)
    shirt = (95, 95, 95, 255)
    pants = (60, 60, 60, 255)
    shoes = (35, 35, 35, 255)
    # Blocky, player-like proportions: 8 px per Minecraft pixel horizontally, scaled to fit
    d.rectangle((16, 0, 47, 31), fill=skin)
    d.rectangle((16, 0, 47, 7), fill=hair)
    d.rectangle((16, 8, 19, 15), fill=hair)
    d.rectangle((44, 8, 47, 15), fill=hair)
    for x in (20, 36):
        d.rectangle((x, 16, x + 7, 19), fill=(0, 0, 0, 255))
    for x, length in ((23, 12), (38, 7)):
        d.rectangle((x, 20, x + 1, 20 + length), fill=(15, 15, 15, 255))
    d.rectangle((28, 24, 35, 27), fill=(70, 70, 70, 255))
    d.rectangle((16, 32, 47, 79), fill=shirt)
    arm = 2 if frame else -2
    d.rectangle((0, 32 + arm, 15, 79 + arm), fill=skin)
    d.rectangle((0, 32 + arm, 15, 43 + arm), fill=shirt)
    d.rectangle((48, 32 - arm, 63, 79 - arm), fill=skin)
    d.rectangle((48, 32 - arm, 63, 43 - arm), fill=shirt)
    leg = 3 if frame else 0
    d.rectangle((16, 80, 31, 127 - leg), fill=pants)
    d.rectangle((32, 80, 47, 127 - (3 - leg)), fill=pants)
    d.rectangle((16, 120 - leg, 31, 127 - leg), fill=shoes)
    d.rectangle((32, 120 - (3 - leg), 47, 127 - (3 - leg)), fill=shoes)
    return rough(img, rng, 8)


def sprite_grinner(frame):
    rng = random.Random(30 + frame)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pale = (215, 215, 215, 255)
    dark = (20, 20, 20, 255)
    # Oversized head
    d.ellipse((12, 0, 52, 40), fill=pale)
    for x in (22, 42):
        d.ellipse((x - 6, 10, x + 6, 22), fill=(0, 0, 0, 255))
        d.point((x - 1, 14), fill=(255, 255, 255, 255))
    d.chord((14, 18, 50, 36), 0, 180, fill=(0, 0, 0, 255))
    for i in range(8):
        x = 17 + i * 4
        d.polygon([(x, 27), (x + 3, 27), (x + 1, 31)], fill=(245, 245, 245, 255))
    # Thin body in a dark rag, long pale arms
    d.polygon([(24, 40), (40, 40), (42, 84), (22, 84)], fill=dark)
    sway = 2 if frame else -2
    d.polygon([(24, 42), (21, 42), (12 + sway, 104), (16 + sway, 104)], fill=pale)
    d.polygon([(40, 42), (43, 42), (52 - sway, 104), (48 - sway, 104)], fill=pale)
    step = 3 if frame else 0
    d.rectangle((24 - step, 84, 29 - step, 127), fill=pale)
    d.rectangle((35 + step, 84, 40 + step, 127), fill=pale)
    return rough(img, rng, 8)


def grain(img, rng, amount):
    px = img.load()
    for x in range(img.width):
        for y in range(img.height):
            v = px[x, y][0] + rng.randint(-amount, amount)
            v = max(0, min(255, v))
            px[x, y] = (v, v, v, 255)
    return img


def vignette(img, strength):
    w, h = img.size
    px = img.load()
    for x in range(w):
        for y in range(h):
            dx = (x - w / 2 + 0.5) / (w / 2)
            dy = (y - h / 2 + 0.5) / (h / 2)
            f = max(0.0, 1.0 - strength * (dx * dx + dy * dy))
            v = int(px[x, y][0] * f)
            px[x, y] = (v, v, v, 255)
    return img


def scanlines(img):
    px = img.load()
    for y in range(0, img.height, 2):
        for x in range(img.width):
            v = int(px[x, y][0] * 0.75)
            px[x, y] = (v, v, v, 255)
    return img


def finish_face(img, seed, noise, vig):
    rng = random.Random(seed)
    img = img.convert("L").convert("RGBA")
    return scanlines(vignette(grain(img, rng, noise), vig))


def face_hollow():
    img = Image.new("RGBA", (128, 128), (10, 10, 10, 255))
    glow = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for cx in (42, 86):
        gd.rectangle((cx - 14, 52, cx + 14, 66), fill=(160, 160, 160, 255))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(6)))
    d = ImageDraw.Draw(img)
    for cx in (42, 86):
        d.rectangle((cx - 10, 55, cx + 10, 63), fill=(255, 255, 255, 255))
    return finish_face(img, 40, 18, 0.9)


def face_echo():
    img = Image.new("RGBA", (16, 16), (150, 150, 150, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 3), fill=(45, 45, 45, 255))
    d.rectangle((0, 4, 1, 6), fill=(45, 45, 45, 255))
    d.rectangle((14, 4, 15, 6), fill=(45, 45, 45, 255))
    for x in (3, 10):
        d.rectangle((x, 7, x + 2, 8), fill=(0, 0, 0, 255))
    d.rectangle((4, 9, 4, 15), fill=(20, 20, 20, 255))
    d.rectangle((11, 9, 11, 13), fill=(20, 20, 20, 255))
    d.rectangle((6, 12, 9, 13), fill=(60, 60, 60, 255))
    img = img.resize((128, 128), Image.NEAREST)
    return finish_face(img, 41, 22, 0.8)


def face_grinner():
    img = Image.new("RGBA", (128, 128), (205, 205, 205, 255))
    d = ImageDraw.Draw(img)
    for cx in (38, 90):
        d.ellipse((cx - 18, 22, cx + 18, 60), fill=(0, 0, 0, 255))
        d.rectangle((cx - 2, 34, cx + 1, 37), fill=(255, 255, 255, 255))
    d.chord((4, 60, 124, 120), 0, 180, fill=(0, 0, 0, 255))
    for i in range(12):
        x = 10 + i * 9
        d.polygon([(x, 90), (x + 7, 90), (x + 3, 104)], fill=(240, 240, 240, 255))
        d.polygon([(x + 2, 118), (x + 8, 118), (x + 5, 104)], fill=(225, 225, 225, 255))
    return finish_face(img, 42, 20, 0.75)


def main():
    os.makedirs(ENTITY, exist_ok=True)
    os.makedirs(FACES, exist_ok=True)
    for name in ("hollow.png", "echo.png", "grinner.png"):
        path = os.path.join(ENTITY, name)
        if os.path.exists(path):
            os.remove(path)
    for frame in (0, 1):
        sheet(sprite_hollow(frame)).save(os.path.join(ENTITY, f"hollow_{frame}.png"))
        sheet(sprite_echo(frame)).save(os.path.join(ENTITY, f"echo_{frame}.png"))
        sheet(sprite_grinner(frame)).save(os.path.join(ENTITY, f"grinner_{frame}.png"))
    face_hollow().save(os.path.join(FACES, "hollow.png"))
    face_echo().save(os.path.join(FACES, "echo.png"))
    face_grinner().save(os.path.join(FACES, "grinner.png"))
    face_hollow().resize((64, 64), Image.NEAREST).save(os.path.join(ROOT, "icon.png"))


if __name__ == "__main__":
    main()
