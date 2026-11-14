#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D SceneSampler;

in vec2 uv;

out vec4 fragColor;

void main() {
    vec3 c = texture(SceneSampler, uv).rgb;
    float k = ColorModulator.a;
    float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
    float shadow = 1.0 - smoothstep(0.04, 0.42, lum);
    float light = smoothstep(0.3, 0.85, lum);
    vec3 graded = c;
    graded = mix(graded, graded * vec3(0.84, 0.95, 1.2) + vec3(0.0, 0.004, 0.014), shadow * k);
    graded = mix(graded, graded * vec3(1.12, 1.01, 0.86), light * k);
    float glum = dot(graded, vec3(0.2126, 0.7152, 0.0722));
    graded = mix(vec3(glum), graded, 1.0 + 0.1 * k);
    graded = mix(graded, graded * graded * (3.0 - 2.0 * graded), 0.18 * k);
    fragColor = vec4(clamp(graded, 0.0, 1.0), 1.0);
}
