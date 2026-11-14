#version 330

in vec3 normal;
in vec2 surface;
in float light;

out vec4 fragColor;

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
    vec3 n = normalize(normal);
    vec2 p = vec2(surface.x * 9.0, surface.y * 7.0);
    float needles = noise(p * vec2(3.0, 1.0)) * 0.55 + noise(p * vec2(7.0, 2.5)) * 0.45;
    float strands = fract(surface.x * 16.0 + surface.y * 3.0 + needles * 0.8);
    vec3 dark = vec3(0.07, 0.2, 0.09);
    vec3 bright = vec3(0.2, 0.46, 0.18);
    vec3 colour = mix(dark, bright, needles * 0.8 + step(0.75, strands) * 0.25);
    float snow = smoothstep(0.45, 0.85, n.y + (needles - 0.5) * 0.5);
    colour = mix(colour, vec3(0.88, 0.93, 1.0), snow * 0.75);
    float shade = 0.5 + 0.5 * max(dot(n, normalize(vec3(0.3, 0.8, 0.4))), 0.0);
    fragColor = vec4(colour * shade * light, 1.0);
}
