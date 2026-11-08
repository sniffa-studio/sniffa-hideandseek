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

const float BODY = 0.07;
const float EYE_STRENGTH = 0.14;
const float EYE_BLOCKS = 7.0;

const float GUST_CELLS = 2.4;
const float GUST_STRETCH = 5.0;
const float GUST_CYCLES = 300.0;
const float GUST_STRENGTH = 0.38;

const float FLAKE_CELLS = 0.5;
const float FLAKE_SHARE = 0.4;
const float FLAKE_CYCLES = 900.0;
const float FLAKE_FALL = 260.0;
const float FLAKE_PIXELS = 1.4;
const float FLAKE_STRENGTH = 1.0;

const float WARN_CELLS = 3.0;
const float WARN_SHARE = 0.18;
const float WARN_CYCLES = 120.0;
const float WARN_STRENGTH = 0.28;

const float LINES_NEAR_FROM = 6.0;
const float LINES_NEAR_TO = 26.0;
const float LINES_FAR = 0.3;

const float CALM_CYCLES = 600.0;
const float URGENT_CYCLES = 2400.0;
const float URGENT_SECONDS = 10.0;
const float PULSE_STRENGTH = 0.25;

const float WHITE_HOT = 0.4;
const float WHITE_HOT_SECONDS = 5.0;

const float FADE_FROM = 26.0;
const float FADE_TO = 95.0;

const float VISIBLE_FROM = 48.0;
const float VISIBLE_TO = 96.0;

const vec3 SNOW = vec3(0.95, 0.97, 1.0);

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(vec2(mod(i.x, period), i.y));
    float b = hash(vec2(mod(i.x + 1.0, period), i.y));
    float c = hash(vec2(mod(i.x, period), i.y + 1.0));
    float d = hash(vec2(mod(i.x + 1.0, period), i.y + 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float band(float phase, float share, float soft) {
    float into = fract(phase);
    return smoothstep(0.0, soft, into) * (1.0 - smoothstep(share - soft, share, into));
}

float gusts(vec2 coord, float around, float time) {
    float columns = max(1.0, floor(around / GUST_CELLS + 0.5));
    float swirl = 0.6 * sin(TAU * (coord.y / (GUST_CELLS * 3.0) + time * GUST_CYCLES * 0.3));
    vec2 p = vec2(coord.x / around * columns + swirl - time * GUST_CYCLES * 0.02 * columns, coord.y / GUST_CELLS * GUST_STRETCH);
    float slow = noise(vec2(p.x, p.y / GUST_STRETCH), columns);
    float fine = noise(vec2(p.x * 2.0, p.y / GUST_STRETCH * 2.0), columns * 2.0);
    return smoothstep(0.45, 0.85, slow * 0.65 + fine * 0.35);
}

float flakes(vec2 coord, float around, float time, float share, float perPixel) {
    float columns = max(1.0, floor(around / FLAKE_CELLS + 0.5));
    vec2 grid = vec2(coord.x / around * columns - time * FLAKE_CYCLES * 0.05 * columns, coord.y / FLAKE_CELLS + time * FLAKE_FALL);
    vec2 id = vec2(mod(floor(grid.x), columns), floor(grid.y));
    vec2 at = vec2(hash(id + 3.0), hash(id + 9.0));
    vec2 from = (fract(grid) - at) * FLAKE_CELLS;
    float pixels = length(from) / max(perPixel, 1.0e-6);
    return step(hash(id + 21.0), share) * (1.0 - smoothstep(0.0, FLAKE_PIXELS, pixels));
}

void main() {
    float columns = max(TextureMat[3][0], 1.0);
    float around = columns * PERIOD_CELLS;
    float time = GameTime;

    float dx = dFdx(wallCoord.x);
    float dy = dFdy(wallCoord.x);
    dx -= around * round(dx / around);
    dy -= around * round(dy / around);
    float perPixel = max(length(vec2(dx, dFdx(wallCoord.y))), length(vec2(dy, dFdy(wallCoord.y))));
    float legible = 1.0 - smoothstep(0.1, 0.6, perPixel);

    float secondsLeft = TextureMat[0][0];
    float urgent = 1.0 - smoothstep(URGENT_SECONDS * 0.5, URGENT_SECONDS, secondsLeft);
    float beat = pulse(time * mix(CALM_CYCLES, URGENT_CYCLES, urgent));
    float throb = 1.0 + PULSE_STRENGTH * (0.4 + 0.6 * urgent) * beat;
    float whiteHot = (1.0 - smoothstep(0.0, WHITE_HOT_SECONDS, secondsLeft)) * WHITE_HOT;

    float close = 1.0 - smoothstep(LINES_NEAR_FROM, LINES_NEAR_TO, distanceToEye);
    float drawn = mix(LINES_FAR, 1.0, close);

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float visible = 1.0 - smoothstep(VISIBLE_FROM, VISIBLE_TO, distanceFromViewer);
    float eyeline = exp(-abs(heightFromViewer) / EYE_BLOCKS) * EYE_STRENGTH;

    float storm = gusts(wallCoord, around, time) * GUST_STRENGTH * (1.0 + 0.6 * urgent) * mix(0.5, 1.0, legible);
    float snow = flakes(wallCoord, around, time, mix(FLAKE_SHARE, FLAKE_SHARE * 1.6, urgent), perPixel) * FLAKE_STRENGTH * legible * drawn;

    float warnings = max(1.0, floor(around / WARN_CELLS + 0.5));
    float phase = (wallCoord.x + wallCoord.y * 0.5) / (around / warnings) - time * WARN_CYCLES;
    float warn = band(phase, WARN_SHARE, clamp(fwidth(phase) * 1.5, 0.002, 0.25)) * WARN_STRENGTH * legible;

    float red = (BODY + eyeline + warn) * throb;
    float white = storm * throb + snow;

    float strength = standing * visible;
    float total = (red + white) * strength;

    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 colour = (mix(tint, vec3(1.0), whiteHot) * red + SNOW * white) / max(red + white, 1.0e-5);

    fragColor = vec4(colour, clamp(total, 0.0, 1.0) * ColorModulator.a);
}
