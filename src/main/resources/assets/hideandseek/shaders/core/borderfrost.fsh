#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;

out vec4 fragColor;

const float TAU = 6.2831853;

const float ACROSS = 2.0;
const float RISE = 4.0;

const float LINE_FALLOFF = 4.5;
const float LINE_STRENGTH = 0.3;
const float GLOW_FALLOFF = 1.6;
const float GLOW_STRENGTH = 0.22;
const float LINE_WHITE = 0.55;

const float SNOW_REACH = 0.42;
const float SNOW_WOBBLE = 0.3;
const float SNOW_SOFT = 0.7;
const float SNOW_STRENGTH = 0.3;
const float SNOW_GRAIN = 0.55;

const float FROST_REACH = 0.34;
const float FROST_WOBBLE = 0.4;
const float FROST_SOFT = 0.6;
const float FROST_STRENGTH = 0.26;
const float FROST_GRAIN = 1.3;
const float NEEDLE_GRAIN = 13.0;

const vec3 SNOW = vec3(0.93, 0.97, 1.0);
const float SNOW_WHITE = 0.85;

const float GLINT_GRID = 3.0;
const float GLINT_SHARE = 0.1;
const float GLINT_CYCLES = 260.0;
const float GLINT_SHARPNESS = 14.0;
const float GLINT_SIZE = 0.035;
const float GLINT_STRENGTH = 1.0;

const float SHIMMER_BLOCKS = 9.0;
const float SHIMMER_CYCLES = 200.0;
const float SHIMMER_STRENGTH = 0.18;
const float SHRINK_PACE = -1.0;

const float BREATH_CYCLES = 600.0;
const float BREATH_STRENGTH = 0.25;

const float NEAR_BLOCKS = 3.0;
const float NEAR_FADE = 32.0;
const float NEAR_BOOST = 0.8;

const float FADE_SHARE = 0.5;

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 inside = fract(p);
    vec2 blend = inside * inside * (3.0 - 2.0 * inside);

    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));

    return mix(mix(a, b, blend.x), mix(c, d, blend.x), blend.y);
}

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

float glints(vec2 spot, float time) {
    vec2 grid = spot * GLINT_GRID;
    vec2 cell = floor(grid);

    if (hash(cell + 71.0) > GLINT_SHARE) {
        return 0.0;
    }

    vec2 centre = vec2(hash(cell + 13.0), hash(cell + 29.0)) * 0.6 + 0.2;
    float reach = length(fract(grid) - centre);
    float shape = exp(-pow(reach / (GLINT_SIZE * GLINT_GRID), 2.0));

    float speed = 0.6 + 0.8 * hash(cell + 47.0);
    return shape * pow(pulse(time * GLINT_CYCLES * speed + hash(cell + 5.0)), GLINT_SHARPNESS);
}

void main() {
    float along = wallCoord.x;
    bool upright = wallCoord.y > RISE - 0.5;

    float shrinking = step(TextureMat[0][3], -0.5);
    float pace = mix(1.0, SHRINK_PACE, shrinking);
    float time = GameTime;

    float shimmer = 1.0 + SHIMMER_STRENGTH * pulse(along / SHIMMER_BLOCKS - time * SHIMMER_CYCLES * pace);
    float breath = 1.0 + BREATH_STRENGTH * pulse(time * BREATH_CYCLES) * shrinking;

    float reach = max(TextureMat[3][0], 1.0);
    float fade = 1.0 - smoothstep(reach * FADE_SHARE, reach, distanceFromViewer);
    float near = 1.0 + NEAR_BOOST * (1.0 - smoothstep(NEAR_BLOCKS, NEAR_FADE, distanceFromViewer));

    float across;
    float ice;
    float sparkle;

    if (upright) {
        across = clamp((wallCoord.y - RISE) / ACROSS, 0.0, 1.0);

        float edge = FROST_REACH * (1.0 - FROST_WOBBLE + FROST_WOBBLE * 2.0 * (0.7 * noise(vec2(along * FROST_GRAIN, 3.0)) + 0.3 * noise(vec2(along * FROST_GRAIN * 4.0, 9.0))));
        float needles = noise(vec2(along * NEEDLE_GRAIN, across * 2.0));
        float tip = edge * (0.85 + 0.25 * needles);
        float settle = 1.0 - smoothstep(0.0, tip, across);

        ice = (1.0 - smoothstep(tip * (1.0 - FROST_SOFT), tip, across)) * mix(0.35, 1.0, settle) * mix(0.7, 1.0, needles) * FROST_STRENGTH;
        sparkle = glints(vec2(along, across * 1.6), time) * step(across, tip);
    } else {
        across = abs(wallCoord.y) / ACROSS;

        float side = wallCoord.y < 0.0 ? 17.0 : 41.0;
        float drift = noise(vec2(along * SNOW_GRAIN, side));
        float flake = noise(vec2(along * SNOW_GRAIN * 5.0, side + across * 3.0));
        float edge = SNOW_REACH * (1.0 - SNOW_WOBBLE + SNOW_WOBBLE * 2.0 * (0.75 * drift + 0.25 * flake));

        float settle = 1.0 - smoothstep(0.0, edge, across);
        ice = (1.0 - smoothstep(edge * (1.0 - SNOW_SOFT), edge, across)) * mix(0.5, 1.0, settle) * mix(0.75, 1.0, flake) * SNOW_STRENGTH;
        sparkle = glints(vec2(along, wallCoord.y * 1.2), time) * step(across, edge);
    }

    float line = exp(-across * LINE_FALLOFF * ACROSS) * LINE_STRENGTH;
    float glow = exp(-across * GLOW_FALLOFF * ACROSS) * GLOW_STRENGTH;

    float light = (line + glow) * shimmer * breath;
    float frost = ice + sparkle * GLINT_STRENGTH;

    float strength = fade * near;
    float total = (light + frost) * strength;

    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 lineColour = mix(tint, vec3(1.0), LINE_WHITE);
    vec3 snowColour = mix(tint, SNOW, SNOW_WHITE);

    vec3 colour = (lineColour * line + tint * glow) * shimmer * breath + snowColour * frost;
    colour /= max(light + frost, 1.0e-4);

    fragColor = vec4(colour, clamp(total, 0.0, 1.0) * ColorModulator.a);
}
