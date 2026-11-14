#version 330

in vec2 corner;
in vec3 tint;
in float strength;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float falloff = (1.0 - r2) * (1.0 - r2);
    fragColor = vec4(tint * falloff * 0.28 * strength, 1.0);
}
