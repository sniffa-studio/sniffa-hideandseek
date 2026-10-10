#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;

out vec4 fragColor;

const float TAU = 6.2831853;

const float CORE_FALLOFF = 7.0;
const float CORE_STRENGTH = 0.95;
const float GLOW_FALLOFF = 2.2;
const float GLOW_STRENGTH = 0.5;
const float LINE_WHITE = 0.6;

const float SHIMMER_BLOCKS = 6.0;
const float SHIMMER_CYCLES = 300.0;
const float SHIMMER_STRENGTH = 0.3;
const float SHRINK_PACE = -1.0;

const float BREATH_CYCLES = 600.0;
const float BREATH_STRENGTH = 0.25;

const float FADE_SHARE = 0.7;

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

void main() {
    float across = abs(wallCoord.y);

    float shrinking = step(TextureMat[0][3], -0.5);
    float pace = mix(1.0, SHRINK_PACE, shrinking);

    float shimmer = 1.0 + SHIMMER_STRENGTH * pulse(wallCoord.x / SHIMMER_BLOCKS - GameTime * SHIMMER_CYCLES * pace);
    float breath = 1.0 + BREATH_STRENGTH * pulse(GameTime * BREATH_CYCLES) * shrinking;

    float reach = max(TextureMat[3][0], 1.0);
    float fade = 1.0 - smoothstep(reach * FADE_SHARE, reach, distanceFromViewer);

    float core = exp(-across * CORE_FALLOFF) * CORE_STRENGTH;
    float glow = exp(-across * GLOW_FALLOFF) * GLOW_STRENGTH;
    float total = (core + glow) * shimmer * breath * fade;

    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 colour = (mix(tint, vec3(1.0), LINE_WHITE) * core + tint * glow) / (core + glow);

    fragColor = vec4(colour, clamp(total, 0.0, 1.0) * ColorModulator.a);
}
