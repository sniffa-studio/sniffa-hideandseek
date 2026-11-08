#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;

out vec4 fragColor;

const float TAU = 6.2831853;

const float RIME_FALLOFF = 2.4;
const float RIME_STRENGTH = 0.7;
const float RIME_CELLS = 0.35;
const float CORE_FALLOFF = 9.0;
const float CORE_STRENGTH = 0.55;

const float CRYSTAL_CELLS = 0.45;
const float CRYSTAL_SHARE = 0.35;
const float CRYSTAL_PIXELS = 1.8;
const float CRYSTAL_CYCLES = 700.0;
const float CRYSTAL_STRENGTH = 1.4;

const float SHIMMER_BLOCKS = 7.0;
const float SHIMMER_CYCLES = 260.0;
const float SHIMMER_STRENGTH = 0.25;
const float SHRINK_PACE = -1.0;

const float BREATH_CYCLES = 600.0;
const float BREATH_STRENGTH = 0.25;

const float FADE_SHARE = 0.5;

const vec3 FROST = vec3(0.94, 0.97, 1.0);
const vec3 ICE = vec3(0.7, 0.88, 1.0);
const float TINT_SHARE = 0.2;

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

void main() {
    float across = abs(wallCoord.y);

    float shrinking = step(TextureMat[0][3], -0.5);
    float pace = mix(1.0, SHRINK_PACE, shrinking);

    float ragged = noise(vec2(wallCoord.x / RIME_CELLS, wallCoord.y / RIME_CELLS)) * 0.6
                 + noise(vec2(wallCoord.x / RIME_CELLS * 2.7, wallCoord.y / RIME_CELLS * 2.7)) * 0.4;
    float rime = exp(-across * RIME_FALLOFF) * RIME_STRENGTH * smoothstep(0.15, 0.75, ragged + 0.45 - across * 0.25);
    float core = exp(-across * CORE_FALLOFF) * CORE_STRENGTH;

    vec2 grid = vec2(wallCoord.x / CRYSTAL_CELLS, wallCoord.y / CRYSTAL_CELLS);
    vec2 id = floor(grid);
    vec2 at = vec2(hash(id + 3.1), hash(id + 7.7));
    vec2 from = fract(grid) - at;
    float pixels = length(from) / max(length(fwidth(grid)), 1.0e-6);
    float phase = fract(GameTime * CRYSTAL_CYCLES + hash(id) * 5.0);
    float size = CRYSTAL_PIXELS * sin(3.1415927 * phase);
    float crystal = step(hash(id + 11.0), CRYSTAL_SHARE) * (1.0 - smoothstep(0.0, max(size, 0.01), pixels))
                  * exp(-across * 1.2) * CRYSTAL_STRENGTH;

    float shimmer = 1.0 + SHIMMER_STRENGTH * pulse(wallCoord.x / SHIMMER_BLOCKS - GameTime * SHIMMER_CYCLES * pace);
    float breath = 1.0 + BREATH_STRENGTH * pulse(GameTime * BREATH_CYCLES) * shrinking;

    float reach = max(TextureMat[3][0], 1.0);
    float fade = 1.0 - smoothstep(reach * FADE_SHARE, reach, distanceFromViewer);

    float soft = (rime + core) * shimmer * breath * fade;
    float bright = crystal * fade;
    float total = soft + bright;

    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 ice = mix(ICE, tint, TINT_SHARE);
    vec3 colour = (mix(ice, FROST, 0.65) * rime * shimmer * breath * fade + FROST * core * shimmer * breath * fade + vec3(1.0) * bright) / total;

    fragColor = vec4(colour, clamp(total, 0.0, 1.0) * ColorModulator.a);
}
