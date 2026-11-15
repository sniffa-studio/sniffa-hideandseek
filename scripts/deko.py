import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/hideandseek'
FUNCTIONS = ROOT / 'datapacks/sniffa-props/data/sniffa/function'

ATLAS = 512
TILE = 32
COLUMNS = ATLAS // TILE
TEXTURE = 'hideandseek:item/props/deko'
JITTER = 0.0
SHEET = 1024
MAX_FACE = 64
PAD = 2
NUDGE = 0.04
NO_EDGES = {'warnstreifen', 'riffel', 'bildschirm', 'band_schwarz', 'glaskugel', 'netz', 'stern_weiss', 'stern_rot', 'eis_flaeche', 'flamme_tief', 'flamme_kern', 'eiszapfen', 'feder', 'feder_hell', 'sack', 'schneeball', 'glas', 'lampen', 'stern', 'flamme', 'glut', 'schnee_rand', 'bonbons', 'kugeln', 'tanne', 'beere'}
RIVETS = {'eisen', 'kessel'}


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
            amount = 1 + JITTER * rnd.uniform(-spread, spread)
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
                    img.putpixel((x, y), shade(base, 1.12 + JITTER * rnd.uniform(-0.03, 0.03)))
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
    base = hexcolour('#eef3f9')
    shadow = hexcolour('#d9e3ee')
    for y in range(TILE):
        for x in range(TILE):
            wave = math.sin((x + y * 0.4) * 0.45) + math.sin((x * 0.3 - y) * 0.7)
            img.putpixel((x, y), shadow if wave > 1.45 else base)
    for x, y in ((3, 4), (11, 9), (22, 3), (27, 15), (7, 20), (17, 25), (29, 28), (14, 15)):
        img.putpixel((x, y), hexcolour('#ffffff'))
        img.putpixel((x + 1, y), hexcolour('#c9e6ff'))


def snow_side(img, rnd):
    base = hexcolour('#eef4fb')
    for x in range(TILE):
        depth = 1 + rnd.choice((0, 0, 1, 1, 1, 2))
        for y in range(TILE):
            if y < depth or y < 2:
                img.putpixel((x, y), shade(base, 1 + JITTER * rnd.uniform(-0.03, 0.03)))


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
            img.putpixel((x, y), shade(colour, 1 + JITTER * rnd.uniform(-0.03, 0.03)))


def planks(base, height=4):
    def paint(img, rnd):
        offsets = [rnd.randrange(TILE) for _ in range(TILE // height + 1)]
        tones = [rnd.uniform(0.92, 1.08) for _ in range(TILE // height + 1)]
        for y in range(TILE):
            board = y // height
            for x in range(TILE):
                amount = tones[board] + JITTER * rnd.uniform(-0.04, 0.04)
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
                img.putpixel((x, y), shade(base, lines[x] + JITTER * rnd.uniform(-0.04, 0.04)))
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
            img.putpixel((x, y), shade(colour, 1 + JITTER * rnd.uniform(-0.03, 0.03)))


def fir(img, rnd):
    dark = hexcolour('#1e4f2e')
    mid = hexcolour('#2b6a3a')
    light = hexcolour('#3f8a47')
    for y in range(TILE):
        for x in range(TILE):
            k = (x + 2 * y) % 6
            colour = dark if k in (0, 1) else mid
            if (x * 7 + y * 3) % 11 == 0:
                colour = light
            img.putpixel((x, y), colour)
    for x, y in ((4, 5), (19, 8), (10, 21), (26, 26), (28, 13)):
        img.putpixel((x, y), hexcolour('#c4232c'))
        img.putpixel((x + 1, y), hexcolour('#e2484d'))


def burlap(img, rnd):
    base = hexcolour('#a58156')
    for y in range(TILE):
        for x in range(TILE):
            amount = 1 + JITTER * rnd.uniform(-0.05, 0.05)
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
            img.putpixel((x, y), shade(colour, 1 + JITTER * rnd.uniform(-0.03, 0.03)))


def ice(img, rnd):
    top = hexcolour('#e9f6ff')
    low = hexcolour('#9fd2f2')
    for y in range(TILE):
        for x in range(TILE):
            img.putpixel((x, y), shade(mix(top, low, y / TILE), 1 + JITTER * rnd.uniform(-0.04, 0.04)))


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
            img.putpixel((x, y), shade(colour, 1 + JITTER * rnd.uniform(-0.04, 0.04)))


def awning_of(value):
    base = hexcolour(value)
    white = hexcolour('#f4efe6')

    def paint(img, rnd):
        for y in range(TILE):
            for x in range(TILE):
                colour = base if (x // 4) % 2 == 0 else white
                img.putpixel((x, y), shade(colour, 1 + JITTER * rnd.uniform(-0.03, 0.03)))
    return paint


def candies(img, rnd):
    palette = {'rot': '#e0303c', 'gold': '#f4c542', 'gruen': '#3dbb5a', 'blau': '#3b82e0', 'lila': '#b25ce0', 'silber': '#f0f0f0'}
    glass = hexcolour('#d9eef2')
    for name, slot in KUGEL_SLOTS.items():
        x0 = (slot % 3) * 10
        y0 = (slot // 3) * 10
        for y in range(10):
            for x in range(10):
                if y < 3:
                    colour = glass
                else:
                    colour = hexcolour(rnd.choice([palette[name], palette[name], '#ffffff', palette[rnd.choice(list(palette))]]))
                img.putpixel((x0 + x, y0 + y), shade(colour, 1 + JITTER * rnd.uniform(-0.05, 0.05)))


def baubles(img, rnd):
    palette = {'rot': '#c8202e', 'gold': '#e2b23a', 'gruen': '#1f8a45', 'blau': '#2f6fd0', 'lila': '#8b3cc4', 'silber': '#c9d2da'}
    for name, slot in KUGEL_SLOTS.items():
        base = hexcolour(palette[name])
        x0 = (slot % 3) * 10
        y0 = (slot // 3) * 10
        for y in range(10):
            for x in range(10):
                d = math.hypot(x - 3, y - 3) / 8
                img.putpixel((x0 + x, y0 + y), shade(base, 1.35 - d * 0.6))
        img.putpixel((x0 + 2, y0 + 2), hexcolour('#ffffff'))
        img.putpixel((x0 + 3, y0 + 2), hexcolour('#ffffff'))


def stones(img, rnd):
    base = hexcolour('#8d9093')
    for y in range(TILE):
        for x in range(TILE):
            row = y // 8
            edge = y % 8 == 7 or (x + (row % 2) * 6) % 12 == 11
            img.putpixel((x, y), shade(base, (0.68 if edge else 1) + JITTER * rnd.uniform(-0.06, 0.06)))


def speckled(base_hex, dot_hex):
    def paint(img, rnd):
        fill_noise(img, rnd, hexcolour(base_hex), 0.06)
        for k in range(22):
            img.putpixel(((k * 13 + 5) % TILE, (k * 7 + k // 5) % TILE), hexcolour(dot_hex))
    return paint


def cone_paper(img, rnd):
    red = hexcolour('#c8202c')
    white = hexcolour('#f4efe6')
    for y in range(TILE):
        for x in range(TILE):
            colour = red if ((x + y // 2) // 3) % 2 == 0 else white
            img.putpixel((x, y), shade(colour, 1 + JITTER * rnd.uniform(-0.03, 0.03)))


def knit(img, rnd):
    red = hexcolour('#c42232')
    white = hexcolour('#f1ece2')
    for y in range(TILE):
        for x in range(TILE):
            colour = white if y % 8 in (3, 4) else red
            amount = 0.85 if (x + (y % 2)) % 2 == 0 else 1.0
            img.putpixel((x, y), shade(colour, amount + JITTER * rnd.uniform(-0.03, 0.03)))


def honeycomb(img, rnd):
    base = hexcolour('#e2a12a')
    dark = hexcolour('#b9781a')
    for y in range(TILE):
        for x in range(TILE):
            row = y // 4
            edge = y % 4 == 0 or (x + (row % 2) * 2) % 4 == 0
            img.putpixel((x, y), dark if edge else base)


def checks_of(a_hex, b_hex):
    def paint(img, rnd):
        for y in range(TILE):
            for x in range(TILE):
                img.putpixel((x, y), hexcolour(a_hex) if ((x // 2) + (y // 2)) % 2 == 0 else hexcolour(b_hex))
    return paint


def netting(img, rnd):
    for y in range(TILE):
        for x in range(TILE):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                img.putpixel((x, y), hexcolour('#e8e2d0'))


def ice_floor(img, rnd):
    base = hexcolour('#bfe3f5')
    for y in range(TILE):
        for x in range(TILE):
            crack = (x * 3 + y * 5) % 23 == 0 or (x * 7 - y * 2) % 29 == 0
            img.putpixel((x, y), hexcolour('#e8f7ff') if crack else base)


def knit_of(value):
    base = hexcolour(value)

    def paint(img, rnd):
        for y in range(TILE):
            for x in range(TILE):
                colour = hexcolour('#f1ece2') if y % 8 in (3, 4) else base
                amount = 0.85 if (x + (y % 2)) % 2 == 0 else 1.0
                img.putpixel((x, y), shade(colour, amount))
    return paint


def glass_globe(img, rnd):
    for y in range(TILE):
        for x in range(TILE):
            alpha = 70 if (x + y) % 9 else 120
            img.putpixel((x, y), (215, 238, 250, alpha))


def tread_plate(img, rnd):
    for y in range(TILE):
        for x in range(TILE):
            c = hexcolour('#5d6266')
            if (x + y) % 6 == 0 and x % 3 != 2 or (x - y) % 6 == 3 and y % 3 != 2:
                c = hexcolour('#7c8287')
            img.putpixel((x, y), c)


def hazard_stripes(img, rnd):
    for y in range(TILE):
        for x in range(TILE):
            img.putpixel((x, y), hexcolour('#f2c230') if ((x + y) // 3) % 2 == 0 else hexcolour('#202022'))


def belt(img, rnd):
    for y in range(TILE):
        for x in range(TILE):
            img.putpixel((x, y), hexcolour('#2a2a2e') if (x // 2) % 3 else hexcolour('#3c3c42'))


def screen(img, rnd):
    for y in range(TILE):
        for x in range(TILE):
            c = hexcolour('#0f3a2a')
            if y % 5 == 2 and (x * 7 + y) % 11 < 7:
                c = hexcolour('#55f0a0')
            img.putpixel((x, y), c)


def flame(img, rnd):
    for y in range(TILE):
        for x in range(TILE):
            t = y / TILE
            img.putpixel((x, y), mix(hexcolour('#ffd75a'), hexcolour('#ff6a1a'), t))


def bulbs(img, rnd):
    colours = ['#ff4b4b', '#ffd34d', '#57e36b', '#4fa3ff', '#ff8cf2', '#fff2c2']
    for index, value in enumerate(colours):
        base = hexcolour(value)
        x0 = (index % 3) * 10
        y0 = (index // 3) * 10
        for y in range(8):
            for x in range(8):
                img.putpixel((x0 + x, y0 + y), shade(base, 1 + JITTER * rnd.uniform(-0.03, 0.03)))


BULB_SLOTS = {'rot': 0, 'gelb': 1, 'gruen': 2, 'blau': 3, 'rosa': 4, 'warm': 5}
KUGEL_SLOTS = {'rot': 0, 'gold': 1, 'gruen': 2, 'blau': 3, 'lila': 4, 'silber': 5}

FONT = {
    'A': ['010', '101', '111', '101', '101'],
    ' ': ['00', '00', '00', '00', '00'],
    'F': ['111', '100', '110', '100', '100'],
    'B': ['110', '101', '110', '101', '110'],
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
    'S': ['111', '100', '111', '001', '111'],
    'M': ['10001', '11011', '10101', '10001', '10001'],
    'T': ['111', '010', '010', '010', '010'],
    'Z': ['111', '001', '010', '100', '111'],
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
                            img.putpixel((x + col * size + dx, y0 + row * size + dy), shade(ink, 1 + JITTER * rnd.uniform(-0.05, 0.05)))
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
    atlas.add('schild_gluehwein', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'GLÜHWEIN', 64, 14, size=2), density=64 / 46, span=2)
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
    atlas.add('lack_gruen', grain(hexcolour('#1f6b3a')))
    atlas.add('lack_braun', grain(hexcolour('#6b3a1f')))
    atlas.add('lack_blau', grain(hexcolour('#21477e')))
    atlas.add('lack_lila', grain(hexcolour('#5c2a7a')))
    atlas.add('markise_gruen', awning_of('#1f7a3d'))
    atlas.add('markise_braun', awning_of('#8a4b22'))
    atlas.add('markise_blau', awning_of('#2a5aa0'))
    atlas.add('markise_lila', awning_of('#6d2f8f'))
    atlas.add('bonbons', candies)
    atlas.add('kugeln', baubles)
    atlas.add('wurst', flat(hexcolour('#9a4a24'), 0.08))
    atlas.add('broetchen', flat(hexcolour('#d49a55'), 0.08))
    atlas.add('senf', flat(hexcolour('#e2b82a'), 0.05))
    atlas.add('kerze', flat(hexcolour('#f3ead6'), 0.03))
    atlas.add('kerze_rot', flat(hexcolour('#b3202b'), 0.04))
    atlas.add('flamme', flame)
    atlas.add('lack_orange', grain(hexcolour('#c4581c')))
    atlas.add('lack_gelb', grain(hexcolour('#d9a51e')))
    atlas.add('lack_tuerkis', grain(hexcolour('#1d8a86')))
    atlas.add('kupfer', metal(hexcolour('#b8683a')))
    atlas.add('mandel', speckled('#8a4a22', '#c27a3c'))
    atlas.add('tuete', cone_paper)
    atlas.add('crepe', speckled('#e2b46a', '#c98e42'))
    atlas.add('nougat', flat(hexcolour('#5a2e17'), 0.05))
    atlas.add('kakao', flat(hexcolour('#5b3420'), 0.04))
    atlas.add('becher_weiss', flat(hexcolour('#f1ede6'), 0.03))
    atlas.add('topf', metal(hexcolour('#9aa3aa')))
    atlas.add('marone', speckled('#5b2e1a', '#8a4d2c'))
    atlas.add('topf_ton', planks(hexcolour('#a5512e'), 8))
    atlas.add('schneeball', snow, seed='schneeball')
    atlas.add('karotte', flat(hexcolour('#e57a1f'), 0.06))
    atlas.add('kohle', flat(hexcolour('#1d1d20'), 0.08))
    atlas.add('schal', knit)
    atlas.add('stroh', grain(hexcolour('#c9a24a')))
    atlas.add('fell', speckled('#7a4f2e', '#6a4226'))
    atlas.add('fell_hell', speckled('#d8c3a0', '#c9b088'))
    atlas.add('geweih', grain(hexcolour('#cdb68e')))
    atlas.add('schild_mandeln', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'MANDELN', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_crepes', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'CREPES', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_kakaostand', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'KAKAO', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_maronen', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'MARONEN', 32, 5), density=32 / 13)
    atlas.add('schild_post', lettered(hexcolour('#5a3820'), hexcolour('#f7e7b0'), 'POST', 32, 24, size=2), density=4)
    atlas.add('sack_flick', flat(hexcolour('#8c6a43'), 0.04))
    atlas.add('feder', flat(hexcolour('#f5f2ea'), 0.03))
    atlas.add('feder_hell', flat(hexcolour('#e3d9b6'), 0.03))
    atlas.add('flamme_tief', flat(hexcolour('#ff6a1a'), 0.04))
    atlas.add('flamme_kern', flat(hexcolour('#fff2b0'), 0.02))
    atlas.add('eiszapfen', ice)
    atlas.add('brezel', flat(hexcolour('#8a4a1c'), 0.04))
    atlas.add('honig', flat(hexcolour('#e8a326'), 0.04))
    atlas.add('wabe', honeycomb)
    atlas.add('bienenwachs', flat(hexcolour('#e9c25a'), 0.03))
    atlas.add('karo', checks_of('#c42232', '#f1ece2'))
    atlas.add('stern_weiss', flat(hexcolour('#fff6dc'), 0.02))
    atlas.add('stern_rot', flat(hexcolour('#ff4a4a'), 0.02))
    atlas.add('netz', netting)
    atlas.add('eis_flaeche', ice_floor)
    atlas.add('schild_brezeln', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'BREZELN', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_spielzeug', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'SPIELZEUG', 64, 14, size=2, gap=0), density=64 / 46, span=2)
    atlas.add('schild_sterne', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'STERNE', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_honig', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'HONIG', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_tannen', lettered(hexcolour('#9a6a3c'), hexcolour('#2a1508'), 'TANNEN', 32, 15), density=32 / 13)
    atlas.add('apfel', flat(hexcolour('#c42029'), 0.04))
    atlas.add('vanille', flat(hexcolour('#f6dc8a'), 0.03))
    atlas.add('kaese', speckled('#f2c94c', '#e0ad2c'))
    atlas.add('kaese_rinde', flat(hexcolour('#c98a2a'), 0.04))
    atlas.add('kaese_flies', flat(hexcolour('#f7d76a'), 0.03))
    atlas.add('kartoffel', flat(hexcolour('#d9b26a'), 0.05))
    atlas.add('strick_gruen', knit_of('#1f7a3d'))
    atlas.add('strick_blau', knit_of('#2155a6'))
    atlas.add('erdbeere', speckled('#d92b3a', '#f5e08a'))
    atlas.add('banane', flat(hexcolour('#f3dc5a'), 0.03))
    atlas.add('ananas', speckled('#f2b632', '#a8741a'))
    atlas.add('schoko', flat(hexcolour('#4a2614'), 0.04))
    atlas.add('schild_bratapfel', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'BRATAPFEL', 64, 14, size=2, gap=0), density=64 / 46, span=2)
    atlas.add('schild_raclette', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'RACLETTE', 64, 14, size=2, gap=0), density=64 / 46, span=2)
    atlas.add('schild_strick', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'MUETZEN', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_schoko', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'SCHOKO', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_frohe', lettered(hexcolour('#2f1a0c'), hexcolour('#f7e7b0'), 'FROHE WEIHNACHTEN', 128, 22, size=2), density=128 / 46, span=4)
    atlas.add('paket', flat(hexcolour('#b98a55'), 0.04))
    atlas.add('kordel', flat(hexcolour('#e8dcc0'), 0.03))
    atlas.add('brief', flat(hexcolour('#f4efe2'), 0.02))
    atlas.add('puffer', speckled('#d89a3e', '#a8681e'))
    atlas.add('apfelmus', flat(hexcolour('#e9cf7a'), 0.04))
    atlas.add('glas_hell', flat(hexcolour('#dcecef'), 0.02))
    atlas.add('glaskugel', glass_globe)
    atlas.add('bande', awning_of('#c42232'))
    atlas.add('schild_post_stand', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'POSTAMT', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_puffer', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'PUFFER', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_werkstatt', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'WICHTEL', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('band_schwarz', belt)
    atlas.add('riffel', tread_plate)
    atlas.add('warnstreifen', hazard_stripes)
    atlas.add('schild_fabrik', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'GESCHENKE', 64, 14, size=2, gap=0), density=64 / 40, span=2)
    atlas.add('schild_presse', lettered(hexcolour('#1b2a4a'), hexcolour('#f2c14e'), 'SPIELZEUG', 64, 12, size=2, gap=0), density=64 / 48, span=2)
    atlas.add('bildschirm', screen)
    atlas.add('kiste_schild', lettered(hexcolour('#c9a26a'), hexcolour('#7a1420'), 'NORDPOL', 32, 9), density=32 / 7)
    atlas.add('creme', flat(hexcolour('#efe3c8'), 0.04))
    atlas.add('stein', stones)
    atlas.add('fass', planks(hexcolour('#7a4e2c'), 4))
    atlas.add('schild_lebkuchen', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'LEBKUCHEN', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_wurst', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'BRATWURST', 64, 14, size=2, gap=0), density=64 / 46, span=2)
    atlas.add('schild_kerzen', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'KERZEN', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_kugeln', lettered(hexcolour('#2f1a0c'), hexcolour('#f2c14e'), 'KUGELN', 64, 14, size=2), density=64 / 46, span=2)
    atlas.add('schild_markt', lettered(hexcolour('#5a3820'), hexcolour('#f7e7b0'), 'WEIHNACHTSMARKT', 128, 32, size=2, gap=1), density=128 / 46, span=4)
    atlas.add('schild_kakao', lettered(hexcolour('#9a6a3c'), hexcolour('#2a1508'), 'KAKAO', 32, 12), density=2)
    return atlas


class Box:
    def __init__(self, a, b, mat, faces=None, rotation=None, emit=0, skip=(), uv=None, shade=True):
        self.solid = True
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


FACE_AXES = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}


class FaceSheet:
    def __init__(self, atlas):
        self.atlas = atlas
        self.requests = {}
        self.slots = {}

    def request(self, mat, pw, ph, side, fixed, cuts, emit):
        tile = self.atlas.tiles[mat]
        pw = max(1, min(MAX_FACE if not fixed else TILE * tile.span, round(pw)))
        ph = max(1, min(MAX_FACE if not fixed else TILE, round(ph)))
        ha, va = FACE_AXES[side]
        edges = not fixed and not emit and mat not in NO_EDGES and side != 'down'
        key = (mat, pw, ph, fixed, edges and ha not in cuts, edges and va not in cuts, side == 'down', side == 'up')
        self.requests[key] = True
        return key

    def source(self, mat):
        tile = self.atlas.tiles[mat]
        crop = self.atlas.image.crop((tile.column * TILE, tile.row * TILE, (tile.column + tile.span) * TILE, (tile.row + 1) * TILE))
        if tile.span > 1:
            return crop
        big = Image.new('RGBA', (MAX_FACE, MAX_FACE))
        for ox in range(0, MAX_FACE, TILE):
            for oy in range(0, MAX_FACE, TILE):
                big.paste(crop, (ox, oy))
        return big

    def paint(self, key):
        mat, pw, ph, fixed, h_edges, v_edges, down, up = key
        src = self.source(mat)
        ox, oy = fixed if fixed else (0, 0)
        region = src.crop((ox, oy, ox + pw, oy + ph)).copy()
        if down:
            for y in range(ph):
                for x in range(pw):
                    c = region.getpixel((x, y))
                    region.putpixel((x, y), shade(c, 0.78))
            return region
        if pw < 3 or ph < 3:
            h_edges = v_edges = False

        def light(c):
            return (clamp(c[0] * 1.3 + 20), clamp(c[1] * 1.3 + 20), clamp(c[2] * 1.3 + 20), c[3])

        def dark(c):
            return (clamp(c[0] * 0.66), clamp(c[1] * 0.66), clamp(c[2] * 0.66), c[3])
        base = [[region.getpixel((x, y)) for x in range(pw)] for y in range(ph)]
        if v_edges:
            for x in range(pw):
                region.putpixel((x, 0), light(base[0][x]))
                if x > 0 or not h_edges:
                    region.putpixel((x, ph - 1), dark(base[ph - 1][x]))
        if h_edges:
            for y in range(ph):
                region.putpixel((0, y), light(base[y][0]))
                if y > 0 or not v_edges:
                    region.putpixel((pw - 1, y), dark(base[y][pw - 1]))
        if h_edges and v_edges and mat in RIVETS and pw >= 8 and ph >= 8:
            for x, y in ((1, 1), (pw - 2, 1), (1, ph - 2), (pw - 2, ph - 2)):
                region.putpixel((x, y), light(light(base[y][x])))
        return region

    def pack(self):
        keys = sorted(self.requests, key=lambda k: (-k[2], -k[1]))
        x = y = row = 0
        placed = {}
        for key in keys:
            pw, ph = key[1] + 2 * PAD, key[2] + 2 * PAD
            if x + pw > SHEET:
                x = 0
                y += row
                row = 0
            placed[key] = (x + PAD, y + PAD)
            x += pw
            row = max(row, ph)
        height = y + row
        size = 64
        while size < height:
            size *= 2
        self.height = size
        self.image = Image.new('RGBA', (SHEET, size), (0, 0, 0, 0))
        for key, (px, py) in placed.items():
            region = self.paint(key)
            w, h = region.size
            padded = Image.new('RGBA', (w + 2 * PAD, h + 2 * PAD))
            padded.paste(region.resize((w + 2 * PAD, h + 2 * PAD), Image.NEAREST), (0, 0))
            for k in range(PAD):
                padded.paste(region.crop((0, 0, w, 1)), (PAD, k))
                padded.paste(region.crop((0, h - 1, w, h)), (PAD, h + PAD + k))
            strip = padded.crop((PAD, 0, PAD + 1, h + 2 * PAD))
            strip_r = padded.crop((PAD + w - 1, 0, PAD + w, h + 2 * PAD))
            for k in range(PAD):
                padded.paste(strip, (k, 0))
                padded.paste(strip_r, (w + PAD + k, 0))
            padded.paste(region, (PAD, PAD))
            self.image.paste(padded, (px - PAD, py - PAD))
        self.slots = placed

    def uv(self, key):
        px, py = self.slots[key]
        su = 16 / SHEET
        sv = 16 / self.height
        return [round(px * su, 5), round(py * sv, 5), round((px + key[1]) * su, 5), round((py + key[2]) * sv, 5)]


def model_json(sheet, boxes, shift, seed, check=True):
    elements = []
    for box in boxes:
        a = [box.a[i] - shift[i] for i in range(3)]
        b = [box.b[i] - shift[i] for i in range(3)]
        lo = [round(min(a[i], b[i]), 4) for i in range(3)]
        hi = [round(max(a[i], b[i]), 4) for i in range(3)]
        if check:
            lo = [max(v, -16) if v > -16 - 6 * NUDGE else v for v in lo]
            hi = [min(v, 32) if v < 32 + 6 * NUDGE else v for v in hi]
        for value in lo + hi:
            if check and (value < -16 or value > 32):
                raise ValueError(f'out of range {lo} {hi} for {box.mat}')
        faces = {}
        for side in ('north', 'east', 'south', 'west', 'up', 'down'):
            if side in box.skip:
                continue
            mat = box.faces.get(side, box.mat)
            tile = sheet.atlas.tiles[mat]
            w, h = face_size(box, side)
            fixed = None
            if side in box.uv:
                fixed = tuple(round(v) for v in box.uv[side])
            elif tile.density > 1 or tile.name.startswith('schild') or tile.name == 'lampen':
                fixed = (0, 0)
            key = sheet.request(mat, w * tile.density, h * tile.density, side, fixed, getattr(box, 'cuts', set()), box.emit)
            faces[side] = {'_key': key, 'texture': '#0'}
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


def resolve(sheet, model):
    for element in model['elements']:
        for face in element['faces'].values():
            face['uv'] = sheet.uv(face.pop('_key'))
    return model


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
    boxes = gift(cx, cz, spec['w'], spec['h'], spec['w'], 0, 'papier_' + colour, 'band_' + colour)
    clean(boxes)
    return boxes


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


def patch(tile_slots, colour, inset=2):
    slot = tile_slots[colour]
    u = (slot % 3) * 10 + inset
    v = (slot // 3) * 10 + inset
    return {s: (u, v) for s in ('north', 'south', 'east', 'west', 'up', 'down')}


def mug(x, y, z, mat='becher'):
    return [
        Box((x, y, z), (x + 2.4, y + 3, z + 2.4), mat),
        Box((x + 2.4, y + 0.6, z + 0.7), (x + 3.2, y + 2.4, z + 1.7), mat),
        Box((x + 0.3, y + 2.4, z + 0.3), (x + 2.1, y + 2.8, z + 2.1), 'gluehwein'),
    ]


def bottle(x, y, z):
    return [
        Box((x - 0.9, y, z - 0.9), (x + 0.9, y + 5, z + 0.9), 'flasche'),
        Box((x - 0.4, y + 5, z - 0.4), (x + 0.4, y + 7, z + 0.4), 'flasche'),
    ]


def candy_jar(x, y, z, colour):
    return [
        Box((x - 2.2, y, z - 2.2), (x + 2.2, y + 5.6, z + 2.2), 'bonbons', uv=patch(KUGEL_SLOTS, colour, 0)),
        Box((x - 2.5, y + 5.6, z - 2.5), (x + 2.5, y + 6.5, z + 2.5), 'messing'),
        Box((x - 0.7, y + 6.5, z - 0.7), (x + 0.7, y + 7.2, z + 0.7), 'messing'),
    ]


def candle(x, y, z, height, mat='kerze', width=1.4):
    w = width / 2
    return [
        Box((x - w, y, z - w), (x + w, y + height, z + w), mat),
        Box((x - 0.3, y + height, z - 0.3), (x + 0.3, y + height + 1.1, z + 0.3), 'flamme', emit=15, shade=False),
    ]


def nutcracker(x, y, z, scale=1.0):
    s = scale
    return [
        Box((x - 1.3 * s, y, z - 0.9 * s), (x - 0.2 * s, y + 3.5 * s, z + 0.9 * s), 'eisen'),
        Box((x + 0.2 * s, y, z - 0.9 * s), (x + 1.3 * s, y + 3.5 * s, z + 0.9 * s), 'eisen'),
        Box((x - 1.6 * s, y + 3.5 * s, z - 1.1 * s), (x + 1.6 * s, y + 7.5 * s, z + 1.1 * s), 'lack_rot'),
        Box((x - 1.7 * s, y + 3.9 * s, z - 1.2 * s), (x + 1.7 * s, y + 4.4 * s, z + 1.2 * s), 'messing'),
        Box((x - 2.4 * s, y + 4 * s, z - 0.7 * s), (x - 1.6 * s, y + 7.2 * s, z + 0.7 * s), 'lack_rot'),
        Box((x + 1.6 * s, y + 4 * s, z - 0.7 * s), (x + 2.4 * s, y + 7.2 * s, z + 0.7 * s), 'lack_rot'),
        Box((x - 1.3 * s, y + 7.5 * s, z - 1.1 * s), (x + 1.3 * s, y + 10 * s, z + 1.1 * s), 'holz_hell'),
        Box((x - 1.35 * s, y + 7.4 * s, z - 1.2 * s), (x + 1.35 * s, y + 8.6 * s, z - 0.6 * s), 'zuckerguss'),
        Box((x - 1.5 * s, y + 10 * s, z - 1.3 * s), (x + 1.5 * s, y + 13 * s, z + 1.3 * s), 'eisen'),
        Box((x - 1.55 * s, y + 10.2 * s, z - 1.35 * s), (x + 1.55 * s, y + 10.8 * s, z + 1.35 * s), 'messing'),
    ]


def bauble(x, y, z, colour, size=2.0):
    h = size / 2
    return [
        Box((x - h, y - h, z - h), (x + h, y + h, z + h), 'kugeln', uv=patch(KUGEL_SLOTS, colour, 1)),
        Box((x - 0.4, y + h, z - 0.4), (x + 0.4, y + h + 0.6, z + 0.4), 'messing'),
    ]


def table():
    b = [
        Box((-4, 0, -4), (4, 1, 4), 'holz_dunkel'),
        Box((-3, 1, -3), (3, 12, 3), 'fass'),
        Box((-3.3, 2.5, -3.3), (3.3, 3.3, 3.3), 'eisen'),
        Box((-3.3, 9.5, -3.3), (3.3, 10.3, 3.3), 'eisen'),
        Box((-1.2, 12, -1.2), (1.2, 16, 1.2), 'holz_dunkel'),
        Box((-6.5, 16, -6.5), (6.5, 17.5, 6.5), 'holz_hell'),
        Box((-6.6, 15.6, -6.6), (6.6, 16, 6.6), 'holz_dunkel'),
        Box((-6, 17.5, -6.3), (6, 18, -3), 'schnee', faces=SNOW_SIDES, skip=('down',)),
        Box((-2.6, 17.5, -1.6), (-0.2, 18.4, 1.6), 'tanne'),
    ]
    b += mug(1, 17.5, -1.5)
    b += mug(-4.5, 17.5, 2)
    b += candle(-1.4, 18.4, 0, 2.6, 'kerze_rot', 1.0)
    return b


def euler_matrix(ax, ay, az):
    cx, sx = math.cos(math.radians(ax)), math.sin(math.radians(ax))
    cy, sy = math.cos(math.radians(ay)), math.sin(math.radians(ay))
    cz, sz = math.cos(math.radians(az)), math.sin(math.radians(az))
    rx = ((1, 0, 0), (0, cx, -sx), (0, sx, cx))
    ry = ((cy, 0, sy), (0, 1, 0), (-sy, 0, cy))
    rz = ((cz, -sz, 0), (sz, cz, 0), (0, 0, 1))

    def mul(a, b):
        return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))
    return mul(rz, mul(ry, rx))


def world_bounds(box):
    lo = [min(box.a[i], box.b[i]) for i in range(3)]
    hi = [max(box.a[i], box.b[i]) for i in range(3)]
    if not box.rotation:
        return lo, hi
    origin, angles = box.rotation
    m = euler_matrix(*angles)
    points = []
    for x in (lo[0], hi[0]):
        for y in (lo[1], hi[1]):
            for z in (lo[2], hi[2]):
                v = (x - origin[0], y - origin[1], z - origin[2])
                points.append(tuple(origin[i] + sum(m[i][k] * v[k] for k in range(3)) for i in range(3)))
    return [min(p[i] for p in points) for i in range(3)], [max(p[i] for p in points) for i in range(3)]


def footprint_of(boxes, minimum=2.5):
    cells = set()
    for box in boxes:
        if not getattr(box, 'solid', True):
            continue
        lo, hi = world_bounds(box)
        need = [min(minimum, 0.9 * (hi[i] - lo[i])) for i in range(3)]
        xs = range(math.floor((lo[0] + 8) / 16), math.ceil((hi[0] + 8) / 16))
        ys = range(math.floor(lo[1] / 16), math.ceil(hi[1] / 16))
        zs = range(math.floor((lo[2] + 8) / 16), math.ceil((hi[2] + 8) / 16))
        for bx in xs:
            ox = min(hi[0], bx * 16 + 8) - max(lo[0], bx * 16 - 8)
            if ox < need[0]:
                continue
            for by in ys:
                oy = min(hi[1], by * 16 + 16) - max(lo[1], by * 16)
                if oy < need[1] or by < 0:
                    continue
                for bz in zs:
                    oz = min(hi[2], bz * 16 + 8) - max(lo[2], bz * 16 - 8)
                    if oz >= need[2]:
                        cells.add((bx, by, bz))
    return sorted(cells)


def clone_box(piece, a, b):
    clone = Box(tuple(a), tuple(b), piece.mat, faces=dict(piece.faces), rotation=piece.rotation, emit=piece.emit,
                skip=set(piece.skip), uv=dict(piece.uv), shade=piece.shade)
    clone.solid = piece.solid
    clone.cuts = set(getattr(piece, 'cuts', set()))
    return clone


def split_at(boxes, axis, cuts):
    out = []
    for piece in boxes:
        rotated = [i for i in range(3) if piece.rotation and piece.rotation[1][i]]
        if any(i != axis for i in rotated) or piece.uv:
            out.append(piece)
            continue
        lo = min(piece.a[axis], piece.b[axis])
        hi = max(piece.a[axis], piece.b[axis])
        inner = [c for c in cuts if lo + 0.5 < c < hi - 0.5]
        if not inner:
            out.append(piece)
            continue
        edges = [lo] + inner + [hi]
        for k in range(len(edges) - 1):
            a = list(piece.a)
            b = list(piece.b)
            a[axis], b[axis] = edges[k], edges[k + 1]
            clone = clone_box(piece, a, b)
            clone.cuts.add(axis)
            out.append(clone)
    return out


def raw_bounds(box):
    return [min(box.a[i], box.b[i]) for i in range(3)], [max(box.a[i], box.b[i]) for i in range(3)]


def group_boxes(boxes, axis=None):
    lows = [min(raw_bounds(b)[0][i] for b in boxes) for i in range(3)]
    highs = [max(raw_bounds(b)[1][i] for b in boxes) for i in range(3)]
    starts = []
    for i in range(3):
        if axis is not None and i != 1:
            centre = axis[0] if i == 0 else axis[1]
            starts.append(centre - 24)
        else:
            span = highs[i] - lows[i]
            count = max(1, math.ceil(span / 48))
            starts.append((lows[i] + highs[i]) / 2 - count * 24)
    pieces = boxes
    for i in range(3):
        cuts = [starts[i] + 48 * k for k in range(-4, 8)]
        pieces = split_at(pieces, i, cuts)
    tiles = {}
    loose = []
    for box in pieces:
        lo, hi = raw_bounds(box)
        key = tuple(math.floor((lo[i] - starts[i] + 0.01) / 48) for i in range(3))
        centre = [starts[i] + 48 * key[i] + 24 for i in range(3)]
        if all(lo[i] - centre[i] >= -24.001 and hi[i] - centre[i] <= 24.001 for i in range(3)):
            tiles.setdefault(key, (centre, []))[1].append(box)
        else:
            loose.append(box)
    groups = list(tiles.values())
    for box in loose:
        lo, hi = raw_bounds(box)
        for centre, members in groups:
            if all(lo[i] - centre[i] >= -24 and hi[i] - centre[i] <= 24 for i in range(3)):
                members.append(box)
                break
        else:
            centre = [round((lo[i] + hi[i]) / 2 * 2) / 2 for i in range(3)]
            if axis is not None:
                centre[0], centre[2] = axis[0], axis[1]
            if not all(lo[i] - centre[i] >= -24 and hi[i] - centre[i] <= 24 for i in range(3)):
                raise ValueError(f'box does not fit {lo} {hi}')
            groups.append((centre, [box]))
    return groups


def auto_parts(item, boxes, tags=(), axis=None):
    parts = []
    for k, (centre, members) in enumerate(group_boxes(boxes, axis)):
        parts.append(Part(f'{item}_{k + 1}', members, (centre[0] / 16, centre[1] / 16, centre[2] / 16), tags=tags))
    return parts


def soft(boxes):
    for box in boxes:
        box.solid = False
    return boxes


def dot(x, y, z, mat, size=0.7, depth=0.3):
    h = size / 2
    return Box((x - h, y - h, z - depth), (x + h, y + h, z), mat)


def heart_big(cx, cy, z, s, ribbon=True):
    m = 'lebkuchen'
    b = [
        Box((cx - 1.6 * s, cy - 1.6 * s, z - 0.8), (cx + 1.6 * s, cy + 1.6 * s, z), m, rotation=((cx, cy, z), (0, 0, 45))),
        Box((cx - 2.4 * s, cy + 0.2 * s, z - 0.8), (cx - 0.1 * s, cy + 2.2 * s, z), m),
        Box((cx + 0.1 * s, cy + 0.2 * s, z - 0.8), (cx + 2.4 * s, cy + 2.2 * s, z), m),
    ]
    for k in range(7):
        a = math.radians(200 - k * 36.6)
        b.append(dot(cx + 1.85 * s * math.cos(a) * 0.95, cy + 0.6 * s + 1.4 * s * math.sin(a) * 0.9, z - 0.8, 'zuckerguss', 0.45 * s / 1.4 + 0.25))
    b.append(dot(cx - 0.6 * s, cy + 0.4 * s, z - 0.8, 'beere', 0.5 * s / 1.4 + 0.3))
    b.append(dot(cx + 0.6 * s, cy + 0.4 * s, z - 0.8, 'band_gruen', 0.5 * s / 1.4 + 0.3))
    if ribbon:
        b.append(Box((cx - 0.2, cy + 2.2 * s, z - 0.5), (cx + 0.2, cy + 2.2 * s + 4, z - 0.2), 'band_rot'))
    return b


def gingerbread_figure(x, y, z, s=1.0):
    m = 'lebkuchen'
    return [
        Box((x - 1.5 * s, y + 2.6 * s, z - 0.4), (x + 1.5 * s, y + 6.2 * s, z + 0.4), m),
        Box((x - 1.3 * s, y + 6.2 * s, z - 0.4), (x + 1.3 * s, y + 8.8 * s, z + 0.4), m),
        Box((x - 3.1 * s, y + 4.8 * s, z - 0.4), (x + 3.1 * s, y + 6 * s, z + 0.4), m),
        Box((x - 1.5 * s, y, z - 0.4), (x - 0.2 * s, y + 2.8 * s, z + 0.4), m),
        Box((x + 0.2 * s, y, z - 0.4), (x + 1.5 * s, y + 2.8 * s, z + 0.4), m),
        dot(x - 0.5 * s, y + 7.8 * s, z - 0.4, 'kessel', 0.45, 0.15),
        dot(x + 0.5 * s, y + 7.8 * s, z - 0.4, 'kessel', 0.45, 0.15),
        dot(x, y + 5 * s, z - 0.4, 'beere', 0.55, 0.15),
        dot(x, y + 3.8 * s, z - 0.4, 'band_gruen', 0.55, 0.15),
    ]


ROOF_COLOUR = {'rot': 'lack_rot', 'gruen': 'lack_gruen', 'braun': 'lack_braun', 'blau': 'lack_blau', 'lila': 'lack_lila', 'orange': 'lack_orange', 'gelb': 'lack_gelb', 'tuerkis': 'lack_tuerkis'}

COUNTER_Y = 17
COUNTER_Z = (-24, -12)
SHELF = (19.5, 33.5)
SHELF_Z = (30, 37)


def stall_big(colour, sign):
    roof = ROOF_COLOUR[colour]
    b = [
        Box((-37, 0, -22), (37, 15, -19.5), 'holz_dunkel'),
        Box((-37, 12.8, -22.6), (37, 13.8, -22), 'messing'),
        Box((-37, 1.2, -22.6), (37, 2.2, -22), 'messing'),
        Box((-40, 15, -25), (40, 17, -12), 'holz_hell'),
        Box((-40, 0, -22), (-37, 15, -8), 'holz_dunkel'),
        Box((37, 0, -22), (40, 15, -8), 'holz_dunkel'),
        Box((-40, 0, -26), (-36, 50, -22), 'holz'),
        Box((36, 0, -26), (40, 50, -22), 'holz'),
        Box((-40, 0, 36), (-36, 50, 40), 'holz'),
        Box((36, 0, 36), (40, 50, 40), 'holz'),
        Box((-36, 0, 37), (36, 50, 39), 'holz'),
        Box((-39.5, 0, 10), (-37.5, 50, 36), 'holz'),
        Box((37.5, 0, 10), (39.5, 50, 36), 'holz'),
        Box((-36, SHELF[0] - 1.5, SHELF_Z[0]), (36, SHELF[0], SHELF_Z[1]), 'holz_hell'),
        Box((-36, SHELF[1] - 1.5, SHELF_Z[0]), (36, SHELF[1], SHELF_Z[1]), 'holz_hell'),
        Box((-41, 46, -27), (41, 50, -22), 'holz_dunkel'),
        Box((-41, 46, 36), (41, 50, 41), 'holz_dunkel'),
        Box((-41, 46, -22), (-37, 50, 36), 'holz_dunkel'),
        Box((37, 46, -22), (41, 50, 36), 'holz_dunkel'),
    ]
    ridge_z, ridge_y, eave_y = 7.0, 72.0, 49.0
    front_z, back_z = -31.0, 45.0
    for z0, sign_dir in ((front_z, -1), (back_z, 1)):
        run = abs(ridge_z - z0)
        rise = ridge_y - eave_y
        angle = math.degrees(math.atan2(rise, run))
        length = math.hypot(run, rise) + 0.6
        rot = ((0, eave_y, z0), (angle if sign_dir > 0 else -angle, 0, 0))
        if sign_dir < 0:
            plank = Box((-41.5, eave_y, z0), (41.5, eave_y + 2, z0 + length), roof, rotation=rot, faces={'down': 'holz'})
            snow = Box((-41, eave_y + 2, z0 + 0.6), (41, eave_y + 2.7, z0 + length), 'schnee', rotation=rot, skip=('down', 'north'))
        else:
            plank = Box((-41.5, eave_y, z0 - length), (41.5, eave_y + 2, z0), roof, rotation=rot, faces={'down': 'holz'})
            snow = Box((-41, eave_y + 2, z0 - length), (41, eave_y + 2.7, z0 - 0.6), 'schnee', rotation=rot, skip=('down', 'south'))
        b += [plank, snow]
    for side in (-1, 1):
        x0, x1 = (-40, -38) if side < 0 else (38, 40)
        for k in range(7):
            y = 50 + k * 3.2
            inset = (ridge_y - eave_y) and (k * 3.2 + 1) / (ridge_y - eave_y) * 36
            b.append(Box((x0, y, front_z + 4 + inset), (x1, y + 3.2, back_z - 4 - inset), 'holz'))
    b += [
        Box((-41.5, ridge_y - 1, ridge_z - 2), (41.5, ridge_y + 2, ridge_z + 2), 'holz_dunkel'),
        Box((-41.5, ridge_y + 2, ridge_z - 2), (41.5, ridge_y + 2.6, ridge_z + 2), 'schnee', skip=('down',)),
    ]
    for k, x in enumerate(range(-40, 41, 3)):
        drop = (3.5, 1.6, 2.6, 1.2, 4.2, 2)[k % 6]
        b += soft([Box((x - 0.6, eave_y - 0.2 - drop * 0.5, front_z - 1.4), (x + 0.6, eave_y + 0.6, front_z - 0.2), 'eiszapfen'),
                   Box((x - 0.3, eave_y - 0.2 - drop, front_z - 1.1), (x + 0.3, eave_y - 0.2 - drop * 0.5, front_z - 0.5), 'eiszapfen')])
        b += soft([Box((x - 1.6, eave_y + 0.6, front_z - 1.8), (x + 1.6, eave_y + 2.2, front_z + 0.2), 'schnee')])
    edge_colours = ['warm', 'rot', 'warm', 'gelb', 'warm', 'gruen']
    for k, x in enumerate(range(-39, 40, 4)):
        b += soft([Box((x - 0.6, 47.2, -31.8), (x + 0.6, 48.6, -30.8), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, edge_colours[k % 6]))])
    for side in (-1, 1):
        x = side * 42.1
        for z0, z1 in ((front_z, ridge_z), (back_z, ridge_z)):
            for k in range(1, 9):
                t = k / 9
                z = z0 + (z1 - z0) * t
                y = eave_y + 2.4 + (ridge_y - eave_y) * t
                b += soft([Box((x - 0.6, y, z - 0.6), (x + 0.6, y + 1.4, z + 0.6), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, edge_colours[(k + (side > 0)) % 6]))])
    b += soft([
        Box((-1.6, 75.4, ridge_z - 0.8), (1.6, 80.4, ridge_z + 0.8), 'stern', emit=15, shade=False, rotation=((0, 77.9, ridge_z), (0, 0, 45))),
        Box((-0.8, 74.6, ridge_z - 0.8), (0.8, 81.4, ridge_z + 0.8), 'stern', emit=15, shade=False),
        Box((-3.4, 77.1, ridge_z - 0.8), (3.4, 78.7, ridge_z + 0.8), 'stern', emit=15, shade=False),
    ])
    colours = list(BULB_SLOTS)
    for k, x in enumerate(range(-40, 40, 5)):
        long = k % 2 == 0
        tile = roof if k % 2 == 0 else 'creme'
        b += soft([
            Box((x, 40.5 if long else 42.5, -28), (x + 5, 46, -27), tile),
            Box((x + 1.6, 39.6 if long else 41.6, -28), (x + 3.4, 40.5 if long else 42.5, -27), tile),
        ])
    b += soft([Box((-41, 46, -29.4), (41, 49, -27), 'tanne')])
    for x in (-30, -10, 10, 30):
        b += soft(flat_bow(x, 47.5, -29.4, 'band_rot', 1.1))
    for k, x in enumerate(range(-38, 39, 4)):
        sag = 1.6 * math.sin(math.pi * ((x + 38) % 16) / 16)
        b += soft([Box((x - 0.7, 37.4 - sag, -28.3), (x + 0.7, 39.2 - sag, -27), 'lampen', emit=15, shade=False,
                       uv=patch(BULB_SLOTS, colours[k % len(colours)]))])
    b += soft([
        Box((-23, 51, -33), (23, 61.1, -31.8), 'holz_dunkel', faces={'north': sign}, uv={'north': (0, 0)}),
        Box((-23.6, 50.4, -33.3), (23.6, 51.2, -31.5), 'messing'),
        Box((-23.6, 60.9, -33.3), (23.6, 61.7, -31.5), 'messing'),
        Box((-23.6, 61.7, -33.4), (23.6, 62.3, -31.4), 'schnee', skip=('down',)),
        Box((-20, 49, -32.6), (-19, 51, -31.8), 'eisen'),
        Box((19, 49, -32.6), (20, 51, -31.8), 'eisen'),
    ])
    for x in (-33, 33):
        b += soft([
            Box((x - 0.2, 40, -24.2), (x + 0.2, 46, -23.8), 'eisen'),
            Box((x - 1.8, 33.4, -25.8), (x + 1.8, 34.2, -22.2), 'eisen'),
            Box((x - 1.4, 34.2, -25.4), (x + 1.4, 38, -22.6), 'glas', emit=15, shade=False),
            Box((x - 1.8, 38, -25.8), (x + 1.8, 38.8, -22.2), 'eisen'),
            Box((x - 0.9, 38.8, -24.9), (x + 0.9, 40, -23.1), 'eisen'),
        ])
    return b


def gluehwein_big():
    b = []
    b += [
        Box((4, 17, -22), (16, 25, -13), 'kessel'),
        Box((3.4, 25, -22.6), (16.6, 26.2, -12.4), 'kessel'),
        Box((9, 26.2, -18.5), (11, 27.8, -16.5), 'messing'),
        Box((15, 22, -18), (15.8, 30, -17.2), 'messing', rotation=((15.4, 22, -17.6), (0, 0, -20))),
        Box((3, 17, -22.8), (17, 17.8, -12.2), 'glut', emit=12),
    ]
    for x in (-34, -30, -26, -22, -14, -10):
        b += mug(x, COUNTER_Y, -21)
    for x in (24, 28.5, 33):
        b += mug(x, COUNTER_Y, -18)
    for k, x in enumerate(range(-33, 34, 6)):
        b += bottle(x, SHELF[0], 33.5)
        b += mug(x - 1.2, SHELF[1], 32)
    return b


def lebkuchen_big():
    b = []
    for x in (-27, -9, 9, 27):
        b += heart_big(x, 30, -24.2, 2.4)
    for x, colour in ((-34, 'rot'), (-28.5, 'gruen'), (-23, 'gold')):
        b += candy_jar(x, COUNTER_Y, -18, colour)
    for x in (-12, -4, 4, 12):
        b += gingerbread_figure(x, COUNTER_Y, -18, 1.0)
    for x in (22, 27, 32):
        b += heart_big(x, COUNTER_Y + 3.2, -15, 1.1, ribbon=False)
    for k, x in enumerate(range(-30, 31, 10)):
        b += gingerbread_figure(x, SHELF[0], 33.5, 1.0)
        b += heart_big(x, SHELF[1] + 4, 33.6, 1.3, ribbon=False)
    for x in (-38.5, 38.5):
        b += [Box((x - 0.6, COUNTER_Y, -20), (x + 0.6, COUNTER_Y + 9, -18.8), 'zucker')]
        b += arc((x - 1.6 if x > 0 else x + 1.6, COUNTER_Y + 9, -19.4), 1.6, 0 if x > 0 else 180, 180 if x > 0 else 0, 5, 1.2, 1.2, 'zucker')
    return b


def wurst_big():
    b = [
        Box((-30, 17, -23), (-2, 19, -13), 'kessel'),
        Box((-30.3, 19, -23.3), (-1.7, 19.8, -12.7), 'eisen'),
        Box((-29.5, 19, -22.5), (-2.5, 19.6, -13.5), 'glut', emit=13),
    ]
    for z in (-21.6, -19.4, -17.2, -15):
        b.append(Box((-29.8, 19.8, z - 0.2), (-2.2, 20.1, z + 0.2), 'eisen'))
    for k, x in enumerate(range(-27, -3, 3)):
        turn = (-8, 6, -4, 10, -6, 4, -9, 7)[k % 8]
        b.append(Box((x - 0.8, 20.1, -22), (x + 0.8, 21.5, -14), 'wurst', rotation=((x, 20.8, -18), (0, turn, 0))))
    b += [
        Box((4, 17, -23), (18, 18.4, -14), 'holz_hell'),
        Box((4, 18.4, -23), (18, 19, -22.4), 'holz_hell'),
        Box((4, 18.4, -14.6), (18, 19, -14), 'holz_hell'),
    ]
    for x in (6, 9, 12, 15):
        for z in (-21, -17.5):
            b.append(Box((x - 1.2, 18.4, z - 0.9), (x + 1.2, 19.9, z + 0.9), 'broetchen'))
    b += [
        Box((24, 17, -20), (26, 23, -18), 'senf'),
        Box((24.5, 23, -19.5), (25.5, 24.4, -18.5), 'senf'),
        Box((28, 17, -20), (30, 23, -18), 'becher'),
        Box((28.5, 23, -19.5), (29.5, 24.4, -18.5), 'becher'),
    ]
    for x in range(-30, 31, 6):
        b += [Box((x - 2.2, SHELF[0], 31), (x + 2.2, SHELF[0] + 1.6, 35), 'broetchen')]
        b += [Box((x - 2.2, SHELF[1], 31), (x + 2.2, SHELF[1] + 0.6, 35), 'zuckerguss'), Box((x - 1.6, SHELF[1] + 0.6, 31.6), (x + 1.6, SHELF[1] + 1.8, 34.4), 'wurst')]
    for x in range(-34, 35, 4):
        if -26 < x < 26:
            continue
        b += soft([Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing'), Box((x - 0.7, 30, -24.9), (x + 0.7, 36, -24.1), 'wurst')])
    return b


def kerzen_big():
    b = []
    for k, (x, h) in enumerate(((-35, 6), (-32.5, 9), (-30, 5), (-27, 8), (-24, 11), (-21, 7))):
        b += candle(x, COUNTER_Y, -18, h, 'kerze' if k % 2 == 0 else 'kerze_rot', 1.8)
    b += nutcracker(-10, COUNTER_Y, -18, 1.3)
    b += nutcracker(-3, COUNTER_Y, -18, 1.0)
    b += nutcracker(4, COUNTER_Y, -18, 1.3)
    b += [Box((14, 17, -23), (36, 18, -14), 'holz_hell')]
    for x in (17, 21, 25, 29, 33):
        b += [Box((x - 1.2, 18, -20), (x + 1.2, 19.6, -17), 'holz'), Box((x - 0.8, 19.6, -19.6), (x + 0.8, 20.8, -17.4), 'holz_hell')]
    for k, x in enumerate(range(-32, 33, 8)):
        b += candle(x - 1.5, SHELF[0], 33.5, 4 + k % 3, 'kerze_rot' if k % 2 else 'kerze', 1.6)
        b += candle(x + 1.5, SHELF[0], 33.5, 3 + (k + 1) % 3, 'kerze' if k % 2 else 'kerze_rot', 1.4)
        b += nutcracker(x, SHELF[1], 33.5, 0.75)
    for x in (-30, -18, 18, 30):
        b += soft([
            Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing'),
            Box((x - 1.6, 33, -24.9), (x + 1.6, 36.2, -24.2), 'stern', emit=10, rotation=((x, 34.6, -24.5), (0, 0, 45))),
            Box((x - 0.7, 32.2, -24.9), (x + 0.7, 37, -24.2), 'stern', emit=10),
        ])
    return b


def kugel_big():
    b = []
    colours = list(KUGEL_SLOTS)
    for k, x in enumerate(range(-36, 37, 4)):
        drop = (2, 5, 3.5, 6.5, 2.5, 4.5)[k % 6]
        b += soft([Box((x - 0.1, 37 - drop, -24.6), (x + 0.1, 46, -24.4), 'messing')])
        b += soft(bauble(x, 37 - drop - 1.6, -24.5, colours[k % len(colours)], 3.0))
    b += [Box((-36, 17, -23), (-8, 18.2, -14), 'holz_hell')]
    for row, z in enumerate((-21, -16.5)):
        for k, x in enumerate(range(-33, -8, 4)):
            b += bauble(x + row * 2, 20, z, colours[(k + row * 2) % len(colours)], 3.0)
    b += [Box((6, 17, -23), (36, 18.2, -14), 'holz_hell')]
    for k, x in enumerate(range(9, 35, 5)):
        b += bauble(x, 20.4, -18.5, colours[(k + 3) % len(colours)], 3.6)
    for k, x in enumerate(range(-32, 33, 5)):
        b += bauble(x, SHELF[0] + 1.8, 33.5, colours[k % len(colours)], 3.2)
        b += bauble(x + 2.5, SHELF[1] + 1.8, 33.5, colours[(k + 2) % len(colours)], 3.2)
    return b


def cone_bag(x, y, z, s=1.0):
    return [
        Box((x - 1.4 * s, y + 2 * s, z - 1.4 * s), (x + 1.4 * s, y + 6 * s, z + 1.4 * s), 'tuete'),
        Box((x - 0.9 * s, y, z - 0.9 * s), (x + 0.9 * s, y + 2 * s, z + 0.9 * s), 'tuete'),
        Box((x - 1.2 * s, y + 6 * s, z - 1.2 * s), (x + 1.2 * s, y + 7 * s, z + 1.2 * s), 'mandel'),
    ]


def mandeln_big():
    b = [
        Box((-14, 17, -23), (6, 18, -13), 'kessel'),
        Box((-12, 18, -22), (4, 23, -14), 'kupfer'),
        Box((-11, 22, -21), (3, 23.2, -15), 'mandel'),
        Box((-4.6, 23.2, -18.4), (-3.4, 30, -17.6), 'holz_hell', rotation=((-4, 23.2, -18), (0, 0, 25))),
        Box((-13, 17, -22.5), (5, 17.6, -13.5), 'glut', emit=12),
    ]
    for x in (12, 16, 20, 24, 28, 32):
        for z in (-21.5, -17):
            b += cone_bag(x, COUNTER_Y, z, 0.9)
    for x in (-34, -29, -24):
        b += candy_jar(x, COUNTER_Y, -18, ('rot', 'gold', 'gruen')[(x // 5) % 3])
    for x in range(-32, 33, 6):
        b += cone_bag(x, SHELF[0], 33.5, 0.9)
        b += cone_bag(x + 3, SHELF[1], 33.5, 0.9)
    for x in (-30, -18, 18, 30):
        b += soft(cone_bag(x, 30, -24.5, 0.8) + [Box((x - 0.15, 37, -24.6), (x + 0.15, 46, -24.3), 'messing')])
    return b


def crepe_plate(x, z):
    return [
        Box((x - 5, 17, z - 5), (x + 5, 20, z + 5), 'kessel'),
        Box((x - 4.6, 20, z - 4.6), (x + 4.6, 20.4, z + 4.6), 'eisen'),
        Box((x - 3.6, 20.4, z - 3.6), (x + 3.6, 20.7, z + 3.6), 'crepe'),
        Box((x - 4.6, 17.6, z - 5.4), (x + 4.6, 18.4, z - 5), 'glut', emit=10),
    ]


def crepes_big():
    b = []
    b += crepe_plate(-26, -18)
    b += crepe_plate(-12, -18)
    b += [
        Box((-3, 17, -21), (3, 22, -15), 'creme'),
        Box((-2.6, 22, -20.6), (2.6, 22.4, -15.4), 'crepe'),
        Box((-1, 22, -18.6), (4, 22.8, -17.4), 'holz_hell', rotation=((0, 22.4, -18), (0, 0, 15))),
    ]
    for x in (8, 12.5, 17):
        b += [Box((x - 1.8, COUNTER_Y, -20), (x + 1.8, COUNTER_Y + 4.5, -16.4), 'nougat'), Box((x - 2, COUNTER_Y + 4.5, -20.2), (x + 2, COUNTER_Y + 5.4, -16.2), 'lack_rot')]
    for k, x in enumerate((24, 29, 34)):
        b += [Box((x - 2.4, COUNTER_Y, -21), (x + 2.4, COUNTER_Y + 0.6, -15), 'creme')]
        for i in range(3):
            b += [Box((x - 1.8, COUNTER_Y + 0.6 + i * 0.8, -20.4), (x + 1.8, COUNTER_Y + 1.4 + i * 0.8, -15.6), 'crepe', rotation=((x, COUNTER_Y + 1, -18), (0, 20 * i - 15, 0)))]
    for x in range(-32, 33, 8):
        b += [Box((x - 3, SHELF[0], 31), (x + 3, SHELF[0] + 0.8, 36), 'creme')]
        for i in range(4):
            b += [Box((x - 2.4, SHELF[0] + 0.8 + i, 31.6), (x + 2.4, SHELF[0] + 1.6 + i, 35.4), 'crepe')]
        b += [Box((x - 1.8, SHELF[1], 32), (x + 1.8, SHELF[1] + 4.5, 35.6), 'nougat'), Box((x - 2, SHELF[1] + 4.5, 31.8), (x + 2, SHELF[1] + 5.4, 35.8), 'lack_rot')]
    return b


def cocoa_mug(x, y, z):
    return [
        Box((x, y, z), (x + 2.6, y + 3.2, z + 2.6), 'becher_weiss'),
        Box((x + 2.6, y + 0.6, z + 0.8), (x + 3.4, y + 2.6, z + 1.8), 'becher_weiss'),
        Box((x + 0.3, y + 2.6, z + 0.3), (x + 2.3, y + 3, z + 2.3), 'kakao'),
        Box((x + 0.5, y + 3, z + 0.5), (x + 2.1, y + 4, z + 2.1), 'zuckerguss'),
        Box((x + 0.9, y + 4, z + 0.9), (x + 1.7, y + 4.6, z + 1.7), 'zuckerguss'),
    ]


def kakao_big():
    b = [
        Box((-16, 17, -22), (-4, 25, -13), 'kessel'),
        Box((-16.6, 25, -22.6), (-3.4, 26.2, -12.4), 'kessel'),
        Box((-11, 26.2, -18.5), (-9, 27.8, -16.5), 'messing'),
        Box((-17, 17, -22.8), (-3, 17.8, -12.2), 'glut', emit=12),
        Box((2, 17, -22), (12, 25, -13), 'topf'),
        Box((1.6, 25, -22.4), (12.4, 26, -12.6), 'topf'),
        Box((6, 26, -18.5), (8, 27.4, -16.5), 'messing'),
    ]
    for x in (-35, -31, -27, -23, 16, 20, 24, 28, 32):
        b += cocoa_mug(x, COUNTER_Y, -21)
    for x in range(-33, 34, 6):
        b += cocoa_mug(x - 1.3, SHELF[0], 32)
        b += bottle(x, SHELF[1], 33.5)
    for x in (-30, -18, 18, 30):
        b += soft([
            Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing'),
            Box((x - 1.6, 33, -24.9), (x + 1.6, 36.2, -24.2), 'stern', emit=10, rotation=((x, 34.6, -24.5), (0, 0, 45))),
            Box((x - 0.7, 32.2, -24.9), (x + 0.7, 37, -24.2), 'stern', emit=10),
        ])
    return b


def wheel(cx, cy, z, r, mat='holz_dunkel'):
    b = []
    for k in range(8):
        b.append(Box((cx - r * 0.42, cy + r - 1, z - 0.8), (cx + r * 0.42, cy + r, z + 0.8), mat, rotation=((cx, cy, z), (0, 0, k * 45))))
    for k in range(4):
        b.append(Box((cx - 0.4, cy - r + 0.6, z - 0.5), (cx + 0.4, cy + r - 0.6, z + 0.5), mat, rotation=((cx, cy, z), (0, 0, k * 45))))
    b.append(Box((cx - 1, cy - 1, z - 1.2), (cx + 1, cy + 1, z + 1.2), 'eisen'))
    return b


def chestnut_oven():
    b = []
    for z in (-7.5, 7.5):
        b += wheel(-2, 6, z, 6)
    b += [
        Box((-14, 9, -6.5), (14, 11, 6.5), 'holz_dunkel'),
        Box((-22, 9.5, -0.6), (-14, 10.5, 0.6), 'holz'),
        Box((-22, 9.5, -5), (-21, 10.5, 5), 'holz'),
        Box((-13, 11, -5.5), (8, 23, 5.5), 'kessel'),
        Box((-13.4, 23, -5.9), (8.4, 24, 5.9), 'eisen'),
        Box((-13.4, 11, -5.9), (8.4, 12, 5.9), 'messing'),
        Box((-9, 13, -5.8), (-1, 19, -5.5), 'glut', emit=13),
        Box((-9.5, 12.5, -6.2), (-0.5, 13, -5.4), 'eisen'),
        Box((-9.5, 19, -6.2), (-0.5, 19.5, -5.4), 'eisen'),
        Box((-6, 24, -3), (-1, 25.4, 3), 'kupfer'),
        Box((-5.4, 25.4, -2.4), (-1.6, 26.4, 2.4), 'marone'),
        Box((3, 24, -1.5), (6, 40, 1.5), 'eisen'),
        Box((2.4, 40, -2.1), (6.6, 41.4, 2.1), 'eisen'),
        Box((8, 11, -5), (14, 20, 5), 'holz'),
        Box((8, 20, -5.4), (14.4, 21, 5.4), 'holz_hell'),
        snow_cap(8, -5.4, 14.4, 5.4, 21, 0.6),
        snow_cap(-13.4, -5.9, 2, 5.9, 24, 0.5),
    ]
    for x in (9.5, 12.5):
        for z in (-3, 0.5, 3.5):
            b += cone_bag(x, 21, z, 0.7)
    b += soft([
        Box((-12, 28, -0.6), (1, 33, 0.6), 'holz_dunkel', faces={'north': 'schild_maronen', 'south': 'schild_maronen'}, uv={'north': (0, 0), 'south': (0, 0)}),
        Box((-11, 24, -0.3), (-10.4, 28, 0.3), 'eisen'),
        Box((-1.4, 24, -0.3), (-0.8, 28, 0.3), 'eisen'),
        Box((-12.2, 33, -0.8), (1.2, 33.6, 0.8), 'schnee', skip=('down',)),
    ])
    return b


def light_arch():
    b = []
    for side in (-1, 1):
        x = side * 29.5
        b += [
            Box((x - 2.5, 0, -2.5), (x + 2.5, 2, 2.5), 'eisen'),
            Box((x - 1.5, 2, -1.5), (x + 1.5, 44, 1.5), 'eisen'),
            Box((x - 2, 20, -2), (x + 2, 21, 2), 'messing'),
            snow_cap(x - 2.6, -2.6, x + 2.6, 2.6, 2, 0.5),
        ]
        b.append(Box((x - 1.5, 0, -1.5), (x + 1.5, 44, 1.5), 'eisen'))
        for k in range(8):
            y = 4 + k * 5
            b += soft([Box((x - 1.9, y, -1.9), (x + 1.9, y + 2, 1.9), 'tanne')])
            if k % 2 == 0:
                b += soft(flat_bow(x, y + 1, -1.9, 'band_rot', 0.7))
    colours = list(BULB_SLOTS)
    steps = 18
    for k in range(steps + 1):
        t = k / steps
        angle = math.pi * t
        x = -29.5 * math.cos(angle)
        y = 44 + 14 * math.sin(angle)
        tangent = math.degrees(math.atan2(14 * math.cos(angle), 29.5 * math.sin(angle) + 1e-6))
        seg = 6.2
        b += soft([Box((x - seg / 2, y - 2.2, -2.2), (x + seg / 2, y + 2.2, 2.2), 'tanne', rotation=((x, y, 0), (0, 0, tangent)))])
        b += soft([Box((x - 0.7, y - 3.4, -0.7), (x + 0.7, y - 1.6, 0.7), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[k % 6]))])
        if k % 3 == 0:
            b += soft([Box((x - 0.6, y + 1.6, -0.6), (x + 0.6, y + 2.8, 0.6), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, 'warm'))])
    b += soft([
        Box((-2, 58.4, -0.8), (2, 64, 0.8), 'stern', emit=15, shade=False, rotation=((0, 61.2, 0), (0, 0, 45))),
        Box((-1, 57.6, -0.8), (1, 64.8, 0.8), 'stern', emit=15, shade=False),
        Box((-3.6, 60.4, -0.8), (3.6, 62, 0.8), 'stern', emit=15, shade=False),
    ])
    return b


def potted_tree():
    b = [
        Box((-6, 0, -6), (6, 8, 6), 'topf_ton'),
        Box((-6.6, 7, -6.6), (6.6, 9, 6.6), 'topf_ton'),
        Box((-5.6, 8, -5.6), (5.6, 8.6, 5.6), 'schnee'),
        Box((-1.2, 8, -1.2), (1.2, 14, 1.2), 'holz_dunkel'),
        Box((-6.8, 3.5, -6.8), (6.8, 5, 6.8), 'band_rot'),
    ]
    layers = ((12, 11, 7), (18, 9, 7), (24, 7, 6.5), (30, 5, 6), (35, 3, 5))
    for y, r, h in layers:
        b.append(Box((-r, y, -r), (r, y + h, r), 'tanne'))
        b.append(Box((-r * 0.85, y, -r * 0.85), (r * 0.85, y + h, r * 0.85), 'tanne', rotation=((0, y, 0), (0, 45, 0))))
        b.append(Box((-r + 0.6, y + h - 0.6, -r + 0.6), (r - 0.6, y + h - 0.1, r - 0.6), 'schnee', skip=('down',)))
    colours = list(KUGEL_SLOTS)
    bulbs_ = list(BULB_SLOTS)
    k = 0
    for y, r, h in layers[:-1]:
        for j in range(6):
            a = math.radians(j * 60 + y * 7)
            x, z = (r + 0.4) * math.cos(a), (r + 0.4) * math.sin(a)
            b += soft(bauble(x, y + 2.2, z, colours[k % len(colours)], 1.8))
            b += soft([Box((x * 0.92 - 0.5, y + h - 1.8, z * 0.92 - 0.5), (x * 0.92 + 0.5, y + h - 0.6, z * 0.92 + 0.5), 'lampen', emit=15, shade=False,
                           uv=patch(BULB_SLOTS, bulbs_[(k + 2) % 6]))])
            k += 1
    b += soft([
        Box((-2, 41, -0.7), (2, 46, 0.7), 'stern', emit=15, shade=False, rotation=((0, 43.5, 0), (0, 0, 45))),
        Box((-1, 40, -0.7), (1, 47, 0.7), 'stern', emit=15, shade=False),
    ])
    return b


def ball(cx, cy, cz, r, mat='schneeball'):
    return [
        Box((cx - r, cy - r * 0.72, cz - r * 0.72), (cx + r, cy + r * 0.72, cz + r * 0.72), mat),
        Box((cx - r * 0.72, cy - r, cz - r * 0.72), (cx + r * 0.72, cy + r, cz + r * 0.72), mat),
        Box((cx - r * 0.72, cy - r * 0.72, cz - r), (cx + r * 0.72, cy + r * 0.72, cz + r), mat),
        Box((cx - r * 0.86, cy - r * 0.86, cz - r * 0.86), (cx + r * 0.86, cy + r * 0.86, cz + r * 0.86), mat),
    ]


def snowman():
    b = []
    b += ball(0, 9, 0, 9.5)
    b += ball(0, 23, 0, 7)
    b += ball(0, 33.5, 0, 5.2)
    b += [
        Box((-5.6, 0, -5.6), (5.6, 1.2, 5.6), 'schneeball'),
        Box((-0.6, 34, -9.6), (0.6, 35.2, -5), 'karotte'),
        Box((-0.4, 34.2, -10.6), (0.4, 35, -9.6), 'karotte'),
        Box((-2.4, 36, -5.2), (-1.2, 37.2, -4.6), 'kohle'),
        Box((1.2, 36, -5.2), (2.4, 37.2, -4.6), 'kohle'),
    ]
    for k, x in enumerate((-2.8, -1.4, 0, 1.4, 2.8)):
        y = 31.4 - 0.5 * (2 - abs(k - 2))
        b.append(Box((x - 0.4, y - 0.4, -5.1), (x + 0.4, y + 0.4, -4.5)
                     , 'kohle'))
    for y in (25, 21.5, 18):
        b.append(Box((-0.7, y - 0.7, -7.6), (0.7, y + 0.7, -6.8), 'kohle'))
    b += [
        Box((-5.6, 27.6, -5.6), (5.6, 30, 5.6), 'schal'),
        Box((-4.4, 18, -6.6), (-2, 28.6, -5.4), 'schal', rotation=((-3.2, 28.6, -6), (8, 0, -6))),
        Box((-6, 37.6, -6), (6, 38.6, 6), 'kessel'),
        Box((-4, 38.6, -4), (4, 46, 4), 'kessel'),
        Box((-4.1, 39.4, -4.1), (4.1, 40.8, 4.1), 'band_rot'),
        Box((1.6, 40.8, -4.3), (3.2, 42.4, -4), 'tanne_blatt'),
        Box((2.2, 41.2, -4.4), (2.8, 41.8, -4.2), 'beere'),
    ]
    for side in (-1, 1):
        b.append(Box((side * 6, 24, -0.5), (side * 18, 25, 0.5), 'holz', rotation=((side * 6, 24.5, 0), (0, 0, side * 28))))
        b.append(Box((side * 14, 28, -0.4), (side * 17.5, 28.8, 0.4), 'holz', rotation=((side * 14, 28.4, 0), (0, 0, side * 70))))
    b += [
        Box((9.5, 0, 3), (10.5, 30, 4), 'holz_hell', rotation=((10, 0, 3.5), (0, 0, 10))),
        Box((7.4, -0.5, 1.6), (12.6, 6, 5.4), 'stroh', rotation=((10, 3, 3.5), (0, 0, 10))),
        Box((7.2, 5, 1.4), (12.8, 6.4, 5.6), 'band_rot', rotation=((10, 3, 3.5), (0, 0, 10))),
    ]
    return b


def rod(p0, p1, thickness, mat):
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    length = math.hypot(dx, dy) + thickness
    mx, my, z = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2, p0[2]
    angle = math.degrees(math.atan2(dy, dx)) - 90
    h = thickness / 2
    return Box((mx - h, my - length / 2, z - h), (mx + h, my + length / 2, z + h), mat, rotation=((mx, my, z), (0, 0, angle)))


def reindeer():
    b = []
    fur = 'fell'
    light = 'fell_hell'
    b += [
        Box((-4.5, 14, -11), (4.5, 23, 11), fur),
        Box((-4, 13.4, -9), (4, 14, 9), light),
        Box((-1.5, 21, 10), (1.5, 24, 13), light),
        Box((-3, 19, -16), (3, 30, -10), fur, rotation=((0, 22, -12), (-30, 0, 0))),
        Box((-3.6, 27, -21), (3.6, 34, -14), fur),
        Box((-2.6, 27.4, -25), (2.6, 31.6, -20.8), light),
        Box((-1.2, 29.4, -26), (1.2, 31.4, -24.8), 'kohle'),
        Box((-3.7, 31, -20), (-2.8, 32.2, -18.6), 'kohle'),
        Box((2.8, 31, -20), (3.7, 32.2, -18.6), 'kohle'),
        Box((-6.4, 32, -16.6), (-3.6, 33.6, -15.6), fur, rotation=((-3.6, 32.8, -16), (0, 0, -25))),
        Box((3.6, 32, -16.6), (6.4, 33.6, -15.6), fur, rotation=((3.6, 32.8, -16), (0, 0, 25))),
        Box((-4.6, 25, -14.6), (4.6, 27, -11.6), 'band_rot', rotation=((0, 22, -12), (-30, 0, 0))),
        Box((-1.2, 22.4, -16.4), (1.2, 24.8, -14.2), 'messing'),
    ]
    for x in (-3, 3):
        for z in (-8, 8):
            b += [
                Box((x - 1.3, 3, z - 1.3), (x + 1.3, 14.6, z + 1.3), fur),
                Box((x - 1.4, 0, z - 1.4), (x + 1.4, 3, z + 1.4), 'kohle'),
            ]
    for side in (-1, 1):
        joints = [(side * 2.2, 33.4), (side * 4.4, 39), (side * 5.4, 44), (side * 4.6, 48.6)]
        for (x0, y0), (x1, y1) in zip(joints, joints[1:]):
            b.append(rod((x0, y0, -16.5), (x1, y1, -16.5), 1.2, 'geweih'))
        for (x0, y0), (x1, y1) in (((side * 4.4, 39), (side * 8, 42.4)), ((side * 5.4, 44), (side * 9, 46.6)), ((side * 3.3, 36.2), (side * 2.2, 40.4)), ((side * 5, 42), (side * 3.4, 45.6))):
            b.append(rod((x0, y0, -16.5), (x1, y1, -16.5), 0.9, 'geweih'))
    colours = list(BULB_SLOTS)
    for k in range(9):
        z = -9 + k * 2.2
        b += soft([Box((-0.6, 23, z - 0.6), (0.6, 24.2, z + 0.6), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[k % 6]))])
    b += [Box((-4.8, 17, -6), (4.8, 23.3, 2), 'samt'), Box((-5, 16, -6.2), (5, 17, 2.2), 'messing')]
    return b


def bench():
    b = []
    for x in (-5, 21):
        b += [
            Box((x - 1, 0, -6), (x + 1, 8, -4), 'eisen'),
            Box((x - 1, 0, 4), (x + 1, 18, 6), 'eisen'),
            Box((x - 1, 7, -6), (x + 1, 8.2, 6), 'eisen'),
            Box((x - 1, 10, -6.5), (x + 1, 11, -2), 'eisen'),
            Box((x - 1, 9, -6.5), (x + 1, 11, -5.5), 'eisen'),
        ]
    for k in range(4):
        z = -5 + k * 2.6
        b.append(Box((-7, 8.2, z), (23, 9.4, z + 2.2), 'holz'))
    for k in range(3):
        y = 11 + k * 2.4
        b.append(Box((-7, y, 4.2), (23, y + 1.8, 5.4), 'holz'))
    b += [
        snow_cap(-6.5, -5.2, 22.5, 1, 9.4, 0.8),
        snow_cap(-7, 4.1, 23, 5.5, 18.2, 0.5),
        Box((-6, 8.2, -7), (-2.8, 8.9, -2), 'schnee'),
    ]
    b += cocoa_mug(14, 10.2, -3)
    return b


def mailbox():
    b = [
        Box((-1.5, 0, -1.5), (1.5, 14, 1.5), 'eisen'),
        Box((-2.6, 0, -2.6), (2.6, 1.2, 2.6), 'eisen'),
        Box((-6, 14, -5), (6, 28, 5), 'lack_rot'),
        Box((-6.4, 27, -5.4), (6.4, 29, 5.4), 'lack_rot'),
        Box((-5, 29, -4.4), (5, 30.4, 4.4), 'lack_rot'),
        Box((-4, 25, -5.3), (4, 25.7, -5), 'kohle'),
        Box((-4.4, 24.6, -5.5), (4.4, 25, -5)
            , 'messing'),
        Box((-4.4, 25.7, -5.5), (4.4, 26.1, -5), 'messing'),
        Box((-4, 16, -5.6), (4, 22, -5), 'holz_dunkel', faces={'north': 'schild_post'}, uv={'north': (0, 0)}),
        Box((-6.2, 14, -5.3), (6.2, 14.8, 5.3), 'messing'),
        snow_cap(-5, -4.4, 5, 4.4, 30.4, 0.8),
        Box((-1.4, 30, -4.6), (1.4, 33.4, -4.2), 'zuckerguss', rotation=((0, 30, -4.4), (0, 0, 12))),
        Box((-1.2, 31, -4.8), (1, 31.4, -4.6), 'band_rot', rotation=((0, 30, -4.4), (0, 0, 12))),
    ]
    b += soft(flat_bow(0, 23, -5.2, 'band_gruen', 0.9))
    return b


def nutcracker_big():
    s = 4.2
    b = [
        Box((-7, 0, -7), (7, 3, 7), 'holz_dunkel'),
        Box((-7.3, 2.4, -7.3), (7.3, 3.2, 7.3), 'messing'),
        snow_cap(-7, -7, 7, -3, 3, 0.5),
    ]
    b += nutcracker(0, 3, 0, s)
    b += [
        Box((-1.2 * s, 3 + 9.2 * s, -1.4 * s), (1.2 * s, 3 + 9.5 * s, -1.2 * s), 'zuckerguss'),
        Box((-0.9 * s, 3 + 8.2 * s, -1.25 * s), (0.9 * s, 3 + 8.7 * s, -1.15 * s), 'kohle'),
        Box((-0.5 * s, 3 + 8.9 * s, -1.25 * s), (-0.2 * s, 3 + 9.2 * s, -1.15 * s), 'kohle'),
        Box((0.2 * s, 3 + 8.9 * s, -1.25 * s), (0.5 * s, 3 + 9.2 * s, -1.15 * s), 'kohle'),
        Box((-0.25 * s, 3 + 13 * s, -0.25 * s), (0.25 * s, 3 + 13.8 * s, 0.25 * s), 'messing'),
        Box((2.4 * s - 0.6, 3 + 3 * s, -0.6), (2.4 * s + 0.6, 3 + 9 * s, 0.6), 'messing'),
        Box((2.4 * s - 1.2, 3 + 9 * s, -1.2), (2.4 * s + 1.2, 3 + 9.8 * s, 1.2), 'messing'),
    ]
    for y in (5.2, 6.2):
        b.append(Box((-0.3, 3 + y * s, -1.15 * s - 0.2), (0.3, 3 + y * s + 0.6, -1.1 * s), 'messing'))
    return b


def schwibbogen():
    b = [Box((-26, 0, -4), (26, 3, 4), 'holz_dunkel'), Box((-26.3, 2.4, -4.3), (26.3, 3.2, 4.3), 'messing')]
    b += arc((0, 3, 0), 22, 180, 0, 16, 2.4, 3, 'holz_dunkel', 1.1)
    b += arc((0, 3, 0), 18.5, 180, 0, 14, 1.2, 2.2, 'holz', 1.1)
    for k in range(7):
        t = (k + 1) / 8
        a = math.pi * t
        x = -22 * math.cos(a)
        y = 3 + 22 * math.sin(a)
        b += soft(candle(x, y + 1.2, 0, 3.5, 'kerze', 1.4))
        b += [Box((x - 1.2, y + 0.6, -1.2), (x + 1.2, y + 1.2, 1.2), 'messing')]
    for x, h, mat in ((-14, 9, 'lack_rot'), (-8, 7, 'lack_blau'), (8, 7, 'lack_gruen'), (14, 9, 'lack_rot')):
        b += figure_big(x, 3, 0, mat, h / 7.6)
    for x in (-3, 3):
        b += [
            Box((x - 2.4, 3, -2.4), (x + 2.4, 6, 2.4), 'tanne_blatt'),
            Box((x - 1.6, 6, -1.6), (x + 1.6, 9, 1.6), 'tanne_blatt'),
            Box((x - 0.8, 9, -0.8), (x + 0.8, 11.5, 0.8), 'tanne_blatt'),
        ]
    b += [Box((-0.8, 12, -1), (0.8, 18, 1), 'holz_hell'), Box((-3, 15, -1), (3, 16.5, 1), 'holz_hell')]
    return b


def angel_statue():
    b = [
        Box((-7, 0, -7), (7, 4, 7), 'stein'),
        Box((-6, 4, -6), (6, 12, 6), 'stein'),
        Box((-7, 12, -7), (7, 14, 7), 'stein'),
        snow_cap(-7, -7, 7, 7, 14, 0.6),
        Box((-6.1, 7, -6.1), (6.1, 9, 6.1), 'messing'),
    ]
    robe = 'zuckerguss'
    b += [
        Box((-4.2, 14.6, -3.4), (4.2, 26, 3.4), robe),
        Box((-3.4, 26, -2.8), (3.4, 33, 2.8), robe),
        Box((-4.6, 14.6, -3.8), (4.6, 16, 3.8), 'messing'),
        Box((-2.2, 33, -2.2), (2.2, 37.4, 2.2), 'holz_hell'),
        Box((-2.4, 36, -2.4), (2.4, 38.2, 2.4), 'stroh'),
        Box((-3, 39.2, -0.3), (3, 39.8, 0.3), 'stern', emit=12),
        Box((-0.3, 39.2, -3), (0.3, 39.8, 3), 'stern', emit=12),
        Box((-3.4, 38.8, -3.4), (3.4, 39.2, 3.4), 'stern', emit=12),
        Box((-1.2, 29.4, -6.6), (1.2, 30.6, -3), 'messing', rotation=((0, 30, -3), (-20, 0, 0))),
        Box((-2, 28.6, -8.4), (2, 31.4, -6.6), 'messing', rotation=((0, 30, -3), (-20, 0, 0))),
        Box((-3.4, 28.6, -4.6), (-1.6, 31, -2.8), 'holz_hell'),
        Box((1.6, 28.6, -4.6), (3.4, 31, -2.8), 'holz_hell'),
    ]
    for side in (-1, 1):
        for k in range(6):
            length = 13 - k * 1.6
            y = 31 - k * 2.2
            b.append(Box((side * 1.5 if side > 0 else side * (1.5 + length), y, 3), (side * (1.5 + length) if side > 0 else side * 1.5, y + 2.4, 4.2),
                         'feder' if k % 2 == 0 else 'feder_hell', rotation=((side * 1.5, 30, 3.6), (0, side * -28, side * 18))))
    b += [
        Box((-4.4, 14.6, -3.6), (4.4, 15.4, 3.6), 'messing'),
        Box((-0.4, 26, -2.9), (0.4, 31, -2.7), 'messing'),
    ]
    return b


def gingerbread_house():
    b = [
        Box((-15, 0, -13), (15, 1, 13), 'zuckerguss'),
        Box((-13, 1, -11), (13, 18, 11), 'lebkuchen'),
    ]
    rise, run = 12.0, 13.0
    angle = math.degrees(math.atan2(rise, run))
    length = math.hypot(rise, run) + 1.5
    for side in (-1, 1):
        z0 = -13.5 if side < 0 else 13.5
        rot = ((0, 17, z0), (-angle if side < 0 else angle, 0, 0))
        if side < 0:
            b.append(Box((-15, 17, z0), (15, 19, z0 + length), 'lebkuchen', rotation=rot))
            b.append(Box((-15.2, 18.6, z0 - 0.2), (15.2, 20, z0 + 2.4), 'zuckerguss', rotation=rot))
            for k in range(1, 4):
                zz = z0 + k * length / 4
                b.append(Box((-15.1, 18.8, zz - 0.5), (15.1, 19.5, zz + 0.5), 'zuckerguss', rotation=rot))
            for k, x in enumerate(range(-12, 13, 6)):
                b.append(Box((x - 1, 19, z0 + 3.2 + (k % 2) * 4), (x + 1, 20.2, z0 + 5.2 + (k % 2) * 4), ('beere', 'band_gruen', 'senf')[k % 3], rotation=rot))
        else:
            b.append(Box((-15, 17, z0 - length), (15, 19, z0), 'lebkuchen', rotation=rot))
            b.append(Box((-15.2, 18.6, z0 - 2.4), (15.2, 20, z0 + 0.2), 'zuckerguss', rotation=rot))
            for k in range(1, 4):
                zz = z0 - k * length / 4
                b.append(Box((-15.1, 18.8, zz - 0.5), (15.1, 19.5, zz + 0.5), 'zuckerguss', rotation=rot))
    b.append(Box((-15.4, 28.6, -1.4), (15.4, 30.6, 1.4), 'zuckerguss'))
    for x in range(-14, 15, 2):
        b.append(Box((x - 0.5, 15.6 - (x % 4 == 0) * 1.2, -11.6), (x + 0.5, 17.4, -11), 'zuckerguss'))
    for k in range(6):
        y = 18 + k * 2
        inset = (k * 2 + 1) / rise * run
        for x0, x1 in ((-13, -12), (12, 13)):
            b.append(Box((x0, y, -11 + inset), (x1, y + 2, 11 - inset), 'lebkuchen'))
    b += [
        Box((-3.5, 1, -11.6), (3.5, 11, -11), 'kakao'),
        Box((-4.2, 10.4, -11.8), (4.2, 11.6, -11), 'zuckerguss'),
        Box((-0.6, 5.4, -12), (0.6, 6.6, -11.6), 'messing'),
        Box((-11, 8, -11.6), (-6, 13, -11), 'glas', emit=12, shade=False),
        Box((6, 8, -11.6), (11, 13, -11), 'glas', emit=12, shade=False),
        Box((-11.4, 7.4, -11.8), (-5.6, 8, -11), 'zuckerguss'),
        Box((5.6, 7.4, -11.8), (11.4, 8, -11), 'zuckerguss'),
        Box((-13.4, 1, -11.4), (13.4, 2, 11.4), 'zuckerguss'),
        Box((6, 24, 0), (9, 32, 3), 'lebkuchen'),
        Box((5.6, 32, -0.4), (9.4, 33, 3.4), 'zuckerguss'),
    ]
    for x in range(-12, 13, 4):
        b += [dot(x, 15, -11, ('beere', 'band_gruen', 'senf')[(x // 4) % 3], 1.4, 0.6)]
    for x in (-15.5, 15.5):
        for y in (3, 7, 11, 15):
            b.append(Box((x - 0.4, y, -11.5), (x + 0.4, y + 2.4, -10.7), 'zucker'))
    for z in (-15, 15):
        for x in (-12, -6, 0, 6, 12):
            b += [Box((x - 0.9, 1, z - 0.9), (x + 0.9, 3.4, z + 0.9), 'zucker')]
    return b


def poinsettia():
    b = [
        Box((-4.5, 0, -4.5), (4.5, 7, 4.5), 'topf_ton'),
        Box((-5, 6, -5), (5, 8, 5), 'topf_ton'),
        Box((-5.2, 2.5, -5.2), (5.2, 3.6, 5.2), 'messing'),
        Box((-4, 7, -4), (4, 8, 4), 'nougat'),
        Box((-0.6, 8, -0.6), (0.6, 13, 0.6), 'tanne_blatt'),
    ]
    for ring, (y, length, tilt) in enumerate(((11, 7.5, 20), (13, 6.5, 32), (14.6, 4.6, 45))):
        for k in range(6):
            yaw = k * 60 + ring * 30
            mat = 'tanne_blatt' if ring == 0 and k % 2 == 0 else 'lack_rot'
            b.append(Box((-1.4, y, 0), (1.4, y + 0.6, length), mat, rotation=((0, y, 0), (-tilt, yaw, 0))))
    b += [Box((-1, 14.8, -1), (1, 15.8, 1), 'senf')]
    return b


def gift_sack():
    b = []
    b += ball(0, 8, 0, 9, 'sack')
    b += ball(0, 15, 0, 7.4, 'sack')
    b += [
        Box((-4.4, 19, -4), (4.4, 22.4, 4), 'sack'),
        Box((-3, 22.4, -2.6), (3, 24, 2.6), 'sack'),
        Box((-4.8, 20.4, -4.4), (4.8, 21.8, 4.4), 'band_gold'),
        Box((-0.6, 18.4, -4.8), (0.6, 21.8, -4.4), 'band_gold', rotation=((0, 20.4, -4.4), (0, 0, 25))),
        Box((-0.6, 18.4, -4.8), (0.6, 21.8, -4.4), 'band_gold', rotation=((0, 20.4, -4.4), (0, 0, -25))),
        Box((-5.6, 0, -5.6), (5.6, 1, 5.6), 'sack'),
    ]
    for x, y, z in ((-2, 5, -8.6), (3, 11, -7.6), (-5, 13, -6), (5, 4, -8.2)):
        b += [Box((x - 1, y - 1, z - 0.3), (x + 1, y + 1, z), 'sack_flick')]
    b += gift(0.5, -0.5, 6, 6, 5, 23.6, 'papier_rot', 'band_rot', turn=15)
    b += gift(-3, 0, 5, 5, 5, 22.4, 'papier_blau', 'band_blau', turn=-25, bow=False)
    b += gift(3.6, 1, 4.6, 5, 4.6, 22.6, 'papier_gruen', 'band_gruen', turn=35, bow=False)
    b += gift(12.5, -4, 7, 6, 7, 0, 'papier_gold', 'band_gold', turn=20)
    b += gift(-12, 5, 6, 5, 6, 0, 'papier_lila', 'band_lila', turn=-15)
    return b


def advent_wreath():
    b = [
        Box((-12, 0, -12), (12, 6, 12), 'holz_dunkel'),
        Box((-12.4, 5.4, -12.4), (12.4, 6.6, 12.4), 'messing'),
        snow_cap(-12, -12, 12, -8, 6, 0.5),
    ]
    for k in range(12):
        yaw = k * 30
        b.append(Box((-2.4, 6.6, 6.6), (2.4, 10.4, 11), 'tanne', rotation=((0, 6.6, 0), (0, yaw, 0))))
    for k in range(4):
        a = math.radians(45 + k * 90)
        x, z = 8.8 * math.cos(a), 8.8 * math.sin(a)
        b += soft(candle(x, 9, z, 7 + (3 - k) * 0, 'kerze_rot', 2.4))
        b += [Box((x - 1.6, 8.6, z - 1.6), (x + 1.6, 9.4, z + 1.6), 'messing')]
    for k in range(8):
        a = math.radians(k * 45 + 22)
        x, z = 11 * math.cos(a), 11 * math.sin(a)
        b += soft(bauble(x, 9, z, list(KUGEL_SLOTS)[k % 6], 1.8))
    for k in range(4):
        b += [Box((-0.4, 7, -11.4), (0.4, 9.6, -10.6), 'band_rot', rotation=((0, 6.6, 0), (0, 45 + k * 90, 0)))]
    return b


def penguin():
    b = [
        Box((-5, 1, -4.5), (5, 15, 4.5), 'kohle'),
        Box((-3.8, 2, -4.9), (3.8, 13, -4.4), 'zuckerguss'),
        Box((-4, 15, -4), (4, 22, 4), 'kohle'),
        Box((-2.8, 15.4, -4.4), (2.8, 20, -3.9), 'zuckerguss'),
        Box((-2, 19.4, -4.6), (-0.8, 20.6, -4.2), 'kohle'),
        Box((0.8, 19.4, -4.6), (2, 20.6, -4.2), 'kohle'),
        Box((-1, 17.4, -6.4), (1, 18.6, -4.2), 'karotte'),
        Box((-3.5, 0, -6), (-0.5, 1, -1), 'karotte'),
        Box((0.5, 0, -6), (3.5, 1, -1), 'karotte'),
        Box((-6, 6, -2), (-5, 14, 2), 'kohle', rotation=((-5, 14, 0), (0, 0, -12))),
        Box((5, 6, -2), (6, 14, 2), 'kohle', rotation=((5, 14, 0), (0, 0, 12))),
        Box((-4.6, 14, -4.6), (4.6, 16, 4.6), 'schal'),
        Box((1.6, 8, -5.2), (4, 14.2, -4.4), 'schal'),
        Box((-4.2, 22, -4.2), (4.2, 23.4, 4.2), 'schal'),
        Box((-3, 23.4, -3), (3, 26, 3), 'schal'),
        Box((-1.4, 26, -1.4), (1.4, 28.4, 1.4), 'zuckerguss'),
    ]
    return b


def woodpile():
    b = [Box((-14, 0, -5), (14, 1, 5), 'holz_dunkel')]
    for row in range(5):
        y = 1 + row * 4.4
        count = 6 - row // 2
        start = -12 + (row % 2) * 2.2
        for k in range(count):
            x = start + k * 4.6
            if x > 12:
                continue
            b += [
                Box((x - 2, y, -5), (x + 2, y + 4, 5), 'holz'),
                Box((x - 1.4, y + 0.6, -5.2), (x + 1.4, y + 3.4, -5), 'holz_hell'),
                Box((x - 0.5, y + 1.5, -5.25), (x + 0.5, y + 2.5, -5.2), 'holz'),
            ]
    b += [
        snow_cap(-14, -5, 14, 5, 23, 0.8),
        Box((-14, 23, -5), (14, 24, 5), 'holz_dunkel'),
        Box((15, 0, -3), (21, 8, 3), 'holz_dunkel'),
        Box((14.8, 8, -3.2), (21.2, 8.8, 3.2), 'holz_hell'),
        Box((17.6, 8.8, -0.4), (18.4, 22, 0.4), 'holz_hell', rotation=((18, 8.8, 0), (0, 0, -15))),
        Box((15.6, 8, -0.6), (19, 11, 0.6), 'eisen', rotation=((18, 8.8, 0), (0, 0, -15))),
    ]
    return b


def fire_basket():
    b = [
        Box((-1, 0, -8), (1, 10, -6), 'eisen'),
        Box((-1, 0, 6), (1, 10, 8), 'eisen'),
        Box((-8, 0, -1), (-6, 10, 1), 'eisen'),
        Box((6, 0, -1), (8, 10, 1), 'eisen'),
        Box((-8, 10, -8), (8, 11, 8), 'eisen'),
    ]
    for y in (11, 15, 19):
        for side in (-1, 1):
            b.append(Box((-8, y, side * 8 - 0.5), (8, y + 1, side * 8 + 0.5), 'eisen'))
            b.append(Box((side * 8 - 0.5, y, -8), (side * 8 + 0.5, y + 1, 8), 'eisen'))
    for x in (-7.5, -2.5, 2.5, 7.5):
        for side in (-1, 1):
            b.append(Box((x - 0.5, 11, side * 8 - 0.5), (x + 0.5, 21, side * 8 + 0.5), 'eisen'))
            b.append(Box((side * 8 - 0.5, 11, x - 0.5), (side * 8 + 0.5, 21, x + 0.5), 'eisen'))
    b += [
        Box((-7, 11, -7), (7, 15, 7), 'glut', emit=15, shade=False),
        Box((-5, 15, -2), (5, 17, 2), 'holz_dunkel', rotation=((0, 16, 0), (0, 30, 0))),
        Box((-5, 16.4, -2), (5, 18.4, 2), 'holz_dunkel', rotation=((0, 17.4, 0), (0, -40, 0))),
    ]
    tongues = ((0, 0, 9, 2.6, 0), (-2.6, -1, 6.5, 1.8, -14), (2.4, 1.2, 7, 1.8, 16), (-1, 2.6, 5.5, 1.6, 10), (1.4, -2.6, 6, 1.6, -10), (-3.6, 2, 4, 1.4, -20), (3.4, -1.8, 4.4, 1.4, 22))
    for x, z, h, w, lean in tongues:
        b.append(Box((x - w / 2, 15, z - w / 2), (x + w / 2, 15 + h * 0.55, z + w / 2), 'flamme_tief', emit=15, shade=False, rotation=((x, 15, z), (0, 0, lean))))
        b.append(Box((x - w * 0.32, 15 + h * 0.45, z - w * 0.32), (x + w * 0.32, 15 + h, z + w * 0.32), 'flamme', emit=15, shade=False, rotation=((x, 15, z), (0, 0, lean))))
    b.append(Box((-1, 15, -1), (1, 19, 1), 'flamme_kern', emit=15, shade=False))
    return b


def pretzel(x, y, z, s=1.0):
    b = arc((x, y, z), 2.4 * s, 0, 360, 10, 0.9 * s, 0.8 * s, 'brezel', 1.05)
    b += [
        rod((x - 1.9 * s, y + 1.4 * s, z), (x + 1.2 * s, y - 2.6 * s, z), 0.8 * s, 'brezel'),
        rod((x + 1.9 * s, y + 1.4 * s, z), (x - 1.2 * s, y - 2.6 * s, z), 0.8 * s, 'brezel'),
    ]
    for dx, dy in ((-1.6, 1.8), (1.5, 1.9), (0, 2.4), (-2.3, -0.4), (2.2, -0.2)):
        b.append(Box((x + dx * s - 0.25, y + dy * s - 0.25, z - 0.6 * s), (x + dx * s + 0.25, y + dy * s + 0.25, z - 0.35 * s), 'zuckerguss'))
    return b


def pretzel_stand(x, y, z):
    b = [
        Box((x - 2.4, y, z - 2.4), (x + 2.4, y + 1, z + 2.4), 'holz_dunkel'),
        Box((x - 0.4, y + 1, z - 0.4), (x + 0.4, y + 14, z + 0.4), 'holz_hell'),
        Box((x - 3, y + 8, z - 0.3), (x + 3, y + 8.6, z + 0.3), 'holz_hell'),
    ]
    b += pretzel(x - 2.6, y + 6, z - 0.6, 0.8)
    b += pretzel(x + 2.6, y + 6, z - 0.6, 0.8)
    b += pretzel(x, y + 12, z - 0.6, 0.8)
    return b


def brezel_big():
    b = []
    for x in range(-33, 34, 6):
        b += soft([Box((x - 0.15, 35, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft(pretzel(x, 32, -24.4, 1.0))
    for x in (-26, -12):
        b += pretzel_stand(x, COUNTER_Y, -18)
    b += [Box((2, COUNTER_Y, -22), (18, COUNTER_Y + 1.2, -14), 'holz_hell')]
    for row, z in enumerate((-20, -16)):
        for k, x in enumerate(range(4, 18, 5)):
            b += [Box((x - 1.4 + row, COUNTER_Y + 1.2, z - 1.4), (x + 1.4 + row, COUNTER_Y + 2.4, z + 1.4), 'broetchen')]
    b += [Box((24, COUNTER_Y, -20), (30, COUNTER_Y + 3, -16), 'sack'), Box((24.4, COUNTER_Y + 3, -19.6), (29.6, COUNTER_Y + 3.6, -16.4), 'zuckerguss')]
    for x in range(-30, 31, 10):
        b += pretzel(x, SHELF[0] + 3, 34, 1.0)
        b += pretzel(x + 5, SHELF[1] + 3, 34, 1.0)
    return b


def rocking_horse(x, y, z, s=1.0, mat='lack_rot'):
    b = arc((x, y + 6 * s, z - 2 * s), 6 * s, 235, 305, 5, 0.8 * s, 0.8 * s, 'holz_hell', 1.1)
    b += arc((x, y + 6 * s, z + 2 * s), 6 * s, 235, 305, 5, 0.8 * s, 0.8 * s, 'holz_hell', 1.1)
    b += [Box((x - 2.6 * s, y + 0.6 * s, z - 2.4 * s), (x + 2.6 * s, y + 1.2 * s, z + 2.4 * s), 'holz_hell')]
    for lx in (-2, 2):
        for lz in (-1.3, 1.3):
            b.append(Box((x + lx * s - 0.4 * s, y + 1 * s, z + lz * s - 0.4 * s), (x + lx * s + 0.4 * s, y + 4.4 * s, z + lz * s + 0.4 * s), 'holz'))
    b += [
        Box((x - 3.2 * s, y + 4.4 * s, z - 1.4 * s), (x + 3.2 * s, y + 6.6 * s, z + 1.4 * s), mat),
        Box((x + 2.2 * s, y + 6 * s, z - 0.9 * s), (x + 3.6 * s, y + 9.4 * s, z + 0.9 * s), mat),
        Box((x + 3 * s, y + 8 * s, z - 0.8 * s), (x + 5 * s, y + 9.6 * s, z + 0.8 * s), mat),
        Box((x + 1.6 * s, y + 7 * s, z - 0.4 * s), (x + 2.4 * s, y + 10 * s, z + 0.4 * s), 'stroh'),
        Box((x - 4 * s, y + 4 * s, z - 0.3 * s), (x - 3.2 * s, y + 6.6 * s, z + 0.3 * s), 'stroh'),
        Box((x - 1 * s, y + 6.6 * s, z - 1.5 * s), (x + 1 * s, y + 7 * s, z + 1.5 * s), 'messing'),
    ]
    return b


def teddy(x, y, z, s=1.0):
    m = 'fell'
    b = ball(x, y + 2.6 * s, z, 2.6 * s, m)
    b += ball(x, y + 6.4 * s, z, 2 * s, m)
    b += [
        Box((x - 0.8 * s, y + 5.6 * s, z - 2.6 * s), (x + 0.8 * s, y + 6.6 * s, z - 1.8 * s), 'fell_hell'),
        Box((x - 0.3 * s, y + 6.2 * s, z - 2.75 * s), (x + 0.3 * s, y + 6.6 * s, z - 2.6 * s), 'kohle'),
        Box((x - 1 * s, y + 7 * s, z - 2 * s), (x - 0.5 * s, y + 7.5 * s, z - 1.8 * s), 'kohle'),
        Box((x + 0.5 * s, y + 7 * s, z - 2 * s), (x + 1 * s, y + 7.5 * s, z - 1.8 * s), 'kohle'),
        Box((x - 2.2 * s, y + 7.6 * s, z - 0.5 * s), (x - 1.2 * s, y + 8.8 * s, z + 0.5 * s), m),
        Box((x + 1.2 * s, y + 7.6 * s, z - 0.5 * s), (x + 2.2 * s, y + 8.8 * s, z + 0.5 * s), m),
        Box((x - 3.4 * s, y + 2.4 * s, z - 0.8 * s), (x - 2.2 * s, y + 4.4 * s, z + 0.8 * s), m),
        Box((x + 2.2 * s, y + 2.4 * s, z - 0.8 * s), (x + 3.4 * s, y + 4.4 * s, z + 0.8 * s), m),
        Box((x - 1.6 * s, y + 4.4 * s, z - 2.2 * s), (x + 1.6 * s, y + 5 * s, z - 1.7 * s), 'band_rot'),
    ]
    return b


def toy_train(x, y, z, s=1.0):
    b = [
        Box((x - 3 * s, y + 1 * s, z - 1.2 * s), (x + 1 * s, y + 3.6 * s, z + 1.2 * s), 'lack_rot'),
        Box((x + 1 * s, y + 1 * s, z - 1.2 * s), (x + 3 * s, y + 5 * s, z + 1.2 * s), 'lack_blau'),
        Box((x + 0.8 * s, y + 5 * s, z - 1.4 * s), (x + 3.2 * s, y + 5.6 * s, z + 1.4 * s), 'kohle'),
        Box((x - 2.4 * s, y + 3.6 * s, z - 0.5 * s), (x - 1.4 * s, y + 5.4 * s, z + 0.5 * s), 'kohle'),
        Box((x - 7 * s, y + 1 * s, z - 1.2 * s), (x - 3.6 * s, y + 3 * s, z + 1.2 * s), 'lack_gruen'),
        Box((x - 3.6 * s, y + 1.6 * s, z - 0.3 * s), (x - 3 * s, y + 2 * s, z + 0.3 * s), 'messing'),
    ]
    for wx in (-6, -4.6, -2, 0, 2):
        for wz in (-1.3, 1.3):
            b.append(Box((x + wx * s - 0.6 * s, y, z + wz * s - 0.2 * s), (x + wx * s + 0.6 * s, y + 1.2 * s, z + wz * s + 0.2 * s), 'kohle'))
    return b


def spielzeug_big():
    b = []
    b += rocking_horse(-28, COUNTER_Y, -18, 1.0)
    b += teddy(-17, COUNTER_Y, -18, 0.9)
    b += toy_train(-5, COUNTER_Y, -18, 1.0)
    b += nutcracker(8, COUNTER_Y, -18, 0.9)
    b += teddy(18, COUNTER_Y, -18, 0.8)
    b += rocking_horse(29, COUNTER_Y, -18, 0.9, 'lack_blau')
    for k, x in enumerate(range(-30, 31, 12)):
        b += teddy(x, SHELF[0], 33.5, 0.7)
        b += toy_train(x + 6, SHELF[0], 33.5, 0.7)
        b += rocking_horse(x + 2, SHELF[1], 33.5, 0.6, ('lack_rot', 'lack_gruen', 'lack_blau')[k % 3])
    for x in (-30, -18, 18, 30):
        b += soft([Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft(bauble(x, 34.6, -24.5, list(KUGEL_SLOTS)[(x // 6) % 6], 2.6))
    return b


STAR_COLOURS = ('stern_weiss', 'stern', 'stern_rot')


def advent_star(x, y, z, s, mat):
    b = [Box((x - 1.2 * s, y - 1.2 * s, z - 1.2 * s), (x + 1.2 * s, y + 1.2 * s, z + 1.2 * s), mat, emit=15, shade=False)]
    for axis in range(3):
        for sign in (-1, 1):
            a = [x - 0.7 * s, y - 0.7 * s, z - 0.7 * s]
            c = [x + 0.7 * s, y + 0.7 * s, z + 0.7 * s]
            centre = (x, y, z)[axis]
            if sign > 0:
                a[axis], c[axis] = centre + 1.2 * s, centre + 3.4 * s
            else:
                a[axis], c[axis] = centre - 3.4 * s, centre - 1.2 * s
            if axis == 1 and sign > 0:
                continue
            b.append(Box(tuple(a), tuple(c), mat, emit=15, shade=False))
    for ax, az in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        b.append(Box((x - 0.55 * s, y - 0.55 * s, z - 0.55 * s), (x + 0.55 * s, y + 0.55 * s, z + 3 * s), mat, emit=15, shade=False,
                     rotation=((x, y, z), (0, 45 * ax + (90 if az < 0 else 0), 0))))
    b.append(Box((x - 0.5 * s, y + 1.2 * s, z - 0.5 * s), (x + 0.5 * s, y + 2.2 * s, z + 0.5 * s), 'messing'))
    return b


def sterne_big():
    b = []
    for k, x in enumerate(range(-32, 33, 8)):
        drop = (2, 5, 3, 6)[k % 4]
        b += soft([Box((x - 0.12, 38 - drop, -24.6), (x + 0.12, 46, -24.4), 'messing')])
        b += soft(advent_star(x, 35 - drop, -24.5, 1.0, STAR_COLOURS[k % 3]))
    for k, x in enumerate((-30, -22, -14)):
        b += advent_star(x, COUNTER_Y + 4.4, -18, 1.0, STAR_COLOURS[k % 3])
        b += [Box((x - 0.4, COUNTER_Y, -18.4), (x + 0.4, COUNTER_Y + 1, -17.6), 'messing')]
    for k in range(6):
        x = -2 + (k % 3) * 6
        z = -21 + (k // 3) * 5
        b += [
            Box((x - 2.2, COUNTER_Y, z - 0.25), (x + 2.2, COUNTER_Y + 0.5, z + 0.25), 'stroh'),
            Box((x - 0.25, COUNTER_Y, z - 2.2), (x + 0.25, COUNTER_Y + 0.5, z + 2.2), 'stroh'),
            Box((x - 1.6, COUNTER_Y + 0.1, z - 0.2), (x + 1.6, COUNTER_Y + 0.45, z + 0.2), 'stroh', rotation=((x, COUNTER_Y, z), (0, 45, 0))),
            Box((x - 1.6, COUNTER_Y + 0.1, z - 0.2), (x + 1.6, COUNTER_Y + 0.45, z + 0.2), 'stroh', rotation=((x, COUNTER_Y, z), (0, -45, 0))),
        ]
    for k, x in enumerate((22, 30)):
        b += advent_star(x, COUNTER_Y + 4.4, -18, 1.2, STAR_COLOURS[(k + 1) % 3])
        b += [Box((x - 0.4, COUNTER_Y, -18.4), (x + 0.4, COUNTER_Y + 1, -17.6), 'messing')]
    for k, x in enumerate(range(-28, 29, 14)):
        b += advent_star(x, SHELF[0] + 3.6, 33.5, 0.9, STAR_COLOURS[k % 3])
        b += advent_star(x + 7, SHELF[1] + 3.6, 33.5, 0.9, STAR_COLOURS[(k + 1) % 3])
    return b


def honey_jar(x, y, z, s=1.0):
    return [
        Box((x - 1.6 * s, y, z - 1.6 * s), (x + 1.6 * s, y + 3.6 * s, z + 1.6 * s), 'honig', emit=4),
        Box((x - 1.3 * s, y + 3.6 * s, z - 1.3 * s), (x + 1.3 * s, y + 4.2 * s, z + 1.3 * s), 'messing'),
        Box((x - 1.9 * s, y + 4, z - 1.9 * s), (x + 1.9 * s, y + 4.5 * s, z + 1.9 * s), 'karo'),
        Box((x - 1.65 * s, y + 3, z - 1.65 * s), (x + 1.65 * s, y + 3.4 * s, z + 1.65 * s), 'band_rot'),
    ]


def honig_big():
    b = []
    for row, z in enumerate((-21, -16.5)):
        for x in range(-34, -12, 4):
            b += honey_jar(x + row * 1.5, COUNTER_Y, z, 0.9)
    b += [
        Box((-8, COUNTER_Y, -22), (6, COUNTER_Y + 4, -14), 'wabe'),
        Box((-6, COUNTER_Y + 4, -20), (4, COUNTER_Y + 6, -16), 'wabe'),
    ]
    for k, x in enumerate(range(12, 35, 4)):
        b += candle(x, COUNTER_Y, -19, 5 + (k % 3) * 2, 'bienenwachs', 1.6)
    for x in range(-32, 33, 5):
        b += honey_jar(x, SHELF[0], 33.5, 1.0)
        b += candle(x + 2, SHELF[1], 33.5, 5, 'bienenwachs', 1.4)
    for x in (-30, -18, 18, 30):
        b += soft([Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft(honey_jar(x, 31, -24.5, 0.9))
    return b


def carousel_base():
    b = []
    for k in range(8):
        b.append(Box((-9.6, 0, 0), (9.6, 3, 23), 'holz_dunkel', rotation=((0, 0, 0), (0, k * 45 + 22.5, 0))))
        b.append(Box((-9.8, 2.4, 22.4), (9.8, 3.2, 23.4), 'messing', rotation=((0, 0, 0), (0, k * 45 + 22.5, 0))))
    return b


def carousel_spin():
    b = [Box((-2, 3, -2), (2, 60, 2), 'messing')]
    for k in range(8):
        yaw = k * 45 + 22.5
        b.append(Box((-9.4, 3, 0), (9.4, 4, 22), 'holz_hell', rotation=((0, 3, 0), (0, yaw, 0))))
    roofs = ((44, 23.5, 'lack_rot'), (47, 19, 'creme'), (50, 14.5, 'lack_rot'), (53, 10, 'creme'), (56, 5.5, 'lack_rot'))
    for y, r, mat in roofs:
        for k in range(8):
            side = 2 * r * math.tan(math.radians(22.5)) + 0.2
            b.append(Box((-side / 2, y, 0), (side / 2, y + 3, r), mat, rotation=((0, y, 0), (0, k * 45 + 22.5, 0))))
    b += [Box((-2.4, 59, -2.4), (2.4, 62, 2.4), 'messing')]
    b += [Box((-1.4, 62, -0.6), (1.4, 66, 0.6), 'stern', emit=15, shade=False)]
    colours = list(BULB_SLOTS)
    for k in range(16):
        a = math.radians(k * 22.5)
        x, z = 22.6 * math.cos(a), 22.6 * math.sin(a)
        b += soft([Box((x - 0.6, 42.6, z - 0.6), (x + 0.6, 44, z + 0.6), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[k % 6]))])
    for k in range(4):
        a = math.radians(k * 90 + 45)
        x, z = 15.5 * math.cos(a), 15.5 * math.sin(a)
        b.append(Box((x - 0.4, 4, z - 0.4), (x + 0.4, 44, z + 0.4), 'messing'))
        horse = rocking_horse(0, 0, 0, 1.0, ('lack_rot', 'lack_blau', 'zuckerguss', 'lack_gruen')[k])
        horse = [h for h in horse if h.mat not in ('holz_hell',)]
        yaw = -math.degrees(a)
        for h in horse:
            h.a = (h.a[0] + x, h.a[1] + 9, h.a[2] + z)
            h.b = (h.b[0] + x, h.b[1] + 9, h.b[2] + z)
            if h.rotation:
                o, ang = h.rotation
                h.rotation = ((o[0] + x, o[1] + 9, o[2] + z), (ang[0], ang[1] + yaw, ang[2]))
            else:
                h.rotation = ((x, 9, z), (0, yaw, 0))
        b += horse
    for k in range(4):
        a = math.radians(k * 90)
        x, z = 15.5 * math.cos(a), 15.5 * math.sin(a)
        b += [
            Box((x - 3, 4, z - 2.4), (x + 3, 7, z + 2.4), 'lack_rot', rotation=((x, 4, z), (0, -k * 90, 0))),
            Box((x - 3, 7, z + 1.4), (x + 3, 11, z + 2.4), 'lack_rot', rotation=((x, 4, z), (0, -k * 90, 0))),
            Box((x - 3.2, 10.6, z + 1.2), (x + 3.2, 11.4, z + 2.6), 'messing', rotation=((x, 4, z), (0, -k * 90, 0))),
        ]
    return b


def nativity():
    b = [
        Box((-30, 0, -10), (30, 1, 14), 'stroh'),
        Box((-28, 0, 12), (-25, 40, 15), 'holz_dunkel'),
        Box((25, 0, 12), (28, 40, 15), 'holz_dunkel'),
        Box((-28, 0, -10), (-25, 34, -7), 'holz_dunkel'),
        Box((25, 0, -10), (28, 34, -7), 'holz_dunkel'),
        Box((-25, 1, 13), (25, 34, 14.5), 'holz'),
        Box((-27.5, 1, -7), (-26, 30, 12), 'holz'),
        Box((26, 1, -7), (27.5, 30, 12), 'holz'),
    ]
    angle = math.degrees(math.atan2(6, 25))
    b += [
        Box((-31, 34, -13), (31, 36, 17), 'stroh', rotation=((0, 34, -13), (-angle, 0, 0))),
        snow_cap(-30.5, -12.5, 30.5, 16.5, 36, 0.8),
    ]
    b[-1].rotation = ((0, 34, -13), (-angle, 0, 0))
    b += [
        Box((-6, 1, -2), (6, 5, 4), 'holz'),
        Box((-6.6, 4, -2.6), (6.6, 5.6, 4.6), 'holz_hell'),
        Box((-5, 5.6, -1), (5, 6.6, 3), 'stroh'),
        Box((-2.2, 6, -0.2), (2.2, 7.6, 2.2), 'zuckerguss'),
        Box((-0.8, 6.4, -0.6), (0.8, 7.8, 0.8), 'holz_hell'),
        Box((-2, 50, 0), (2, 54, 2), 'stern', emit=15, shade=False, rotation=((0, 52, 1), (0, 0, 45))),
        Box((-0.8, 47, 0.2), (0.8, 57, 1.8), 'stern', emit=15, shade=False),
        Box((-4, 51.2, 0.2), (4, 52.8, 1.8), 'stern', emit=15, shade=False),
        Box((-0.4, 40, 0.6), (0.4, 47, 1.4), 'holz_dunkel'),
    ]
    b += figure_big(-12, 1, 2, 'lack_blau', 1.9)
    b += figure_big(12, 1, 2, 'lack_braun', 2.0, staff=True)
    for sx in (-20, 19):
        b += ball(sx, 4, 6, 3, 'schneeball')
        b += [Box((sx + (2 if sx > 0 else -4.6), 4.6, 4.8), (sx + (4.6 if sx > 0 else -2), 7.2, 7.2), 'kohle')]
        for lx in (-1.6, 1.6):
            b.append(Box((sx + lx - 0.5, 0, 5.2), (sx + lx + 0.5, 2, 6.8), 'kohle'))
    return b


def bundled_tree(x, z, h, lean):
    rot = ((x, 0, z), (0, 0, lean))
    b = [Box((x - 0.6, 0, z - 0.6), (x + 0.6, 4, z + 0.6), 'holz_dunkel', rotation=rot)]
    for k, (y, r) in enumerate(((3, 3.2), (3 + h * 0.3, 2.6), (3 + h * 0.6, 1.8), (3 + h * 0.85, 1))):
        b.append(Box((x - r, y, z - r), (x + r, y + h * 0.32, z + r), 'tanne', rotation=rot))
    for y in (6, 6 + h * 0.4):
        b.append(Box((x - 2.9, y, z - 2.9), (x + 2.9, y + 0.6, z + 2.9), 'netz', rotation=rot))
    return b


def tree_sale():
    b = []
    for x in (-30, -10, 10, 30):
        b += [Box((x - 1, 0, 6), (x + 1, 20, 8), 'holz_dunkel'), snow_cap(x - 1.1, 5.9, x + 1.1, 8.1, 20, 0.5)]
    for y in (6, 15):
        b += [Box((-31, y, 6.2), (31, y + 2, 7.8), 'holz')]
        b += [snow_cap(-31, 6.1, 31, 7.9, y + 2, 0.4)]
    for k, x in enumerate(range(-27, 28, 6)):
        b += bundled_tree(x, 4.2, 22 + (k % 3) * 3, (-6, 4, -3, 7, -5, 2, 6, -4, 3, -6)[k % 10])
    b += [
        Box((-6, 0, -9), (6, 2, -3), 'holz'),
        Box((-6, 2, -9), (6, 3, -8), 'holz'),
        Box((-6, 2, -4), (6, 3, -3), 'holz'),
        Box((-7, 0.6, -6.6), (7, 1.4, -5.4), 'eisen'),
        Box((6, 2, -6.6), (16, 3, -5.4), 'holz_hell', rotation=((6, 2.5, -6), (0, 0, 15))),
    ]
    b += wheel(-7.6, 3, -6, 3, 'holz_dunkel')
    b += wheel(7.6, 3, -6, 3, 'holz_dunkel')
    b += bundled_tree(-2, -6, 18, 80)
    for part in b[-7:]:
        o, ang = part.rotation
        part.rotation = ((-2, 3, -6), (0, 0, 80))
    b += [
        Box((18, 0, -6), (19, 22, -5), 'holz_dunkel'),
        Box((12, 16, -6.6), (25, 22, -5.4), 'holz_dunkel', faces={'north': 'schild_tannen'}, uv={'north': (0, 0)}),
        snow_cap(11.8, -6.8, 25.2, -5.2, 22, 0.5),
    ]
    return b


def frozen_fountain():
    b = []
    for k in range(8):
        yaw = k * 45 + 22.5
        b.append(Box((-9.6, 0, 19), (9.6, 7, 23.4), 'stein', rotation=((0, 0, 0), (0, yaw, 0))))
        b.append(Box((-10, 7, 18.6), (10, 8.4, 24), 'stein', rotation=((0, 0, 0), (0, yaw, 0))))
        b.append(Box((-9.8, 8.4, 19.4), (9.8, 9.2, 23.6), 'schnee', rotation=((0, 0, 0), (0, yaw, 0))))
        for j in (-6, 0, 6):
            b += soft([Box((j - 0.4, 3.6, 23.4), (j + 0.4, 7, 24), 'eiszapfen', rotation=((0, 0, 0), (0, yaw, 0)))])
    for k in range(8):
        b.append(Box((-8, 0, 0), (8, 5.2, 19.2), 'eis_flaeche', rotation=((0, 0, 0), (0, k * 45 + 22.5, 0))))
    b += [
        Box((-3, 5, -3), (3, 22, 3), 'stein'),
        Box((-8, 22, -8), (8, 24, 8), 'stein'),
        Box((-7.6, 24, -7.6), (7.6, 24.8, 7.6), 'schnee'),
        Box((-2, 24, -2), (2, 32, 2), 'stein'),
        Box((-3.4, 32, -3.4), (3.4, 34, 3.4), 'stein'),
        Box((-3, 34, -3), (3, 35, 3), 'schnee'),
        Box((-2.2, 5.2, -2.2), (2.2, 5.4, 2.2), 'eis_flaeche'),
    ]
    for k in range(12):
        a = math.radians(k * 30)
        x, z = 7.6 * math.cos(a), 7.6 * math.sin(a)
        length = (3, 5, 2.5, 6, 4)[k % 5]
        b += soft([
            Box((x - 0.6, 22 - length * 0.5, z - 0.6), (x + 0.6, 22, z + 0.6), 'eiszapfen'),
            Box((x - 0.3, 22 - length, z - 0.3), (x + 0.3, 22 - length * 0.5, z + 0.3), 'eiszapfen'),
        ])
    return b


def birdhouse():
    b = [
        Box((-1, 0, -1), (1, 22, 1), 'holz_dunkel'),
        Box((-2.4, 0, -2.4), (2.4, 1, 2.4), 'schnee'),
        Box((-4, 22, -4), (4, 30, 4), 'holz'),
        Box((-1.4, 25, -4.2), (1.4, 27.8, -4), 'kohle'),
        Box((-0.4, 23.4, -5.6), (0.4, 24, -4), 'holz_hell'),
        Box((-5, 21.4, -5), (5, 22.4, 5), 'holz_dunkel'),
    ]
    angle = 40
    b += [
        Box((-5.6, 30, -5.4), (0.4, 31, 5.4), 'lack_rot', rotation=((0, 30, 0), (0, 0, angle))),
        Box((-0.4, 30, -5.4), (5.6, 31, 5.4), 'lack_rot', rotation=((0, 30, 0), (0, 0, -angle))),
        Box((-5.4, 31, -5.2), (0.2, 31.8, 5.2), 'schnee', rotation=((0, 30, 0), (0, 0, angle))),
        Box((-0.2, 31, -5.2), (5.4, 31.8, 5.2), 'schnee', rotation=((0, 30, 0), (0, 0, -angle))),
    ]
    b += [
        Box((-1.6, 22.4, -7.4), (1.6, 25, -5.2), 'lack_rot'),
        Box((-1.4, 25, -7.2), (1.4, 27, -5.6), 'lack_rot'),
        Box((-1.6, 25.2, -8.4), (-0.4, 25.8, -7.2), 'karotte'),
        Box((-0.6, 26, -7.3), (0, 26.6, -7.15), 'kohle'),
        Box((-1, 22.4, -4.6), (1, 23, -2), 'holz'),
    ]
    for x, z in ((-2.4, 3), (2, 2.4), (0.6, 3.6), (-1, 2)):
        b.append(Box((x - 0.4, 22.4, z - 0.4), (x + 0.4, 23, z + 0.4), 'mandel'))
    return b


def snow_pile():
    b = []
    b += ball(-3, 4, 0, 7, 'schneeball')
    b += ball(5, 3, 2, 5, 'schneeball')
    b += ball(0, 8, 1, 4.4, 'schneeball')
    b += [Box((-11, 0, -7), (11, 1.2, 9), 'schneeball')]
    shovel = ((7, 4, -2), (0, 0, -20))
    b += [
        Box((6.4, 5, -2.6), (7.6, 22, -1.4), 'holz_hell', rotation=shovel),
        Box((5.2, 21.6, -2.8), (8.8, 22.6, -1.2), 'kohle', rotation=shovel),
        Box((4, 0.6, -5), (10, 6, -4.4), 'lack_rot', rotation=shovel),
        Box((4.2, 0.6, -5.4), (9.8, 1.2, -1), 'lack_rot', rotation=shovel),
    ]
    return b


def apple(x, y, z, s=1.0):
    return [
        Box((x - 1.3 * s, y, z - 1.3 * s), (x + 1.3 * s, y + 2.4 * s, z + 1.3 * s), 'apfel'),
        Box((x - 0.9 * s, y + 2.4 * s, z - 0.9 * s), (x + 0.9 * s, y + 2.8 * s, z + 0.9 * s), 'zuckerguss'),
        Box((x - 0.15, y + 2.8 * s, z - 0.15), (x + 0.15, y + 3.6 * s, z + 0.15), 'holz_dunkel'),
    ]


def bratapfel_big():
    b = [
        Box((-30, 17, -22), (-6, 18.2, -14), 'kessel'),
        Box((-30.4, 18.2, -22.4), (-5.6, 18.8, -13.6), 'eisen'),
    ]
    for row, z in enumerate((-20.4, -16)):
        for x in range(-28, -7, 4):
            b += apple(x + row * 2, 18.8, z, 1.0)
    for x in (2, 7, 12):
        b += [Box((x - 1.6, COUNTER_Y, -20), (x + 1.6, COUNTER_Y + 2.4, -16.8), 'becher_weiss'), Box((x - 1.3, COUNTER_Y + 2, -19.7), (x + 1.3, COUNTER_Y + 2.4, -17.1), 'vanille')]
    b += [Box((20, 17, -23), (34, 26, -14), 'kessel'), Box((22, 18, -23.4), (32, 24, -23), 'glut', emit=13), Box((19.6, 26, -23.4), (34.4, 27.2, -13.6), 'eisen')]
    for x in range(-32, 33, 6):
        b += apple(x, SHELF[0], 33.5, 1.0)
        b += apple(x + 3, SHELF[1], 33.5, 1.0)
    for x in (-30, -18, 18, 30):
        b += soft([Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft(apple(x, 32.4, -24.5, 1.2))
    return b


def raclette_big():
    b = []
    for x in (-24, -8):
        b += [
            Box((x - 6, COUNTER_Y, -22), (x + 6, COUNTER_Y + 1.4, -14), 'eisen'),
            Box((x - 1, COUNTER_Y + 1.4, -18.8), (x + 1, COUNTER_Y + 10, -17.2), 'eisen'),
            Box((x - 6, COUNTER_Y + 9, -22.6), (x + 6, COUNTER_Y + 11, -13.4), 'kessel'),
            Box((x - 5.4, COUNTER_Y + 8.4, -22), (x + 5.4, COUNTER_Y + 9, -14), 'glut', emit=13),
            Box((x - 5, COUNTER_Y + 1.4, -21.4), (x + 4, COUNTER_Y + 6.6, -14.6), 'kaese'),
            Box((x - 5, COUNTER_Y + 6.6, -21.4), (x + 4, COUNTER_Y + 7.4, -14.6), 'kaese_rinde'),
            Box((x + 4, COUNTER_Y + 2.6, -21), (x + 5.6, COUNTER_Y + 6.4, -15), 'kaese_flies'),
        ]
    for x in (6, 14):
        b += [Box((x - 3, COUNTER_Y, -21), (x + 3, COUNTER_Y + 0.6, -15), 'becher_weiss')]
        for dx, dz in ((-1.4, -1.4), (1.2, -0.6), (-0.4, 1.4)):
            b += [Box((x + dx - 1, COUNTER_Y + 0.6, -18 + dz - 0.8), (x + dx + 1, COUNTER_Y + 2, -18 + dz + 0.8), 'kartoffel')]
        b += [Box((x - 2.4, COUNTER_Y + 2, -19.4), (x + 2.4, COUNTER_Y + 2.5, -16.6), 'kaese')]
    for x in (24, 30):
        b += [Box((x - 2.4, COUNTER_Y, -21), (x + 2.4, COUNTER_Y + 4.8, -15.4), 'kaese'), Box((x - 2.5, COUNTER_Y + 4.8, -21.1), (x + 2.5, COUNTER_Y + 5.2, -15.3), 'kaese_rinde')]
    for x in range(-30, 31, 10):
        b += [Box((x - 3.4, SHELF[0], 31), (x + 3.4, SHELF[0] + 4, 36), 'kaese'), Box((x - 3.5, SHELF[0] + 4, 30.9), (x + 3.5, SHELF[0] + 4.4, 36.1), 'kaese_rinde')]
        b += [Box((x + 1.6, SHELF[1], 31.6), (x + 6.6, SHELF[1] + 3.6, 35.4), 'kaese'), Box((x + 1.5, SHELF[1] + 3.6, 31.5), (x + 6.7, SHELF[1] + 4, 35.5), 'kaese_rinde')]
    return b


def beanie(x, y, z, colour, s=1.0):
    return [
        Box((x - 2 * s, y, z - 2 * s), (x + 2 * s, y + 1.4 * s, z + 2 * s), 'schal'),
        Box((x - 1.8 * s, y + 1.4 * s, z - 1.8 * s), (x + 1.8 * s, y + 4 * s, z + 1.8 * s), colour),
        Box((x - 0.9 * s, y + 4 * s, z - 0.9 * s), (x + 0.9 * s, y + 5.4 * s, z + 0.9 * s), 'zuckerguss'),
    ]


def scarf(x, y, z, length, colour):
    return [
        Box((x - 1.4, y - length, z - 0.3), (x + 1.4, y, z + 0.3), colour),
        Box((x - 1.4, y - length - 1.2, z - 0.3), (x + 1.4, y - length, z + 0.3), 'zuckerguss'),
    ]


def strick_big():
    b = []
    colours = ('schal', 'strick_gruen', 'strick_blau')
    for k, x in enumerate(range(-34, 35, 5)):
        b += soft([Box((x - 0.15, 42, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft(scarf(x, 42, -24.4, 8 + (k % 3) * 2, colours[k % 3]))
    for k, x in enumerate(range(-32, -6, 5)):
        b += beanie(x, COUNTER_Y, -18, colours[k % 3], 1.0)
    b += [Box((-2, COUNTER_Y, -22), (14, COUNTER_Y + 2.4, -14), 'schal')]
    b += [Box((-1.6, COUNTER_Y + 2.4, -21.6), (13.6, COUNTER_Y + 4.4, -14.4), 'strick_gruen')]
    b += [Box((-1.2, COUNTER_Y + 4.4, -21.2), (13.2, COUNTER_Y + 6, -14.8), 'strick_blau')]
    for k, x in enumerate((21, 27, 33)):
        for side in (-1, 1):
            b += [
                Box((x + side * 1.4 - 1.1, COUNTER_Y, -19.4), (x + side * 1.4 + 1.1, COUNTER_Y + 3.6, -17.6), colours[k % 3]),
                Box((x + side * 1.4 + side * 1.1 - 0.4 + (0.4 if side < 0 else 0), COUNTER_Y + 1.4, -19), (x + side * 1.4 + side * 1.1 + 0.4 + (0.4 if side < 0 else 0), COUNTER_Y + 2.8, -18), colours[k % 3]),
            ]
    for k, x in enumerate(range(-30, 31, 8)):
        b += beanie(x, SHELF[0], 33.5, colours[k % 3], 1.0)
        b += beanie(x + 4, SHELF[1], 33.5, colours[(k + 1) % 3], 1.0)
    return b


def fruit_skewer(x, y, z, k):
    b = [Box((x - 0.15, y, z - 0.15), (x + 0.15, y + 10, z + 0.15), 'holz_hell')]
    fruits = (('erdbeere', 1.2), ('banane', 1.0), ('erdbeere', 1.2), ('ananas', 1.1))
    yy = y + 3
    for j in range(3):
        mat, r = fruits[(k + j) % 4]
        dipped = j < 2
        b.append(Box((x - r, yy, z - r), (x + r, yy + 2 * r, z + r), 'schoko' if dipped else mat))
        if dipped:
            b.append(Box((x - r * 0.8, yy + 2 * r, z - r * 0.8), (x + r * 0.8, yy + 2 * r + 0.6, z + r * 0.8), mat))
        yy += 2 * r + 0.8
    return b


def schoko_big():
    b = []
    for x in (-26, -10):
        b += [Box((x - 6, COUNTER_Y, -22), (x + 6, COUNTER_Y + 3, -14), 'zuckerguss')]
        for dx in (-4, -1.5, 1, 3.5):
            for dz in (-20, -16):
                b += fruit_skewer(x + dx, COUNTER_Y + 3, dz, int(dx * 3 + dz))
    b += [Box((4, COUNTER_Y, -22), (14, COUNTER_Y + 5, -14), 'kessel'), Box((4.6, COUNTER_Y + 5, -21.4), (13.4, COUNTER_Y + 5.4, -14.6), 'schoko')]
    for k, x in enumerate((22, 28, 34)):
        b += [Box((x - 2.2, COUNTER_Y, -21), (x + 2.2, COUNTER_Y + 4, -15), 'erdbeere' if k != 1 else 'banane')]
    for x in range(-30, 31, 10):
        b += [Box((x - 3, SHELF[0], 31.6), (x + 3, SHELF[0] + 2, 35.4), 'zuckerguss')]
        for dx in (-1.6, 1.6):
            b += fruit_skewer(x + dx, SHELF[0] + 2, 33.5, int(x + dx))
    for x in (-30, -18, 18, 30):
        b += soft([Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft([Box((x - 1.6, 32, -25.6), (x + 1.6, 36, -23.4), 'erdbeere'), Box((x - 1.3, 30.4, -25.3), (x + 1.3, 32, -23.7), 'schoko')])
    return b


def star_gate():
    b = []
    for side in (-1, 1):
        x = side * 33
        b += [
            Box((x - 6, 0, -6), (x + 6, 3, 6), 'stein'),
            snow_cap(x - 6, -6, x + 6, 6, 3, 0.6),
            Box((x - 4.4, 3, -4.4), (x + 4.4, 48, 4.4), 'tanne'),
        ]
        for k in range(6):
            y = 6 + k * 7
            b += soft([Box((x - 4.8, y, -4.8), (x + 4.8, y + 1.4, 4.8), 'band_rot')])
            b += soft([Box((x - 0.6, y + 3.6, -5.4), (x + 0.6, y + 4.8, -4.8), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, list(BULB_SLOTS)[k % 6]))])
            b += soft([Box((x - 0.6, y + 3.6, 4.8), (x + 0.6, y + 4.8, 5.4), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, list(BULB_SLOTS)[(k + 3) % 6]))])
    b += arc((0, 48, 0), 33, 180, 0, 22, 8.8, 8.8, 'tanne', 1.08)
    colours = list(BULB_SLOTS)
    for k in range(1, 24):
        a = math.pi * k / 24
        x = -33 * math.cos(a)
        y = 48 + 33 * math.sin(a)
        nx, ny = -math.cos(a), math.sin(a)
        b += soft([Box((x + nx * 4.4 - 0.6, y + ny * 4.4 - 0.6, -5.4), (x + nx * 4.4 + 0.6, y + ny * 4.4 + 0.6, -4.4), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[k % 6]))])
        b += soft([Box((x - nx * 4.4 - 0.6, y - ny * 4.4 - 0.6, -5.4), (x - nx * 4.4 + 0.6, y - ny * 4.4 + 0.6, -4.4), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[(k + 2) % 6]))])
    b += soft([
        Box((-4, 84, -1.2), (4, 96, 1.2), 'stern', emit=15, shade=False, rotation=((0, 90, 0), (0, 0, 45))),
        Box((-1.6, 82, -1.2), (1.6, 98, 1.2), 'stern', emit=15, shade=False),
        Box((-8, 88.4, -1.2), (8, 91.6, 1.2), 'stern', emit=15, shade=False),
        Box((-2.6, 87.4, -1.6), (2.6, 92.6, 1.6), 'stern_weiss', emit=15, shade=False),
    ])
    b += soft([
        Box((-23, 44, -1.5), (23, 52, 0), 'holz_dunkel', faces={'north': 'schild_frohe', 'south': 'schild_frohe'}, uv={'north': (0, 0), 'south': (0, 0)}),
        Box((-23.6, 43.4, -1.8), (23.6, 44.2, 0.3), 'messing'),
        Box((-23.6, 51.8, -1.8), (23.6, 52.6, 0.3), 'messing'),
        Box((-23.6, 52.6, -1.9), (23.6, 53.2, 0.4), 'schnee', skip=('down',)),
        Box((-14.2, 52.6, -0.4), (-13.8, 66, 0), 'eisen'),
        Box((13.8, 52.6, -0.4), (14.2, 66, 0), 'eisen'),
    ])
    return b


def market_fence():
    b = []
    for x in (-15, 0, 15):
        b += [Box((x - 1.4, 0, -1.4), (x + 1.4, 16, 1.4), 'holz_dunkel'), Box((x - 1.8, 16, -1.8), (x + 1.8, 17, 1.8), 'holz_dunkel'), snow_cap(x - 1.8, -1.8, x + 1.8, 1.8, 17, 0.5)]
    for y in (4, 11):
        b += [Box((-15, y, -0.8), (15, y + 2, 0.8), 'holz'), snow_cap(-15, -0.8, 15, 0.8, y + 2, 0.3)]
    for k in range(12):
        x = -14 + k * 2.55
        sag = 2.2 * math.sin(math.pi * ((x + 15) % 15) / 15)
        b += soft([Box((x - 1.4, 13 - sag, -1.6), (x + 1.4, 15 - sag, 1.6), 'tanne')])
    for x in (-7.5, 7.5):
        b += soft(flat_bow(x, 13.6, -1.6, 'band_rot', 0.9))
    colours = list(BULB_SLOTS)
    for k, x in enumerate(range(-13, 14, 3)):
        sag = 2.2 * math.sin(math.pi * ((x + 15) % 15) / 15)
        b += soft([Box((x - 0.5, 12.2 - sag, -2), (x + 0.5, 13.2 - sag, -1.4), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[k % 6]))])
    return b


def double_lamp():
    b = [b_ for b_ in lantern_mast() if b_.mat != 'tanne' and not (b_.mat == 'band_gruen')]
    b = [x for x in b if not (x.a[1] > 38 and x.a[1] < 46 and abs(x.a[0]) > 5)]
    b += [
        Box((-14, 44, -1), (14, 46, 1), 'eisen'),
        Box((-14.6, 43.4, -1.4), (-13.4, 46.6, 1.4), 'messing'),
        Box((13.4, 43.4, -1.4), (14.6, 46.6, 1.4), 'messing'),
        Box((-1.8, 46, -1.8), (1.8, 50, 1.8), 'eisen'),
        Box((-1, 50, -1), (1, 53, 1), 'messing'),
    ]
    for x in (-11, 11):
        b += [
            Box((x - 0.3, 38, -0.3), (x + 0.3, 44, 0.3), 'eisen'),
            Box((x - 3, 37, -3), (x + 3, 38, 3), 'eisen'),
            Box((x - 2.4, 30.6, -2.4), (x + 2.4, 37, 2.4), 'glas', emit=15, shade=False),
            Box((x - 3, 29.6, -3), (x + 3, 30.6, 3), 'eisen'),
            Box((x - 1, 28, -1), (x + 1, 29.6, 1), 'messing'),
            snow_cap(x - 3, -3, x + 3, 3, 38, 0.4),
        ]
        for sx in (-1, 1):
            for sz in (-1, 1):
                b.append(Box((x + sx * 2.7 - 0.35, 30.6, sz * 2.7 - 0.35), (x + sx * 2.7 + 0.35, 37, sz * 2.7 + 0.35), 'eisen'))
    b += ring((0, 30, -2.6), 4.4, 2.4, 1.8, 12, 'tanne')
    b += flat_bow(0, 34.4, -3.5, 'band_rot')
    return b


def seating():
    b = [
        Box((-14, 0, -4), (14, 1, 4), 'holz_dunkel'),
        Box((-14, 12, -6), (14, 13.6, 6), 'holz_hell'),
        Box((-11, 1, -1), (-9, 12, 1), 'holz_dunkel'),
        Box((9, 1, -1), (11, 12, 1), 'holz_dunkel'),
        snow_cap(-13, 2, 13, 5.6, 13.6, 0.4),
    ]
    for z in (-12, 12):
        b += [
            Box((-14, 7, z - 2.4), (14, 8.4, z + 2.4), 'holz_hell'),
            Box((-11, 0, z - 1), (-9.6, 7, z + 1), 'holz_dunkel'),
            Box((9.6, 0, z - 1), (11, 7, z + 1), 'holz_dunkel'),
            snow_cap(-13.6, z - 2, -6, z + 2, 8.4, 0.4),
        ]
    b += mug(-6, 13.6, -3)
    b += mug(4, 13.6, 1)
    b += candle(0, 13.6, -2, 2.6, 'kerze_rot', 1.0)
    b += [Box((-0.8, 13.6, -0.8), (0.8, 46, 0.8), 'holz_dunkel')]
    for k in range(8):
        yaw = k * 45 + 22.5
        b.append(Box((-6.6, 44, 0), (6.6, 45.6, 16), 'lack_rot' if k % 2 == 0 else 'creme', rotation=((0, 46, 0), (14, yaw, 0))))
    b += [Box((-1.6, 46, -1.6), (1.6, 48, 1.6), 'lack_rot')]
    return b


def parcel(x, y, z, w, h, d, turn=0):
    rot = ((x, y, z), (0, turn, 0)) if turn else None
    return [
        Box((x - w / 2, y, z - d / 2), (x + w / 2, y + h, z + d / 2), 'paket', rotation=rot),
        Box((x - w / 2 - 0.1, y, z - 0.3), (x + w / 2 + 0.1, y + h + 0.1, z + 0.3), 'kordel', rotation=rot, skip=('down',)),
        Box((x - 0.3, y, z - d / 2 - 0.1), (x + 0.3, y + h + 0.15, z + d / 2 + 0.1), 'kordel', rotation=rot, skip=('down',)),
    ]


def letter(x, y, z, turn=0):
    rot = ((x, y, z), (0, turn, 0))
    return [
        Box((x - 1.6, y, z - 1.1), (x + 1.6, y + 0.3, z + 1.1), 'brief', rotation=rot),
        Box((x - 0.4, y + 0.3, z - 0.3), (x + 0.4, y + 0.45, z + 0.3), 'beere', rotation=rot),
    ]


def post_big():
    b = []
    for k, (x, w, h) in enumerate(((-32, 6, 5), (-25, 5, 4), (-19, 6, 6), (-12, 4, 3))):
        b += parcel(x, COUNTER_Y, -18, w, h, 5, (k - 1.5) * 8)
    b += parcel(-25, COUNTER_Y + 4, -18, 3.4, 2.6, 3, 15)
    for k in range(9):
        b += letter(-4 + (k % 3) * 4, COUNTER_Y + (k // 3) * 0.32, -19 + (k % 2) * 2.4, (k * 23) % 50 - 25)
    b += [Box((10, COUNTER_Y, -21), (16, COUNTER_Y + 3, -15), 'holz_dunkel'), Box((10.4, COUNTER_Y + 3, -20.6), (15.6, COUNTER_Y + 3.4, -15.4), 'brief')]
    b += [Box((22, COUNTER_Y, -20), (24, COUNTER_Y + 4, -18), 'holz_hell'), Box((21.4, COUNTER_Y + 4, -20.6), (24.6, COUNTER_Y + 5, -17.4), 'lack_rot')]
    b += [Box((28, COUNTER_Y, -21), (34, COUNTER_Y + 0.6, -15), 'lack_rot'), Box((28.6, COUNTER_Y + 0.6, -20.4), (33.4, COUNTER_Y + 1, -15.6), 'kohle')]
    for x in range(-33, 34, 6):
        b += parcel(x, SHELF[0], 33.5, 4.4, 3.6, 4, (x % 3) * 6)
        for j in range(3):
            b += letter(x + 1, SHELF[1] + j * 0.32, 33.5, j * 12 - 12)
    for x in (-30, -18, 18, 30):
        b += soft([Box((x - 0.15, 36, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft([Box((x - 2.2, 32.6, -25), (x + 2.2, 36, -24), 'brief'), Box((x - 0.5, 34, -25.1), (x + 0.5, 35, -25), 'beere')])
    return b


def puffer_big():
    b = []
    for x in (-24, -8):
        b += [
            Box((x - 6.5, COUNTER_Y, -22.5), (x + 6.5, COUNTER_Y + 2, -13.5), 'kessel'),
            Box((x - 6, COUNTER_Y + 2, -22), (x + 6, COUNTER_Y + 2.4, -14), 'eisen'),
            Box((x - 6.6, COUNTER_Y + 0.4, -23), (x + 6.6, COUNTER_Y + 1.2, -22.5), 'glut', emit=12),
            Box((x + 6.5, COUNTER_Y + 1.4, -18.6), (x + 12, COUNTER_Y + 2.2, -17.4), 'holz_dunkel'),
        ]
        for dx, dz in ((-3.4, -20), (0, -20), (3.4, -20), (-1.7, -16.4), (1.7, -16.4)):
            b += [Box((x + dx - 1.5, COUNTER_Y + 2.4, dz - 1.5), (x + dx + 1.5, COUNTER_Y + 3, dz + 1.5), 'puffer')]
    b += [Box((6, COUNTER_Y, -22), (18, COUNTER_Y + 1, -14), 'holz_hell')]
    for k in range(4):
        b += [Box((8 + k * 2.5, COUNTER_Y + 1 + k * 0.6, -20), (11 + k * 2.5, COUNTER_Y + 1.6 + k * 0.6, -16), 'puffer')]
    for x in (24, 30):
        b += [Box((x - 2, COUNTER_Y, -20), (x + 2, COUNTER_Y + 4.4, -16), 'glas_hell'), Box((x - 1.7, COUNTER_Y + 0.2, -19.7), (x + 1.7, COUNTER_Y + 3.6, -16.3), 'apfelmus'), Box((x - 2.2, COUNTER_Y + 4.4, -20.2), (x + 2.2, COUNTER_Y + 5, -15.8), 'karo')]
    for x in range(-32, 33, 6):
        b += [Box((x - 2, SHELF[0], 31.5), (x + 2, SHELF[0] + 4.4, 35.5), 'glas_hell'), Box((x - 1.7, SHELF[0] + 0.2, 31.8), (x + 1.7, SHELF[0] + 3.6, 35.2), 'apfelmus'), Box((x - 2.2, SHELF[0] + 4.4, 31.3), (x + 2.2, SHELF[0] + 5, 35.7), 'karo')]
        b += [Box((x - 2.4, SHELF[1], 31.5), (x + 2.4, SHELF[1] + 2, 35.5), 'sack')]
    return b


def workbench_big():
    b = [
        Box((-34, COUNTER_Y, -22), (-6, COUNTER_Y + 1.6, -13), 'holz_hell'),
        Box((-30, COUNTER_Y + 1.6, -21), (-26, COUNTER_Y + 6, -17), 'holz'),
        Box((-30.4, COUNTER_Y + 6, -21.4), (-25.6, COUNTER_Y + 6.6, -16.6), 'eisen'),
        Box((-22, COUNTER_Y + 1.6, -20), (-12, COUNTER_Y + 2.2, -18.8), 'holz_hell', rotation=((-17, COUNTER_Y + 1.9, -19.4), (0, 20, 0))),
        Box((-19, COUNTER_Y + 1.6, -17), (-18, COUNTER_Y + 2.4, -14), 'holz_dunkel'),
        Box((-19.6, COUNTER_Y + 2.4, -17.6), (-17.4, COUNTER_Y + 4, -16.2), 'eisen'),
        Box((-10, COUNTER_Y + 1.6, -20), (-8, COUNTER_Y + 2, -15), 'stroh'),
    ]
    b += rocking_horse(4, COUNTER_Y, -18, 0.9, 'zuckerguss')
    b += teddy(14, COUNTER_Y, -18, 0.75)
    b += toy_train(27, COUNTER_Y, -18, 0.9)
    for k, x in enumerate(range(-31, 32, 9)):
        b += nutcracker(x, SHELF[0], 33.5, 0.6)
        b += rocking_horse(x + 4, SHELF[1], 33.5, 0.5, ('lack_rot', 'lack_blau', 'lack_gruen')[k % 3])
    for x in (-32, -20, 20, 32):
        b += soft([Box((x - 0.15, 38, -24.6), (x + 0.15, 46, -24.3), 'messing')])
        b += soft([Box((x - 0.6, 33, -25), (x + 0.6, 38, -24.2), 'eisen'), Box((x - 1.8, 32, -25.2), (x + 1.8, 33, -24), 'holz_dunkel')])
    return b


def snow_globe():
    b = [
        Box((-14, 0, -14), (14, 6, 14), 'holz_dunkel'),
        Box((-14.4, 5, -14.4), (14.4, 6.6, 14.4), 'messing'),
        Box((-12, 6.6, -12), (12, 8, 12), 'holz_dunkel'),
        Box((-14.4, 0.6, -14.4), (14.4, 1.4, 14.4), 'messing'),
        Box((-11, 8, -11), (11, 9.4, 11), 'schnee'),
    ]
    b += [
        Box((-5, 9.4, -3), (3, 15, 3), 'lebkuchen'),
        Box((-5.6, 15, -3.4), (3.6, 16, 3.4), 'zuckerguss'),
        Box((-4.6, 16, -3), (3.2, 18, 3), 'lack_rot'),
        Box((-1.4, 9.4, -3.2), (0.6, 12.6, -3), 'kakao'),
        Box((-4.4, 11.6, -3.2), (-2.6, 13.4, -3), 'glas', emit=12, shade=False),
        Box((1, 11.6, -3.2), (2.6, 13.4, -3), 'glas', emit=12, shade=False),
        Box((6, 9.4, 1), (7, 12, 2), 'holz_dunkel'),
        Box((4.4, 12, -0.6), (8.6, 15, 3.6), 'tanne'),
        Box((5.2, 15, 0.2), (7.8, 18, 2.8), 'tanne'),
        Box((5.9, 18, 0.9), (7.1, 20, 2.1), 'tanne'),
        Box((6.1, 20, 1.1), (6.9, 20.8, 1.9), 'stern', emit=15, shade=False),
    ]
    for k, (x, y, z) in enumerate(((-7, 22, -4), (3, 26, 6), (-2, 30, 2), (8, 19, -6), (-8, 15, 6), (4, 33, -3), (0, 24, -8), (-5, 28, 7))):
        b += soft([Box((x - 0.5, y - 0.5, z - 0.5), (x + 0.5, y + 0.5, z + 0.5), 'zuckerguss')])
    radius = 12.5
    centre_y = 20
    layers = 12
    for k in range(layers + 1):
        a0 = -math.pi / 2 + math.pi * k / (layers + 1)
        a1 = -math.pi / 2 + math.pi * (k + 1) / (layers + 1)
        y0 = centre_y + radius * math.sin(a0)
        y1 = centre_y + radius * math.sin(a1)
        if y1 <= 8:
            continue
        y0 = max(y0, 8)
        r = radius * max(math.cos(a0), math.cos(a1))
        t = 0.6
        b += soft([
            Box((-r, y0, -r), (r, y1, -r + t), 'glaskugel', shade=False),
            Box((-r, y0, r - t), (r, y1, r), 'glaskugel', shade=False),
            Box((-r, y0, -r + t), (-r + t, y1, r - t), 'glaskugel', shade=False),
            Box((r - t, y0, -r + t), (r, y1, r - t), 'glaskugel', shade=False),
        ])
    b += soft([Box((-2.5, centre_y + radius - 0.6, -2.5), (2.5, centre_y + radius, 2.5), 'glaskugel', shade=False)])
    return b


def ice_rink():
    b = [Box((-38, 0, -22), (38, 0.8, 22), 'eis_flaeche')]
    rail = 'bande'
    for z in (-24, 22):
        for x0, x1 in ((-40, -6), (6, 40)) if z < 0 else ((-40, 40),):
            b += [Box((x0, 0, z), (x1, 8, z + 2), rail), Box((x0, 8, z - 0.2), (x1, 9, z + 2.2), 'holz_hell'), snow_cap(x0, z - 0.2, x1, z + 2.2, 9, 0.4)]
    for x in (-40, 38):
        b += [Box((x, 0, -22), (x + 2, 8, 22), rail), Box((x - 0.2, 8, -22), (x + 2.2, 9, 22), 'holz_hell'), snow_cap(x - 0.2, -22, x + 2.2, 22, 9, 0.4)]
    for x in (-6, 6):
        b += [Box((x - 1, 0, -25), (x + 1, 14, -23), 'holz_dunkel')]
    b += [Box((-7, 14, -25), (7, 17, -23), 'holz_dunkel'), Box((-7.4, 17, -25.4), (7.4, 17.6, -22.6), 'schnee')]
    for x in (-36, 36):
        for z in (-21, 20):
            b += [Box((x - 0.6, 0.8, z - 0.6), (x + 0.6, 30, z + 0.6), 'eisen'), Box((x - 1.6, 30, z - 1.6), (x + 1.6, 34, z + 1.6), 'glas', emit=15, shade=False), Box((x - 2, 34, z - 2), (x + 2, 35, z + 2), 'eisen')]
    for k in range(6):
        x = -30 + k * 12
        b += soft([Box((x - 0.6, 0.8, -6 + (k % 2) * 8), (x + 0.6, 1.2, -2 + (k % 2) * 8), 'zuckerguss', rotation=((x, 1, -4 + (k % 2) * 8), (0, k * 30, 0)))])
    for x in (-2.2, 2.2):
        b += [
            Box((x - 0.9, 9, -26.4), (x + 0.9, 12, -25.4), 'zuckerguss'),
            Box((x - 0.9, 9, -27.6), (x + 0.9, 10, -25.4), 'zuckerguss'),
            Box((x - 0.15, 8, -27.6), (x + 0.15, 9, -25.4), 'eisen'),
        ]
    return b


def reindeer_pen():
    b = []
    for x in (-30, -15, 15, 30):
        b += [Box((x - 1, 0, -21), (x + 1, 14, -19), 'holz_dunkel'), snow_cap(x - 1, -21, x + 1, -19, 14, 0.5)]
        b += [Box((x - 1, 0, 19), (x + 1, 14, 21), 'holz_dunkel'), snow_cap(x - 1, 19, x + 1, 21, 14, 0.5)]
    for z in (-5, 5):
        for x in (-30, 30):
            b += [Box((x - 1, 0, z - 1), (x + 1, 14, z + 1), 'holz_dunkel'), snow_cap(x - 1, z - 1, x + 1, z + 1, 14, 0.5)]
    for y in (4, 10):
        for x0, x1 in ((-30, -6), (6, 30)):
            b += [Box((x0, y, -20.6), (x1, y + 2, -19.4), 'holz')]
        b += [Box((-30, y, 19.4), (30, y + 2, 20.6), 'holz')]
        for x in (-30, 30):
            b += [Box((x - 0.6, y, -20), (x + 0.6, y + 2, 20), 'holz')]
    b += [Box((-6, 0, -21.2), (6, 1, -18.8), 'stroh')]
    deer = reindeer()
    deer = [d for d in deer if d.solid]
    for (dx, dz, turn) in ((-14, 4, 30), (13, -3, -110)):
        for d in reindeer():
            if not d.solid:
                continue
            a, c = d.a, d.b
            d.a = (a[0] + dx, a[1], a[2] + dz)
            d.b = (c[0] + dx, c[1], c[2] + dz)
            if d.rotation:
                o, ang = d.rotation
                d.rotation = ((o[0] + dx, o[1], o[2] + dz), (ang[0], ang[1] + turn, ang[2]))
            else:
                d.rotation = ((dx, 0, dz), (0, turn, 0))
            d.solid = False
            b.append(d)
    b += [
        Box((18, 0, 8), (28, 8, 16), 'stroh'),
        Box((17.6, 2, 7.6), (28.4, 3, 16.4), 'kordel'),
        Box((17.6, 5, 7.6), (28.4, 6, 16.4), 'kordel'),
        snow_cap(18, 8, 28, 16, 8, 0.6),
        Box((-26, 0, 10), (-14, 5, 16), 'holz'),
        Box((-25.4, 3.6, 10.6), (-14.6, 4.6, 15.4), 'stroh'),
    ]
    return b


def wall_wreath():
    b = ring((0, 8, 6.2), 4.6, 2.4, 2.4, 12, 'tanne')
    for k in range(6):
        a = math.radians(30 + k * 60)
        x, y = 4.6 * math.cos(a), 8 + 4.6 * math.sin(a)
        b.append(Box((x - 0.6, y - 0.6, 4.4), (x + 0.6, y + 0.6, 5.2), ('beere', 'senf', 'beere', 'zuckerguss', 'beere', 'senf')[k]))
    b += flat_bow(0, 3.6, 5.4, 'band_rot', 1.1)
    b += [Box((-0.4, 12.6, 7.4), (0.4, 15, 8), 'band_rot')]
    return b


def wall_lantern():
    b = [
        Box((-2.2, 6, 7), (2.2, 14, 8), 'eisen'),
        Box((-0.6, 12, 1), (0.6, 13.2, 7), 'eisen'),
        Box((-0.6, 9, 6), (0.6, 12, 7), 'eisen', rotation=((0, 10.5, 6.5), (-35, 0, 0))),
        Box((-0.2, 10, 0.8), (0.2, 12, 1.2), 'eisen'),
        Box((-2.6, 9, -1.6), (2.6, 10, 3.6), 'eisen'),
        Box((-2, 3.6, -1), (2, 9, 3), 'glas', emit=15, shade=False),
        Box((-2.6, 2.6, -1.6), (2.6, 3.6, 3.6), 'eisen'),
        Box((-1, 1.6, 0), (1, 2.6, 2), 'messing'),
        snow_cap(-2.6, -1.6, 2.6, 3.6, 10, 0.4),
    ]
    for sx in (-1, 1):
        for sz in (-1, 3):
            b.append(Box((sx * 2.2 - 0.3, 3.6, sz - 0.3 if sz > 0 else -1.3), (sx * 2.2 + 0.3, 9, sz + 0.3 if sz > 0 else -0.7), 'eisen'))
    return b


def roller(x0, x1, y, z, r=0.9):
    return Box((x0, y - r, z - r), (x1, y + r, z + r), 'eisen')


def gauge(x, y, z, face=-1):
    zz = z if face < 0 else z
    return [
        Box((x - 1.4, y - 1.4, zz - 0.5), (x + 1.4, y + 1.4, zz), 'messing'),
        Box((x - 1, y - 1, zz - 0.6), (x + 1, y + 1, zz - 0.5), 'zuckerguss'),
        Box((x - 0.12, y - 0.1, zz - 0.7), (x + 0.6, y + 0.1, zz - 0.6), 'kohle', rotation=((x, y, zz - 0.65), (0, 0, 35))),
    ]


def lamp_dot(x, y, z, colour, size=1.0):
    h = size / 2
    return Box((x - h, y - h, z - 0.5), (x + h, y + h, z), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colour))


def conveyor():
    b = []
    for side in (-1, 1):
        z = side * 6.5
        b += [Box((-24, 6, z - 1), (24, 9, z + 1), 'eisen'), Box((-24, 8.6, z - 1.3), (24, 9.4, z + 1.3), 'lack_gelb')]
        for x in (-20, -6, 6, 20):
            b += [Box((x - 1, 0, z - 1), (x + 1, 6, z + 1), 'eisen'), Box((x - 1.6, 0, z - 1.6), (x + 1.6, 0.8, z + 1.6), 'kessel')]
    b += [Box((-24, 7.6, -5.5), (24, 8.4, 5.5), 'band_schwarz')]
    for x in range(-22, 23, 4):
        b += soft([Box((x - 0.8, 6.6, -5.5), (x + 0.8, 7.6, 5.5), 'messing')])
    b += [Box((-24, 3, -5.5), (24, 3.6, 5.5), 'eisen')]
    b += gift(-15, 0, 7, 6, 7, 8.4, 'papier_rot', 'band_rot', turn=10)
    b += gift(-1, 0, 6, 7, 6, 8.4, 'papier_gruen', 'band_gruen', turn=-8)
    b += gift(13, 0, 8, 5, 7, 8.4, 'papier_blau', 'band_blau', turn=4)
    return b


def wrapping_machine():
    b = [
        Box((-22, 0, -14), (22, 2, 14), 'kessel'),
        Box((-20, 2, -12), (4, 30, 12), 'lack_rot'),
        Box((-20.4, 28, -12.4), (4.4, 31, 12.4), 'messing'),
        Box((-20.4, 2, -12.4), (4.4, 4, 12.4), 'messing'),
        Box((-14, 31, -6), (-2, 34, 6), 'eisen'),
        Box((-16, 34, -8), (0, 36, 8), 'eisen'),
        Box((-18, 36, -10), (2, 40, 10), 'kupfer'),
        Box((-18.4, 39.6, -10.4), (2.4, 41, 10.4), 'messing'),
        Box((-17, 8, -12.6), (1, 24, -12), 'glas_hell'),
        Box((-17.4, 7.6, -12.8), (1.4, 8.4, -12), 'messing'),
        Box((-17.4, 23.6, -12.8), (1.4, 24.4, -12), 'messing'),
        Box((4, 2, -8), (22, 9, 8), 'eisen'),
        Box((4, 9, -7), (22, 10, 7), 'band_schwarz'),
        Box((4, 8.8, -8.4), (22, 9.6, -7), 'lack_gelb'),
        Box((4, 8.8, 7), (22, 9.6, 8.4), 'lack_gelb'),
        Box((4, 10, -8), (8, 18, 8), 'lack_rot'),
        Box((4.4, 10, -6), (4.8, 15, 6), 'kohle'),
    ]
    for k, colour in enumerate(('rot', 'gruen', 'gold')):
        z = -7 + k * 7
        b += [Box((-26, 16, z - 2.4), (-20, 22, z + 2.4), 'band_' + colour), Box((-27, 18.4, z - 0.6), (-20, 19.6, z + 0.6), 'eisen')]
    b += gauge(-14, 26, -12)
    b += gauge(-6, 26, -12)
    for k, colour in enumerate(('rot', 'gelb', 'gruen')):
        b += soft([lamp_dot(-2 + k * 2.4 - 2, 5.6, -12, colour, 1.3)])
    for k, (y, gx) in enumerate(((12, -12), (16, -6), (20, -10))):
        b += gift(gx, -10 + k * 3, 4, 3.6, 4, y, ('papier_rot', 'papier_gruen', 'papier_gold')[k], ('band_rot', 'band_gruen', 'band_gold')[k], turn=k * 15, bow=False)
    b += gift(15, 0, 6, 5, 6, 10, 'papier_lila', 'band_lila', turn=12)
    b += [rod((2, 30, 10), (2, 44, 10), 1.6, 'kupfer'), rod((2, 44, 10), (-8, 44, 10), 1.6, 'kupfer')]
    b += soft([Box((-1, 40, -2), (1, 44, 0), 'eisen')])
    return b


def candy_machine():
    b = [
        Box((-14, 0, -12), (14, 2, 12), 'kessel'),
        Box((-12, 2, -4), (12, 18, 10), 'lack_gruen'),
        Box((-12.4, 16, -4.4), (12.4, 18.6, 10.4), 'messing'),
        Box((-12.4, 2, -4.4), (12.4, 4, 10.4), 'messing'),
    ]
    for k, colour in enumerate(('zuckerguss', 'apfel')):
        x = -6 + k * 12
        b += [Box((x - 4.4, 18.6, -1), (x + 4.4, 34, 7.8), colour), Box((x - 4.8, 34, -1.4), (x + 4.8, 35.4, 8.2), 'eisen'), Box((x - 4.6, 24, -1.2), (x + 4.6, 25, 8), 'messing')]
    b += [
        Box((-2, 10, -6), (2, 14, -4), 'eisen'),
        Box((-1.4, 10.6, -8), (1.4, 13.4, -6), 'zucker'),
        Box((-12, 2, -12), (12, 8, -4), 'eisen'),
        Box((-12, 8, -11.6), (12, 8.6, -4.4), 'band_schwarz'),
    ]
    for k, x in enumerate((-9, -5, -1, 3, 7)):
        b.append(Box((x - 0.6, 8.6, -11), (x + 0.6, 9.8, -5), 'zucker'))
        b += arc((x + 1.2, 9.2, -11), 1.2, 180, 0, 3, 1, 1.2, 'zucker')
    b += gauge(-6, 13, -4)
    b += gauge(6, 13, -4)
    b += soft([lamp_dot(0, 16, -4, 'rot', 1.2)])
    for k in range(4):
        b.append(Box((-12 + k * 6.6 - 0.3, 18.6, 10), (-12 + k * 6.6 + 0.3, 30, 10.6), 'zucker'))
    return b


def toy_press():
    b = [
        Box((-14, 0, -12), (14, 3, 12), 'kessel'),
        Box((-12, 3, 4), (-6, 40, 10), 'eisen'),
        Box((6, 3, 4), (12, 40, 10), 'eisen'),
        Box((-14, 38, 2), (14, 44, 12), 'lack_blau'),
        Box((-14.4, 43.6, 1.6), (14.4, 45, 12.4), 'messing'),
        Box((-2, 22, -2), (2, 38, 6), 'messing'),
        Box((-8, 18, -6), (8, 22, 8), 'eisen'),
        Box((-8.4, 17.4, -6.4), (8.4, 18, 8.4), 'kohle'),
        Box((-10, 3, -10), (10, 6, 8), 'eisen'),
        Box((-9, 6, -9), (9, 6.6, 7), 'band_schwarz'),
    ]
    b += rocking_horse(0, 6.6, -1, 0.7, 'lack_rot')
    b += gauge(-9, 32, 2)
    b += soft([lamp_dot(9, 32, 2, 'gelb', 1.6), lamp_dot(9, 28, 2, 'gruen', 1.6)])
    for k in range(8):
        b.append(Box((11.6, 18, 5.4), (16, 20, 8.6), 'messing', rotation=((12, 19, 7), (0, 0, k * 45))))
    b.append(Box((11.4, 16, 5), (14.6, 22, 9), 'eisen'))
    return b


def control_desk():
    b = [
        Box((-14, 0, -4), (14, 12, 6), 'kessel'),
        Box((-14.4, 11.6, -6), (14.4, 13, 6.4), 'messing'),
        Box((-14, 12, -6), (14, 18, 6), 'eisen', rotation=((0, 12, 6), (-30, 0, 0))),
        Box((-12, 18, 4), (12, 28, 6), 'eisen'),
        Box((-11, 19, 3.6), (11, 27, 4), 'bildschirm', emit=10, shade=False),
    ]
    colours = ['rot', 'gelb', 'gruen', 'blau', 'warm', 'rosa']
    for k in range(12):
        x = -11 + (k % 6) * 4.4
        z = -3 + (k // 6) * 4
        b += soft([Box((x - 0.8, 13, z - 0.8), (x + 0.8, 14.2, z + 0.8), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[k % 6]), rotation=((0, 12, 6), (-30, 0, 0)))])
    b += [Box((8, 16, -1), (9, 22, 0), 'eisen', rotation=((8.5, 16, -0.5), (0, 0, -20))), Box((9.6, 21.6, -1.6), (12, 24, 0.8), 'apfel')]
    b += gauge(-6, 23, 3.6)
    return b


def toy_shelf():
    b = []
    for x in (-23, 22):
        b.append(Box((x, 0, -6), (x + 1, 46, 6), 'eisen'))
    for y in (2, 16, 30, 44):
        b += [Box((-23, y, -6), (23, y + 1.4, 6), 'holz_hell'), Box((-23, y - 0.4, -6.4), (23, y, 6.4), 'eisen')]
    b += [Box((-23, 0, 5), (23, 46, 6), 'holz_dunkel')]
    b += teddy(-16, 3.4, 0, 0.7)
    b += toy_train(-2, 3.4, 0, 0.8)
    b += rocking_horse(14, 3.4, 0, 0.6, 'lack_blau')
    b += nutcracker(-15, 17.4, 0, 0.6)
    b += parcel(-4, 17.4, 0, 6, 5, 6, 10)
    b += teddy(10, 17.4, 0, 0.6)
    b += parcel(17, 17.4, 0, 5, 4, 5, -12)
    for k, x in enumerate((-17, -9, -1, 7, 15)):
        b += gift(x, 0, 5, 5, 5, 31.4, ('papier_rot', 'papier_gruen', 'papier_blau', 'papier_gold', 'papier_lila')[k], ('band_rot', 'band_gruen', 'band_blau', 'band_gold', 'band_lila')[k], turn=(k * 13) % 30 - 15)
    return b


def crate(x, y, z, s=12, turn=0):
    rot = ((x, y + s / 2, z), (0, turn, 0)) if turn else None
    h = s / 2
    b = [Box((x - h, y, z - h), (x + h, y + s, z + h), 'holz', rotation=rot)]
    for dy in (0.5, s - 1.5):
        b.append(Box((x - h - 0.2, y + dy, z - h - 0.2), (x + h + 0.2, y + dy + 1, z + h + 0.2), 'holz_dunkel', rotation=rot))
    b.append(Box((x - h * 0.6, y + s * 0.35, z - h - 0.3), (x + h * 0.6, y + s * 0.65, z - h), 'kiste_schild', rotation=rot))
    b.append(Box((x - h + 0.6, y + s, z - h + 0.6), (x + h - 0.6, y + s + 0.6, z + h - 0.6), 'stroh', rotation=rot))
    return b


def crate_stack():
    b = []
    b += crate(-6, 0, -6, 12, 4)
    b += crate(7, 0, -5, 12, -6)
    b += crate(0, 0, 8, 12, 10)
    b += crate(0, 12.6, -4, 12, -10)
    b += crate(14, 0, 9, 9, 20)
    b += gift(-6, 7, 5, 5, 5, 12.6, 'papier_rot', 'band_rot', turn=20)
    return b


def wall_gear():
    b = []
    for k in range(8):
        b.append(Box((-2, 14, 5.4), (2, 18.6, 7.4), 'messing', rotation=((0, 8, 6.4), (0, 0, k * 45))))
    b += ring((0, 8, 6.4), 4.4, 3.2, 2, 12, 'messing')
    for k in range(4):
        b.append(Box((-0.6, 4, 5.6), (0.6, 12, 7.2), 'messing', rotation=((0, 8, 6.4), (0, 0, k * 45))))
    b += [Box((-1.6, 6.4, 5), (1.6, 9.6, 8), 'eisen')]
    return b


def wall_gear_mount():
    return [Box((-2.4, 5.6, 7.2), (2.4, 10.4, 8), 'kessel'), Box((-1, 7, 6.6), (1, 9, 7.2), 'eisen')]


def ceiling_lamp():
    b = [
        Box((-1.6, 15, -1.6), (1.6, 16, 1.6), 'eisen'),
        Box((-0.2, 8, -0.2), (0.2, 15, 0.2), 'kohle'),
        Box((-1, 7, -1), (1, 8.4, 1), 'eisen'),
        Box((-5, 5, -5), (5, 7, 5), 'lack_gruen'),
        Box((-6.4, 3.6, -6.4), (6.4, 5, 6.4), 'lack_gruen'),
        Box((-6.6, 3.2, -6.6), (6.6, 3.6, 6.6), 'messing'),
        Box((-2.6, 2, -2.6), (2.6, 3.6, 2.6), 'glas', emit=15, shade=False),
    ]
    return b


def pipe_wall():
    b = []
    b += [Box((-24, 8.5, 3.7), (24, 11.5, 6.7), 'kupfer'), Box((-24, 3, 4.6), (24, 5, 6.6), 'eisen')]
    for x in (-18, 0, 18):
        b += [Box((x - 2.4, 7.4, 3.2), (x + 2.4, 12.6, 8), 'messing'), Box((x - 0.6, 2.4, 6.2), (x + 0.6, 5.6, 8), 'eisen')]
    b += [Box((-8, 12, 4.4), (-6, 14, 6), 'eisen'), Box((-10, 14, 3.6), (-4, 15, 6.8), 'apfel')]
    b += gauge(9, 10, 3.6)
    return b


def octagon(cx, cz, r, y0, y1, mat, faces=None, emit=0):
    side = 2 * r * math.tan(math.radians(22.5))
    return [
        Box((cx - r, y0, cz - side / 2), (cx + r, y1, cz + side / 2), mat, faces=faces, emit=emit),
        Box((cx - side / 2, y0, cz - r), (cx + side / 2, y1, cz + r), mat, faces=faces, emit=emit),
        Box((cx - side / 2, y0, cz - r), (cx + side / 2, y1, cz + r), mat, faces=faces, emit=emit, rotation=((cx, y0, cz), (0, 45, 0))),
        Box((cx - r, y0, cz - side / 2), (cx + r, y1, cz + side / 2), mat, faces=faces, emit=emit, rotation=((cx, y0, cz), (0, 45, 0))),
    ]


def band_ring(cx, cz, r, y, h, mat):
    return octagon(cx, cz, r + 0.4, y, y + h, mat)


def rivets(x0, x1, y, z, step=4):
    return [Box((x - 0.4, y - 0.4, z - 0.4), (x + 0.4, y + 0.4, z), 'messing') for x in frange(x0, x1, step)]


def frange(a, b, step):
    out = []
    v = a
    while v <= b + 1e-6:
        out.append(v)
        v += step
    return out


def pipe_run(points, r, mat):
    b = []
    for p0, p1 in zip(points, points[1:]):
        lo = [min(p0[i], p1[i]) - r for i in range(3)]
        hi = [max(p0[i], p1[i]) + r for i in range(3)]
        b.append(Box(tuple(lo), tuple(hi), mat))
    for p in points[1:-1]:
        b.append(Box((p[0] - r - 0.5, p[1] - r - 0.5, p[2] - r - 0.5), (p[0] + r + 0.5, p[1] + r + 0.5, p[2] + r + 0.5), 'messing'))
    return b


def valve_wheel(x, y, z, r=3):
    b = ring((x, y, z), r, 0.9, 0.8, 8, 'apfel')
    b += [Box((x - r, y - 0.35, z - 0.3), (x + r, y + 0.35, z + 0.3), 'apfel'), Box((x - 0.35, y - r, z - 0.3), (x + 0.35, y + r, z + 0.3), 'apfel'),
          Box((x - 0.8, y - 0.8, z - 0.6), (x + 0.8, y + 0.8, z + 0.6), 'messing')]
    return b


def big_gauge(x, y, z, r=3.4):
    return [
        Box((x - r, y - r, z - 0.8), (x + r, y + r, z), 'messing'),
        Box((x - r + 0.8, y - r + 0.8, z - 0.9), (x + r - 0.8, y + r - 0.8, z - 0.8), 'zuckerguss'),
        Box((x - 0.2, y - 0.2, z - 1.0), (x + r - 1.4, y + 0.2, z - 0.9), 'apfel', rotation=((x, y, z - 0.95), (0, 0, 40))),
        Box((x - 0.5, y - 0.5, z - 1.1), (x + 0.5, y + 0.5, z - 0.9), 'kohle'),
    ]


def gear(r, teeth, thick, mat='messing'):
    b = ring((0, 0, 0), r, r * 0.22, thick, max(12, teeth), mat)
    for k in range(teeth):
        b.append(Box((-r * 0.13, r, -thick / 2), (r * 0.13, r + r * 0.2, thick / 2), mat, rotation=((0, 0, 0), (0, 0, k * 360 / teeth))))
    for k in range(3):
        b.append(Box((-r * 0.1, -r, -thick * 0.35), (r * 0.1, r, thick * 0.35), mat, rotation=((0, 0, 0), (0, 0, k * 60))))
    b.append(Box((-r * 0.25, -r * 0.25, -thick * 0.7), (r * 0.25, r * 0.25, thick * 0.7), 'eisen'))
    return b


def placed(boxes, dx, dy, dz):
    for box in boxes:
        box.a = (box.a[0] + dx, box.a[1] + dy, box.a[2] + dz)
        box.b = (box.b[0] + dx, box.b[1] + dy, box.b[2] + dz)
        if box.rotation:
            o, ang = box.rotation
            box.rotation = ((o[0] + dx, o[1] + dy, o[2] + dz), ang)
    return boxes


def hazard(x0, z0, x1, z1, y):
    w = 1.6
    return [
        Box((x0 - 0.2, y - 0.6, z0 - 0.2), (x1 + 0.2, y + 0.2, z0 + w), 'warnstreifen'),
        Box((x0 - 0.2, y - 0.6, z1 - w), (x1 + 0.2, y + 0.2, z1 + 0.2), 'warnstreifen'),
        Box((x0 - 0.2, y - 0.6, z0 + w), (x0 + w, y + 0.2, z1 - w), 'warnstreifen'),
        Box((x1 - w, y - 0.6, z0 + w), (x1 + 0.2, y + 0.2, z1 - w), 'warnstreifen'),
    ]


def mega_wrapper():
    b = [
        Box((-56, 0, -40), (56, 4, 40), 'kessel', faces={'up': 'riffel'}),
        *hazard(-56, -40, 56, 40, 4),
    ]
    b += [
        Box((-44, 4, -20), (20, 76, 28), 'lack_rot'),
        Box((-44, 4, -28), (20, 24, -20), 'lack_rot'),
        Box((-44, 52, -28), (20, 76, -20), 'lack_rot'),
        Box((-44, 24, -28), (-36, 52, -20), 'lack_rot'),
        Box((12, 24, -28), (20, 52, -20), 'lack_rot'),
        Box((-36, 24, -20.6), (12, 52, -20), 'kohle'),
        Box((-36, 24, -28.2), (12, 52, -27.6), 'glaskugel', shade=False),
        Box((-37, 23, -28.8), (13, 24.4, -27.4), 'messing'),
        Box((-37, 51.6, -28.8), (13, 53, -27.4), 'messing'),
        Box((-12.6, 24, -28.6), (-11.4, 52, -27.6), 'messing'),
    ]
    for y in (14, 62, 74):
        b.append(Box((-44.6, y, -28.6), (20.6, y + 2, 28.6), 'messing'))
    for x in (-44.6, 18.6):
        for z in (-28.6, 26.6):
            b.append(Box((x, 4, z), (x + 2, 76, z + 2), 'eisen'))
    b += rivets(-42, 18, 58, -28)
    b += rivets(-42, 18, 18, -28)
    b += [Box((-35, 24.6, -26), (11, 26, -21), 'band_schwarz')]
    for k, x in enumerate((-30, -20, -6, 4)):
        b += gift(x, -23.5, 5, 5, 4, 26, ('papier_rot', 'papier_gold', 'papier_blau', 'papier_gruen')[k], ('band_rot', 'band_gold', 'band_blau', 'band_gruen')[k], turn=k * 12)
    b += soft([Box((-35.5, 50, -27.4), (11.5, 51, -21), 'glut', emit=13)])
    b += [
        Box((-40, 76, -24), (16, 82, 24), 'kupfer'),
        Box((-36, 82, -20), (12, 88, 20), 'kupfer'),
        Box((-32, 88, -16), (8, 94, 16), 'kupfer'),
        Box((-40.4, 81, -24.4), (16.4, 82.6, 24.4), 'messing'),
        Box((-32.4, 93, -16.4), (8.4, 94.6, 16.4), 'messing'),
        Box((-28, 94, -12), (4, 95, 12), 'kohle'),
    ]
    for k, (x, z) in enumerate(((-22, -6), (-10, 4), (-16, 8), (-4, -6), (-24, 6))):
        b += gift(x, z, 6, 5, 6, 94.5, ('papier_rot', 'papier_lila', 'papier_gold', 'papier_blau', 'papier_weiss')[k], ('band_rot', 'band_lila', 'band_gold', 'band_blau', 'band_weiss')[k], turn=k * 20 - 30, bow=k % 2 == 0)
    b += soft([
        Box((-42, 64, -29.4), (-2, 74, -28.6), 'holz_dunkel', faces={'north': 'schild_fabrik'}, uv={'north': (0, 0)}),
        Box((-42.6, 63.4, -29.6), (-1.4, 64.2, -28.4), 'messing'),
        Box((-42.6, 73.8, -29.6), (-1.4, 74.6, -28.4), 'messing'),
    ])
    b += pipe_run([(14, 76, 14), (14, 100, 14), (30, 100, 14), (30, 110, 14)], 3, 'kupfer')
    b += pipe_run([(-38, 76, 18), (-38, 104, 18), (-38, 104, 30), (-38, 110, 30)], 2.4, 'eisen')
    b += pipe_run([(-44, 40, 10), (-52, 40, 10), (-52, 4, 10)], 2.2, 'kupfer')
    b += valve_wheel(-52, 30, 7.4)
    b += [
        Box((20, 4, -12), (56, 16, 12), 'eisen'),
        Box((20, 16, -10.6), (56, 17, 10.6), 'band_schwarz'),
        Box((20, 15.6, -12.4), (56, 17.6, -10.6), 'lack_gelb'),
        Box((20, 15.6, 10.6), (56, 17.6, 12.4), 'lack_gelb'),
        Box((20, 17, -12), (28, 40, 12), 'lack_rot'),
        Box((20.6, 17, -9), (21.2, 30, 9), 'kohle'),
        Box((19.6, 39.6, -12.4), (28.4, 41, 12.4), 'messing'),
    ]
    for k, x in enumerate((34, 44, 52)):
        b += gift(x, 0, 7, 6, 7, 17, ('papier_gruen', 'papier_rot', 'papier_gold')[k], ('band_gruen', 'band_rot', 'band_gold')[k], turn=k * 15 - 10)
    b += big_gauge(-38, 36, -28)
    b += big_gauge(-38, 46, -28)
    colours = ['rot', 'gelb', 'gruen', 'blau', 'warm', 'rot', 'gelb', 'gruen']
    for k, x in enumerate(range(-40, 18, 7)):
        b += soft([lamp_dot(x, 10, -28, colours[k % 8], 1.8)])
    b += [Box((-20, 4, -40), (4, 10, -32), 'kessel'), Box((-20.4, 9.6, -40.4), (4.4, 11, -31.6), 'messing')]
    for k in range(6):
        b += soft([lamp_dot(-17 + k * 3.8, 12.2, -33, colours[k], 1.4)])
    b.append(Box((-17 + 5 * 3.8 - 0.6, 11, -36), (-17 + 5 * 3.8 + 0.6, 16, -35), 'eisen', rotation=((-17 + 5 * 3.8, 11, -35.5), (-20, 0, 0))))
    return b


def mega_wrapper_gear():
    return gear(11, 12, 3)


def candy_works():
    b = [Box((-40, 0, -40), (40, 4, 40), 'kessel', faces={'up': 'riffel'}), *hazard(-40, -40, 40, 40, 4)]
    for k, (cx, mat) in enumerate(((-20, 'apfel'), (20, 'zuckerguss'))):
        b += octagon(cx, 14, 15, 4, 70, mat)
        for y in (20, 40, 60):
            b += band_ring(cx, 14, 15, y, 2, 'messing')
        b += octagon(cx, 14, 16, 70, 74, 'eisen')
        b += octagon(cx, 14, 8, 74, 80, 'eisen')
        b += pipe_run([(cx, 66, 29), (cx, 92, 29), (0, 92, 29), (0, 104, 29)], 2.6, 'kupfer')
        b += [Box((cx - 5, 30, -1.6), (cx + 5, 44, -0.8), 'glaskugel', shade=False), Box((cx - 4.4, 30.4, -1), (cx + 4.4, 38, -0.2), mat, emit=6)]
        b += big_gauge(cx, 52, -1)
    b += [Box((-8, 4, -36), (8, 60, -20), 'lack_gruen'), Box((-8.4, 58, -36.4), (8.4, 61, -19.6), 'messing'), Box((-8.4, 4, -36.4), (8.4, 8, -19.6), 'messing')]
    b += [Box((-5, 60, -33), (5, 100, -23), 'glaskugel', shade=False)]
    for k in range(20):
        y = 61 + k * 1.9
        b.append(Box((-2.2, y, -29.2), (2.2, y + 1.9, -26.8), 'apfel' if k % 2 == 0 else 'zuckerguss', rotation=((0, y, -28), (0, k * 32, 0))))
    b += [Box((-6, 100, -34), (6, 104, -22), 'messing')]
    b += [
        Box((-36, 4, -40), (36, 12, -36), 'eisen'),
        Box((-36, 12, -39.6), (36, 13, -36.4), 'band_schwarz'),
    ]
    for k, x in enumerate(range(-32, 33, 6)):
        b.append(Box((x - 0.6, 13, -39), (x + 0.6, 14.2, -37), 'zucker', rotation=((x, 13.6, -38), (0, 90, 0))))
        b += arc((x, 13.6, -36.4), 1.2, 270, 90, 3, 1, 1.2, 'zucker')
    for k, x in enumerate((-28, -18, 18, 28)):
        b += soft([lamp_dot(x, 8, -36, ('rot', 'gruen', 'gelb', 'blau')[k], 1.8)])
    b += [Box((-40, 4, 30), (40, 40, 40), 'eisen'), Box((-40.4, 38, 29.6), (40.4, 41, 40.4), 'messing')]
    return b


def candy_stirrer():
    b = [Box((-1, -4, -1), (1, 5, 1), 'messing'), Box((-2.4, 5, -2.4), (2.4, 7, 2.4), 'eisen')]
    for k in range(4):
        b.append(Box((-7, -0.6, -0.6), (7, 0.6, 0.6), 'messing', rotation=((0, 0, 0), (0, k * 45, 0))))
    for k in range(8):
        a = math.radians(k * 45)
        b.append(Box((7 * math.cos(a) - 1, -1, 7 * math.sin(a) - 1), (7 * math.cos(a) + 1, 1, 7 * math.sin(a) + 1), 'apfel' if k % 2 else 'zuckerguss'))
    return b


def boiler():
    b = [Box((-40, 0, -32), (40, 4, 32), 'kessel', faces={'up': 'riffel'}), *hazard(-40, -32, 40, 32, 4)]
    for x in (-28, -8, 12, 28):
        b += [Box((x - 3, 4, -14), (x + 3, 20, 14), 'eisen')]
    tank = []
    for k in range(4):
        tank.append(Box((-36, -18, -7.5), (36, 18, 7.5), 'kupfer', rotation=((0, 0, 0), (k * 45, 0, 0))))
    for x in (-30, -12, 6, 24):
        for k in range(4):
            tank.append(Box((x - 1, -19.4, -8), (x + 1, 19.4, 8), 'messing', rotation=((0, 0, 0), (k * 45, 0, 0))))
    b += placed(tank, 0, 38, 0)
    b += [Box((-38, 22, -18), (-34, 54, 18), 'eisen'), Box((34, 22, -18), (38, 54, 18), 'eisen')]
    b += [
        Box((-40, 4, -32), (-12, 34, -14), 'kessel'),
        Box((-36, 8, -32.4), (-16, 26, -32), 'eisen'),
        Box((-34, 10, -32.6), (-18, 24, -32.4), 'glut', emit=15, shade=False),
        Box((-35, 9, -32.8), (-17, 10, -32.4), 'messing'),
        Box((-35, 24, -32.8), (-17, 25, -32.4), 'messing'),
    ]
    for x in (-31, -26, -21):
        b.append(Box((x - 0.5, 10, -32.9), (x + 0.5, 24, -32.6), 'kohle'))
    b += pipe_run([(-26, 34, -23), (-26, 70, -23), (-26, 110, -23)], 4, 'eisen')
    b += [Box((-31, 104, -28), (-21, 108, -18), 'messing')]
    b += pipe_run([(20, 56, 0), (20, 76, 0), (36, 76, 0), (36, 110, 0)], 3, 'kupfer')
    b += pipe_run([(-8, 56, 0), (-8, 66, 0), (-8, 66, -26), (8, 66, -26), (8, 20, -26)], 2.2, 'eisen')
    b += valve_wheel(8, 40, -28.6, 4)
    b += big_gauge(-2, 46, -18.6, 4.4)
    b += big_gauge(4, 30, -18.6, 3.4)
    b += [Box((24.8, 34.8, -24.6), (27.2, 37.2, -17), 'eisen')]
    for k, x in enumerate((-6, 0, 6)):
        b += soft([lamp_dot(x + 26, 14, -32, ('rot', 'gelb', 'gruen')[k], 2)])
    b += [Box((14, 4, -32), (38, 22, -24), 'kessel'), Box((13.6, 21.6, -32.4), (38.4, 23, -23.6), 'messing')]
    return b


def boiler_gear():
    return gear(10, 10, 2.6, 'eisen')


def giant_press():
    b = [Box((-40, 0, -32), (40, 6, 32), 'kessel', faces={'up': 'riffel'}), *hazard(-40, -32, 40, 32, 6)]
    for x in (-36, 24):
        b += [Box((x, 6, -4), (x + 12, 96, 16), 'lack_blau'), Box((x - 0.4, 40, -4.4), (x + 12.4, 42, 16.4), 'messing'), Box((x - 0.4, 70, -4.4), (x + 12.4, 72, 16.4), 'messing')]
        b += rivets(x + 2, x + 10, 30, -4, 4)
        b += rivets(x + 2, x + 10, 60, -4, 4)
    b += [
        Box((-40, 96, -8), (40, 108, 20), 'lack_blau'),
        Box((-40.4, 95, -8.4), (40.4, 97, 20.4), 'messing'),
        Box((-40.4, 107, -8.4), (40.4, 109, 20.4), 'messing'),
        Box((-6, 52, -2), (6, 96, 10), 'messing'),
        Box((-18, 40, -8), (18, 52, 14), 'eisen'),
        Box((-18.4, 39, -8.4), (18.4, 40, 14.4), 'kohle'),
        Box((-22, 6, -12), (22, 14, 14), 'eisen'),
        Box((-20, 14, -10), (20, 15, 12), 'band_schwarz'),
    ]
    b += placed(rocking_horse(0, 0, 0, 1.4, 'lack_rot'), 0, 15, 1)
    b += soft([
        Box((-24, 98, -8.8), (24, 106, -8.2), 'holz_dunkel', faces={'north': 'schild_presse'}, uv={'north': (0, 0)}),
    ])
    for k, x in enumerate((-32, -28, 28, 32)):
        b += soft([lamp_dot(x, 86, -4, ('rot', 'gelb', 'gruen', 'blau')[k], 2)])
    b += big_gauge(-30, 50, -4)
    b += [Box((28.8, 58.8, -5.6), (31.2, 61.2, -4), 'eisen')]
    b += pipe_run([(-30, 108, 6), (-30, 110, 6)], 3, 'kupfer')
    for k, x in enumerate((-30, 30)):
        b += [Box((x - 4, 6, -30), (x + 4, 12, -22), 'kessel')]
        b += gift(x, -26, 6, 6, 6, 12, ('papier_gruen', 'papier_rot')[k], ('band_gruen', 'band_rot')[k], turn=15)
    return b


def press_wheel():
    return gear(11, 10, 3, 'eisen')


STALLS = {
    'marktstand': ('Glühweinstand', 'rot', 'schild_gluehwein', gluehwein_big, []),
    'stand_lebkuchen': ('Lebkuchenstand', 'gruen', 'schild_lebkuchen', lebkuchen_big, []),
    'stand_wurst': ('Bratwurststand', 'braun', 'schild_wurst', wurst_big, [('rauch', (-1, 1.25, -1.1), 1.4)]),
    'stand_kerzen': ('Kerzenstand', 'blau', 'schild_kerzen', kerzen_big, []),
    'stand_kugeln': ('Kugelstand', 'lila', 'schild_kugeln', kugel_big, []),
    'stand_mandeln': ('Mandelstand', 'orange', 'schild_mandeln', mandeln_big, []),
    'stand_crepes': ('Crêpesstand', 'gelb', 'schild_crepes', crepes_big, []),
    'stand_kakao': ('Kakaostand', 'tuerkis', 'schild_kakaostand', kakao_big, []),
    'stand_brezeln': ('Brezelstand', 'braun', 'schild_brezeln', brezel_big, []),
    'stand_spielzeug': ('Spielzeugstand', 'rot', 'schild_spielzeug', spielzeug_big, []),
    'stand_sterne': ('Sternestand', 'blau', 'schild_sterne', sterne_big, []),
    'stand_honig': ('Honigstand', 'gelb', 'schild_honig', honig_big, []),
    'stand_bratapfel': ('Bratapfelstand', 'rot', 'schild_bratapfel', bratapfel_big, []),
    'stand_raclette': ('Raclettestand', 'orange', 'schild_raclette', raclette_big, []),
    'stand_strick': ('Strickstand', 'lila', 'schild_strick', strick_big, []),
    'stand_schoko': ('Schokofruchtstand', 'braun', 'schild_schoko', schoko_big, []),
    'stand_post': ('Wichtelpost', 'blau', 'schild_post_stand', post_big, []),
    'stand_puffer': ('Kartoffelpufferstand', 'gelb', 'schild_puffer', puffer_big, [('rauch', (0.95, 1.25, -1.1), 1.0)]),
    'stand_werkstatt': ('Wichtelwerkstatt', 'gruen', 'schild_werkstatt', workbench_big, []),
}


def stall(art):
    _, colour, sign, wares, _ = STALLS[art]
    return stall_big(colour, sign) + wares()


def gate_big():
    b = []
    for side in (-1, 1):
        x0, x1 = (-39, -25) if side < 0 else (25, 39)
        cx = (x0 + x1) / 2
        b += [
            Box((x0 - 1.5, 0, -8.5), (x1 + 1.5, 4, 8.5), 'stein'),
            snow_cap(x0 - 1.5, -8.5, x1 + 1.5, 8.5, 4, 0.7),
            Box((x0 + 1, 4, -6), (x1 - 1, 60, 6), 'holz_dunkel'),
            Box((x0, 4, -7), (x0 + 2.5, 60, 7), 'holz'),
            Box((x1 - 2.5, 4, -7), (x1, 60, 7), 'holz'),
        ]
        for k, y in enumerate((10, 22, 34, 46)):
            b.append(Box((x0 - 0.8, y, -7.8), (x1 + 0.8, y + 4, 7.8), 'tanne'))
            b += soft(flat_bow(cx, y + 2, -7.8, 'band_rot', 1.2))
            b += soft(flat_bow(cx, y + 2, 7.8 + 0.6, 'band_rot', 1.2))
    b += [
        Box((-43, 60, -8), (43, 68, 8), 'holz_dunkel'),
        Box((-43, 64, -9), (43, 66, 9), 'messing'),
        snow_cap(-43, -8, 43, 8, 68, 0.8),
    ]
    b += soft([
        Box((-43, 56, -9.6), (43, 60, -8), 'tanne'),
        Box((-43, 56, 8), (43, 60, 9.6), 'tanne'),
        Box((-23, 69.4, -1.5), (23, 80.9, 0), 'holz', faces={'north': 'schild_markt', 'south': 'schild_markt'}, uv={'north': (0, 0), 'south': (0, 0)}),
        Box((-23.8, 68.8, -1.9), (23.8, 69.8, 0.4), 'messing'),
        Box((-23.8, 80.6, -1.9), (23.8, 81.6, 0.4), 'messing'),
        Box((-23.8, 81.6, -2), (23.8, 82.4, 0.5), 'schnee', skip=('down',)),
        Box((-1.8, 83.6, -0.8), (1.8, 88.8, 0.8), 'stern', emit=15, shade=False, rotation=((0, 86.2, 0), (0, 0, 45))),
        Box((-0.9, 82.8, -0.8), (0.9, 89.6, 0.8), 'stern', emit=15, shade=False),
    ])
    colours = list(BULB_SLOTS)
    for k, x in enumerate(range(-24, 25, 3)):
        t = (x + 24) / 48
        sag = 6 * math.sin(math.pi * t)
        for z in (-9.6, 8.8):
            b += soft([Box((x - 0.7, 54.6 - sag, z), (x + 0.7, 56.4 - sag, z + 0.8), 'lampen', emit=15, shade=False, uv=patch(BULB_SLOTS, colours[k % 6]))])
    for x in (-24, 24):
        b += soft([
            Box((x - 0.2, 52, -0.2), (x + 0.2, 60, 0.2), 'eisen'),
            Box((x - 2.4, 45, -2.4), (x + 2.4, 46, 2.4), 'eisen'),
            Box((x - 1.9, 46, -1.9), (x + 1.9, 51, 1.9), 'glas', emit=15, shade=False),
            Box((x - 2.4, 51, -2.4), (x + 2.4, 52, 2.4), 'eisen'),
        ])
    return b


def figure_big(x, y, z, robe, s, wings=False, staff=False):
    b = [
        Box((x - 1.4 * s, y, z - 1.2 * s), (x + 1.4 * s, y + 5 * s, z + 1.2 * s), robe),
        Box((x - 1.1 * s, y + 5 * s, z - 1.1 * s), (x + 1.1 * s, y + 7.2 * s, z + 1.1 * s), 'holz_hell'),
        Box((x - 1.25 * s, y + 6.6 * s, z - 1.25 * s), (x + 1.25 * s, y + 7.6 * s, z + 1.25 * s), robe),
    ]
    if wings:
        for turn in (40, -40):
            b.append(Box((x - 0.25 * s, y + 2.5 * s, z + 1.2 * s), (x + 0.25 * s, y + 6 * s, z + 3.4 * s), 'zuckerguss', rotation=((x, y + 4 * s, z + 1.2 * s), (0, turn, 0))))
    if staff:
        b.append(Box((x + 1.6 * s, y, z - 0.25), (x + 2.1 * s, y + 8 * s, z + 0.25), 'holz'))
    return b


def pyramid_big_base():
    b = [
        Box((-23, 0, -23), (23, 2, 23), 'holz_dunkel'),
        Box((-22, 2, -22), (22, 3.5, 22), 'holz_hell'),
        Box((-23.3, 1.4, -23.3), (23.3, 2.2, 23.3), 'messing'),
        snow_cap(-23, -23, 23, -19, 2, 0.5),
        Box((-2.5, 3.5, -2.5), (2.5, 6, 2.5), 'messing'),
    ]
    for sx in (-1, 1):
        for sz in (-1, 1):
            b += [Box((sx * 19 - 2, 3.5, sz * 19 - 2), (sx * 19 + 2, 5, sz * 19 + 2), 'messing')]
            b += candle(sx * 19, 5, sz * 19, 9, 'kerze_rot', 2.4)
    return b


def pyramid_big_spin():
    b = [Box((-1.2, 6, -1.2), (1.2, 100, 1.2), 'holz_hell')]
    tiers = ((7, 17), (34, 13), (58, 9))
    tops = (34, 58, 78)
    for (y, r), top in zip(tiers, tops):
        b += [
            Box((-r, y, -r), (r, y + 1.8, r), 'holz'),
            Box((-r - 0.4, y + 0.4, -r - 0.4), (r + 0.4, y + 1.4, r + 0.4), 'messing'),
        ]
        pr = r - 1.6
        for sx in (-1, 1):
            for sz in (-1, 1):
                b.append(Box((sx * pr - 0.7, y + 1.8, sz * pr - 0.7), (sx * pr + 0.7, top, sz * pr + 0.7), 'holz_hell'))
    robes = ['lack_rot', 'lack_blau', 'lack_gruen', 'samt', 'lack_braun', 'lack_lila']
    for k in range(6):
        a = math.radians(30 + k * 60)
        b += figure_big(11.5 * math.cos(a), 8.8, 11.5 * math.sin(a), robes[k], 1.6, staff=k % 2 == 0)
    for k in range(5):
        a = math.radians(k * 72)
        x, z = 8.5 * math.cos(a), 8.5 * math.sin(a)
        b += [
            Box((x - 2.8, 35.8, z - 2.8), (x + 2.8, 39, z + 2.8), 'tanne_blatt'),
            Box((x - 2, 39, z - 2), (x + 2, 42.4, z + 2), 'tanne_blatt'),
            Box((x - 1.2, 42.4, z - 1.2), (x + 1.2, 45.6, z + 1.2), 'tanne_blatt'),
            Box((x - 0.6, 45.6, z - 0.6), (x + 0.6, 47, z + 0.6), 'stern', emit=15, shade=False),
        ]
    for k in range(3):
        a = math.radians(90 + k * 120)
        b += figure_big(5.4 * math.cos(a), 59.8, 5.4 * math.sin(a), 'zuckerguss', 1.3, wings=True)
    b += [
        Box((-10, 78, -10), (10, 79.4, 10), 'holz'),
        Box((-2.4, 92, -2.4), (2.4, 96, 2.4), 'messing'),
    ]
    for k in range(8):
        b.append(Box((2, 93.4, -2), (22, 94.2, 2), 'holz_hell', rotation=((0, 94, 0), (28, k * 45, 0))))
    b += [Box((-1, 96, -1), (1, 98.4, 1), 'stern', emit=15, shade=False)]
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
    def __init__(self, item, boxes, offset, tags=(), components=None):
        self.item = item
        self.boxes = boxes
        self.tags = tags
        self.components = components
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


def same_frame(box):
    if not box.rotation:
        return None
    origin, angles = box.rotation
    return (tuple(round(v, 3) for v in origin), tuple(round(v, 3) for v in angles))


def visible_faces(box):
    lo = [min(box.a[i], box.b[i]) for i in range(3)]
    hi = [max(box.a[i], box.b[i]) for i in range(3)]
    names = {(0, -1): 'west', (0, 1): 'east', (1, -1): 'down', (1, 1): 'up', (2, -1): 'north', (2, 1): 'south'}
    for axis in range(3):
        for sign in (-1, 1):
            if names[(axis, sign)] in box.skip or (axis == 1 and sign < 0):
                continue
            plane = lo[axis] if sign < 0 else hi[axis]
            others = [i for i in range(3) if i != axis]
            yield axis, sign, plane, [(lo[i], hi[i]) for i in others]


def overlap(r1, r2):
    area = 1.0
    for (a0, a1), (b0, b1) in zip(r1, r2):
        d = min(a1, b1) - max(a0, b0)
        if d <= 1e-4:
            return 0.0
        area *= d
    return area


def clean(boxes):
    fixed = 0
    for _ in range(30):
        planes = {}
        for index, box in enumerate(boxes):
            for axis, sign, plane, rect in visible_faces(box):
                planes.setdefault((same_frame(box), axis, sign, round(plane, 3)), []).append((index, rect))
        moved = set()
        for (frame, axis, sign, plane), faces in planes.items():
            if len(faces) < 2:
                continue
            for i in range(len(faces)):
                for j in range(i + 1, len(faces)):
                    (ia, ra), (ib, rb) = faces[i], faces[j]
                    if ia in moved or ib in moved or overlap(ra, rb) <= 0:
                        continue
                    area_a = (ra[0][1] - ra[0][0]) * (ra[1][1] - ra[1][0])
                    area_b = (rb[0][1] - rb[0][0]) * (rb[1][1] - rb[1][0])
                    target = ib if area_b <= area_a else ia
                    box = boxes[target]
                    a = list(box.a)
                    b = list(box.b)
                    if sign > 0:
                        if a[axis] >= b[axis]:
                            a[axis] += NUDGE
                        else:
                            b[axis] += NUDGE
                    else:
                        if a[axis] <= b[axis]:
                            a[axis] -= NUDGE
                        else:
                            b[axis] -= NUDGE
                    box.a, box.b = tuple(a), tuple(b)
                    moved.add(target)
                    fixed += 1
        if not moved:
            return fixed
    return fixed


def count_fights(boxes):
    planes = {}
    for index, box in enumerate(boxes):
        for axis, sign, plane, rect in visible_faces(box):
            planes.setdefault((same_frame(box), axis, sign, round(plane, 3)), []).append((index, rect))
    total = 0
    for faces in planes.values():
        for i in range(len(faces)):
            for j in range(i + 1, len(faces)):
                if overlap(faces[i][1], faces[j][1]) > 0:
                    total += 1
    return total


_PROPS = None


def props():
    global _PROPS
    if _PROPS is None:
        _PROPS = build_props()
        for prop in _PROPS:
            boxes = [b for part in prop.parts if part.boxes is not None for b in part.boxes]
            prop.fixed = clean(boxes)
            prop.fights = count_fights(boxes)
    return _PROPS


def build_props():
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
    stall_area = [(x, y, z) for x in (-1, 0, 1) for z in (0, 2) for y in (0, 1)]
    for art, (title, _, _, _, extras) in STALLS.items():
        boxes = stall(art)
        parts = auto_parts(art, boxes)
        for item, offset, strength in extras:
            parts.append(Part(item, None, offset, components=f'"minecraft:custom_model_data":{{floats:[{strength}f]}}'))
        result.append(Prop(art, parts, footprint_of(boxes), title=title))
    gate_boxes = gate_big()
    result.append(Prop('eingangstor', auto_parts('eingangstor', gate_boxes), footprint_of(gate_boxes), title='Eingangstor'))
    for art, title, maker in (('maronenofen', 'Maronenofen', chestnut_oven), ('lichterbogen', 'Lichterbogen', light_arch), ('tannenbaum', 'Tannenbaum', potted_tree),
                              ('schneemann', 'Schneemann', snowman), ('rentier', 'Rentier', reindeer), ('bank', 'Bank', bench), ('briefkasten', 'Wunschzettel-Briefkasten', mailbox),
                              ('nussknacker', 'Nussknacker', nutcracker_big), ('schwibbogen', 'Schwibbogen', schwibbogen), ('engel', 'Engel', angel_statue),
                              ('lebkuchenhaus', 'Lebkuchenhaus', gingerbread_house), ('weihnachtsstern', 'Weihnachtsstern', poinsettia), ('geschenksack', 'Geschenksack', gift_sack),
                              ('adventskranz', 'Adventskranz', advent_wreath), ('pinguin', 'Pinguin', penguin), ('holzstapel', 'Holzstapel', woodpile), ('feuerkorb', 'Feuerkorb', fire_basket),
                              ('krippe', 'Weihnachtskrippe', nativity), ('christbaumverkauf', 'Christbaumverkauf', tree_sale), ('brunnen', 'Brunnen', frozen_fountain),
                              ('vogelhaus', 'Vogelhaus', birdhouse), ('schneehaufen', 'Schneehaufen', snow_pile),
                              ('sterntor', 'Sterntor', star_gate), ('zaun', 'Marktzaun', market_fence), ('doppellaterne', 'Doppellaterne', double_lamp), ('sitzgruppe', 'Sitzgruppe', seating),
                              ('schneekugel', 'Schneekugel', snow_globe), ('eisbahn', 'Eisbahn', ice_rink), ('rentiergehege', 'Rentiergehege', reindeer_pen)):
        boxes = maker()
        parts = auto_parts(art, boxes)
        if art == 'maronenofen':
            parts.append(Part('rauch', None, (0.28, 2.6, 0), components='"minecraft:custom_model_data":{floats:[0.8f]}'))
        if art == 'feuerkorb':
            parts.append(Part('rauch', None, (0, 1.4, 0), components='"minecraft:custom_model_data":{floats:[0.6f]}'))
        result.append(Prop(art, parts, footprint_of(boxes), title=title))
    base = carousel_base()
    spin = carousel_spin()
    result.append(Prop('karussell', auto_parts('karussell_fuss', base, axis=(0, 0)) + auto_parts('karussell_dreh', spin, tags=('deko_dreh',), axis=(0, 0)),
                       [(x, y, z) for x in (-1, 0, 1) for z in (-1, 0, 1) for y in range(4)], title='Kinderkarussell'))
    for art, title, maker in (('tuerkranz', 'Türkranz', wall_wreath), ('wandlaterne', 'Wandlaterne', wall_lantern)):
        prop = Prop(art, [Part(art, maker(), (0, 0, 0))], [], title=title)
        prop.wall = True
        result.append(prop)
    for art, title, maker in (('fliessband', 'Fließband', conveyor), ('verpackungsmaschine', 'Verpackungsmaschine', wrapping_machine),
                              ('zuckerstangenmaschine', 'Zuckerstangenmaschine', candy_machine), ('spielzeugpresse', 'Spielzeugpresse', toy_press),
                              ('kontrollpult', 'Kontrollpult', control_desk), ('spielzeugregal', 'Spielzeugregal', toy_shelf), ('kistenstapel', 'Kistenstapel', crate_stack)):
        boxes = maker()
        result.append(Prop(art, auto_parts(art, boxes), footprint_of(boxes), title=title))
    def giant(art, title, body, extras):
        boxes = body()
        parts = auto_parts(art, boxes)
        for name, maker, offset, tag in extras:
            parts.append(Part(name, placed(maker(), offset[0] * 16, offset[1] * 16, offset[2] * 16), offset, tags=(tag,)))
        result.append(Prop(art, parts, footprint_of(boxes), title=title))

    giant('riesen_verpackung', 'Riesen-Verpackungsmaschine', mega_wrapper, [('riesen_verpackung_rad', mega_wrapper_gear, (0.625, 3.9375, -1.906), 'deko_dreh_z')])
    giant('riesen_zuckerwerk', 'Zuckerstangenwerk', candy_works, [('ruehrwerk_l', candy_stirrer, (-1.25, 5.25, 0.875), 'deko_dreh'), ('ruehrwerk_r', candy_stirrer, (1.25, 5.25, 0.875), 'deko_dreh')])
    giant('dampfkessel', 'Dampfkessel', boiler, [('dampfkessel_rad', boiler_gear, (1.625, 2.25, -1.625), 'deko_dreh_z')])
    giant('riesen_presse', 'Riesenpresse', giant_press, [('riesen_presse_rad', press_wheel, (1.875, 3.75, -0.40625), 'deko_dreh_z')])
    gear = Prop('zahnrad', [Part('zahnrad_halter', wall_gear_mount(), (0, 0, 0)), Part('zahnrad_rad', wall_gear(), (0, 0.5, 0), tags=('deko_dreh_z',))], [], title='Zahnrad')
    gear.wall = True
    result.append(gear)
    pipe = Prop('rohrleitung', [Part('rohrleitung', pipe_wall(), (0, 0, 0))], [], title='Rohrleitung')
    pipe.wall = True
    result.append(pipe)
    lamp = Prop('fabriklampe', [Part('fabriklampe', ceiling_lamp(), (0, 0, 0))], [], light=(0, 0, 0), title='Fabriklampe')
    lamp.ceiling = True
    result.append(lamp)
    result.append(Prop('stehtisch', [Part('stehtisch', table(), (0, None, 0))], column(2), title='Stehtisch'))
    base = pyramid_big_base()
    spin = pyramid_big_spin()
    result.append(Prop('pyramide', auto_parts('pyramide_fuss', base, axis=(0, 0)) + auto_parts('pyramide_dreh', spin, tags=('deko_dreh',), axis=(0, 0)),
                       [(x, y, z) for x in (-1, 0, 1) for z in (-1, 0, 1) for y in range(5)] + [(0, 5, 0)], title='Weihnachtspyramide'))
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
    model_id = f'hideandseek:props/deko/{model}' if part.boxes is not None or model.startswith('geschenk_') else f'hideandseek:props/{model}'
    tags = ','.join(f'"{t}"' for t in ('sniffa_prop', 'sniffa_deko_teil', 'deko_teil_neu') + tuple(part.tags))
    extra = f',{part.components}' if part.components else ''
    text = (
        f'{{Tags:[{tags}],Rotation:[$(yaw)f,0f],'
        f'item:{{id:"minecraft:stick",count:1,components:{{"minecraft:item_model":"{model_id}"{extra}}}}}'
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
    modus = '_wand' if getattr(prop, 'wall', False) else '_decke' if getattr(prop, 'ceiling', False) else ''
    write(FUNCTIONS / f'{prop.art}.mcfunction', f'function sniffa:intern/deko/platzieren {{art:"{prop.art}",name:"{prop.title}",modus:"{modus}"}}\n')


def main():
    atlas = build_atlas()
    sheet = FaceSheet(atlas)
    models = []
    for prop in props():
        for part in prop.parts:
            if part.boxes is None:
                continue
            models.append((part.item, model_json(sheet, part.boxes, part_shift(part), part.item)))
        functions(prop)
    for size in GIFT_SIZES:
        prop = next(p for p in props() if p.art == 'geschenk_' + size)
        shift = part_shift(prop.parts[0])
        for colour in GIFT_COLOURS:
            name = f'geschenk_{size}_{colour}'
            models.append((name, model_json(sheet, gift_model(size, colour), shift, name)))
    previews = []
    for prop in props():
        boxes = [b for part in prop.parts if part.boxes is not None for b in part.boxes]
        if boxes:
            previews.append((prop.art, model_json(sheet, boxes, (0, 0, 0), prop.art, check=False)))
    sheet.pack()
    preview_dir = ROOT / 'build/deko-preview'
    preview_dir.mkdir(parents=True, exist_ok=True)
    for art, model in previews:
        model = resolve(sheet, model)
        model['textures'] = {'0': '../../src/main/resources/assets/hideandseek/textures/item/props/deko', 'particle': '../../src/main/resources/assets/hideandseek/textures/item/props/deko'}
        (preview_dir / f'{art}.json').write_text(json.dumps(model))
    texture = ASSETS / 'textures/item/props/deko.png'
    texture.parent.mkdir(parents=True, exist_ok=True)
    sheet.image.save(texture)
    for name, model in models:
        write_json(ASSETS / f'models/item/props/deko/{name}.json', resolve(sheet, model))
        write_json(ASSETS / f'items/props/deko/{name}.json', item_definition(name))
    print('sheet', SHEET, sheet.height, len(sheet.requests), 'faces')
    for prop in props():
        print(f'{prop.art}: {prop.fixed} nudged, {prop.fights} left')


if __name__ == '__main__':
    main()
