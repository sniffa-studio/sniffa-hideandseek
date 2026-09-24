#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D FrostSampler;

in vec2 texCoord;

out vec4 fragColor;

const float OPEN_COLD = 0.80;
const float OPEN_HOT = 0.24;

const float SPIKE = 0.26;

const float SOFT_HOT = 0.50;
const float SOFT_COLD = 0.14;

const float PULSE_SLOW = 2.0;
const float PULSE_FAST = 12.0;
const float PULSE_DEPTH = 0.26;

const float MAX_OPACITY = 0.66;

const float COLD_PRESENCE = 0.55;

const float FROST_STRENGTH = 0.85;

const vec3 ICE = vec3(0.60, 0.85, 1.00);
const vec3 EMBER = vec3(1.00, 0.32, 0.10);

float shards(float angle, float seconds) {
    return sin(angle * 17.0 + seconds * 0.15) * 0.50
         + sin(angle * 29.0 - seconds * 0.11) * 0.30
         + sin(angle * 43.0 + 2.2) * 0.20;
}

void main() {
    float warmth = clamp(TextureMat[0].x, 0.0, 1.0);
    float seconds = TextureMat[0].y;
    float chill = 1.0 - warmth;

    vec2 fromCentre = (texCoord - 0.5) * 2.0;
    float spread = length(fromCentre);
    float around = atan(fromCentre.y, fromCentre.x);

    float edge = mix(OPEN_COLD, OPEN_HOT, warmth)
               - SPIKE * chill * (shards(around, seconds) * 0.5 + 0.5);

    float softness = mix(SOFT_COLD, SOFT_HOT, warmth);
    float frame = smoothstep(edge, edge + softness, spread);

    float snow = texture(FrostSampler, texCoord).a * chill * FROST_STRENGTH;
    frame = clamp(frame + snow, 0.0, 1.0);

    if (frame <= 0.0) {
        discard;
    }

    float beat = 1.0 + PULSE_DEPTH * warmth * sin(seconds * mix(PULSE_SLOW, PULSE_FAST, warmth));

    float strength = COLD_PRESENCE + (1.0 - COLD_PRESENCE) * warmth;

    float alpha = frame * beat * strength * MAX_OPACITY * ColorModulator.a;
    if (alpha < 0.004) {
        discard;
    }

    vec3 tint = mix(ICE, EMBER, warmth);

    fragColor = vec4(tint, clamp(alpha, 0.0, 1.0));
}
