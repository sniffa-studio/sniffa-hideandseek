#version 330

in vec2 corner;
in vec3 tint;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float core = exp(-r2 * 6.0);
    fragColor = vec4(tint * (core * 2.4 + exp(-r2 * 30.0) * 1.5), 1.0);
}
