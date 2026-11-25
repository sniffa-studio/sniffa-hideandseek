#version 330

in vec2 corner;
in vec3 tint;
in float strength;

out vec4 fragColor;

void main() {
    vec2 spread = abs(corner);
    float r2 = dot(corner, corner);
    float arms = max(0.0, 1.0 - spread.x * 7.0) * (1.0 - spread.y) + max(0.0, 1.0 - spread.y * 7.0) * (1.0 - spread.x);
    float core = exp(-r2 * 12.0);
    float light = arms * 0.8 + core * 1.4;
    if (light < 0.01) discard;
    fragColor = vec4(tint * light * strength, 1.0);
}
