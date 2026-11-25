#version 330

in vec2 face;
in float sweep;
in float strength;

out vec4 fragColor;

void main() {
    if (sweep > 1.0) discard;
    vec2 texel = floor(face * vec2(128.0, 80.0)) / vec2(128.0, 80.0);
    float across = texel.x + (1.0 - texel.y) * 0.34;
    float centre = sweep * 1.75 - 0.3;
    float band = 1.0 - step(0.035, abs(across - centre));
    float edge = (1.0 - step(0.07, abs(across - centre))) * 0.45;
    float trail = (1.0 - step(0.012, abs(across - centre + 0.11))) * 0.35;
    float light = max(max(band, edge), trail) * strength;
    if (light < 0.01) discard;
    fragColor = vec4(vec3(light), 1.0);
}
