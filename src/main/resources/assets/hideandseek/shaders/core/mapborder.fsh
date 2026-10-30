#version 330

in vec2 shapeCoord;
in vec4 tint;

out vec4 fragColor;

const float LINE_PIXELS = 1.6;
const float LINE_STRENGTH = 0.95;

const float GLOW_PIXELS = 14.0;
const float GLOW_STRENGTH = 0.30;

const float COMB_PIXELS = 26.0;
const float COMB_CELL_PIXELS = 13.0;
const float COMB_STRENGTH = 0.30;

const vec2 HEX_STEP = vec2(1.7320508, 1.0);
const vec2 HEX_HALF = vec2(0.8660254, 0.5);

float combEdge(vec2 cell) {
    vec2 first = mod(cell, HEX_STEP) - HEX_STEP * 0.5;
    vec2 second = mod(cell + HEX_HALF, HEX_STEP) - HEX_STEP * 0.5;
    vec2 nearest = dot(first, first) < dot(second, second) ? first : second;

    return 0.5 - max(
        abs(nearest.x) * 0.8660254 + abs(nearest.y) * 0.5,
        abs(nearest.y)
    );
}

void main() {
    float d = length(shapeCoord);

    float perPixel = length(vec2(dFdx(shapeCoord.x), dFdy(shapeCoord.x)));
    if (perPixel <= 0.0) {
        discard;
    }

    float radiusPixels = 1.0 / perPixel;

    float fromLine = abs(d - 1.0) * radiusPixels;

    float line = (1.0 - smoothstep(0.0, LINE_PIXELS, fromLine)) * LINE_STRENGTH;
    float glow = exp(-fromLine / GLOW_PIXELS) * GLOW_STRENGTH;

    float circumference = 6.2831853 * radiusPixels;
    float columns = max(4.0, floor(circumference / (COMB_CELL_PIXELS * 1.7320508) + 0.5));
    float unit = circumference / (columns * 1.7320508);

    float angle = atan(shapeCoord.y, shapeCoord.x);
    vec2 cell = vec2(
        angle * 0.1591549 * columns * 1.7320508,
        (d - 1.0) * radiusPixels / unit
    );

    float comb = 1.0 - smoothstep(0.0, 0.10, combEdge(cell));
    comb *= (1.0 - smoothstep(0.0, COMB_PIXELS, fromLine)) * COMB_STRENGTH;

    float ring = clamp(line + glow + comb, 0.0, 1.0);
    float alpha = ring * tint.a;

    if (alpha < 0.004) {
        discard;
    }

    vec3 colour = mix(tint.rgb, vec3(1.0), smoothstep(0.5, 1.0, line) * 0.7);

    fragColor = vec4(colour, alpha);
}
