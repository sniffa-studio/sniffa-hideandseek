#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 sole;
in float mirrored;
in float fade;

out vec4 fragColor;

const vec3 CORE = vec3(0.86, 0.95, 1.0);
const vec3 EDGE = vec3(0.45, 0.78, 1.0);
const float TREAD = 9.0;
const float GLOW = 0.35;
const float STRENGTH = 0.9;

float ellipse(vec2 p, vec2 centre, vec2 radius) {
    vec2 d = (p - centre) / radius;
    return dot(d, d);
}

void main() {
    vec2 p = sole;
    if (mirrored > 0.5) {
        p.x = 1.0 - p.x;
    }

    float front = ellipse(p, vec2(0.46, 0.66), vec2(0.40, 0.32));
    float heel = ellipse(p, vec2(0.52, 0.2), vec2(0.32, 0.18));
    float shape = min(front, heel);

    float inside = 1.0 - smoothstep(0.85, 1.0, shape);
    float halo = (1.0 - smoothstep(1.0, 1.9, shape)) * GLOW;
    float tread = 0.75 + 0.25 * step(0.5, fract(p.y * TREAD + 0.25 * step(0.4, p.y)));
    float rim = smoothstep(0.55, 0.95, shape) * inside;

    float alpha = (inside * tread + halo) * fade * STRENGTH;
    if (alpha < 0.01) {
        discard;
    }

    vec3 colour = mix(CORE, EDGE, clamp(rim + (1.0 - inside), 0.0, 1.0));
    fragColor = vec4(colour, clamp(alpha, 0.0, 1.0));
}
