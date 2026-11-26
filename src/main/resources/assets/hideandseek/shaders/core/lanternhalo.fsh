#version 330

in vec2 corner;
in vec3 tint;
in float strength;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float halo = exp(-r2 * 4.0) * (1.0 - r2);
    float inner = exp(-r2 * 22.0);
    fragColor = vec4(tint * (halo * 0.42 + inner * 0.3) * strength, 1.0);
}
