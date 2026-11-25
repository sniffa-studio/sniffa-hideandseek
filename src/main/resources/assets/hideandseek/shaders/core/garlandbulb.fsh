#version 330

in vec2 corner;
in vec3 tint;
in float strength;
in float far;
in float night;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float core = exp(-r2 * 260.0);
    float glass = exp(-r2 * 64.0);
    float halo = exp(-r2 * 5.5) * (1.0 - r2);
    vec3 filament = mix(tint, vec3(1.0, 0.93, 0.78), 0.75);
    vec3 light = filament * core * 1.7 + tint * glass * 0.95 + tint * halo * (0.16 + 0.1 * far) * (0.3 + 0.7 * night);
    fragColor = vec4(light * strength, 1.0);
}
