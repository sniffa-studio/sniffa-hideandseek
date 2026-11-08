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

const float SHARD_CELLS = 1.15;
const float SHARD_JITTER = 0.85;

const float EDGE_PIXELS = 1.6;
const float EDGE_CELLS = 0.035;
const float EDGE_STRENGTH = 0.85;
const float EDGE_GLOW_CELLS = 0.16;
const float EDGE_GLOW_STRENGTH = 0.32;

const float FILL = 0.07;
const float FILL_FAR = 0.04;
const float FACET_STRENGTH = 0.55;
const float FACET_SPREAD = 0.35;

const float FROST_CELLS = 0.16;
const float FROST_EDGE = 0.14;
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
const float LINES_FAR = 0.22;

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

const float VISIBLE_FROM = 48.0;
const float VISIBLE_TO = 96.0;

const float FADE_FROM = 40.0;
const float FADE_TO = 150.0;

const vec3 ICE = vec3(0.72, 0.9, 1.0);
const vec3 ICE_DEEP = vec3(0.38, 0.66, 0.95);
const vec3 FROST = vec3(0.93, 0.97, 1.0);
const float TINT_SHARE = 0.22;

float seamless(float delta, float around) {
    return delta - around * round(delta / around);
}

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec2 hash2(vec2 id) {
    return vec2(hash(id), hash(id + vec2(17.13, 41.71)));
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

float frostNoise(vec2 p, float period) {
    float total = 0.0;
    float weight = 0.5;
    for (int octave = 0; octave < 3; octave++) {
        total += noise(p, period) * weight;
        p *= 2.0;
        period *= 2.0;
        weight *= 0.5;
    }
    return total / 0.875;
}

vec2 site(vec2 id, float columns, float time, float creep) {
    vec2 wrapped = vec2(mod(id.x, columns), id.y);
    vec2 base = hash2(wrapped);
    vec2 drift = creep * vec2(sin(TAU * (time * CREEP_CYCLES + base.x)), cos(TAU * (time * CREEP_CYCLES + base.y)));
    return 0.5 + (base - 0.5) * SHARD_JITTER + drift * 0.12;
}

vec4 shards(vec2 p, float columns, float time, float creep) {
    vec2 cell = floor(p);
    vec2 inside = fract(p);

    vec2 nearestOffset = vec2(0.0);
    vec2 nearestId = vec2(0.0);
    float nearest = 8.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbour = vec2(float(x), float(y));
            vec2 offset = neighbour + site(cell + neighbour, columns, time, creep) - inside;
            float reach = dot(offset, offset);
            if (reach < nearest) {
                nearest = reach;
                nearestOffset = offset;
                nearestId = cell + neighbour;
            }
        }
    }

    float toEdge = 8.0;
    for (int y = -2; y <= 2; y++) {
        for (int x = -2; x <= 2; x++) {
            vec2 neighbour = vec2(float(x), float(y));
            vec2 offset = neighbour + site(cell + neighbour, columns, time, creep) - inside;
            vec2 between = offset - nearestOffset;
            if (dot(between, between) > 1.0e-5) {
                toEdge = min(toEdge, dot(0.5 * (nearestOffset + offset), normalize(between)));
            }
        }
    }

    return vec4(nearestOffset, toEdge, hash(vec2(mod(nearestId.x, columns), nearestId.y)));
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

    float shardColumns = max(1.0, floor(around / SHARD_CELLS + 0.5));
    vec2 scale = vec2(shardColumns / around, 1.0 / SHARD_CELLS);
    vec2 p = wallCoord * scale;

    vec4 shard = shards(p, shardColumns, time, creep);
    vec2 fromSite = shard.xy;
    float toEdge = shard.z;
    float seed = shard.w;

    vec2 alongX = vec2(seamless(dFdx(wallCoord.x), around), dFdx(wallCoord.y)) * scale;
    vec2 alongY = vec2(seamless(dFdy(wallCoord.x), around), dFdy(wallCoord.y)) * scale;
    float perPixel = max(length(alongX), length(alongY));
    float pixelsToEdge = toEdge / max(perPixel, 1.0e-6);

    float cellsPerPixel = perPixel * SHARD_CELLS;
    float legible = 1.0 - smoothstep(0.1, 0.6, cellsPerPixel);

    float close = 1.0 - smoothstep(LINES_NEAR_FROM, LINES_NEAR_TO, distanceToEye);
    float drawn = mix(LINES_FAR, 1.0, close);

    float edgeWidth = min(EDGE_PIXELS, EDGE_CELLS / max(perPixel, 1.0e-6));
    float edge = (1.0 - smoothstep(0.0, edgeWidth, pixelsToEdge)) * EDGE_STRENGTH;
    float edgeGlow = (1.0 - smoothstep(0.0, EDGE_GLOW_CELLS, toEdge)) * EDGE_GLOW_STRENGTH;

    vec2 light = normalize(vec2(-0.6, 0.8));
    float facet = 0.5 + 0.5 * dot(normalize(-fromSite + 1.0e-4), light) * min(length(fromSite) * 2.0, 1.0);
    float shade = mix(1.0 - FACET_SPREAD, 1.0 + FACET_SPREAD, seed) * mix(0.7, 1.15, facet);

    float hot = TextureMat[3][1] * exp(-distanceToEye / HOT_BLOCKS);
    float below = smoothstep(FROST_BELOW_FROM, FROST_BELOW_TO, -heightFromViewer) * FROST_BELOW;
    float frostField = frostNoise(vec2(wallCoord.x / FROST_CELLS, wallCoord.y / FROST_CELLS), floor(around / FROST_CELLS + 0.5));
    float nearEdge = 1.0 - smoothstep(0.0, FROST_EDGE, toEdge);
    float frostAmount = clamp(nearEdge * 0.75 + below + HOT_FROST * hot, 0.0, 1.0) * mix(0.55, 1.0, nearEdge);
    float frost = smoothstep(1.0 - frostAmount, 1.0 - frostAmount * 0.4, frostField) * FROST_STRENGTH * legible;

    float folds = max(1.0, floor(around / SHIMMER_CELLS + 0.5));
    float band = fract((wallCoord.x + wallCoord.y * 0.35) / around * folds - time * SHIMMER_CYCLES);
    float shimmer = exp(-pow((band - 0.5) / SHIMMER_WIDTH, 2.0)) * SHIMMER_STRENGTH * facet * legible;

    float twinkleSeed = hash(vec2(seed * 91.7, floor(time * SPARKLE_CYCLES + seed * 13.0)));
    float twinkle = step(1.0 - SPARKLE_SHARE, twinkleSeed);
    float twinklePhase = fract(time * SPARKLE_CYCLES + seed * 13.0);
    float twinkleSize = SPARKLE_PIXELS * sin(3.1415927 * twinklePhase);
    float sparklePixels = length(fromSite) / max(perPixel, 1.0e-6);
    float sparkle = twinkle * (1.0 - smoothstep(0.0, max(twinkleSize, 0.01), sparklePixels)) * SPARKLE_STRENGTH * legible * close;

    float snowColumns = max(1.0, floor(around / SNOW_CELLS + 0.5));
    vec2 snowGrid = vec2(wallCoord.x / around * snowColumns, wallCoord.y / SNOW_CELLS + time * SNOW_CYCLES);
    vec2 snowId = vec2(mod(floor(snowGrid.x), snowColumns), floor(snowGrid.y));
    vec2 snowAt = hash2(snowId + 5.0);
    vec2 snowFrom = (fract(snowGrid) - snowAt) * vec2(SNOW_CELLS, SNOW_CELLS);
    float flake = step(hash(snowId + 23.0), SNOW_SHARE) * (1.0 - smoothstep(0.0, SNOW_PIXELS, length(snowFrom) / max(perPixel * SHARD_CELLS, 1.0e-6)));
    flake *= SNOW_STRENGTH * legible * close;

    float crack = cracks(vec2(wallCoord.x, wallCoord.y), around, length(vec2(seamless(dFdx(wallCoord.x), around), dFdx(wallCoord.y)))) * legible;

    float cellBlocks = TextureMat[3][3];
    float shardHeight = heightFromViewer - fromSite.y * SHARD_CELLS * cellBlocks;
    float rise = min(abs(shardHeight) / REVEAL_BLOCKS, 1.0) + seed * REVEAL_JITTER;
    float revealed = TextureMat[3][2] * (1.0 + REVEAL_JITTER + REVEAL_EDGE + REVEAL_TAIL) - rise;
    float shown = clamp(revealed / REVEAL_EDGE, 0.0, 1.0);
    float flash = revealed > 0.0 ? exp(-revealed * REVEAL_FLASH) * REVEAL_FLASH_STRENGTH : 0.0;

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float near = 1.0 + NEAR_BOOST * (1.0 - smoothstep(NEAR_BLOCKS, NEAR_FADE, distanceFromViewer));
    float breath = (0.5 + 0.5 * sin(TAU * time * BREATH_CYCLES)) * BREATH_STRENGTH * shrinking;
    float visible = 1.0 - smoothstep(VISIBLE_FROM, VISIBLE_TO, distanceFromViewer);
    float strength = standing * near * shown * visible;

    float lift = breath + flash + HOT_LIGHT * hot * 0.5;

    float glass = mix(FILL_FAR, FILL, legible) * shade * (1.0 + FACET_STRENGTH * (facet - 0.5)) * (1.0 + lift) * strength;
    float rime = (frost + flash * 0.6) * strength;
    float glint = (edgeGlow * legible * drawn + shimmer) * (1.0 + lift) * strength;
    float bright = (edge * legible * drawn * (1.0 + lift) + sparkle + flake + crack) * strength;

    float total = glass + rime + glint + bright;
    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 ice = mix(ICE, tint, TINT_SHARE);
    vec3 deep = mix(ICE_DEEP, tint, TINT_SHARE * 1.5);
    vec3 glassColour = mix(deep, ice, facet);

    vec3 colour = (glassColour * glass + FROST * rime + ice * glint + vec3(1.0) * bright) / total;
    float alpha = clamp(total, 0.0, 1.0) * ColorModulator.a;

    fragColor = vec4(colour, alpha);
}
