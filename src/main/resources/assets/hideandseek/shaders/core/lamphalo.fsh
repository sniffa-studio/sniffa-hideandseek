#version 330

in vec2 corner;
in vec3 tint;
in float strength;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float halo = exp(-r2 * 4.5) * (1.0 - r2);
    float inner = exp(-r2 * 26.0);
    vec3 hot = mix(tint, vec3(1.0, 0.9, 0.72), 0.45);
    fragColor = vec4((tint * halo * 0.4 + hot * inner * 0.32) * strength, 1.0);
}
