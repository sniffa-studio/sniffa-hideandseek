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


def ice(img, rnd):
    top = hexcolour('#e9f6ff')
    low = hexcolour('#9fd2f2')
    for y in range(TILE):
        for x in range(TILE):
            img.putpixel((x, y), shade(mix(top, low, y / TILE), 1 + rnd.uniform(-0.04, 0.04)))


def gingerbread(img, rnd):
    fill_noise(img, rnd, hexcolour('#8b4f22'), 0.06)
    for _ in range(14):
        img.putpixel((rnd.randrange(TILE), rnd.randrange(TILE)), hexcolour('#a8662f'))


def blanket(img, rnd):
    red = hexcolour('#2f5d3a')
    line = hexcolour('#e7d9b8')
    for y in range(TILE):
        for x in range(TILE):
            colour = line if x % 6 == 0 or y % 6 == 0 else red
            img.putpixel((x, y), shade(colour, 1 + rnd.uniform(-0.04, 0.04)))


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
    atlas.add('band_tanne', ribbon(hexcolour('#2c8a45')))
    atlas.add('tanne_blatt', flat(hexcolour('#23633a'), 0.08))
    atlas.add('beere', flat(hexcolour('#d3202c'), 0.06))
    atlas.add('eis', ice)
    atlas.add('lebkuchen', gingerbread)
    atlas.add('zuckerguss', flat(hexcolour('#f7f2ea'), 0.03))
    atlas.add('flasche', flat(hexcolour('#1f5a36'), 0.1))
    atlas.add('stern', flat(hexcolour('#ffd75a'), 0.05))
    atlas.add('gluehwein', flat(hexcolour('#5b0f1c'), 0.05))
    atlas.add('decke', blanket)
    atlas.add('schild_kakao', lettered(hexcolour('#9a6a3c'), hexcolour('#2a1508'), 'KAKAO', 32, 12), density=2)
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


SNOW_SIDES = {'north': 'schnee_rand', 'south': 'schnee_rand', 'east': 'schnee_rand', 'west': 'schnee_rand'}


def snow_cap(x0, z0, x1, z1, y, thick=0.6, rotation=None):
    return Box((x0, y, z0), (x1, y + thick, z1), 'schnee', faces=SNOW_SIDES, skip=('down',), rotation=rotation)


def ribbon_loop(cx, cz, top, length, height, width, band_name, tilt, turn, forward):
    t = 0.7
    z0, z1 = (cz, cz + length) if forward else (cz - length, cz)
    end = (z1 - t, z1) if forward else (z0, z0 + t)
    rot = ((cx, top, cz), (tilt, turn, 0))
    x0, x1 = cx - width / 2, cx + width / 2
    return [
        Box((x0, top, z0), (x1, top + t, z1), band_name, rotation=rot),
        Box((x0, top + height - t, z0), (x1, top + height, z1), band_name, rotation=rot),
        Box((x0, top, end[0]), (x1, top + height, end[1]), band_name, rotation=rot),
    ]


def gift(cx, cz, w, h, d, y0, paper_name, band_name, turn=0.0, bow=True, band=None):
    band = band or max(1.5, round(w / 6 * 2) / 2)
    lid = max(1.5, round(h * 0.24 * 2) / 2)
    o = 0.5 if w < 16 else 1.0
    x0, x1 = cx - w / 2, cx + w / 2
    z0, z1 = cz - d / 2, cz + d / 2
    y1 = y0 + h
    body_top = y1 - lid
    e = 0.12
    rot = ((cx, y0 + h / 2, cz), (0, turn, 0)) if turn else None
    boxes = [
        Box((x0, y0, z0), (x1, body_top, z1), paper_name, rotation=rot),
        Box((x0 - o, body_top, z0 - o), (x1 + o, y1, z1 + o), paper_name, rotation=rot),
        Box((x0 - e, y0, cz - band / 2), (x1 + e, body_top, cz + band / 2), band_name, rotation=rot, skip=('down', 'up')),
        Box((cx - band / 2, y0, z0 - e), (cx + band / 2, body_top, z1 + e), band_name, rotation=rot, skip=('down', 'up')),
        Box((x0 - o - e, body_top - e, cz - band / 2), (x1 + o + e, y1 + e, cz + band / 2), band_name, rotation=rot),
        Box((cx - band / 2, body_top - e, z0 - o - e), (cx + band / 2, y1 + e * 2, z1 + o + e), band_name, rotation=rot),
    ]
    if bow:
        top = y1 + e * 2
        length = max(2.5, w * 0.3)
        height = max(1.8, w * 0.2)
        width = band * 0.9
        boxes += ribbon_loop(cx, cz, top, length, height, width, band_name, -32, turn, True)
        boxes += ribbon_loop(cx, cz, top, length, height, width, band_name, 32, turn, False)
        tail = max(2.5, w * 0.34)
        for sign in (-1, 1):
            boxes.append(Box((cx - width / 2, top, cz), (cx + width / 2, top + 0.4, cz + tail), band_name,
                             rotation=((cx, top, cz), (14, turn + 90 * sign + 20, 0))))
        knot = band * 1.1
        boxes.append(Box((cx - knot / 2, top - 0.2, cz - knot / 2), (cx + knot / 2, top + knot * 0.8, cz + knot / 2), band_name,
                         rotation=((cx, top, cz), (0, turn, 0)) if turn else None))
    return boxes


GIFT_SIZES = {
    'klein': dict(w=10, h=9, centre=(0, 0), footprint=[(0, 0, 0)]),
    'mittel': dict(w=14, h=13, centre=(0, 0), footprint=[(0, 0, 0)]),
    'gross': dict(w=28, h=26, centre=(8, 8), footprint=[(x, y, z) for x in (0, 1) for z in (0, 1) for y in (0, 1)]),
}


def gift_model(size, colour):
    spec = GIFT_SIZES[size]
    cx, cz = spec['centre']
    return gift(cx, cz, spec['w'], spec['h'], spec['w'], 0, 'papier_' + colour, 'band_' + colour)


def stack_model():
    boxes = []
    boxes += gift(6, 6, 20, 15, 20, 0, 'papier_rot', 'band_rot', bow=False)
    boxes += gift(6, 6, 14, 11, 14, 16, 'papier_gruen', 'band_gruen', turn=22.5, bow=False)
    boxes += gift(6, 6, 9, 8, 9, 28, 'papier_gold', 'band_gold', turn=-15)
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


def flat_bow(cx, cy, z, mat, size=1.0):
    s = size
    return [
        Box((cx - 1.1 * s, cy - 0.9 * s, z - 0.6), (cx + 1.1 * s, cy + 0.9 * s, z), mat),
        Box((cx - 3.4 * s, cy - 0.8 * s, z - 0.4), (cx - 1.1 * s, cy + 1.4 * s, z), mat, rotation=((cx - 1.1 * s, cy, z), (0, 0, -18))),
        Box((cx + 1.1 * s, cy - 0.8 * s, z - 0.4), (cx + 3.4 * s, cy + 1.4 * s, z), mat, rotation=((cx + 1.1 * s, cy, z), (0, 0, 18))),
        Box((cx - 1.5 * s, cy - 4 * s, z - 0.3), (cx - 0.4 * s, cy - 0.9 * s, z), mat, rotation=((cx - 1, cy - 0.9 * s, z), (0, 0, -12))),
        Box((cx + 0.4 * s, cy - 4 * s, z - 0.3), (cx + 1.5 * s, cy - 0.9 * s, z), mat, rotation=((cx + 1, cy - 0.9 * s, z), (0, 0, 12))),
    ]


def ribs(x, z, y0, y1, half, depth, mat):
    return [
        Box((x - 0.4, y0, z - half - depth), (x + 0.4, y1, z - half), mat),
        Box((x - 0.4, y0, z + half), (x + 0.4, y1, z + half + depth), mat),
        Box((x - half - depth, y0, z - 0.4), (x - half, y1, z + 0.4), mat),
        Box((x + half, y0, z - 0.4), (x + half + depth, y1, z + 0.4), mat),
    ]


def lantern_mast():
    m = 'eisen'
    boxes = [
        Box((-5, 0, -5), (5, 1.5, 5), m),
        Box((-4.5, 1.5, -4.5), (4.5, 3, 4.5), m),
        Box((-3.5, 3, -3.5), (3.5, 9, 3.5), m),
        Box((-4, 9, -4), (4, 10, 4), m),
        Box((-2.5, 10, -2.5), (2.5, 12, 2.5), m),
        Box((-1.5, 12, -1.5), (1.5, 44, 1.5), m),
        Box((-2.2, 22, -2.2), (2.2, 23.2, 2.2), 'messing'),
        Box((-2.5, 43, -2.5), (2.5, 44.5, 2.5), m),
        Box((-3, 44.5, -3), (3, 46, 3), m),
        Box((-3.6, 46, -3.6), (3.6, 47.2, 3.6), m),
        Box((-6.5, 39.5, -0.7), (6.5, 40.9, 0.7), m),
        Box((-7.3, 39.1, -1), (-6.3, 41.3, 1), 'messing'),
        Box((6.3, 39.1, -1), (7.3, 41.3, 1), 'messing'),
        snow_cap(-5.1, -5.1, 5.1, 5.1, 1.5, 0.5),
        snow_cap(-4.1, -4.1, 4.1, 4.1, 10, 0.5),
        snow_cap(-6.6, -0.8, 6.6, 0.8, 40.9, 0.5),
    ]
    boxes += ribs(0, 0, 3, 9, 3.5, 0.5, m)
    boxes += ribs(0, 0, 12, 43, 1.5, 0.4, m)
    boxes += ring((0, 30, -2.6), 4.4, 2.4, 1.8, 12, 'tanne')
    boxes += flat_bow(0, 34.4, -3.5, 'band_gruen')
    for angle, ry in ((40, 0.6), (140, 0.4), (250, -0.2), (320, 0.3)):
        x = 4.4 * math.cos(math.radians(angle))
        y = 30 + 4.4 * math.sin(math.radians(angle))
        boxes.append(Box((x - 0.6, y - 0.6, -3.9), (x + 0.6, y + 0.6, -3.2), 'beere'))
    return boxes


def lantern_head():
    m = 'eisen'
    boxes = [
        Box((-4.2, 47.2, -4.2), (4.2, 48.6, 4.2), m),
        Box((-3.4, 48.6, -3.4), (3.4, 57.4, 3.4), 'glas', emit=15, shade=False),
        Box((-4.4, 57.4, -4.4), (4.4, 58.6, 4.4), m),
        Box((-3.8, 58.6, -3.8), (3.8, 59.6, 3.8), m),
        Box((-2.8, 59.6, -2.8), (2.8, 60.6, 2.8), m),
        Box((-1.8, 60.6, -1.8), (1.8, 61.6, 1.8), m),
        Box((-0.6, 61.6, -0.6), (0.6, 62.4, 0.6), m),
        Box((-1, 62.4, -1), (1, 64.2, 1), 'messing'),
        snow_cap(-4.5, -4.5, 4.5, 4.5, 58.6, 0.4),
        snow_cap(-3.9, -3.9, 3.9, 3.9, 59.6, 0.4),
        snow_cap(-2.9, -2.9, 2.9, 2.9, 60.6, 0.4),
    ]
    for sx in (-1, 1):
        for sz in (-1, 1):
            boxes.append(Box((sx * 3.7 - 0.55, 48.6, sz * 3.7 - 0.55), (sx * 3.7 + 0.55, 57.4, sz * 3.7 + 0.55), m))
    for side in (-1, 1):
        boxes.append(Box((-0.3, 48.6, side * 3.4 - 0.25), (0.3, 57.4, side * 3.4 + 0.25), m))
        boxes.append(Box((side * 3.4 - 0.25, 48.6, -0.3), (side * 3.4 + 0.25, 57.4, 0.3), m))
        boxes.append(Box((-3.4, 52.7, side * 3.4 - 0.25), (3.4, 53.3, side * 3.4 + 0.25), m))
        boxes.append(Box((side * 3.4 - 0.25, 52.7, -3.4), (side * 3.4 + 0.25, 53.3, 3.4), m))
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


def rounded_rod(x0, y0, x1, y1, r, mat, z=0.0):
    return [
        Box((x0 - r, y0, z - r * 0.65), (x1 + r, y1, z + r * 0.65), mat),
        Box((x0 - r * 0.65, y0, z - r), (x1 + r * 0.65, y1, z + r), mat),
    ]


def candy_cane():
    boxes = [
        Box((-4.5, 0, -4.5), (4.5, 1.2, 4.5), 'schnee', faces=SNOW_SIDES),
        Box((-3.2, 1.2, -3.2), (3.2, 2.4, 3.2), 'schnee', faces=SNOW_SIDES),
        Box((-1.8, 2.4, -1.8), (1.8, 3.2, 1.8), 'schnee', faces=SNOW_SIDES),
    ]
    boxes += rounded_rod(0, 0, 0, 36, 1.6, 'zucker')
    boxes += arc((5.2, 36, 0), 5.2, 180, 0, 12, 3.2, 2.1, 'zucker')
    boxes += arc((5.2, 36, 0), 5.2, 180, 0, 12, 2.1, 3.2, 'zucker')
    boxes += rounded_rod(10.4, 29, 10.4, 36, 1.6, 'zucker')
    boxes += [Box((9.3, 28.2, -1.1), (11.5, 29, 1.1), 'zucker')]
    boxes += [Box((-1.9, 23.5, -1.9), (1.9, 25.3, 1.9), 'band_tanne')]
    boxes += flat_bow(0, 24.4, -1.9, 'band_tanne', 0.9)
    for dx, angle in ((-2.2, 35), (2.2, -35)):
        boxes.append(Box((dx - 1.6, 26.2, -2.5), (dx + 1.6, 27.2, -2), 'tanne_blatt', rotation=((dx, 26.7, -2.2), (0, 0, angle))))
    boxes += [
        Box((-0.9, 26.4, -2.9), (0, 27.3, -2.2), 'beere'),
        Box((0, 26.9, -2.9), (0.9, 27.8, -2.2), 'beere'),
    ]
    return boxes


def sleigh():
    b = []
    for z in (-6, 6):
        b.append(Box((-7, 0, z - 1), (20, 1.5, z + 1), 'messing'))
        b += arc((20, 4.75, z), 4, -90, 90, 10, 1.5, 2, 'messing')
        b.append(Box((14, 7.6, z - 1), (20, 9.1, z + 1), 'messing'))
        b += arc((-7, 2.5, z), 1.8, 270, 130, 5, 1.5, 2, 'messing')
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
        Box((0, 8, -7.9), (8, 10.5, -7.5), 'messing'),
        Box((3, 7, -7.95), (5, 11.5, -7.5), 'messing'),
        Box((0, 8, 7.5), (8, 10.5, 7.9), 'messing'),
        Box((3, 7, 7.5), (5, 11.5, 7.95), 'messing'),
        Box((-8, 6.5, -6.5), (0, 9.5, 6.5), 'samt'),
        Box((-8, 9.5, -6.5), (-6.5, 16.5, 6.5), 'samt'),
        Box((-6.5, 9.4, -6.6), (-0.5, 10.2, 6.6), 'decke'),
        Box((-1.3, 4.5, -6.6), (-0.5, 10.2, 6.6), 'decke'),
        snow_cap(-9.8, -7.8, -7.7, 7.8, 18.5, 0.5),
        snow_cap(-8, -7.9, 16, -6.1, 12.8, 0.4),
        snow_cap(-8, 6.1, 16, 7.9, 12.8, 0.4),
        Box((4, 6.5, -5), (12, 15, 2), 'sack'),
        Box((5, 15, -4), (11, 17, 1), 'sack', rotation=((8, 15, -1.5), (0, 20, 0))),
        Box((7, 16.5, -2.5), (9, 18, -0.5), 'band_gold'),
        Box((19, 9, -0.6), (20.2, 22, 0.6), 'messing'),
        Box((17.5, 21.5, -0.6), (20.2, 22.7, 0.6), 'messing'),
        Box((16.6, 16.5, -1.6), (19, 17.2, 1.6), 'eisen'),
        Box((16.9, 17.2, -1.3), (18.7, 20.6, 1.3), 'glas', emit=15, shade=False),
        Box((16.6, 20.6, -1.6), (19, 21.3, 1.6), 'eisen'),
        Box((17.5, 21.3, -0.3), (18.1, 21.6, 0.3), 'eisen'),
    ]
    b += gift(9, 4.5, 6, 6, 5, 6.5, 'papier_blau', 'band_blau', turn=15)
    b += gift(2.5, 3.5, 5, 5, 5, 9.5, 'papier_gruen', 'band_gruen', turn=-10, bow=False)
    b += gift(13, -3.5, 4.5, 4, 4.5, 6.5, 'papier_rot', 'band_rot', turn=30)
    return b


def heart(cx, cy, z):
    m = 'lebkuchen'
    return [
        Box((cx - 1.6, cy - 1.6, z - 0.5), (cx + 1.6, cy + 1.6, z), m, rotation=((cx, cy, z), (0, 0, 45))),
        Box((cx - 2.4, cy + 0.2, z - 0.5), (cx - 0.1, cy + 2.2, z), m),
        Box((cx + 0.1, cy + 0.2, z - 0.5), (cx + 2.4, cy + 2.2, z), m),
        Box((cx - 1.2, cy + 0.4, z - 0.6), (cx + 1.2, cy + 1.2, z - 0.5), 'zuckerguss'),
        Box((cx - 0.15, cy + 2.2, z - 0.35), (cx + 0.15, cy + 5, z - 0.15), 'band_gruen'),
    ]


def market_stall():
    b = []
    b += [
        Box((-24, 0, -6.5), (24, 15, -4.5), 'holz_dunkel'),
        Box((-24, 12.6, -7), (24, 13.6, -6.5), 'messing'),
        Box((-24, 1, -7), (24, 2, -6.5), 'messing'),
        Box((-24, 15, -9), (24, 17, 0), 'holz_hell'),
        Box((-24, 0, -6.5), (-21.5, 15, 6), 'holz'),
        Box((21.5, 0, -6.5), (24, 15, 6), 'holz'),
        Box((-24, 0, -9), (-21, 31, -6), 'holz'),
        Box((21, 0, -9), (24, 31, -6), 'holz'),
        Box((-24, 0, 30), (-21, 31, 33), 'holz'),
        Box((21, 0, 30), (24, 31, 33), 'holz'),
        Box((-21, 0, 31), (21, 31, 33), 'holz'),
        Box((-21, 11, 26), (21, 12, 31), 'holz_hell'),
        Box((-21, 20, 26), (21, 21, 31), 'holz_hell'),
        Box((-24, 28, -10), (24, 31, -7), 'holz_dunkel'),
        Box((-24, 28, 30), (24, 31, 33), 'holz_dunkel'),
        Box((-24, 28, -7), (-22, 31, 30), 'holz_dunkel'),
        Box((22, 28, -7), (24, 31, 30), 'holz_dunkel'),
        Box((-24, 26, -11), (24, 28.5, -9.5), 'tanne'),
    ]
    rise = 11.0
    run = 24.0
    angle = math.degrees(math.atan2(rise, run))
    length = math.hypot(rise, run) + 1
    front = ((0, 31, -12), (-angle, 0, 0))
    back = ((0, 31, 36), (angle, 0, 0))
    b += [
        Box((-24, 31, -12), (24, 32.4, -12 + length), 'lack_rot', rotation=front, faces={'down': 'markise', 'north': 'markise'}),
        Box((-24, 31, 36 - length), (24, 32.4, 36), 'lack_rot', rotation=back, faces={'down': 'markise', 'south': 'markise'}),
        Box((-24, 32.4, -12), (24, 33.4, -12 + length), 'schnee', rotation=front, faces=SNOW_SIDES, skip=('down',)),
        Box((-24, 32.4, 36 - length), (24, 33.4, 36), 'schnee', rotation=back, faces=SNOW_SIDES, skip=('down',)),
    ]
    for side in (-1, 1):
        x0, x1 = (-24, -22.5) if side < 0 else (22.5, 24)
        for k in range(5):
            y = 31 + k * 2.2
            inset = k * 4.8 + 1
            b.append(Box((x0, y, -12 + inset), (x1, y + 2.2, 36 - inset), 'holz'))
    b += [
        Box((-1.2, 42.2, 10.8), (1.2, 46.4, 13.2), 'stern', emit=15, shade=False, rotation=((0, 44.3, 12), (0, 0, 45))),
        Box((-0.6, 41.8, 11.4), (0.6, 47.6, 12.6), 'stern', emit=15, shade=False),
        Box((-2.9, 43.7, 11.4), (2.9, 44.9, 12.6), 'stern', emit=15, shade=False),
        Box((-16, 31.5, -11.6), (16, 38.5, -10.6), 'holz_dunkel', faces={'north': 'schild_gluehwein'}, uv={'north': (0, 0)}),
        Box((-16.4, 31, -11.8), (16.4, 31.6, -10.4), 'messing'),
        Box((-16.4, 38.4, -11.8), (16.4, 39, -10.4), 'messing'),
        Box((-16.4, 39, -11.9), (16.4, 39.6, -10.3), 'schnee', faces=SNOW_SIDES, skip=('down',)),
    ]
    colours = ['rot', 'gelb', 'gruen', 'blau', 'rosa', 'warm']
    for k, x in enumerate(range(-22, 23, 4)):
        slot = BULB_SLOTS[colours[k % len(colours)]]
        u = (slot % 3) * 10 + 2
        v = (slot // 3) * 10 + 2
        sag = 1.2 * math.sin(math.pi * ((x + 22) % 8) / 8)
        b.append(Box((x - 0.6, 24.6 - sag, -11), (x + 0.6, 26.2 - sag, -9.8), 'lampen', emit=15, shade=False,
                     uv={s: (u, v) for s in ('north', 'south', 'east', 'west', 'up', 'down')}))
    for x in (-18, -6, 6, 18):
        b += flat_bow(x, 27.2, -11, 'band_gruen', 0.9)
    for x in (-12, 0, 12):
        b += heart(x, 21, -9.5)
    b += [
        Box((6, 17, -7), (14, 23, 0), 'kessel'),
        Box((5.5, 23, -7.5), (14.5, 24, 0.5), 'kessel'),
        Box((9, 24, -4.5), (11, 25.5, -2.5), 'messing'),
        Box((13, 21, -4), (13.6, 27, -3.4), 'messing', rotation=((13.3, 21, -3.7), (0, 0, -20))),
        Box((5, 17, -7.5), (15, 17.6, 0.5), 'glut', emit=12),
    ]
    for x in (-18, -14.5, -11, -4.5):
        b.append(Box((x, 17, -7), (x + 2.4, 20, -4.6), 'becher'))
        b.append(Box((x + 2.4, 17.6, -6.3), (x + 3.2, 19.4, -5.3), 'becher'))
        b.append(Box((x + 0.3, 19.4, -6.7), (x + 2.1, 19.8, -4.9), 'gluehwein'))
    for k, x in enumerate(range(-19, 20, 5)):
        shelf = 12 if k % 2 == 0 else 21
        b.append(Box((x - 0.9, shelf, 27.5), (x + 0.9, shelf + 5, 29.3), 'flasche'))
        b.append(Box((x - 0.4, shelf + 5, 28), (x + 0.4, shelf + 7, 28.8), 'flasche'))
        b.append(Box((x + 1.6, 21 if shelf == 12 else 12, 27.4), (x + 3.6, (21 if shelf == 12 else 12) + 2.8, 29.4), 'becher'))
    return b


def signpost():
    b = [
        Box((-1.5, 0, -1.5), (1.5, 42, 1.5), 'holz_dunkel'),
        Box((-2.2, 0, -2.2), (2.2, 3, 2.2), 'holz_dunkel'),
        Box((-2, 42, -2), (2, 43.2, 2), 'holz_dunkel'),
        Box((-1.2, 43.2, -1.2), (1.2, 44.4, 1.2), 'holz_dunkel'),
        snow_cap(-2.1, -2.1, 2.1, 2.1, 43.2, 0.5),
        snow_cap(-1.3, -1.3, 1.3, 1.3, 44.4, 0.5),
        Box((-4, 0, -4), (4, 1, 4), 'schnee', faces=SNOW_SIDES),
        Box((-2.8, 1, -2.8), (2.8, 3.6, 2.8), 'schnee', faces=SNOW_SIDES),
    ]

    def board(y, direction, text, turn):
        length = 16
        x0, x1 = (1.5, 1.5 + length) if direction > 0 else (-1.5 - length, -1.5)
        tile = 'schild_' + text
        rot = ((0, y + 3, 0), (0, turn, 0)) if turn else None
        boxes = [
            Box((x0, y, -0.75), (x1, y + 6, 0.75), 'holz', faces={'north': tile, 'south': tile}, uv={'north': (0, 0), 'south': (0, 0)}, rotation=rot),
            Box((x0, y + 5.6, -0.95), (x1, y + 6.3, 0.95), 'holz_dunkel', rotation=rot),
            Box((x0, y - 0.3, -0.95), (x1, y + 0.4, 0.95), 'holz_dunkel', rotation=rot),
            Box((x0, y + 6.3, -1), (x1, y + 6.8, 1), 'schnee', faces=SNOW_SIDES, skip=('down',), rotation=rot),
        ]
        tip_x = x1 if direction > 0 else x0
        size = 6 / math.sqrt(2)
        tip_rot = ((tip_x, y + 3, 0), (0, turn, 45)) if turn else ((tip_x, y + 3, 0), (0, 0, 45))
        boxes.append(Box((tip_x - size / 2, y + 3 - size / 2, -0.7), (tip_x + size / 2, y + 3 + size / 2, 0.7), 'holz', rotation=tip_rot))
        for k, offset in enumerate((3, 7.5, 12)):
            x = x0 + offset if direction > 0 else x1 - offset
            drop = (2.2, 3.2, 1.6)[k]
            boxes.append(Box((x - 0.5, y - 0.3 - drop * 0.55, -0.4), (x + 0.5, y - 0.3, 0.4), 'eis', rotation=rot))
            boxes.append(Box((x - 0.25, y - 0.3 - drop, -0.2), (x + 0.25, y - 0.3 - drop * 0.55, 0.2), 'eis', rotation=rot))
        return boxes

    b += board(34, 1, 'nordpol', 8)
    b += board(26.5, -1, 'wichtel', -6)
    b += board(19, 1, 'kakao', -10)
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
    result.append(Prop('geschenkstapel', [Part('geschenkstapel', stack_model(), (0.5, None, 0.5))], [(x, y, z) for x in (0, 1) for z in (0, 1) for y in (0, 1, 2)], title='Geschenkstapel'))
    result.append(Prop('laterne', [
        Part('laterne_mast', lantern_mast(), (0, None, 0)),
        Part('laterne_kopf', lantern_head(), (0, None, 0)),
    ], column(3), light=(0, 3, 0), title='Straßenlaterne'))
    result.append(Prop('zuckerstange', [Part('zuckerstange', candy_cane(), (0, None, 0))], column(3) + [(1, 2, 0)], title='Zuckerstange'))
    result.append(Prop('schlitten', [Part('schlitten', sleigh(), (0.5, None, 0))], [(x, y, 0) for x in (0, 1) for y in (0, 1)], title='Schlitten'))
    stall = [(x, y, z) for x in (-1, 0, 1) for z in (0, 2) for y in (0, 1)]
    result.append(Prop('marktstand', [Part('marktstand', market_stall(), (0, None, 0.75))], stall, title='Glühweinstand'))
    result.append(Prop('wegweiser', [Part('wegweiser', signpost(), (0, None, 0))], column(3) + [(1, 1, 0), (-1, 1, 0), (1, 2, 0), (-1, 2, 0)], title='Wegweiser'))
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
