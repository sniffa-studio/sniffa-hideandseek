#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 corner;
in vec3 tint;
in float phase;

out vec4 fragColor;

const float SECONDS = 1200.0;
const float TWINKLE = 0.24;

void main() {
    float shape = 1.0 - smoothstep(0.6, 1.0, length(corner));
    if (shape <= 0.0) {
        discard;
    }
    float speed = 1.1 + 2.4 * fract(phase * 7.31);
    float wave = sin(GameTime * SECONDS * speed + phase * 6.2832) * 0.6 + sin(GameTime * SECONDS * speed * 2.3 + phase * 19.7) * 0.4;
    float twinkle = 1.0 - TWINKLE * (0.5 + 0.5 * wave);
    fragColor = vec4(tint, shape * twinkle * ColorModulator.a);
}
