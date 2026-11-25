#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;

out vec4 fragColor;

const float TAU = 6.2831853;

const float LINE_PIXELS = 1.4;
const float LINE_STRENGTH = 0.7;
const float LINE_CELLS = 0.035;
const float LINE_WHITE = 0.78;

const float GLOW_PIXELS = 9.0;
const float GLOW_STRENGTH = 0.22;
const float GLOW_CELLS = 0.14;

const float RIM_CELLS = 0.05;
const float RIM_STRENGTH = 0.18;

const float FILL = 0.13;
const float FILL_FAR = 0.07;
const float FILL_SHADE = 0.85;

const float FACET_STRENGTH = 0.18;
const float FACET_LIGHT = 0.35;

const float LINES_NEAR_FROM = 8.0;
const float LINES_NEAR_TO = 30.0;
const float LINES_FAR = 0.1;

const float AURORA_CELLS = 26.0;
const float AURORA_HEIGHT_CELLS = 9.0;
const float AURORA_CYCLES = 40.0;
const float AURORA_STRENGTH = 0.16;

const float FROST_CELLS = 0.075;
const float FROST_GRAIN = 22.0;
const float FROST_STRENGTH = 0.3;
const float FROST_NEAR_FROM = 6.0;
const float FROST_NEAR_TO = 40.0;
const float FROST_WHITE = 0.72;

const float CORNER_CELLS = 0.05;
const float CORNER_STRENGTH = 0.22;

const float GLINT_GRID = 9.0;
const float GLINT_SHARE = 0.06;
const float GLINT_CYCLES = 260.0;
const float GLINT_SHARPNESS = 14.0;
const float GLINT_SIZE = 0.05;
const float GLINT_PIXELS = 1.6;
const float GLINT_STRENGTH = 1.1;
const float GLINT_NEAR_FROM = 4.0;
const float GLINT_NEAR_TO = 30.0;

const float SHEEN_CYCLES = 30.0;
const float SHEEN_ACROSS = 0.035;
const float SHEEN_UP = 0.06;
const float SHEEN_SHARPNESS = 6.0;
const float SHEEN_STRENGTH = 0.22;

const float SWEEP_CYCLES = 200.0;
const float SWEEP_CELLS = 18.0;
const float SWEEP_SHARPNESS = 10.0;
const float SWEEP_STRENGTH = 0.45;
const float SHRINK_SWEEP = -1.0;

const float BREATH_CYCLES = 600.0;
const float BREATH_STRENGTH = 0.25;

const float NEAR_BLOCKS = 3.0;
const float NEAR_FADE = 40.0;
const float NEAR_BOOST = 0.95;

const float HOT_BLOCKS = 7.0;
const float HOT_FILL = 0.38;
const float HOT_LIGHT = 2.2;

const int RIPPLES = 3;
const float RIPPLE_SECONDS = 1.6;
const float RIPPLE_SPEED = 9.0;
const float RIPPLE_WIDTH = 1.4;
const float RIPPLE_STRENGTH = 2.6;
const float IMPACT_CELLS = 1.6;
const float IMPACT_SECONDS = 0.35;

const float REVEAL_BLOCKS = 60.0;
const float REVEAL_JITTER = 0.25;
const float REVEAL_EDGE = 0.08;
const float REVEAL_TAIL = 0.3;
const float REVEAL_FLASH = 14.0;
const float REVEAL_FLASH_STRENGTH = 1.2;

const float VISIBLE_FROM = 48.0;
const float VISIBLE_TO = 96.0;

const float FADE_FROM = 40.0;
const float FADE_TO = 150.0;

const vec2 HEX_STEP = vec2(1.7320508, 1.0);
const vec2 HEX_HALF = vec2(0.8660254, 0.5);
const vec3 ICE = vec3(0.9, 0.96, 1.0);

float seamless(float delta) {
    return delta - HEX_STEP.x * round(delta / HEX_STEP.x);
}

vec4 hexCell(vec2 p) {
    vec2 centreA = round(p / HEX_STEP);
    vec2 centreB = round((p - HEX_HALF) / HEX_STEP) + 0.5;

    vec2 offsetA = p - centreA * HEX_STEP;
    vec2 offsetB = p - centreB * HEX_STEP;

    return dot(offsetA, offsetA) < dot(offsetB, offsetB)
        ? vec4(offsetA, centreA)
        : vec4(offsetB, centreB);
}

vec3 hexEdge(vec2 offset) {
    vec2 folded = abs(offset);

    float towardsFlat = folded.y;
    float towardsCorner = dot(folded, HEX_HALF);

    vec2 normal = towardsCorner > towardsFlat ? HEX_HALF : vec2(0.0, 1.0);
    return vec3(0.5 - max(towardsFlat, towardsCorner), normal * sign(offset));
}

float hexCorner(vec2 offset) {
    vec2 folded = abs(offset);
    vec2 side = vec2(0.5773503, 0.0);
    vec2 slant = vec2(0.2886751, 0.5);
    return min(length(folded - side), length(folded - slant));
}

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec2 p, float period) {
    vec2 cell = floor(p);
    vec2 inside = fract(p);
    vec2 blend = inside * inside * (3.0 - 2.0 * inside);

    float left = mod(cell.x, period);
    float right = mod(cell.x + 1.0, period);

    float a = hash(vec2(left, cell.y));
    float b = hash(vec2(right, cell.y));
    float c = hash(vec2(left, cell.y + 1.0));
    float d = hash(vec2(right, cell.y + 1.0));

    return mix(mix(a, b, blend.x), mix(c, d, blend.x), blend.y);
}

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

float aurora(vec2 coord, float around, float time) {
    float folds = max(1.0, floor(around / AURORA_CELLS + 0.5));
    float sway = 0.35 * sin(TAU * (coord.y / AURORA_HEIGHT_CELLS + time * AURORA_CYCLES));
    float slow = sin(TAU * (coord.x / around * folds + sway - time * AURORA_CYCLES * 0.5));
    return 0.5 + 0.5 * slow;
}

float frost(vec2 coord, float around, float toEdge) {
    float period = max(1.0, floor(around * FROST_GRAIN + 0.5));
    vec2 grain = vec2(coord.x / around * period, coord.y * FROST_GRAIN);

    float coarse = noise(grain, period);
    float fine = noise(grain * 3.0, period * 3.0);

    float reach = FROST_CELLS * (0.4 + 0.6 * coarse + 0.3 * fine);
    float fringe = 1.0 - smoothstep(0.0, reach, toEdge);

    return fringe * fringe * mix(0.6, 1.0, fine);
}

float glints(vec2 coord, float around, float time, float pixelsPerCell) {
    float period = max(1.0, floor(around * GLINT_GRID + 0.5));
    vec2 grid = vec2(coord.x / around * period, coord.y * GLINT_GRID);

    vec2 cell = floor(grid);
    cell.x = mod(cell.x, period);

    float chance = hash(cell + 71.0);
    if (chance > GLINT_SHARE) {
        return 0.0;
    }

    vec2 spot = vec2(hash(cell + 13.0), hash(cell + 29.0)) * 0.6 + 0.2;
    float reach = length(fract(grid) - spot) / GLINT_GRID;

    float size = max(GLINT_SIZE / GLINT_GRID, GLINT_PIXELS / max(pixelsPerCell, 1.0e-6));
    float shape = exp(-pow(reach / size, 2.0));

    float speed = 0.6 + 0.8 * hash(cell + 47.0);
    float twinkle = pow(pulse(time * GLINT_CYCLES * speed + hash(cell + 5.0)), GLINT_SHARPNESS);

    return shape * twinkle;
}

float ripples(vec2 centre, float around) {
    float total = 0.0;

    for (int slot = 0; slot < RIPPLES; slot++) {
        vec3 ripple = TextureMat[slot].xyz;
        if (ripple.z < 0.0 || ripple.z > RIPPLE_SECONDS) {
            continue;
        }

        vec2 delta = centre - ripple.xy;
        delta.x -= around * round(delta.x / around);
        float reach = length(delta);

        float fade = 1.0 - ripple.z / RIPPLE_SECONDS;
        float ring = exp(-pow((reach - ripple.z * RIPPLE_SPEED) / RIPPLE_WIDTH, 2.0)) * fade;
        float impact = exp(-reach / IMPACT_CELLS) * max(0.0, 1.0 - ripple.z / IMPACT_SECONDS);

        total += ring + impact;
    }

    return total * RIPPLE_STRENGTH;
}

void main() {
    vec4 cell = hexCell(wallCoord);
    vec2 offset = cell.xy;

    float columns = max(TextureMat[3][0], 1.0);
    vec2 id = vec2(mod(cell.z, columns), cell.w);

    vec3 edge = hexEdge(offset);
    float toEdge = edge.x;
    vec2 normal = edge.yz;

    vec2 alongX = vec2(seamless(dFdx(wallCoord.x)), dFdx(wallCoord.y));
    vec2 alongY = vec2(seamless(dFdy(wallCoord.x)), dFdy(wallCoord.y));

    float perPixel = length(vec2(dot(alongX, normal), dot(alongY, normal)));
    float pixelsToEdge = toEdge / max(perPixel, 1.0e-6);

    float cellsPerPixel = max(length(alongX), length(alongY));
    float legible = 1.0 - smoothstep(0.1, 0.6, cellsPerPixel);

    float seed = hash(id);

    float time = GameTime;
    float around = columns * HEX_STEP.x;

    float lineWidth = min(LINE_PIXELS, LINE_CELLS / max(cellsPerPixel, 1.0e-6));
    float line = (1.0 - smoothstep(0.0, lineWidth, pixelsToEdge)) * LINE_STRENGTH;
    float bloom = (1.0 - smoothstep(0.0, GLOW_PIXELS, pixelsToEdge)) * (1.0 - smoothstep(0.0, GLOW_CELLS, toEdge));
    bloom *= GLOW_STRENGTH;
    float rim = exp(-toEdge / RIM_CELLS) * RIM_STRENGTH;

    float shrinking = step(TextureMat[0][3], -0.5);
    float pace = mix(1.0, SHRINK_SWEEP, shrinking);

    float curtain = aurora(wallCoord, around, time);
    float facet = 1.0 + FACET_STRENGTH * (seed * 2.0 - 1.0) + FACET_LIGHT * dot(offset, vec2(-0.35, 0.6));
    float fill = mix(FILL_FAR, FILL, legible) * (1.0 + AURORA_STRENGTH * (curtain * 2.0 - 1.0)) * mix(1.0, facet, legible);

    float close = 1.0 - smoothstep(LINES_NEAR_FROM, LINES_NEAR_TO, distanceToEye);
    float drawn = mix(LINES_FAR, 1.0, close);

    float sweep = pow(pulse(id.y / SWEEP_CELLS - time * SWEEP_CYCLES * pace + seed * 0.06), SWEEP_SHARPNESS);
    sweep *= SWEEP_STRENGTH * legible;

    float bands = max(1.0, floor(around * SHEEN_ACROSS + 0.5));
    float sheen = pow(pulse(wallCoord.x / around * bands + wallCoord.y * SHEEN_UP - time * SHEEN_CYCLES), SHEEN_SHARPNESS);
    sheen *= SHEEN_STRENGTH * legible;

    float wave = ripples(cell.zw * HEX_STEP, columns * HEX_STEP.x) * legible;
    float hot = TextureMat[3][1] * exp(-distanceToEye / HOT_BLOCKS);

    float cellBlocks = TextureMat[3][3];
    float cellHeight = heightFromViewer - (wallCoord.y - cell.w) * cellBlocks;
    float rise = min(abs(cellHeight) / REVEAL_BLOCKS, 1.0) + seed * REVEAL_JITTER;
    float revealed = TextureMat[3][2] * (1.0 + REVEAL_JITTER + REVEAL_EDGE + REVEAL_TAIL) - rise;
    float shown = clamp(revealed / REVEAL_EDGE, 0.0, 1.0);
    float flash = revealed > 0.0 ? exp(-revealed * REVEAL_FLASH) * REVEAL_FLASH_STRENGTH : 0.0;

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float approach = 1.0 - smoothstep(NEAR_BLOCKS, NEAR_FADE, distanceFromViewer);
    float near = 1.0 + NEAR_BOOST * approach;
    float breath = pulse(time * BREATH_CYCLES) * BREATH_STRENGTH * shrinking;

    float visible = 1.0 - smoothstep(VISIBLE_FROM, VISIBLE_TO, distanceFromViewer);
    float strength = standing * near * shown * visible;

    float nearFrost = 1.0 - smoothstep(FROST_NEAR_FROM, FROST_NEAR_TO, distanceToEye);
    float corner = exp(-hexCorner(offset) / CORNER_CELLS) * CORNER_STRENGTH;
    float rime = (frost(wallCoord, around, toEdge) * FROST_STRENGTH + corner) * legible * mix(0.25, 1.0, nearFrost);

    float pixelsPerCell = 1.0 / max(cellsPerPixel, 1.0e-6);
    float nearGlint = 1.0 - smoothstep(GLINT_NEAR_FROM, GLINT_NEAR_TO, distanceToEye);
    float sparkle = glints(wallCoord, around, time, pixelsPerCell) * GLINT_STRENGTH * nearGlint;

    float lift = sweep + wave + flash + breath;

    float body = (fill + HOT_FILL * hot) * (1.0 + lift + sheen) * strength;
    float shine = (bloom + rim) * legible * drawn * (1.0 + lift + HOT_LIGHT * hot) * strength;
    float core = line * legible * drawn * (1.0 + 0.5 * sweep + wave + flash + HOT_LIGHT * hot) * strength;
    float ice = (rime * (1.0 + 0.5 * lift) + sparkle) * strength;

    vec3 tint = ColorModulator.rgb;
    vec3 lineColour = mix(tint, vec3(1.0), LINE_WHITE);
    vec3 frostColour = mix(tint, ICE, FROST_WHITE);

    float total = body + shine + core + ice;
    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 colour = (tint * FILL_SHADE * body + tint * shine + lineColour * core + frostColour * ice) / total;
    float alpha = clamp(total, 0.0, 1.0) * ColorModulator.a;

    fragColor = vec4(colour, alpha);
}
