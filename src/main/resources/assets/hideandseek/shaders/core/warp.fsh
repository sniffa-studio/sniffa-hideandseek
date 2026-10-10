#version 330

in vec2 shapeCoord;
in vec4 tint;

out vec4 fragColor;

const float CELLS_ACROSS = 18.0;

const float SPEED = 1.35;
const float JITTER = 0.18;
const float DISSOLVE = 0.14;

const float COVER = 0.72;
const float SHADE = 0.22;

const float OUTLINE = 0.05;
const float OUTLINE_STRENGTH = 0.45;
const float SPARK = 0.9;
const float SPARK_WIDTH = 0.05;

const vec2 HEX_STEP = vec2(1.7320508, 1.0);
const vec2 HEX_HALF = vec2(0.8660254, 0.5);

vec4 hexCell(vec2 p) {
    vec2 centreA = round(p / HEX_STEP);
    vec2 centreB = round((p - HEX_HALF) / HEX_STEP) + 0.5;

    vec2 offsetA = p - centreA * HEX_STEP;
    vec2 offsetB = p - centreB * HEX_STEP;

    return dot(offsetA, offsetA) < dot(offsetB, offsetB)
        ? vec4(offsetA, centreA * HEX_STEP)
        : vec4(offsetB, centreB * HEX_STEP);
}

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

void main() {
    float t = tint.a;

    vec2 size = 1.0 / max(fwidth(shapeCoord), vec2(1.0e-6));
    float cell = min(size.x, size.y) / CELLS_ACROSS;
    vec2 pixel = (shapeCoord - 0.5) * size;

    vec4 hex = hexCell(pixel / cell);
    vec2 offset = hex.xy;
    vec2 centre = hex.zw * cell;

    float reach = 0.5 * length(size);
    float fromMiddle = length(centre) / reach;

    float local = t * SPEED - fromMiddle - hash(hex.zw) * JITTER;
    float left = 1.0 - smoothstep(0.0, DISSOLVE, local);

    vec2 folded = abs(offset);
    float toEdge = 0.5 - max(folded.y, dot(folded, HEX_HALF));
    float shrink = 0.5 * (1.0 - left);
    float inside = smoothstep(shrink, shrink + 0.02, toEdge);
    float outline = (1.0 - smoothstep(0.0, OUTLINE, abs(toEdge - shrink))) * step(0.001, left);

    float spark = exp(-pow((local - DISSOLVE * 0.5) / SPARK_WIDTH, 2.0)) * SPARK;

    float body = inside * left * COVER;
    float rim = outline * (OUTLINE_STRENGTH * left + spark);

    float alpha = clamp(body + rim, 0.0, 1.0);

    if (alpha < 0.004) {
        discard;
    }

    vec3 deep = tint.rgb * SHADE;
    vec3 colour = mix(deep, mix(tint.rgb, vec3(1.0), 0.35), clamp(rim / max(alpha, 1.0e-3), 0.0, 1.0));

    fragColor = vec4(colour, alpha);
}
