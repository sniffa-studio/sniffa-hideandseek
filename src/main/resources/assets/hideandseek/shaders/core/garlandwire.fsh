#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in float across;

out vec4 fragColor;

void main() {
    float edge = 1.0 - abs(across);
    fragColor = vec4(ColorModulator.rgb * (0.7 + 0.3 * edge), ColorModulator.a * smoothstep(0.0, 0.35, edge));
}
