#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 texCoord;

out vec4 fragColor;

const vec3 TRACK = vec3(0.03, 0.06, 0.05);
const float TRACK_ALPHA = 0.62;

const vec3 DEEP = vec3(0.24, 0.66, 0.38);
const vec3 INK = vec3(0.31, 0.77, 0.44);
const vec3 MINT = vec3(0.66, 0.95, 0.77);

const float GLOW_REACH = 0.85;
const float GLOW_STRENGTH = 0.40;

const float HEAD_REACH = 1.6;

float capsule(vec2 p, vec2 from, vec2 to, float radius) {
    vec2 toPoint = p - from;
    vec2 along = to - from;
    float t = clamp(dot(toPoint, along) / dot(along, along), 0.0, 1.0);
    return length(toPoint - along * t) - radius;
}

void main() {
    vec4 box = TextureMat[1];

    if (texCoord.x < box.x || texCoord.x > box.z || texCoord.y < box.y || texCoord.y > box.w) {
        discard;
    }

    float fill = clamp(TextureMat[0].x, 0.0, 1.0);
    float unitsX = TextureMat[0].z;
    float unitsY = TextureMat[0].w;
    float pad = TextureMat[2].x;

    vec2 p = vec2(
        (texCoord.x - box.x) / max(box.z - box.x, 1.0e-5) * unitsX,
        (texCoord.y - box.y) / max(box.w - box.y, 1.0e-5) * unitsY
    );

    float radius = 0.5;
    float middle = unitsY * 0.5;
    vec2 head = vec2(pad + radius, middle);
    vec2 tail = vec2(unitsX - pad - radius, middle);

    float toEdge = capsule(p, head, tail, radius);

    float pixel = fwidth(toEdge) + 1.0e-5;

    float inside = 1.0 - smoothstep(-pixel, pixel, toEdge);

    float falloff = 1.0 - smoothstep(0.0, GLOW_REACH, toEdge);
    float glow = falloff * falloff * GLOW_STRENGTH;

    float span = tail.x - head.x;
    float atX = (p.x - head.x) / max(span, 1.0e-5);
    float filled = 1.0 - smoothstep(fill - pixel / max(span, 1.0e-5), fill + pixel / max(span, 1.0e-5), atX);

    float up = clamp((p.y - (middle - radius)) / (radius * 2.0), 0.0, 1.0);
    vec3 body = mix(DEEP, INK, up);

    body = mix(body, MINT, smoothstep(0.62, 0.95, up) * 0.55);

    float headGlow = (1.0 - smoothstep(0.0, HEAD_REACH / max(span, 1.0e-5), fill - atX)) * step(atX, fill);
    body = mix(body, MINT, headGlow * 0.8);

    vec3 tint = mix(TRACK, body, filled);
    float alpha = inside * mix(TRACK_ALPHA, 1.0, filled);

    float spill = glow * (1.0 - inside) * max(filled, headGlow);
    tint = mix(tint, INK, spill > 0.0 ? 1.0 : 0.0);
    alpha = max(alpha, spill);

    alpha *= ColorModulator.a;
    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(tint, clamp(alpha, 0.0, 1.0));
}
