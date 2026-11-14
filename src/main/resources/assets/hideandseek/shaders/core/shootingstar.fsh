#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in float across;
in float toward;

out vec4 fragColor;

void main() {
    float core = exp(-across * across * 6.0);
    float trail = pow(toward, 2.2);
    float head = smoothstep(0.82, 1.0, toward);
    vec3 colour = mix(ColorModulator.rgb, vec3(1.0), head * 0.7);
    float light = core * (trail * 1.6 + head * 2.6) * ColorModulator.a;
    fragColor = vec4(colour * light, 1.0);
}
