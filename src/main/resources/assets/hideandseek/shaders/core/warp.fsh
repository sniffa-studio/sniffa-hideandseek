#version 330

in vec2 shapeCoord;
in vec4 tint;

out vec4 fragColor;

const float TAU = 6.2831853;

const float FLASH_AT = 0.12;
const float FLASH_WIDTH = 0.09;
const float FLASH_STRENGTH = 0.55;

const float STREAKS = 96.0;
const float STREAK_SHARE = 0.5;
const float STREAK_WIDTH = 0.16;
const float STREAK_LENGTH = 0.55;
const float STREAK_STRENGTH = 0.85;
const float STREAK_FROM = 1.7;
const float STREAK_TO = 0.2;
const float STREAK_JITTER = 0.3;

const float RIM_FROM = 0.55;
const float RIM_TO = 1.45;
const float RIM_STRENGTH = 0.55;

const float FADE_FROM = 0.55;

const float WHITE = 0.6;

float hash(float n) {
    return fract(sin(n * 12.9898) * 43758.5453);
}

void main() {
    float t = tint.a;

    vec2 size = 1.0 / max(fwidth(shapeCoord), vec2(1.0e-6));
    vec2 p = (shapeCoord - 0.5) * size / (0.5 * min(size.x, size.y));

    float r = length(p);
    float turn = atan(p.y, p.x) / TAU + 0.5;

    float bin = floor(turn * STREAKS);
    float within = abs(fract(turn * STREAKS) - 0.5) * 2.0;
    float seed = hash(bin);

    float thin = 1.0 - smoothstep(0.0, STREAK_WIDTH + 0.25 * seed, within);
    float lit = step(1.0 - STREAK_SHARE, seed);

    float front = mix(STREAK_FROM, STREAK_TO, smoothstep(0.0, 0.7, t)) + seed * STREAK_JITTER;
    float tail = front + STREAK_LENGTH * (0.6 + 0.8 * seed);
    float along = smoothstep(front, front + 0.05, r) * (1.0 - smoothstep(tail - 0.25, tail, r));

    float fade = 1.0 - smoothstep(FADE_FROM, 1.0, t);
    float streak = thin * lit * along * STREAK_STRENGTH * fade;

    float flash = exp(-pow((t - FLASH_AT) / FLASH_WIDTH, 2.0)) * FLASH_STRENGTH;
    float rim = smoothstep(RIM_FROM, RIM_TO, r) * RIM_STRENGTH * fade;

    float alpha = clamp(streak + rim + flash, 0.0, 1.0);

    if (alpha < 0.004) {
        discard;
    }

    vec3 colour = mix(tint.rgb, vec3(1.0), clamp(flash * 1.5 + streak * WHITE, 0.0, 1.0));

    fragColor = vec4(colour, alpha);
}
