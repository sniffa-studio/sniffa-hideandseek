#version 330

in vec2 corner;
in vec3 tint;
in float strength;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float core = exp(-r2 * 220.0);
    float bulb = exp(-r2 * 70.0);
    float halo = exp(-r2 * 7.0) * (1.0 - r2);
    float light = (core * 1.4 + bulb * 0.9 + halo * 0.3) * strength;
    fragColor = vec4(mix(tint, vec3(1.0), core * 0.6) * light, 1.0);
}
