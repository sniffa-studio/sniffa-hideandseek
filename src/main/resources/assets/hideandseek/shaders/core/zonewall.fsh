#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;

out vec4 fragColor;

const float TAU = 6.2831853;
const float PERIOD_CELLS = 1.7320508;

const float BODY = 0.08;
const float EYE_STRENGTH = 0.14;
const float EYE_BLOCKS = 7.0;

const float STRIPE_CELLS = 3.0;
const float STRIPE_SHARE = 0.5;
const float STRIPE_STRENGTH = 0.05;
const float STRIPE_CYCLES = 120.0;

const float LINE_PIXELS = 2.0;
const float LINE_CELLS = 0.05;
const float LINE_STRENGTH = 0.85;
const float GLOW_PIXELS = 12.0;
const float GLOW_CELLS = 0.18;
const float GLOW_STRENGTH = 0.3;

const float LINES_NEAR_FROM = 6.0;
const float LINES_NEAR_TO = 26.0;
const float LINES_FAR = 0.25;

const float SCAN_CELLS = 0.5;
const float SCAN_STRENGTH = 0.03;
const float SCAN_CYCLES = 900.0;

const float CALM_CYCLES = 600.0;
const float URGENT_CYCLES = 2400.0;
const float URGENT_SECONDS = 10.0;
const float PULSE_STRENGTH = 0.25;
const float URGENT_LINE_BEAT = 0.8;

const float WHITE = 0.35;
const float WHITE_HOT = 0.4;
const float WHITE_HOT_SECONDS = 5.0;

const float EMBER_CELLS = 0.85;
const float EMBER_SHARE = 0.16;
const float EMBER_URGENT_SHARE = 0.34;
const float EMBER_CYCLES = 700.0;
const float EMBER_RADIUS = 0.11;
const float EMBER_STRENGTH = 0.8;
const float EMBER_WHITE = 0.6;

const float FADE_FROM = 26.0;
const float FADE_TO = 95.0;

const float VISIBLE_FROM = 48.0;
const float VISIBLE_TO = 96.0;

const vec2 HEX_STEP = vec2(1.7320508, 1.0);
const vec2 HEX_HALF = vec2(0.8660254, 0.5);

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

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

float embers(vec2 coord, float around, float share) {
    float columns = max(1.0, floor(around / EMBER_CELLS + 0.5));
    vec2 grid = vec2(coord.x / around * columns, coord.y / EMBER_CELLS);
    vec2 id = vec2(mod(floor(grid.x), columns), floor(grid.y));
    vec2 inside = fract(grid);

    float seed = hash(id);
    float speed = 0.6 + 0.8 * hash(id + 9.0);
    float rise = fract(seed * 7.31 + GameTime * EMBER_CYCLES * speed);
    vec2 centre = vec2(0.25 + 0.5 * hash(id + 3.0), rise);

    float reach = length((inside - centre) * vec2(columns / around * EMBER_CELLS, 1.0));
    float soft = clamp(fwidth(grid.y) * 1.5, 1.0e-4, 0.2);
    float spark = 1.0 - smoothstep(EMBER_RADIUS - soft, EMBER_RADIUS + soft, reach);
    return spark * sin(3.1415927 * rise) * step(seed, share);
}

float band(float phase, float share, float soft) {
    float into = fract(phase);
    return smoothstep(0.0, soft, into) * (1.0 - smoothstep(share - soft, share, into));
}

void main() {
    float columns = max(TextureMat[3][0], 1.0);
    float around = columns * PERIOD_CELLS;

    vec4 cell = hexCell(wallCoord);
    vec3 edge = hexEdge(cell.xy);
    float toEdge = edge.x;
    vec2 normal = edge.yz;

    vec2 alongX = vec2(seamless(dFdx(wallCoord.x)), dFdx(wallCoord.y));
    vec2 alongY = vec2(seamless(dFdy(wallCoord.x)), dFdy(wallCoord.y));

    float perPixel = length(vec2(dot(alongX, normal), dot(alongY, normal)));
    float pixelsToEdge = toEdge / max(perPixel, 1.0e-6);

    float cellsPerPixel = max(length(alongX), length(alongY));
    float legible = 1.0 - smoothstep(0.1, 0.6, cellsPerPixel);

    float lineWidth = min(LINE_PIXELS, LINE_CELLS / max(cellsPerPixel, 1.0e-6));
    float line = (1.0 - smoothstep(0.0, lineWidth, pixelsToEdge)) * LINE_STRENGTH;
    float glow = (1.0 - smoothstep(0.0, GLOW_PIXELS, pixelsToEdge)) * (1.0 - smoothstep(0.0, GLOW_CELLS, toEdge))
               * GLOW_STRENGTH;

    float stripes = max(1.0, floor(around / STRIPE_CELLS + 0.5));
    float phase = (wallCoord.x + wallCoord.y) / (around / stripes) - GameTime * STRIPE_CYCLES;
    float stripe = band(phase, STRIPE_SHARE, clamp(fwidth(phase) * 1.5, 0.002, 0.25));

    float scan = band(wallCoord.y / SCAN_CELLS - GameTime * SCAN_CYCLES, 0.15, 0.05) * SCAN_STRENGTH;

    float secondsLeft = TextureMat[0][0];
    float urgent = 1.0 - smoothstep(URGENT_SECONDS * 0.5, URGENT_SECONDS, secondsLeft);
    float beat = pulse(GameTime * mix(CALM_CYCLES, URGENT_CYCLES, urgent));
    float throb = 1.0 + PULSE_STRENGTH * (0.4 + 0.6 * urgent) * beat;
    float whiteHot = (1.0 - smoothstep(0.0, WHITE_HOT_SECONDS, secondsLeft)) * WHITE_HOT;

    float close = 1.0 - smoothstep(LINES_NEAR_FROM, LINES_NEAR_TO, distanceToEye);
    float drawn = mix(LINES_FAR, 1.0, close) * (1.0 + URGENT_LINE_BEAT * urgent * beat);

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float visible = 1.0 - smoothstep(VISIBLE_FROM, VISIBLE_TO, distanceFromViewer);
    float eyeline = exp(-abs(heightFromViewer) / EYE_BLOCKS) * EYE_STRENGTH;

    float ember = embers(wallCoord, around, mix(EMBER_SHARE, EMBER_URGENT_SHARE, urgent))
        * EMBER_STRENGTH * legible * (0.6 + 0.6 * urgent);

    float body = (BODY + eyeline + STRIPE_STRENGTH * stripe * legible + scan) * throb;
    float lines = (line + glow) * legible * drawn * throb;

    float strength = standing * visible;
    float total = (body + lines + ember) * strength;

    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 colour = (tint * body
                 + mix(tint, vec3(1.0), WHITE + whiteHot) * lines
                 + mix(tint, vec3(1.0), EMBER_WHITE) * ember)
                / max(body + lines + ember, 1.0e-5);

    fragColor = vec4(colour, clamp(total, 0.0, 1.0) * ColorModulator.a);
}
