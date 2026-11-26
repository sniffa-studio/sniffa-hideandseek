#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in float elevation;

out vec4 fragColor;

float dither(vec2 pixel) {
    return fract(52.9829189 * fract(dot(pixel, vec2(0.06711056, 0.00583715)))) - 0.5;
}

void main() {
    vec3 horizon = TextureMat[0].rgb;
    vec3 glow = TextureMat[1].rgb;
    float lift = exp(-abs(elevation) / 3.5);
    vec3 colour = mix(horizon, glow, elevation < 0.0 ? lift * lift : lift);
    float cover = elevation <= 0.0 ? 1.0 : exp(-elevation / 12.0);
    cover *= 1.0 - smoothstep(55.0, 70.0, elevation);
    colour += dither(gl_FragCoord.xy) / 255.0;
    fragColor = vec4(colour, cover * ColorModulator.a);
}
