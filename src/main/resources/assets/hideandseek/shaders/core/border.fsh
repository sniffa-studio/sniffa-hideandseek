#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 wallCoord;
in float heightFromViewer;

out vec4 fragColor;

const float CELL_STRETCH = 1.0;

const float LINE_PIXELS = 2.0;
const float LINE_STRENGTH = 0.9;

const float GLOW_PIXELS = 9.0;
const float GLOW_STRENGTH = 0.24;

const float HAZE = 0.05;

const float FADE_FROM = 22.0;
const float FADE_TO = 85.0;

const vec2 HEX_STEP = vec2(1.7320508, 1.0);
const vec2 HEX_HALF = vec2(0.8660254, 0.5);

float seamless(float delta) {
    return delta - HEX_STEP.x * round(delta / HEX_STEP.x);
}

vec2 hexOffset(vec2 p) {
    vec2 centreA = round(p / HEX_STEP);
    vec2 centreB = round((p - HEX_HALF) / HEX_STEP);

    vec2 offsetA = p - centreA * HEX_STEP;
    vec2 offsetB = p - (centreB + 0.5) * HEX_STEP;

    return dot(offsetA, offsetA) < dot(offsetB, offsetB) ? offsetA : offsetB;
}

vec3 hexEdge(vec2 offset) {
    vec2 folded = abs(offset);

    float towardsFlat = folded.y;
    float towardsCorner = dot(folded, HEX_HALF);

    vec2 normal = towardsCorner > towardsFlat ? HEX_HALF : vec2(0.0, 1.0);
    return vec3(0.5 - max(towardsFlat, towardsCorner), normal * sign(offset));
}

void main() {
    vec2 p = wallCoord * vec2(1.0, 1.0 / CELL_STRETCH);

    vec3 edge = hexEdge(hexOffset(p));
    float toEdge = edge.x;
    vec2 normal = edge.yz;

    vec2 alongX = vec2(seamless(dFdx(p.x)), dFdx(p.y));
    vec2 alongY = vec2(seamless(dFdy(p.x)), dFdy(p.y));

    float perPixel = length(vec2(dot(alongX, normal), dot(alongY, normal)));
    float pixelsToEdge = toEdge / max(perPixel, 1.0e-6);

    float cellsPerPixel = max(length(alongX), length(alongY));
    float legible = 1.0 - smoothstep(0.35, 1.1, cellsPerPixel);

    float line = (1.0 - smoothstep(0.0, LINE_PIXELS, pixelsToEdge)) * LINE_STRENGTH;
    float bloom = (1.0 - smoothstep(0.0, GLOW_PIXELS, pixelsToEdge)) * GLOW_STRENGTH;

    float glow = (line + bloom) * legible + HAZE;

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));

    float alpha = clamp(glow * standing, 0.0, 1.0) * ColorModulator.a;

    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(ColorModulator.rgb, alpha);
}
