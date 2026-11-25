#version 330

in vec2 corner;
in vec3 tint;
in float strength;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float halo = exp(-r2 * 3.2) * (1.0 - r2);
    fragColor = vec4(tint * halo * 0.55 * strength, 1.0);
}
