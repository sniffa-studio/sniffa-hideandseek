#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 bandCoord;
in float bandSeed;
in float along;
in float edgeFade;
in float distanceToEye;

out vec4 fragColor;

const float SECONDS = 1200.0;

const float STRENGTH = 0.6;

const float RAY_WIDE = 0.22;
const float RAY_FINE = 0.9;
const float RAY_DRIFT_WIDE = 0.25;
const float RAY_DRIFT_FINE = 0.6;

const float BOTTOM_EDGE = 0.06;
const float TOP_FALLOFF = 1.6;
const float GLOW_NEAR_BOTTOM = 1.2;

const float FLICKER = 0.2;
const float FLARE = 1.2;
const float FLARE_SHARPNESS = 30.0;

const float FAR_FROM = 250.0;
const float FAR_TO = 700.0;

const vec3 GREEN = vec3(0.25, 1.0, 0.55);
const vec3 TEAL = vec3(0.2, 0.85, 0.95);
const vec3 VIOLET = vec3(0.65, 0.4, 1.0);
const vec3 PINK = vec3(1.0, 0.35, 0.65);

float hash1(float n) {
    return fract(sin(n * 127.1) * 43758.5453);
}

float noise1(float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash1(i), hash1(i + 1.0), u);
}

void main() {
    float time = GameTime * SECONDS;
    float u = bandCoord.x;
    float v = bandCoord.y;

    float wide = noise1(u * RAY_WIDE + time * RAY_DRIFT_WIDE + bandSeed * 7.0);
    float fine = noise1(u * RAY_FINE - time * RAY_DRIFT_FINE + bandSeed * 3.0);
    float rays = pow(wide, 2.2) * 0.7 + pow(fine, 4.0) * 0.6;

    float reach = 0.55 + 0.45 * wide;
    float bottom = smoothstep(0.0, BOTTOM_EDGE, v);
    float top = pow(clamp(1.0 - v / reach, 0.0, 1.0), TOP_FALLOFF);
    float profile = bottom * top * (0.5 + GLOW_NEAR_BOTTOM * exp(-v * 4.0));

    float ends = smoothstep(0.0, 0.12, along) * (1.0 - smoothstep(0.88, 1.0, along)) * edgeFade;

    float flicker = 1.0 - FLICKER + FLICKER * sin(time * 0.9 + bandSeed * 5.0);
    float flare = FLARE * pow(max(0.0, sin(time * 0.13 + bandSeed * 9.0)), FLARE_SHARPNESS);

    float far = 1.0 - 0.6 * smoothstep(FAR_FROM, FAR_TO, distanceToEye);

    float intensity = profile * (0.35 + rays) * ends * (flicker + flare) * far * STRENGTH;
    if (intensity < 0.002) {
        discard;
    }

    vec3 colour = mix(GREEN, TEAL, smoothstep(0.1, 0.45, v));
    colour = mix(colour, VIOLET, smoothstep(0.45, 0.9, v));
    colour = mix(colour, PINK, (1.0 - smoothstep(0.0, 0.05, v)) * 0.6);

    fragColor = vec4(colour * ColorModulator.rgb, clamp(intensity, 0.0, 1.0));
}
