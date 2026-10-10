#version 330

in vec2 shapeCoord;
in vec4 tint;

out vec4 fragColor;

const float REACH_SHARE = 0.28;
const float CURVE = 1.25;

const float COMB_PIXELS = 22.0;
const float COMB_LINE = 0.05;
const float COMB_STRENGTH = 0.7;

const float RIM_PIXELS = 2.0;
const float RIM_STRENGTH = 0.35;

const vec2 HEX_STEP = vec2(1.7320508, 1.0);
const vec2 HEX_HALF = vec2(0.8660254, 0.5);

float combEdge(vec2 cell) {
    vec2 first = mod(cell, HEX_STEP) - HEX_STEP * 0.5;
    vec2 second = mod(cell + HEX_HALF, HEX_STEP) - HEX_STEP * 0.5;
    vec2 nearest = dot(first, first) < dot(second, second) ? first : second;
    return 0.5 - max(abs(nearest.x) * 0.8660254 + abs(nearest.y) * 0.5, abs(nearest.y));
}

void main() {
    vec2 size = 1.0 / max(fwidth(shapeCoord), vec2(1.0e-6));
    vec2 pixel = shapeCoord * size;

    vec2 toEdge = min(pixel, size - pixel);
    float reach = REACH_SHARE * min(size.x, size.y);

    float inward = reach - length(max(vec2(reach) - toEdge, vec2(0.0)));
    float glow = pow(1.0 - clamp(inward / reach, 0.0, 1.0), CURVE);

    float edge = combEdge(pixel / COMB_PIXELS);
    float comb = (1.0 - smoothstep(0.0, COMB_LINE, edge)) * COMB_STRENGTH * glow * glow;

    float rim = (1.0 - smoothstep(0.0, RIM_PIXELS, min(toEdge.x, toEdge.y))) * RIM_STRENGTH;

    float alpha = clamp(glow * 1.3 + comb + rim, 0.0, 1.0) * tint.a;

    if (alpha < 0.004) {
        discard;
    }

    vec3 colour = mix(tint.rgb, vec3(1.0), clamp(comb + rim, 0.0, 1.0) * 0.35);

    fragColor = vec4(colour, alpha);
}
