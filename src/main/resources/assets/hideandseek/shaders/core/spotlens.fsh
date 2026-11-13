#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 corner;

out vec4 fragColor;

void main() {
    float r2 = dot(corner, corner);
    if (r2 > 1.0) discard;
    float halo = exp(-r2 * 5.0) * (1.0 - r2);
    float core = exp(-r2 * 60.0);
    float light = (halo * 0.55 + core * 0.9) * ColorModulator.a;
    fragColor = vec4(mix(ColorModulator.rgb, vec3(1.0), core * 0.6) * light, 1.0);
}
