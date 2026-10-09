"""Generates the Null Hours textures: the flat entity sprites, the jumpscare faces and the icon.

Run from the null-hours folder: python3 tools/gen_textures.py
Needs Pillow and numpy. Output goes straight into src/main/resources/assets/nullhours/.

Everything is grayscale on purpose. Shapes are drawn at 3x size and scaled down so the
outlines stay smooth, then shaded by how far each pixel is from the edge of the figure
(so the middle of a limb reads as rounder and lit) and covered in film grain.

Each sprite sheet is 512x512: the figure seen from the front on the left half (256x512)
and a mirrored copy on the right half for the back of the flat model. Every entity has two
frames (_0 and _1) that alternate while it walks.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageOps

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nullhours")
ENTITY = os.path.join(ROOT, "textures", "entity")
FACES = os.path.join(ROOT, "textures", "gui", "jumpscare")

W, H = 256, 512  # one side of the flat sprite: one block wide, two blocks tall
SS = 3  # supersampling factor


# Noise and image helpers

def fbm(w, h, seed, base=8, octaves=5):
    """Smooth fractal noise in 0..1."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w))
    amp, total, cells = 1.0, 0.0, base
    for _ in range(octaves):
        grid = rng.random((cells + 1, cells + 1)).astype(np.float32)
        layer = Image.fromarray(grid, mode="F").resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(layer)
        total += amp
        amp *= 0.5
        cells *= 2
    out /= total
    return (out - out.min()) / (out.max() - out.min() + 1e-9)


def grain(h, w, seed, amount):
    return np.random.default_rng(seed).normal(0.0, amount, (h, w))


def blur(arr, radius):
    img = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8))
    return np.asarray(img.filter(ImageFilter.GaussianBlur(radius))) / 255.0


class Canvas:
    """A grayscale drawing surface at SS times the final size, with coordinates in final pixels."""

    def __init__(self, w, h, fill=0):
        self.w, self.h = w, h
        self.img = Image.new("L", (w * SS, h * SS), fill)
        self.d = ImageDraw.Draw(self.img)

    def _p(self, pts):
        return [(x * SS, y * SS) for x, y in pts]

    def poly(self, pts, fill=255):
        self.d.polygon(self._p(pts), fill=fill)

    def ellipse(self, box, fill=255):
        x0, y0, x1, y1 = box
        self.d.ellipse((x0 * SS, y0 * SS, x1 * SS, y1 * SS), fill=fill)

    def line(self, pts, width, fill=255):
        self.d.line(self._p(pts), fill=fill, width=max(1, int(width * SS)), joint="curve")

    def limb(self, a, b, ra, rb, fill=255):
        """A tapered capsule from point a (radius ra) to point b (radius rb)."""
        (ax, ay), (bx, by) = a, b
        ang = math.atan2(by - ay, bx - ax) + math.pi / 2
        cx, cy = math.cos(ang), math.sin(ang)
        self.poly([(ax + cx * ra, ay + cy * ra), (bx + cx * rb, by + cy * rb),
                   (bx - cx * rb, by - cy * rb), (ax - cx * ra, ay - cy * ra)], fill)
        self.ellipse((ax - ra, ay - ra, ax + ra, ay + ra), fill)
        self.ellipse((bx - rb, by - rb, bx + rb, by + rb), fill)

    def array(self):
        return np.asarray(self.img.resize((self.w, self.h), Image.LANCZOS)) / 255.0


def shade(mask, base, depth, light=0.25, seed=0, texture=0.18, blur_radius=10):
    """
    Gives a flat silhouette some volume: pixels deep inside the shape are lighter, edges
    fall off into shadow, and light comes from the upper left. Returns gray values 0..1.
    """
    h, w = mask.shape
    thick = blur(mask, blur_radius)
    thick = np.clip((thick - 0.5) * 2.0, 0, 1)
    ys, xs = np.mgrid[0:h, 0:w]
    light_dir = 1.0 - (xs / w * 0.6 + ys / h * 0.4)
    tex = fbm(w, h, seed, base=12) - 0.5
    value = base * (1.0 - depth + depth * thick) + light * light_dir * thick + texture * tex * base
    return np.clip(value, 0, 1)


def to_rgba(gray, alpha):
    g = (np.clip(gray, 0, 1) * 255).astype(np.uint8)
    a = (np.clip(alpha, 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(np.dstack([g, g, g, a]), "RGBA")


def cutout(mask, ragged=0.0, seed=0):
    """
    Turns a soft mask into a hard alpha edge (Minecraft cutout rendering drops partly
    transparent pixels anyway). With ragged > 0 the edge is eaten away by noise.
    """
    if ragged:
        n = fbm(mask.shape[1], mask.shape[0], seed, base=24, octaves=4) - 0.5
        mask = blur(mask, 3) + n * ragged
    return (mask > 0.5).astype(float)


def sheet(front):
    img = Image.new("RGBA", (W * 2, H), (0, 0, 0, 0))
    img.paste(front, (0, 0))
    img.paste(ImageOps.mirror(front), (W, 0))
    return img


# Sprites

def sprite_hollow(frame):
    seed = 100 + frame
    c = Canvas(W, H)
    s = 1 if frame else -1
    cx = 128
    # Small head on a long neck
    c.ellipse((cx - 19, 8, cx + 19, 66))
    c.limb((cx, 60), (cx, 92), 9, 11)
    # Narrow, sloping torso
    c.poly([(cx - 48, 100), (cx - 30, 88), (cx + 30, 88), (cx + 48, 100), (cx + 30, 250), (cx + 20, 290),
            (cx - 20, 290), (cx - 30, 250)])
    # Arms hanging far below the hips, with long fingers
    for side in (-1, 1):
        sh = (cx + side * 42, 102)
        el = (cx + side * 52, 250)
        wr = (cx + side * (50 + 4 * s * side), 400)
        c.limb(sh, el, 13, 9)
        c.limb(el, wr, 9, 7)
        for i, spread in enumerate((-10, -4, 3, 9)):
            tip = (wr[0] + side * spread * 0.6, wr[1] + 50 + (i % 2) * 8)
            c.line([wr, (wr[0] + side * spread * 0.3, wr[1] + 25), tip], 3.4)
    # Legs: one steps forward while walking
    for side in (-1, 1):
        hip = (cx + side * 14, 285)
        knee = (cx + side * (18 + 6 * s * side), 395)
        foot = (cx + side * (16 + 10 * s * side), 505)
        c.limb(hip, knee, 13, 8)
        c.limb(knee, foot, 8, 5)
    mask = c.array()
    alpha = cutout(mask, ragged=0.3, seed=seed)
    gray = shade(mask, 0.07, 0.6, light=0.07, seed=seed, texture=0.6)
    gray = gray + grain(H, W, seed, 0.015)
    # Glowing eyes with a soft bloom
    eyes = Canvas(W, H)
    for x in (cx - 9, cx + 9):
        eyes.ellipse((x - 5, 33, x + 5, 38))
    e = eyes.array()
    gray = np.maximum(gray, np.maximum(e, blur(e, 4) * 0.7))
    return to_rgba(gray, alpha)


def sprite_echo(frame):
    seed = 200 + frame
    s = 1 if frame else -1
    cx = 128
    layers = {}

    def part(name, draw):
        c = Canvas(W, H)
        draw(c)
        layers[name] = c.array()

    # A person of ordinary build, a little too still
    part("skin", lambda c: (
        c.ellipse((cx - 27, 6, cx + 27, 76)),
        c.limb((cx, 68), (cx, 92), 12, 14),
        [c.limb((cx + side * 52, 190), (cx + side * (55 + 3 * s * side), 262), 9, 8) for side in (-1, 1)],
        [c.ellipse((cx + side * (55 + 3 * s * side) - 10, 255, cx + side * (55 + 3 * s * side) + 10, 285))
         for side in (-1, 1)],
    ))
    part("hair", lambda c: (
        c.ellipse((cx - 29, 2, cx + 29, 46)),
        c.poly([(cx - 29, 24), (cx - 26, 50), (cx - 20, 36), (cx + 20, 36), (cx + 26, 50), (cx + 29, 24)]),
    ))
    part("shirt", lambda c: (
        c.poly([(cx - 50, 100), (cx - 18, 88), (cx + 18, 88), (cx + 50, 100), (cx + 44, 250), (cx - 44, 250)]),
        [c.limb((cx + side * 46, 104), (cx + side * 53, 196), 15, 12) for side in (-1, 1)],
    ))
    part("pants", lambda c: (
        c.poly([(cx - 44, 244), (cx + 44, 244), (cx + 42, 290), (cx - 42, 290)]),
        [c.limb((cx + side * 22, 280), (cx + side * (24 + 7 * s * side), 488), 21, 15) for side in (-1, 1)],
    ))
    part("shoes", lambda c: [
        c.ellipse((cx + side * (24 + 7 * s * side) - 18, 476, cx + side * (24 + 7 * s * side) + 20, 508))
        for side in (-1, 1)])

    mask = np.clip(sum(layers.values()), 0, 1)
    alpha = cutout(mask)
    gray = np.zeros((H, W))
    tones = {"pants": 0.24, "shirt": 0.42, "shoes": 0.12, "skin": 0.68, "hair": 0.13}
    for i, (name, tone) in enumerate(tones.items()):
        m = layers[name]
        gray = gray * (1 - m) + shade(m, tone, 0.45, light=0.12, seed=seed + i, texture=0.25) * m
    # Fold lines in the shirt and jeans
    folds = fbm(W, H, seed + 9, base=6) - 0.5
    gray += folds * 0.08 * (layers["shirt"] + layers["pants"])

    face = Canvas(W, H)
    for x in (cx - 11, cx + 11):
        face.ellipse((x - 7, 36, x + 7, 46))
    eyes = blur(face.array(), 1.2)
    tears = Canvas(W, H)
    tears.line([(cx - 12, 44), (cx - 13, 58), (cx - 11, 72)], 2.2)
    tears.line([(cx + 11, 44), (cx + 12, 54)], 1.8)
    mouth = Canvas(W, H)
    mouth.ellipse((cx - 7, 58, cx + 7, 63))
    gray = gray * (1 - eyes) + 0.0 * eyes
    gray = gray * (1 - blur(tears.array(), 0.8) * 0.85)
    gray = gray * (1 - blur(mouth.array(), 1) * 0.7)
    gray += grain(H, W, seed, 0.02)
    return to_rgba(gray, alpha)


def sprite_grinner(frame):
    seed = 300 + frame
    s = 1 if frame else -1
    cx = 128
    c = Canvas(W, H)
    # Too-large head, thin body, arms that reach the knees
    c.ellipse((cx - 58, 2, cx + 58, 124))
    c.limb((cx, 112), (cx, 140), 12, 14)
    c.poly([(cx - 40, 150), (cx - 22, 138), (cx + 22, 138), (cx + 40, 150), (cx + 26, 300), (cx - 26, 300)])
    for side in (-1, 1):
        sh = (cx + side * 36, 152)
        el = (cx + side * (46 + 2 * s * side), 262)
        wr = (cx + side * (44 + 6 * s * side), 372)
        c.limb(sh, el, 11, 8)
        c.limb(el, wr, 8, 6)
        for i, spread in enumerate((-8, -3, 2, 7)):
            c.line([wr, (wr[0] + side * spread * 0.5, wr[1] + 22), (wr[0] + side * spread * 0.8, wr[1] + 44 + (i % 2) * 6)], 2.6)
    for side in (-1, 1):
        hip = (cx + side * 16, 296)
        knee = (cx + side * (20 + 7 * s * side), 400)
        foot = (cx + side * (18 + 12 * s * side), 500)
        c.limb(hip, knee, 13, 9)
        c.limb(knee, foot, 9, 7)
    mask = c.array()
    alpha = cutout(mask)
    gray = shade(mask, 0.8, 0.25, light=0.12, seed=seed, texture=0.2, blur_radius=6)

    # Ribs and a dark rag around the hips
    ribs = Canvas(W, H)
    for i in range(5):
        y = 170 + i * 18
        ribs.line([(cx - 24 + i, y), (cx - 6, y + 6)], 2)
        ribs.line([(cx + 24 - i, y), (cx + 6, y + 6)], 2)
    gray -= blur(ribs.array(), 1.5) * 0.25
    rag = Canvas(W, H)
    rag.poly([(cx - 30, 262), (cx + 30, 262), (cx + 34, 330), (cx + 10, 318), (cx - 4, 340), (cx - 18, 320), (cx - 34, 334)])
    r = rag.array() * mask
    gray = gray * (1 - r) + shade(r, 0.14, 0.4, seed=seed + 3, texture=0.5) * r

    # The face: sunken black eyes and a grin from ear to ear
    face = Canvas(W, H)
    for x in (cx - 24, cx + 24):
        face.ellipse((x - 17, 34, x + 17, 72))
    sockets = face.array()
    gray = gray * (1 - blur(sockets, 6) * 0.45)
    gray = gray * (1 - sockets)
    pupils = Canvas(W, H)
    for x in (cx - 21, cx + 21):
        pupils.ellipse((x - 2, 48, x + 2, 52))
    gray = np.maximum(gray, pupils.array())
    mouth = Canvas(W, H)
    mouth.d.chord(((cx - 50) * SS, 70 * SS, (cx + 50) * SS, 112 * SS), 0, 180, fill=255)
    m = mouth.array()
    gray = gray * (1 - blur(m, 3) * 0.3)
    gray = gray * (1 - m)
    teeth = Canvas(W, H)
    for i in range(13):
        x = cx - 45 + i * 7.5
        top = 91 - 2 * abs(i - 6) * 0.3
        teeth.poly([(x, top - 1), (x + 6, top - 1), (x + 3, top + 9)])
        bottom = 108 - abs(i - 6) * 1.4
        teeth.poly([(x + 1, bottom), (x + 6, bottom), (x + 3.5, bottom - 8)])
    gray = np.maximum(gray, teeth.array() * m * 0.92)
    gray += grain(H, W, seed, 0.02)
    return to_rgba(gray, alpha)


# Jumpscare faces (256x256, shown full screen)

F = 256


def finish_face(gray, seed, vignette=0.85, noise=0.06):
    ys, xs = np.mgrid[0:F, 0:F]
    d = ((xs - F / 2) / (F / 2)) ** 2 + ((ys - F / 2) / (F / 2)) ** 2
    gray = gray * np.clip(1.0 - vignette * d * 0.8, 0, 1)
    gray = gray + grain(F, F, seed, noise)
    gray[::2, :] *= 0.86  # scanlines
    return to_rgba(gray, np.ones_like(gray))


def face_hollow():
    seed = 400
    gray = np.full((F, F), 0.03)
    head = Canvas(F, F)
    head.ellipse((60, 10, 196, 270))
    h = blur(head.array(), 18)
    gray += h * fbm(F, F, seed) * 0.07
    eyes = Canvas(F, F)
    for x in (98, 158):
        eyes.ellipse((x - 16, 104, x + 16, 118))
    e = eyes.array()
    gray = np.maximum(gray, blur(e, 14) * 0.55)
    gray = np.maximum(gray, blur(e, 3))
    return finish_face(gray, seed, vignette=0.9, noise=0.05)


def face_echo():
    seed = 401
    gray = np.full((F, F), 0.05)
    head = Canvas(F, F)
    head.ellipse((38, 4, 218, 280))
    h = head.array()
    skin = shade(h, 0.62, 0.55, light=0.15, seed=seed, texture=0.25, blur_radius=40)
    gray = gray * (1 - h) + skin * h
    hair = Canvas(F, F)
    hair.ellipse((30, -40, 226, 80))
    # Uneven fringe: strands hanging down to different lengths
    for i in range(22):
        x = 44 + i * 7.6
        length = 88 + 22 * math.sin(i * 1.7) + (i % 3) * 6
        hair.limb((x, 60), (x + math.sin(i) * 4, length), 6, 1.5)
    hr = blur(hair.array(), 1.5) * h
    hr = np.clip(hr - (fbm(F, F, seed + 2, base=40) - 0.5) * 0.4, 0, 1)
    gray = gray * (1 - hr) + (0.1 + fbm(F, F, seed + 1, base=30) * 0.08) * hr
    # Nose and cheek shadows
    shadow = Canvas(F, F)
    shadow.poly([(128, 120), (120, 172), (138, 172)])
    shadow.ellipse((50, 150, 92, 210))
    shadow.ellipse((164, 150, 206, 210))
    gray *= 1 - blur(shadow.array(), 10) * 0.25
    # Empty eye sockets and what runs out of them
    sockets = Canvas(F, F)
    for x in (88, 168):
        sockets.ellipse((x - 24, 106, x + 24, 136))
    so = sockets.array()
    gray *= 1 - blur(so, 10) * 0.6
    gray *= 1 - blur(so, 2)
    streaks = Canvas(F, F)
    streaks.line([(80, 130), (78, 170), (82, 210), (79, 256)], 6)
    streaks.line([(96, 132), (98, 160)], 4)
    streaks.line([(172, 132), (170, 185), (174, 220)], 5)
    gray *= 1 - blur(streaks.array(), 1.5) * 0.9
    mouth = Canvas(F, F)
    mouth.ellipse((104, 196, 152, 216))
    gray *= 1 - blur(mouth.array(), 3) * 0.85
    return finish_face(gray, seed)


def face_grinner():
    seed = 402
    gray = np.full((F, F), 0.04)
    head = Canvas(F, F)
    head.ellipse((-20, -20, 276, 290))
    h = head.array()
    gray = gray * (1 - h) + shade(h, 0.8, 0.45, light=0.1, seed=seed, texture=0.2, blur_radius=50) * h
    sockets = Canvas(F, F)
    for x in (76, 180):
        sockets.ellipse((x - 40, 40, x + 40, 126))
    so = sockets.array()
    gray *= 1 - blur(so, 16) * 0.5
    gray *= 1 - so
    pupils = Canvas(F, F)
    for x in (82, 174):
        pupils.ellipse((x - 4, 76, x + 4, 84))
    gray = np.maximum(gray, blur(pupils.array(), 1))
    mouth = Canvas(F, F)
    mouth.d.chord((6 * SS, 128 * SS, 250 * SS, 240 * SS), 0, 180, fill=255)
    m = mouth.array()
    gray *= 1 - blur(m, 8) * 0.4
    gray *= 1 - m
    teeth = Canvas(F, F)
    for i in range(16):
        x = 12 + i * 14.5
        top = 184 - abs(i - 7.5) * 1.2
        teeth.poly([(x, top - 2), (x + 12, top - 2), (x + 6, top + 20)])
        bottom = 236 - abs(i - 7.5) ** 1.6
        teeth.poly([(x + 2, bottom), (x + 12, bottom), (x + 7, bottom - 18)])
    gray = np.maximum(gray, shade(teeth.array(), 0.85, 0.3, seed=seed + 1, texture=0.15, blur_radius=2) * teeth.array() * m)
    # Creases at the corners of the grin
    creases = Canvas(F, F)
    creases.line([(10, 184), (2, 150)], 3)
    creases.line([(246, 184), (254, 150)], 3)
    gray *= 1 - blur(creases.array(), 2) * 0.5
    return finish_face(gray, seed, vignette=0.7)


def main():
    os.makedirs(ENTITY, exist_ok=True)
    os.makedirs(FACES, exist_ok=True)
    for frame in (0, 1):
        sheet(sprite_hollow(frame)).save(os.path.join(ENTITY, f"hollow_{frame}.png"))
        sheet(sprite_echo(frame)).save(os.path.join(ENTITY, f"echo_{frame}.png"))
        sheet(sprite_grinner(frame)).save(os.path.join(ENTITY, f"grinner_{frame}.png"))
    face_hollow().save(os.path.join(FACES, "hollow.png"))
    face_echo().save(os.path.join(FACES, "echo.png"))
    face_grinner().save(os.path.join(FACES, "grinner.png"))
    face_hollow().resize((128, 128), Image.LANCZOS).save(os.path.join(ROOT, "icon.png"))


if __name__ == "__main__":
    main()
