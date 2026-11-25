#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in float across;
in float coverage;

out vec4 fragColor;

void main() {
    float edge = 1.0 - abs(across);
    float sheen = smoothstep(0.55, 1.0, 1.0 - abs(across + 0.35));
    vec3 colour = ColorModulator.rgb * (0.65 + 0.35 * edge) + vec3(0.03, 0.05, 0.03) * sheen;
    fragColor = vec4(colour, ColorModulator.a * coverage * smoothstep(0.0, 0.35, edge));
}
