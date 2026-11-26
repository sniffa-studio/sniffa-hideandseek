#version 330

#moj_import <minecraft:globals.glsl>

in float across;
in float rise;
in float along;
in float eye;
in vec3 tint;

out vec4 fragColor;

void main() {
    float u2 = across * across;
    if (u2 > 1.0) discard;
    float core = exp(-u2 * 60.0);
    float soft = exp(-u2 * 8.0);
    float halo = exp(-u2 * 2.5) * (1.0 - u2);

    float seconds = GameTime * 1200.0;
    float drift = 0.9 + 0.1 * sin(rise * 0.35 - seconds * 2.0);
    float foot = smoothstep(0.0, 1.0, rise);
    float fade = 1.0 - smoothstep(0.35, 1.0, along);
    float near = smoothstep(0.5, 2.5, eye);

    float shown = drift * foot * fade * near;
    vec3 white = mix(tint, vec3(1.0), 0.55);
    vec3 light = white * core * 0.7 + tint * (soft * 0.34 + halo * 0.14);
    fragColor = vec4(light * shown, 1.0);
}
