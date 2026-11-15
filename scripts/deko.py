import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/hideandseek'
FUNCTIONS = ROOT / 'datapacks/sniffa-props/data/sniffa/function'

ATLAS = 256
TILE = 32
COLUMNS = ATLAS // TILE
TEXTURE = 'hideandseek:item/props/deko'


def clamp(value):
    return max(0, min(255, int(round(value))))


def shade(colour, amount):
    return tuple(clamp(c * amount) for c in colour[:3]) + (colour[3] if len(colour) > 3 else 255,)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def hexcolour(value):
    value = value.lstrip('#')
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


class Tile:
    def __init__(self, name, index, density=1.0, span=1):
        self.name = name
        self.index = index
        self.density = density
        self.span = span
        self.column = index % COLUMNS
        self.row = index // COLUMNS


class Atlas:
    def __init__(self):
        self.image = Image.new('RGBA', (ATLAS, ATLAS), (0, 0, 0, 0))
        self.tiles = {}
        self.used = 0

    def add(self, name, painter, density=1.0, seed=None, span=1):
        if span > 1 and self.used % COLUMNS + span > COLUMNS:
            self.used += COLUMNS - self.used % COLUMNS
        tile = Tile(name, self.used, density, span)
        self.used += span
        if tile.row >= COLUMNS:
            raise ValueError('atlas full')
        pixels = Image.new('RGBA', (TILE * span, TILE), (0, 0, 0, 0))
        painter(pixels, random.Random(seed if seed is not None else name))
        self.image.paste(pixels, (tile.column * TILE, tile.row * TILE))
        self.tiles[name] = tile
        return tile


def fill_noise(img, rnd, base, spread=0.06, grain=None):
    for y in range(TILE):
        for x in range(TILE):
            amount = 1 + rnd.uniform(-spread, spread)
            if grain:
                amount += grain(x, y)
            img.putpixel((x, y), shade(base, amount))


def paper(base, pattern):
    def paint(img, rnd):
        light = shade(base, 1.28)
        fill_noise(img, rnd, base, 0.04)
        for y in range(TILE):
            for x in range(TILE):
                if pattern == 'dots' and (x % 6 == 2 and y % 6 == 2 or x % 6 == 5 and y % 6 == 5):
                    img.putpixel((x, y), light)
                elif pattern == 'stripes' and (x + y) % 8 in (0, 1):
                    img.putpixel((x, y), light)
                elif pattern == 'stars' and ((x % 8, y % 8) in ((3, 2), (2, 3), (3, 3), (4, 3), (3, 4))):
                    img.putpixel((x, y), light)
                elif pattern == 'checks' and ((x // 4) + (y // 4)) % 2 == 0:
                    img.putpixel((x, y), shade(base, 1.12 + rnd.uniform(-0.03, 0.03)))
    return paint


def ribbon(base):
    def paint(img, rnd):
        fill_noise(img, rnd, base, 0.04)
        for y in range(TILE):
            for x in range(TILE):
                if (x + y) % 5 == 0:
                    img.putpixel((x, y), shade(base, 1.18))
    return paint


def flat(base, spread=0.05):
    def paint(img, rnd):
        fill_noise(img, rnd, base, spread)
    return paint


def snow(img, rnd):
    base = hexcolour('#eef4fb')
    fill_noise(img, rnd, base, 0.025)
    for _ in range(26):
        img.putpixel((rnd.randrange(TILE), rnd.randrange(TILE)), hexcolour('#ffffff'))
    for _ in range(18):
        img.putpixel((rnd.randrange(TILE), rnd.randrange(TILE)), hexcolour('#d6e2f0'))


def snow_side(img, rnd):
    base = hexcolour('#eef4fb')
    for x in range(TILE):
        depth = 1 + rnd.choice((0, 0, 1, 1, 1, 2))
        for y in range(TILE):
            if y < depth or y < 2:
                img.putpixel((x, y), shade(base, 1 + rnd.uniform(-0.03, 0.03)))


def metal(base):
    def paint(img, rnd):
        fill_noise(img, rnd, base, 0.05)
        for y in range(TILE):
            for x in range(TILE):
                if x % 8 == 0:
                    img.putpixel((x, y), shade(base, 1.25))
    return paint


def glass(img, rnd):
    outer = hexcolour('#f3a640')
    inner = hexcolour('#fff4c8')
    for y in range(TILE):
        for x in range(TILE):
            dx = (x - 15.5) / 16
            dy = (y - 18) / 16
            t = max(0.0, 1 - math.sqrt(dx * dx * 2.2 + dy * dy * 1.4))
            colour = mix(outer, inner, min(1, t * 1.5))
            img.putpixel((x, y), shade(colour, 1 + rnd.uniform(-0.03, 0.03)))


def planks(base, height=4):
    def paint(img, rnd):
        offsets = [rnd.randrange(TILE) for _ in range(TILE // height + 1)]
        tones = [rnd.uniform(0.92, 1.08) for _ in range(TILE // height + 1)]
        for y in range(TILE):
            board = y // height
            for x in range(TILE):
                amount = tones[board] + rnd.uniform(-0.04, 0.04)
                if y % height == height - 1:
                    amount *= 0.62
                elif (x + offsets[board]) % 16 == 0:
                    amount *= 0.7
                elif rnd.random() < 0.12:
                    amount *= 0.9
                img.putpixel((x, y), shade(base, amount))
    return paint


def grain(base):
    def paint(img, rnd):
        lines = [rnd.uniform(0.9, 1.08) for _ in range(TILE)]
        for y in range(TILE):
            for x in range(TILE):
                img.putpixel((x, y), shade(base, lines[x] + rnd.uniform(-0.04, 0.04)))
    return paint


def candy(img, rnd):
    red = hexcolour('#d21f2b')
    white = hexcolour('#f7f3ee')
    for y in range(TILE):
        for x in range(TILE):
            band = (x + y) % 8
            colour = red if band < 3 else white
            if band == 3 or band == 7:
                colour = mix(red, white, 0.5)
            img.putpixel((x, y), shade(colour, 1 + rnd.uniform(-0.03, 0.03)))


def fir(img, rnd):
    dark = hexcolour('#1d4d2b')
    light = hexcolour('#3d7a3f')
    for y in range(TILE):
        for x in range(TILE):
            t = rnd.random()
            colour = mix(dark, light, t * t)
            img.putpixel((x, y), colour)
    for _ in range(9):
        x, y = rnd.randrange(1, TILE - 1), rnd.randrange(1, TILE - 1)
        img.putpixel((x, y), hexcolour('#c4232c'))
        img.putpixel((x + 1, y), hexcolour('#e2484d'))


def burlap(img, rnd):
    base = hexcolour('#a58156')
    for y in range(TILE):
        for x in range(TILE):
            amount = 1 + rnd.uniform(-0.05, 0.05)
            if (x % 2 == 0) != (y % 2 == 0):
                amount *= 0.9
            img.putpixel((x, y), shade(base, amount))


def velvet(img, rnd):
    fill_noise(img, rnd, hexcolour('#7a1420'), 0.05)
    for y in range(3, TILE, 6):
        for x in range(3, TILE, 6):
            img.putpixel((x, y), hexcolour('#c9a227'))


def awning(img, rnd):
    red = hexcolour('#c8202c')
    white = hexcolour('#f4efe6')
    for y in range(TILE):
        for x in range(TILE):
            colour = red if (x // 4) % 2 == 0 else white
            img.putpixel((x, y), shade(colour, 1 + rnd.uniform(-0.03, 0.03)))


def bulbs(img, rnd):
    colours = ['#ff4b4b', '#ffd34d', '#57e36b', '#4fa3ff', '#ff8cf2', '#fff2c2']
    for index, value in enumerate(colours):
        base = hexcolour(value)
        x0 = (index % 3) * 10
        y0 = (index // 3) * 10
        for y in range(8):
            for x in range(8):
                img.putpixel((x0 + x, y0 + y), shade(base, 1 + rnd.uniform(-0.03, 0.03)))


BULB_SLOTS = {'rot': 0, 'gelb': 1, 'gruen': 2, 'blau': 3, 'rosa': 4, 'warm': 5}

FONT = {
    'A': ['010', '101', '111', '101', '101'],
    'C': ['011', '100', '100', '100', '011'],
    'D': ['110', '101', '101', '101', '110'],
    'E': ['111', '100', '110', '100', '111'],
    'G': ['011', '100', '101', '101', '011'],
    'H': ['101', '101', '111', '101', '101'],
    'I': ['111', '010', '010', '010', '111'],
    'K': ['101', '101', '110', '101', '101'],
    'L': ['100', '100', '100', '100', '111'],
    'N': ['1001', '1101', '1011', '1001', '1001'],
    'O': ['010', '101', '101', '101', '010'],
    'P': ['110', '101', '110', '100', '100'],
    'R': ['110', '101', '110', '101', '101'],
    'T': ['111', '010', '010', '010', '010'],
    'U': ['101', '101', '101', '101', '111'],
    'Ü': ['101', '000', '101', '101', '111'],
    'W': ['10001', '10001', '10101', '10101', '01010'],
}


def lettered(base, ink, text, width_px, height_px, size=1, gap=1):
    def paint(img, rnd):
        board = Image.new('RGBA', (TILE, TILE))
        for ox in range(0, img.width, TILE):
            planks(base, 6 * size)(board, rnd)
            img.paste(board, (ox, 0))
        for x in range(width_px):
            img.putpixel((x, 0), shade(base, 0.7))
            img.putpixel((x, height_px - 1), shade(base, 0.7))
        for y in range(height_px):
            img.putpixel((0, y), shade(base, 0.7))
            img.putpixel((width_px - 1, y), shade(base, 0.7))
        total = sum(len(FONT[letter][0]) * size for letter in text) + gap * (len(text) - 1)
        x = (width_px - total) // 2
        y0 = (height_px - 5 * size) // 2
        for letter in text:
            for row, line in enumerate(FONT[letter]):
                for col, bit in enumerate(line):
                    if bit != '1':
                        continue
                    for dy in range(size):
                        for dx in range(size):
                            img.putpixel((x + col * size + dx, y0 + row * size + dy), shade(ink, 1 + rnd.uniform(-0.05, 0.05)))
            x += len(FONT[letter][0]) * size + gap
    return paint


GIFT_COLOURS = {
    'rot': ('#b8232e', 'dots', '#e8c14a'),
    'gruen': ('#1f7a3d', 'stripes', '#d8303a'),
    'blau': ('#1f4f9e', 'stars', '#e9eef5'),
    'gold': ('#d2a02a', 'checks', '#b3202b'),
    'lila': ('#6d2f8f', 'dots', '#f0c94a'),
    'weiss': ('#eae6df', 'stars', '#c3222c'),
}


def build_atlas():
    atlas = Atlas()
    for name, (paper_colour, pattern, ribbon_colour) in GIFT_COLOURS.items():
        atlas.add('papier_' + name, paper(hexcolour(paper_colour), pattern))
        atlas.add('band_' + name, ribbon(hexcolour(ribbon_colour)))
    atlas.add('schnee', snow)
    atlas.add('schnee_rand', snow_side)
    atlas.add('eisen', metal(hexcolour('#25332c')))
    atlas.add('glas', glass)
    atlas.add('messing', metal(hexcolour('#c79a3a')))
    atlas.add('holz', planks(hexcolour('#8a5a34')))
    atlas.add('holz_dunkel', planks(hexcolour('#5a3820')))
    atlas.add('holz_hell', grain(hexcolour('#b98a55')))
    atlas.add('lack_rot', grain(hexcolour('#a5202a')))
    atlas.add('samt', velvet)
    atlas.add('zucker', candy)
    atlas.add('tanne', fir)
    atlas.add('sack', burlap)
    atlas.add('kessel', metal(hexcolour('#3a3a3f')))
    atlas.add('becher', flat(hexcolour('#b3262d')))
    atlas.add('lampen', bulbs)
    atlas.add('markise', awning)
    atlas.add('schild_nordpol', lettered(hexcolour('#9a6a3c'), hexcolour('#2a1508'), 'NORDPOL', 32, 12), density=2)
    atlas.add('schild_wichtel', lettered(hexcolour('#9a6a3c'), hexcolour('#2a1508'), 'WICHTEL', 32, 12), density=2)
    atlas.add('schild_gluehwein', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'GLÜHWEIN', 64, 14, size=2), density=2, span=2)
    atlas.add('glut', flat(hexcolour('#ffb347'), 0.08))
    return atlas


class Box:
    def __init__(self, a, b, mat, faces=None, rotation=None, emit=0, skip=(), uv=None, shade=True):
        self.a = a
        self.b = b
        self.mat = mat
        self.faces = faces or {}
        self.rotation = rotation
        self.emit = emit
        self.skip = set(skip)
        self.uv = uv or {}
        self.shade = shade


def face_size(box, side):
    dx = abs(box.b[0] - box.a[0])
    dy = abs(box.b[1] - box.a[1])
    dz = abs(box.b[2] - box.a[2])
    if side in ('north', 'south'):
        return dx, dy
    if side in ('east', 'west'):
        return dz, dy
    return dx, dz


def model_json(atlas, boxes, shift, seed):
    rnd = random.Random(seed)
    elements = []
    scale = 16 / ATLAS
    for box in boxes:
        a = [box.a[i] - shift[i] for i in range(3)]
        b = [box.b[i] - shift[i] for i in range(3)]
        lo = [round(min(a[i], b[i]), 4) for i in range(3)]
        hi = [round(max(a[i], b[i]), 4) for i in range(3)]
        for value in lo + hi:
            if value < -16 or value > 32:
                raise ValueError(f'out of range {lo} {hi} for {box.mat}')
        faces = {}
        for side in ('north', 'east', 'south', 'west', 'up', 'down'):
            if side in box.skip:
                continue
            tile = atlas.tiles[box.faces.get(side, box.mat)]
            w, h = face_size(box, side)
            pw = min(TILE * tile.span, max(1.0, w * tile.density))
            ph = min(TILE, max(1.0, h * tile.density))
            if side in box.uv:
                ox, oy = box.uv[side]
            elif tile.density > 1 or tile.name.startswith('schild') or tile.name == 'lampen':
                ox, oy = 0, 0
            else:
                ox = rnd.uniform(0, TILE * tile.span - pw)
                oy = rnd.uniform(0, TILE - ph)
            u0 = (tile.column * TILE + ox) * scale
            v0 = (tile.row * TILE + oy) * scale
            faces[side] = {
                'uv': [round(u0, 4), round(v0, 4), round(u0 + pw * scale, 4), round(v0 + ph * scale, 4)],
                'texture': '#0',
            }
        element = {'from': lo, 'to': hi, 'faces': faces}
        if box.rotation:
            origin, angles = box.rotation
            element['rotation'] = {
                'origin': [round(origin[i] - shift[i], 4) for i in range(3)],
                'x': angles[0], 'y': angles[1], 'z': angles[2],
            }
        if box.emit:
            element['light_emission'] = box.emit
        if not box.shade:
            element['shade'] = False
        elements.append(element)
    return {
        'textures': {'0': TEXTURE, 'particle': TEXTURE},
        'elements': elements,
    }


def gift(cx, cz, w, h, d, y0, paper_name, band_name, turn=0.0, bow=True, band=None):
    band = band or max(1.5, round(w / 6 * 2) / 2)
    x0, x1 = cx - w / 2, cx + w / 2
    z0, z1 = cz - d / 2, cz + d / 2
    y1 = y0 + h
    e = 0.12
    rot = ((cx, y0 + h / 2, cz), (0, turn, 0)) if turn else None
    boxes = [
        Box((x0, y0, z0), (x1, y1, z1), paper_name, rotation=rot),
        Box((x0 - e, y0 + e, cz - band / 2), (x1 + e, y1 + e, cz + band / 2), band_name, rotation=rot, skip=('down',)),
        Box((cx - band / 2, y0 + e, z0 - e), (cx + band / 2, y1 + e * 2, z1 + e), band_name, rotation=rot, skip=('down',)),
    ]
    if bow:
        knot = band
        loop_len = w * 0.32
        loop_h = max(1.5, w * 0.18)
        top = y1 + e * 2
        centre = (cx, top, cz)
        for sign in (-1, 1):
            tilt = -35 * sign
            boxes.append(Box(
                (cx - band * 0.4, top, cz + (0 if sign > 0 else -loop_len)),
                (cx + band * 0.4, top + loop_h, cz + (loop_len if sign > 0 else 0)),
                band_name, rotation=(centre, (tilt, turn + 45, 0)),
            ))
            boxes.append(Box(
                (cx - band * 0.4, top, cz + (0 if sign > 0 else -loop_len)),
                (cx + band * 0.4, top + loop_h, cz + (loop_len if sign > 0 else 0)),
                band_name, rotation=(centre, (tilt, turn - 45, 0)),
            ))
        boxes.append(Box((cx - knot / 2, top, cz - knot / 2), (cx + knot / 2, top + knot * 0.8, cz + knot / 2), band_name, rotation=((cx, top, cz), (0, turn, 0)) if turn else None))
    return boxes


GIFT_SIZES = {
    'klein': dict(w=10, h=8, centre=(0, 0), footprint=[], height=0),
    'mittel': dict(w=14, h=13, centre=(0, 0), footprint=[(0, 0, 0)], height=1),
    'gross': dict(w=30, h=27, centre=(8, 8), footprint=[(x, y, z) for x in (0, 1) for z in (0, 1) for y in (0, 1)], height=2),
}


def gift_model(size, colour):
    spec = GIFT_SIZES[size]
    cx, cz = spec['centre']
    return gift(cx, cz, spec['w'], spec['h'], spec['w'], 0, 'papier_' + colour, 'band_' + colour)


def stack_model():
    boxes = []
    boxes += gift(6, 6, 20, 16, 20, 0, 'papier_rot', 'band_rot', bow=False)
    boxes += gift(6, 6, 14, 11, 14, 16.25, 'papier_gruen', 'band_gruen', turn=22.5, bow=False)
    boxes += gift(6, 6, 9, 8, 9, 27.5, 'papier_gold', 'band_gold', turn=-15)
    boxes += gift(19.5, 19.5, 8, 8, 8, 0, 'papier_blau', 'band_blau', turn=12)
    boxes += gift(19.5, -2.5, 7, 6, 7, 0, 'papier_lila', 'band_lila', turn=-30)
    boxes += gift(-3, 19, 7, 7, 7, 0, 'papier_weiss', 'band_weiss', turn=20)
    return boxes


def ring(centre, radius, thickness, depth, segments, mat, plane='xy'):
    boxes = []
    cx, cy, cz = centre
    length = 2 * radius * math.tan(math.pi / segments) + thickness * 0.6
    for k in range(segments):
        angle = 360 / segments * k
        a = (cx - length / 2, cy + radius - thickness / 2, cz - depth / 2)
        b = (cx + length / 2, cy + radius + thickness / 2, cz + depth / 2)
        boxes.append(Box(a, b, mat, rotation=(centre, (0, 0, angle))))
    return boxes


def lantern_mast():
    m = 'eisen'
    boxes = [
        Box((-5, 0, -5), (5, 2, 5), m),
        Box((-4, 2, -4), (4, 4, 4), m),
        Box((-3, 4, -3), (3, 7, 3), m),
        Box((-2, 7, -2), (2, 9, 2), m),
        Box((-1.5, 9, -1.5), (1.5, 48, 1.5), m),
        Box((-2.5, 20, -2.5), (2.5, 21.5, 2.5), m),
        Box((-2.5, 46, -2.5), (2.5, 48, 2.5), m),
        Box((-6.5, 42.5, -0.75), (6.5, 44, 0.75), m),
        Box((-5.2, 1.9, -5.2), (5.2, 2.4, 5.2), 'schnee', faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}, skip=('down',)),
        Box((-6.6, 44, -0.85), (6.6, 44.5, 0.85), 'schnee', skip=('down',)),
    ]
    boxes += ring((0, 31, -2.4), 4.2, 2.2, 1.6, 10, 'tanne')
    boxes += [
        Box((-1.2, 34.2, -3.6), (1.2, 36, -2.6), 'band_rot'),
        Box((-3.4, 34.4, -3.4), (-1.2, 36.4, -2.8), 'band_rot', rotation=((-1.2, 35.2, -3.1), (0, 0, -20))),
        Box((1.2, 34.4, -3.4), (3.4, 36.4, -2.8), 'band_rot', rotation=((1.2, 35.2, -3.1), (0, 0, 20))),
        Box((-1.6, 30.5, -3.3), (-0.4, 34.2, -2.8), 'band_rot', rotation=((-1, 34.2, -3), (0, 0, -12))),
        Box((0.4, 30.5, -3.3), (1.6, 34.2, -2.8), 'band_rot', rotation=((1, 34.2, -3), (0, 0, 12))),
    ]
    return boxes


def lantern_head():
    m = 'eisen'
    boxes = [
        Box((-4, 48, -4), (4, 49.5, 4), m),
        Box((-3, 49.5, -3), (3, 57, 3), 'glas', emit=15, shade=False),
        Box((-2, 47, -2), (2, 48, 2), m),
    ]
    for sx in (-1, 1):
        for sz in (-1, 1):
            boxes.append(Box((sx * 3.6 - 0.5, 49.5, sz * 3.6 - 0.5), (sx * 3.6 + 0.5, 57, sz * 3.6 + 0.5), m))
    boxes += [
        Box((-3.6, 49.5, -3.1), (3.6, 50.2, -3), m),
        Box((-4.6, 57, -4.6), (4.6, 58.2, 4.6), m),
        Box((-3.6, 58.2, -3.6), (3.6, 59.6, 3.6), m),
        Box((-2.2, 59.6, -2.2), (2.2, 61, 2.2), m),
        Box((-0.8, 61, -0.8), (0.8, 63, 0.8), 'messing'),
        Box((-4.7, 58.2, -4.7), (4.7, 58.7, 4.7), 'schnee', faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}, skip=('down',)),
        Box((-3.7, 59.6, -3.7), (3.7, 60.1, 3.7), 'schnee', faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}, skip=('down',)),
    ]
    return boxes


def arc(centre, radius, start, end, steps, thickness, depth, mat, length_scale=1.15):
    boxes = []
    cx, cy, cz = centre
    step = (end - start) / steps
    seg = abs(math.radians(step)) * radius * length_scale + thickness * 0.4
    for k in range(steps + 1):
        theta = start + step * k
        px = cx + radius * math.cos(math.radians(theta))
        py = cy + radius * math.sin(math.radians(theta))
        a = (px - thickness / 2, py - seg / 2, cz - depth / 2)
        b = (px + thickness / 2, py + seg / 2, cz + depth / 2)
        boxes.append(Box(a, b, mat, rotation=((px, py, cz), (0, 0, theta - 180))))
    return boxes


def candy_cane():
    boxes = [
        Box((-1.5, 0, -1.5), (1.5, 36, 1.5), 'zucker'),
        Box((-4.5, 0, -4.5), (4.5, 1.2, 4.5), 'schnee', faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}),
        Box((-3, 1.2, -3), (3, 2.4, 3), 'schnee', faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}),
    ]
    boxes += arc((5, 36, 0), 5, 180, 0, 8, 3, 3, 'zucker')
    boxes += [Box((8.5, 28, -1.5), (11.5, 36, 1.5), 'zucker')]
    boxes += [
        Box((-1.8, 24, -1.8), (1.8, 25.5, 1.8), 'band_gruen'),
        Box((-1.2, 23.6, -3.2), (1.2, 25.8, -1.8), 'band_gruen'),
        Box((-4.4, 23.8, -2.6), (-1.2, 26, -2), 'band_gruen', rotation=((-1.2, 24.9, -2.3), (0, 0, -18))),
        Box((1.2, 23.8, -2.6), (4.4, 26, -2), 'band_gruen', rotation=((1.2, 24.9, -2.3), (0, 0, 18))),
    ]
    return boxes


def sleigh():
    b = []
    for z in (-6, 6):
        b.append(Box((-7, 0, z - 1), (20, 1.5, z + 1), 'messing'))
        b += arc((20, 4.75, z), 4, -90, 90, 7, 1.5, 2, 'messing')
        b.append(Box((14, 7.6, z - 1), (20, 9.1, z + 1), 'messing'))
        b += arc((-7, 2.5, z), 1.8, 270, 140, 3, 1.5, 2, 'messing')
        for x in (-3, 5, 13):
            b.append(Box((x - 0.75, 1.5, z - 0.75), (x + 0.75, 5, z + 0.75), 'messing'))
    b += [
        Box((-8, 5, -7.5), (16, 6.5, 7.5), 'lack_rot'),
        Box((-8, 6.5, -7.5), (16, 12, -6.5), 'lack_rot'),
        Box((-8, 6.5, 6.5), (16, 12, 7.5), 'lack_rot'),
        Box((-9.5, 5, -7.5), (-8, 18, 7.5), 'lack_rot'),
        Box((15, 6.5, -7.5), (16.5, 11, 7.5), 'lack_rot'),
        Box((15, 10, -7), (16.5, 15, 7), 'lack_rot', rotation=((15.75, 10, 0), (0, 0, -25))),
        Box((-8, 12, -7.8), (16, 12.8, -6.2), 'messing'),
        Box((-8, 12, 6.2), (16, 12.8, 7.8), 'messing'),
        Box((-9.8, 17.5, -7.8), (-7.7, 18.5, 7.8), 'messing'),
        Box((-8, 6.5, -6.5), (0, 9.5, 6.5), 'samt'),
        Box((-8, 9.5, -6.5), (-6.5, 16.5, 6.5), 'samt'),
        Box((-9.8, 18.5, -7.8), (-7.7, 19, 7.8), 'schnee', skip=('down',)),
        Box((-8, 12.8, -7.9), (16, 13.2, -6.1), 'schnee', skip=('down',)),
        Box((4, 6.5, -5), (12, 15, 2), 'sack'),
        Box((5, 15, -4), (11, 17, 1), 'sack', rotation=((8, 15, -1.5), (0, 20, 0))),
        Box((7, 16.5, -2.5), (9, 18, -0.5), 'band_gold'),
    ]
    b += gift(9, 4.5, 6, 6, 5, 6.5, 'papier_blau', 'band_blau', turn=15)
    b += gift(2.5, 3.5, 5, 5, 5, 9.5, 'papier_gruen', 'band_gruen', turn=-10, bow=False)
    b += gift(13, -3.5, 4.5, 4, 4.5, 6.5, 'papier_rot', 'band_rot', turn=30)
    return b


def market_stall():
    b = []
    b += [
        Box((-23, 0, -7), (23, 15, -5), 'holz_dunkel'),
        Box((-24, 15, -9.5), (24, 17, -3), 'holz_hell'),
        Box((-24, 0, -8), (-21, 15, -4), 'holz'),
        Box((21, 0, -8), (24, 15, -4), 'holz'),
        Box((-24, 0, 21), (24, 40, 23), 'holz'),
        Box((-24, 17, -8), (-21.5, 39, -5.5), 'holz'),
        Box((21.5, 17, -8), (24, 39, -5.5), 'holz'),
        Box((-24, 0, 18), (-21.5, 40, 21), 'holz'),
        Box((21.5, 0, 18), (24, 40, 21), 'holz'),
        Box((-21, 26, 18.5), (21, 27, 21), 'holz_hell'),
        Box((-21, 12, 15), (21, 13, 21), 'holz_hell'),
    ]
    roof = ((0, 38.5, -12), (-12.9, 0, 0))
    b += [
        Box((-24, 38.5, -12), (24, 40, 24), 'lack_rot', rotation=roof, faces={'down': 'markise', 'north': 'markise'}),
        Box((-24, 40, -12), (24, 40.8, 24), 'schnee', rotation=roof, faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}, skip=('down',)),
        Box((-24, 34.5, -9.5), (24, 37.5, -7.5), 'tanne'),
        Box((-16, 39, -11.5), (16, 46, -10.5), 'holz_dunkel', faces={'north': 'schild_gluehwein'}, uv={'north': (0, 0)}),
        Box((-16.3, 46, -11.7), (16.3, 46.6, -10.3), 'schnee', skip=('down',)),
    ]
    colours = ['rot', 'gelb', 'gruen', 'blau', 'rosa', 'warm']
    for k, x in enumerate(range(-22, 23, 4)):
        slot = BULB_SLOTS[colours[k % len(colours)]]
        u = (slot % 3) * 10 + 2
        v = (slot // 3) * 10 + 2
        b.append(Box((x - 0.6, 32.8, -9), (x + 0.6, 34.5, -7.8), 'lampen', emit=15, shade=False,
                     uv={s: (u, v) for s in ('north', 'south', 'east', 'west', 'up', 'down')}))
    for x in (-18, -6, 6, 18):
        b += [
            Box((x - 1.2, 34, -10), (x + 1.2, 36.4, -9.4), 'band_rot'),
            Box((x - 3.2, 34.4, -9.9), (x - 1.2, 36.2, -9.5), 'band_rot', rotation=((x - 1.2, 35.3, -9.7), (0, 0, -20))),
            Box((x + 1.2, 34.4, -9.9), (x + 3.2, 36.2, -9.5), 'band_rot', rotation=((x + 1.2, 35.3, -9.7), (0, 0, 20))),
        ]
    b += [
        Box((7, 17, -8), (15, 23, 0), 'kessel'),
        Box((6.5, 23, -8.5), (15.5, 24, 0.5), 'kessel'),
        Box((10, 24, -5), (12, 25.5, -3), 'messing'),
        Box((6, 17, -8.5), (16, 17.6, 0.5), 'glut', emit=12),
    ]
    for k, x in enumerate((-18, -14.5, -11, -4)):
        b.append(Box((x, 17, -8), (x + 2.4, 20, -5.6), 'becher'))
        b.append(Box((x + 2.4, 17.6, -7.3), (x + 3.2, 19.4, -6.3), 'becher'))
    return b


def signpost():
    b = [
        Box((-1.5, 0, -1.5), (1.5, 40, 1.5), 'holz_dunkel'),
        Box((-2, 40, -2), (2, 41.5, 2), 'holz_dunkel'),
        Box((-2.1, 41.5, -2.1), (2.1, 42.2, 2.1), 'schnee', faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}, skip=('down',)),
        Box((-3.5, 0, -3.5), (3.5, 1.2, 3.5), 'schnee', faces={'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}),
    ]

    def board(y, direction, text):
        length = 16
        if direction > 0:
            x0, x1 = 1.5, 1.5 + length
        else:
            x0, x1 = -1.5 - length, -1.5
        tile = 'schild_' + text
        plain = 'holz'
        north_uv = (0, 0)
        boxes = [
            Box((x0, y, -0.75), (x1, y + 6, 0.75), plain, faces={'north': tile, 'south': tile}, uv={'north': north_uv, 'south': north_uv}),
        ]
        tip_x = x1 if direction > 0 else x0
        size = 6 / math.sqrt(2)
        boxes.append(Box((tip_x - size / 2, y + 3 - size / 2, -0.7), (tip_x + size / 2, y + 3 + size / 2, 0.7), plain, rotation=((tip_x, y + 3, 0), (0, 0, 45))))
        boxes.append(Box((x0, y + 6, -0.85), (x1, y + 6.5, 0.85), 'schnee', skip=('down',)))
        return boxes

    b += board(31, 1, 'nordpol')
    b += board(22, -1, 'wichtel')
    return b


class Part:
    def __init__(self, item, boxes, offset):
        self.item = item
        self.boxes = boxes
        ox, oy, oz = offset
        if oy is None:
            low = min(min(b.a[1], b.b[1]) for b in boxes)
            high = max(max(b.a[1], b.b[1]) for b in boxes)
            oy = next(dy / 2 for dy in range(0, 12) if low - (dy * 8 - 8) >= -16 and high - (dy * 8 - 8) <= 32)
        self.offset = (ox, oy, oz)


class Prop:
    def __init__(self, art, parts, footprint, light=None, title=''):
        self.art = art
        self.parts = parts
        self.footprint = footprint
        self.light = light
        self.title = title


def column(height):
    return [(0, y, 0) for y in range(height)]


def props():
    result = []
    for size, spec in GIFT_SIZES.items():
        ox = spec['centre'][0] / 16
        oz = spec['centre'][1] / 16
        result.append(Prop('geschenk_' + size, [Part('geschenk_' + size + '_rot', gift_model(size, 'rot'), (ox, None, oz))], spec['footprint'], title='Geschenk'))
    result.append(Prop('geschenkstapel', [Part('geschenkstapel', stack_model(), (0.5, None, 0.5))], [(x, y, z) for x in (0, 1) for z in (0, 1) for y in (0, 1)], title='Geschenkstapel'))
    result.append(Prop('laterne', [
        Part('laterne_mast', lantern_mast(), (0, None, 0)),
        Part('laterne_kopf', lantern_head(), (0, None, 0)),
    ], column(3), light=(0, 3, 0), title='Straßenlaterne'))
    result.append(Prop('zuckerstange', [Part('zuckerstange', candy_cane(), (0, None, 0))], column(3), title='Zuckerstange'))
    result.append(Prop('schlitten', [Part('schlitten', sleigh(), (0.5, None, 0))], [(0, 0, 0), (1, 0, 0)], title='Schlitten'))
    result.append(Prop('marktstand', [Part('marktstand', market_stall(), (0, None, 0.5))], [(x, 0, 0) for x in (-1, 0, 1)], title='Glühweinstand'))
    result.append(Prop('wegweiser', [Part('wegweiser', signpost(), (0, None, 0))], column(3), title='Wegweiser'))
    return result


def local(offset):
    x, y, z = offset
    return f'^{fmt(-x)} ^{fmt(y)} ^{fmt(-z)}'


def fmt(value):
    if value == 0:
        return ''
    text = f'{value:.4f}'.rstrip('0').rstrip('.')
    return '0' if text in ('-0', '') else text


def part_shift(part):
    ox, oy, oz = part.offset
    return (ox * 16 - 8, oy * 16 - 8, oz * 16 - 8)


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


def write_json(path, data):
    write(path, json.dumps(data, indent='\t') + '\n')


def item_definition(name):
    return {'model': {'type': 'minecraft:model', 'model': f'hideandseek:item/props/deko/{name}'}}


def summon(prop, part):
    model = f'{prop.art}_$(farbe)' if prop.art.startswith('geschenk_') else part.item
    text = (
        '{Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],'
        f'item:{{id:"minecraft:stick",count:1,components:{{"minecraft:item_model":"hideandseek:props/deko/{model}"}}}}'
    )
    return f'$summon minecraft:item_display {local(part.offset)} {text}}}'


def functions(prop):
    frei = []
    setzen = []
    raeumen = []
    for block in prop.footprint:
        pos = local(block)
        frei.append(f'execute unless block {pos} #sniffa:deko_frei run return 0')
        setzen.append(f'execute if block {pos} #minecraft:air run setblock {pos} minecraft:barrier strict')
        raeumen.append(f'execute if block {pos} minecraft:barrier run setblock {pos} minecraft:air strict')
    if prop.light:
        pos = local(prop.light)
        frei.append(f'execute unless block {pos} #sniffa:deko_frei run return 0')
        setzen.append(f'execute if block {pos} #minecraft:air run setblock {pos} minecraft:light[level=15] strict')
        raeumen.append(f'execute if block {pos} minecraft:light run setblock {pos} minecraft:air strict')
    frei.append('return 1')
    for part in prop.parts:
        setzen.insert(0, summon(prop, part))
    base = FUNCTIONS / 'intern/deko' / prop.art
    write(base / 'frei.mcfunction', '\n'.join(frei) + '\n')
    write(base / 'setzen.mcfunction', '\n'.join(setzen) + '\n')
    write(base / 'raeumen.mcfunction', '\n'.join(raeumen) + '\n' if raeumen else 'return 1\n')
    write(FUNCTIONS / f'{prop.art}.mcfunction', f'function sniffa:intern/deko/platzieren {{art:"{prop.art}",name:"{prop.title}"}}\n')


def main():
    atlas = build_atlas()
    texture = ASSETS / 'textures/item/props/deko.png'
    texture.parent.mkdir(parents=True, exist_ok=True)
    atlas.image.save(texture)
    for prop in props():
        for part in prop.parts:
            if part.boxes is None:
                continue
            write_json(ASSETS / f'models/item/props/deko/{part.item}.json', model_json(atlas, part.boxes, part_shift(part), part.item))
            write_json(ASSETS / f'items/props/deko/{part.item}.json', item_definition(part.item))
        functions(prop)
    for size in GIFT_SIZES:
        prop = next(p for p in props() if p.art == 'geschenk_' + size)
        shift = part_shift(prop.parts[0])
        for colour in GIFT_COLOURS:
            name = f'geschenk_{size}_{colour}'
            write_json(ASSETS / f'models/item/props/deko/{name}.json', model_json(atlas, gift_model(size, colour), shift, name))
            write_json(ASSETS / f'items/props/deko/{name}.json', item_definition(name))


if __name__ == '__main__':
    main()
