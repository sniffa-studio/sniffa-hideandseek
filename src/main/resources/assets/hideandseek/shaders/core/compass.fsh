#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 plate;

out vec4 fragColor;

const float PI = 3.14159265;

const float DIAL_RADIUS = 2.6;

const float DIAL_HALF_WIDTH = 0.045;
const float DIAL_LIGHT = 0.30;

const float SOFT = 0.035;

const float TICK_STEP = PI / 12.0;
const float TICK_GAP = 0.10;
const float TICK_LENGTH = 0.20;
const float CARDINAL_LENGTH = 0.44;

const float TICK_HALF_WIDTH = 0.035;
const float TICK_LIGHT = 0.22;
const float CARDINAL_LIGHT = 0.40;

const float NEEDLE_ARC = 0.22;
const float NEEDLE_LIGHT = 1.35;

const float HEAD_GAP = 0.16;
const float HEAD_LENGTH = 0.90;
const float HEAD_HALF_WIDTH = 0.34;
const float HEAD_LIGHT = 1.15;

const float REACH = 4.0;

const float IDLE_PULSE = 0.18;
const float IDLE_SPEED = 2.2;

float band(float offset, float halfWidth) {
    return 1.0 - smoothstep(halfWidth, halfWidth + SOFT, abs(offset));
}

float span(float value, float from, float to, float fade) {
    return smoothstep(from - fade, from, value) * (1.0 - smoothstep(to - fade, to, value));
}

void main() {
    float seconds = TextureMat[0].x;
    float bearing = TextureMat[0].y;
    float live = TextureMat[0].z;

    vec2 p = plate;
    float radius = length(p);

    if (radius > REACH) {
        discard;
    }

    float angle = atan(p.y, p.x);

    float light = band(radius - DIAL_RADIUS, DIAL_HALF_WIDTH) * DIAL_LIGHT;

    float index = floor(angle / TICK_STEP + 0.5);
    float cardinal = 1.0 - step(0.001, mod(abs(index), 6.0));
    float tickLength = mix(TICK_LENGTH, CARDINAL_LENGTH, cardinal);

    float fromTick = (angle - index * TICK_STEP) * radius;
    float outward = radius - (DIAL_RADIUS + TICK_GAP);

    float tick = band(fromTick, TICK_HALF_WIDTH)
        * span(outward, 0.0, tickLength, SOFT);

    light += tick * mix(TICK_LIGHT, CARDINAL_LIGHT, cardinal);

    light *= 1.0 + IDLE_PULSE * (1.0 - live) * sin(seconds * IDLE_SPEED);

    vec2 forward = vec2(cos(bearing), sin(bearing));
    vec2 sideways = vec2(-forward.y, forward.x);
    float along = dot(p, forward);
    float across = dot(p, sideways);

    float turn = atan(sin(angle - bearing), cos(angle - bearing));
    float focus = exp(-(turn / NEEDLE_ARC) * (turn / NEEDLE_ARC));
    float needle = band(radius - DIAL_RADIUS, DIAL_HALF_WIDTH + 0.02) * focus;

    float headFrom = DIAL_RADIUS + HEAD_GAP;
    float headAt = (along - headFrom) / HEAD_LENGTH;
    float head = band(across, HEAD_HALF_WIDTH * (1.0 - clamp(headAt, 0.0, 1.0)))
        * span(headAt, 0.0, 1.0, 0.12);

    light += (needle * NEEDLE_LIGHT + head * HEAD_LIGHT) * live;

    if (light <= 0.004) {
        discard;
    }

    vec3 tint = mix(ColorModulator.rgb, vec3(1.0), clamp(light - 0.55, 0.0, 1.0) * 0.75);

    float alpha = clamp(light, 0.0, 1.0) * ColorModulator.a;

    fragColor = vec4(tint * alpha, alpha);
}
