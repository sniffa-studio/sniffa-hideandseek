#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;
in vec3 viewPos;
in vec3 sphereNormal;
in vec3 shardPos;

out vec4 fragColor;

const float TAU = 6.2831853;
const float PERIOD_CELLS = 1.7320508;

const float SHARD_CELLS = 7.8;
const float SHARD_JITTER = 0.85;

const float EDGE_PIXELS = 1.6;
const float EDGE_CELLS = 0.035;
const float EDGE_STRENGTH = 0.85;
const float EDGE_GLOW_CELLS = 0.03;
const float EDGE_GLOW_STRENGTH = 0.32;

const float FILL = 0.07;
const float FILL_FAR = 0.035;
const float FACET_STRENGTH = 0.55;
const float FACET_SPREAD = 0.35;

const float FROST_CELLS = 0.16;
const float FROST_EDGE = 0.03;
const float FROST_BELOW_FROM = 0.5;
const float FROST_BELOW_TO = 6.0;
const float FROST_BELOW = 0.35;
const float FROST_STRENGTH = 0.24;

const float SHIMMER_CELLS = 30.0;
const float SHIMMER_CYCLES = 35.0;
const float SHIMMER_WIDTH = 0.06;
const float SHIMMER_STRENGTH = 0.5;

const float SPARKLE_SHARE = 0.22;
const float SPARKLE_CYCLES = 900.0;
const float SPARKLE_PIXELS = 2.4;
const float SPARKLE_STRENGTH = 1.6;

const float SNOW_CELLS = 0.55;
const float SNOW_CYCLES = 260.0;
const float SNOW_SHARE = 0.32;
const float SNOW_PIXELS = 1.3;
const float SNOW_STRENGTH = 0.9;

const float LINES_NEAR_FROM = 10.0;
const float LINES_NEAR_TO = 34.0;
const float LINES_FAR = 0.1;

const float BREATH_CYCLES = 600.0;
const float BREATH_STRENGTH = 0.25;
const float CREEP_CYCLES = 40.0;
const float CREEP_SHRINK = 0.35;

const float NEAR_BLOCKS = 6.0;
const float NEAR_FADE = 56.0;
const float NEAR_BOOST = 0.4;

const float HOT_BLOCKS = 7.0;
const float HOT_FROST = 0.3;
const float HOT_LIGHT = 1.6;

const int RIPPLES = 3;
const float CRACK_SECONDS = 1.6;
const float CRACK_SPEED = 5.0;
const float CRACK_REACH = 2.2;
const float CRACK_ARMS = 7.0;
const float CRACK_WIDTH = 0.012;
const float CRACK_STRENGTH = 1.0;
const float BURST_CELLS = 0.7;
const float BURST_SECONDS = 0.4;

const float REVEAL_BLOCKS = 60.0;
const float REVEAL_JITTER = 0.25;
const float REVEAL_EDGE = 0.08;
const float REVEAL_TAIL = 0.3;
const float REVEAL_FLASH = 14.0;
const float REVEAL_FLASH_STRENGTH = 1.2;

const float VISIBLE_FROM = 260.0;
const float VISIBLE_TO = 700.0;

const float BEVEL_BRIGHT = 1.0;
const float BEVEL_DARK = 0.35;

const float CLEAR_FROM = 50.0;
const float CLEAR_TO = 180.0;

const float MILKY_SHARE = 0.3;
const float MILKY_STRENGTH = 0.22;

const float HAIRLINE_SHARE = 0.6;
const float HAIRLINE_SCALE = 5.0;
const float HAIRLINE_STRENGTH = 0.35;

const float DEPTH_CELLS = 0.3;

const float STREAK_WIDTH = 0.2;
const float STREAK_CYCLES = 8.0;
const float STREAK_STRENGTH = 0.14;

const float RIM_POWER = 2.6;
const float RIM_STRENGTH = 0.32;

const vec3 LIGHT = vec3(-0.55, 0.75, 0.35);
const vec3 SHIMMER_DIRECTION = vec3(0.85, 0.3, 0.45);
const vec3 STREAK_DIRECTION = vec3(0.8, 1.0, 0.3);

const vec3 ICE = vec3(0.72, 0.9, 1.0);
const vec3 ICE_DEEP = vec3(0.38, 0.66, 0.95);
const vec3 FROST = vec3(0.93, 0.97, 1.0);
const float TINT_SHARE = 0.22;

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float hash3(vec3 id) {
    vec3 p = fract(id * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 id) {
    return vec3(hash3(id), hash3(id + vec3(17.13, 41.71, 7.37)), hash3(id + vec3(63.7, 11.3, 29.9)));
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float a = mix(hash3(i), hash3(i + vec3(1.0, 0.0, 0.0)), u.x);
    float b = mix(hash3(i + vec3(0.0, 1.0, 0.0)), hash3(i + vec3(1.0, 1.0, 0.0)), u.x);
    float c = mix(hash3(i + vec3(0.0, 0.0, 1.0)), hash3(i + vec3(1.0, 0.0, 1.0)), u.x);
    float d = mix(hash3(i + vec3(0.0, 1.0, 1.0)), hash3(i + vec3(1.0, 1.0, 1.0)), u.x);
    return mix(mix(a, b, u.y), mix(c, d, u.y), u.z);
}

float frostNoise(vec3 p) {
    float total = 0.0;
    float weight = 0.5;
    for (int octave = 0; octave < 3; octave++) {
        total += noise3(p) * weight;
        p *= 2.0;
        weight *= 0.5;
    }
    return total / 0.875;
}

vec3 site(vec3 id, float time, float creep) {
    vec3 base = hash33(id);
    vec3 drift = creep * vec3(sin(TAU * (time * CREEP_CYCLES + base.x)),
                              cos(TAU * (time * CREEP_CYCLES + base.y)),
                              sin(TAU * (time * CREEP_CYCLES + base.z)));
    return 0.5 + (base - 0.5) * SHARD_JITTER + drift * 0.12;
}

vec4 shards(vec3 p, float time, float creep, out vec3 fromSite, out vec3 edgeNormal) {
    vec3 cell = floor(p);
    vec3 inside = fract(p);

    vec3 nearestOffset = vec3(0.0);
    vec3 nearestId = vec3(0.0);
    float nearest = 8.0;

    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 neighbour = vec3(float(x), float(y), float(z));
                vec3 offset = neighbour + site(cell + neighbour, time, creep) - inside;
                float reach = dot(offset, offset);
                if (reach < nearest) {
                    nearest = reach;
                    nearestOffset = offset;
                    nearestId = cell + neighbour;
                }
            }
        }
    }

    float toEdge = 8.0;
    edgeNormal = vec3(0.0, 1.0, 0.0);
    vec3 nearestCell = nearestId;
    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 id = nearestCell + vec3(float(x), float(y), float(z));
                vec3 offset = id + site(id, time, creep) - cell - inside;
                vec3 between = offset - nearestOffset;
                if (dot(between, between) > 1.0e-5) {
                    vec3 towards = normalize(between);
                    float reach = dot(0.5 * (nearestOffset + offset), towards);
                    if (reach < toEdge) {
                        toEdge = reach;
                        edgeNormal = towards;
                    }
                }
            }
        }
    }

    fromSite = nearestOffset;
    return vec4(toEdge, hash3(nearestId), 0.0, 0.0);
}

float cracks(vec2 point, float around, float perPixel) {
    float total = 0.0;

    for (int slot = 0; slot < RIPPLES; slot++) {
        vec3 ripple = TextureMat[slot].xyz;
        if (ripple.z < 0.0 || ripple.z > CRACK_SECONDS) {
            continue;
        }

        vec2 delta = point - ripple.xy;
        delta.x -= around * round(delta.x / around);
        float reach = length(delta);

        float grown = min(ripple.z * CRACK_SPEED, CRACK_REACH);
        float fade = 1.0 - ripple.z / CRACK_SECONDS;

        float seed = hash(ripple.xy);
        float angle = atan(delta.y, delta.x) / TAU * CRACK_ARMS + seed * 7.0;
        float wobble = 0.04 * sin(reach * 6.0 + seed * 20.0);
        float off = 0.5 - abs(fract(angle + wobble) - 0.5);
        float apart = reach * sin(off * TAU / CRACK_ARMS);
        float width = max(CRACK_WIDTH, perPixel * 1.2);
        float line = 1.0 - smoothstep(width * 0.5, width, apart);
        line *= 1.0 - smoothstep(grown * 0.85, grown, reach);
        line *= 1.0 - smoothstep(0.0, CRACK_REACH, reach) * 0.6;

        float burst = 0.5 * exp(-reach / BURST_CELLS) * max(0.0, 1.0 - ripple.z / BURST_SECONDS);

        total += (line + burst) * fade;
    }

    return total * CRACK_STRENGTH;
}

void main() {
    float columns = max(TextureMat[3][0], 1.0);
    float around = columns * PERIOD_CELLS;
    float time = GameTime;

    float shrinking = step(TextureMat[0][3], -0.5);
    float creep = shrinking * CREEP_SHRINK;

    vec3 p = shardPos / SHARD_CELLS;

    vec3 fromSite;
    vec3 edgeNormal;
    vec4 shard = shards(p, time, creep, fromSite, edgeNormal);
    float toEdge = shard.x;
    float seed = shard.y;

    float perPixel = max(length(dFdx(p)), length(dFdy(p)));
    float pixelsToEdge = toEdge / max(perPixel, 1.0e-6);

    float cellsPerPixel = perPixel * SHARD_CELLS;
    float legible = 1.0 - smoothstep(0.1, 0.6, cellsPerPixel);

    float close = 1.0 - smoothstep(LINES_NEAR_FROM, LINES_NEAR_TO, distanceToEye);
    float drawn = mix(LINES_FAR, 1.0, close);

    vec3 light = normalize(LIGHT);
    float edgeWidth = min(EDGE_PIXELS, EDGE_CELLS / max(perPixel, 1.0e-6));
    float bevel = mix(BEVEL_DARK, BEVEL_BRIGHT, 0.5 + 0.5 * dot(edgeNormal, light));
    float edgeVary = 0.55 + 0.45 * noise3(shardPos * 1.5);

    float edge = (1.0 - smoothstep(0.0, edgeWidth, pixelsToEdge)) * EDGE_STRENGTH * bevel * edgeVary;
    float edgeGlow = (1.0 - smoothstep(0.0, EDGE_GLOW_CELLS, toEdge)) * EDGE_GLOW_STRENGTH * bevel;
    float facet = 0.5 + 0.5 * dot(normalize(-fromSite + 1.0e-4), light) * min(length(fromSite) * 2.0, 1.0);
    float shade = mix(1.0 - FACET_SPREAD, 1.0 + FACET_SPREAD, seed) * mix(0.7, 1.15, facet);

    float hot = TextureMat[3][1] * exp(-distanceToEye / HOT_BLOCKS);
    float below = smoothstep(FROST_BELOW_FROM, FROST_BELOW_TO, -heightFromViewer) * FROST_BELOW;
    float frostField = frostNoise(shardPos / FROST_CELLS);
    float nearEdge = 1.0 - smoothstep(0.0, FROST_EDGE, toEdge);
    float frostAmount = clamp(nearEdge * 0.75 + below + HOT_FROST * hot, 0.0, 1.0) * mix(0.55, 1.0, nearEdge);
    float frost = smoothstep(1.0 - frostAmount, 1.0 - frostAmount * 0.4, frostField) * FROST_STRENGTH * legible;

    float seedMilky = hash(vec2(seed * 57.3, 3.7));
    float milky = smoothstep(1.0 - MILKY_SHARE, 1.0, seedMilky) * MILKY_STRENGTH * (0.6 + 0.4 * frostField) * legible;

    float hairline = 0.0;
    if (hash(vec2(seed * 31.1, 9.2)) < HAIRLINE_SHARE) {
        vec3 hairFrom;
        vec3 hairNormal;
        vec4 hair = shards(p * HAIRLINE_SCALE + vec3(0.0, seed * 17.0, 0.0), time, 0.0, hairFrom, hairNormal);
        float hairPixels = hair.x / max(perPixel * HAIRLINE_SCALE, 1.0e-6);
        hairline = (1.0 - smoothstep(0.0, 1.0, hairPixels)) * HAIRLINE_STRENGTH * legible * close;
    }

    float thickness = smoothstep(0.0, DEPTH_CELLS, toEdge);

    float streakPhase = dot(p, STREAK_DIRECTION) / 3.0 + seed * 0.3 - time * STREAK_CYCLES;
    float streak = exp(-pow((fract(streakPhase) - 0.5) / STREAK_WIDTH, 2.0)) * STREAK_STRENGTH * thickness * legible;

    float band = fract(dot(shardPos, normalize(SHIMMER_DIRECTION)) / SHIMMER_CELLS - time * SHIMMER_CYCLES);
    float shimmer = exp(-pow((band - 0.5) / SHIMMER_WIDTH, 2.0)) * SHIMMER_STRENGTH * facet * legible;

    float twinkleSeed = hash(vec2(seed * 91.7, floor(time * SPARKLE_CYCLES + seed * 13.0)));
    float twinkle = step(1.0 - SPARKLE_SHARE, twinkleSeed);
    float twinklePhase = fract(time * SPARKLE_CYCLES + seed * 13.0);
    float twinkleSize = SPARKLE_PIXELS * sin(3.1415927 * twinklePhase);
    float sparklePixels = length(fromSite) / max(perPixel, 1.0e-6);
    float sparkle = twinkle * (1.0 - smoothstep(0.0, max(twinkleSize, 0.01), sparklePixels)) * SPARKLE_STRENGTH * legible * close;

    vec3 snowGrid = shardPos / SNOW_CELLS + vec3(0.0, time * SNOW_CYCLES, 0.0);
    vec3 snowId = floor(snowGrid);
    vec3 snowFrom = (fract(snowGrid) - hash33(snowId + 5.0)) * SNOW_CELLS;
    float snowPerPixel = perPixel * SHARD_CELLS;
    float flake = step(hash3(snowId + 23.0), SNOW_SHARE) * (1.0 - smoothstep(0.0, SNOW_PIXELS, length(snowFrom) / max(snowPerPixel, 1.0e-6)));
    flake *= SNOW_STRENGTH * legible * close;

    float crack = cracks(vec2(wallCoord.x, wallCoord.y), around, snowPerPixel) * legible;

    float cellBlocks = TextureMat[3][3];
    float shardHeight = heightFromViewer - fromSite.y * SHARD_CELLS * cellBlocks;
    float rise = min(abs(shardHeight) / REVEAL_BLOCKS, 1.0) + seed * REVEAL_JITTER;
    float revealed = TextureMat[3][2] * (1.0 + REVEAL_JITTER + REVEAL_EDGE + REVEAL_TAIL) - rise;
    float shown = clamp(revealed / REVEAL_EDGE, 0.0, 1.0);
    float flash = revealed > 0.0 ? exp(-revealed * REVEAL_FLASH) * REVEAL_FLASH_STRENGTH : 0.0;

    float facing = abs(dot(normalize(viewPos), sphereNormal));
    float rim = pow(1.0 - facing, RIM_POWER) * RIM_STRENGTH;

    float near = 1.0 + NEAR_BOOST * (1.0 - smoothstep(NEAR_BLOCKS, NEAR_FADE, distanceFromViewer));
    float breath = (0.5 + 0.5 * sin(TAU * time * BREATH_CYCLES)) * BREATH_STRENGTH * shrinking;
    float visible = 1.0 - smoothstep(VISIBLE_FROM, VISIBLE_TO, distanceToEye);
    float strength = near * shown * visible;

    float lift = breath + flash + HOT_LIGHT * hot * 0.5;

    float glass = mix(FILL_FAR, FILL, legible) * shade * (1.0 + FACET_STRENGTH * (facet - 0.5)) * (1.0 + lift) * strength;
    float clearing = 1.0 - smoothstep(CLEAR_FROM, CLEAR_TO, distanceToEye);
    float rime = ((frost + milky) * clearing + flash * 0.6) * strength;
    float glint = (edgeGlow * legible * drawn + shimmer + streak + rim) * (1.0 + lift) * strength;
    float bright = (edge * legible * drawn * (1.0 + lift) + hairline + sparkle + flake + crack) * strength;

    float total = glass + rime + glint + bright;
    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 ice = mix(ICE, tint, TINT_SHARE);
    vec3 deep = mix(ICE_DEEP, tint, TINT_SHARE * 1.5);
    vec3 glassColour = mix(ice, mix(deep, ice, facet), thickness);

    vec3 colour = (glassColour * glass + FROST * rime + ice * glint + vec3(1.0) * bright) / total;
    float alpha = clamp(total, 0.0, 1.0) * ColorModulator.a;

    fragColor = vec4(colour, alpha);
}
