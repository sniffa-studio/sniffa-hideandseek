#version 330

#moj_import <minecraft:globals.glsl>

in vec2 puff;
in float seed;
in float light;
in float fade;

out vec4 fragColor;

const vec3 SHADOW = vec3(0.74, 0.81, 0.91);
const vec3 SNOW = vec3(0.97, 0.98, 1.0);
const float DENSITY = 0.62;

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    vec2 centred = puff * 2.0 - 1.0;
    float swirl = GameTime * 600.0;
    vec2 p = centred * 2.2 + vec2(seed * 37.0, seed * 11.0) + vec2(swirl * 0.07, -swirl * 0.05);
    float clumps = noise(p) * 0.6 + noise(p * 2.1) * 0.4;
    float edge = length(centred) + (clumps - 0.5) * 0.45;
    float body = 1.0 - smoothstep(0.35, 1.0, edge);
    float alpha = body * DENSITY * fade;
    if (alpha < 0.01) {
        discard;
    }
    float top = clamp(0.55 - centred.y * 0.45 + (clumps - 0.5) * 0.3, 0.0, 1.0);
    vec3 colour = mix(SHADOW, SNOW, top) * light;
    fragColor = vec4(colour, alpha);
}
