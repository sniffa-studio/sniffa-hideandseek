#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 offset;

out vec4 fragColor;

void main() {
    float r = length(offset);
    float core = exp(-r * r * 60.0) * 0.16;
    float halo = exp(-r * r * 9.0) * 0.2;
    float wide = exp(-r * r * 2.6) * 0.07;
    float light = (core + halo + wide) * (1.0 - smoothstep(0.75, 1.0, r));
    if (light < 0.001) {
        discard;
    }
    fragColor = vec4(ColorModulator.rgb, light * ColorModulator.a);
}
