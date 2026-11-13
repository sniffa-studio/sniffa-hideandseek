#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec3 fromEye;
in vec3 shellNormal;
in float along;

out vec4 fragColor;

void main() {
    float strength = TextureMat[0].x;
    float fadeFrom = TextureMat[0].y;

    vec3 view = normalize(fromEye);
    float facing = abs(dot(normalize(shellNormal), view));
    float body = facing * facing * (3.0 - 2.0 * facing);

    float start = smoothstep(0.0, 0.05, along);
    float finish = 1.0 - smoothstep(0.5, 1.0, along);
    float decay = exp(-1.4 * along);

    float eye = length(fromEye);
    float near = smoothstep(0.6, 2.5, eye);
    float far = 1.0 - smoothstep(fadeFrom, fadeFrom * 1.6, eye);

    float glow = strength * body * start * finish * decay * near * far;
    fragColor = vec4(ColorModulator.rgb * glow, 1.0);
}
