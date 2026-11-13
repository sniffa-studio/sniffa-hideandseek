#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec3 fromLight;
in vec3 surfaceNormal;
in float distanceToEye;

out vec4 fragColor;

void main() {
    vec3 axis = normalize(TextureMat[0].xyz);
    float range = TextureMat[0].w;
    float cosOuter = TextureMat[1].x;
    float cosInner = TextureMat[1].y;
    float gain = TextureMat[1].z;
    float fadeFrom = TextureMat[1].w;

    float distanceToLight = length(fromLight);
    vec3 toLight = -fromLight / max(distanceToLight, 1.0e-4);

    float angle = dot(-toLight, axis);
    float cone = smoothstep(cosOuter, cosInner, angle);
    float core = smoothstep(mix(cosOuter, cosInner, 0.55), 1.0, angle);

    float reach = distanceToLight / range;
    float falloff = 1.0 / (1.0 + 4.0 * reach * reach);
    falloff *= 1.0 - smoothstep(0.75, 1.0, reach);

    float facing = max(dot(normalize(surfaceNormal), toLight), 0.0);
    facing = mix(facing, sqrt(facing), 0.5);

    float eyeFade = 1.0 - smoothstep(fadeFrom, fadeFrom * 1.6, distanceToEye);

    float light = (cone * 0.8 + core * 0.35) * falloff * facing * eyeFade;
    fragColor = vec4(ColorModulator.rgb * light * gain, 1.0);
}
