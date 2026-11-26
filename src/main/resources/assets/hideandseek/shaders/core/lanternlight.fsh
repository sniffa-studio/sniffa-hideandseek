#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 local;
in vec3 surfaceNormal;
in vec3 fromEye;

out vec4 fragColor;

void main() {
    vec3 lamp = TextureMat[0].xyz;
    float range = TextureMat[0].w;
    vec3 look = TextureMat[1].xyz;
    float gain = TextureMat[1].w;
    float ahead = TextureMat[2].x;

    vec3 normal = normalize(surfaceNormal);
    vec3 toLamp = lamp - local;
    float distanceToLamp = length(toLamp);
    toLamp /= max(distanceToLamp, 1.0e-4);

    float reach = distanceToLamp / range;
    float falloff = 1.0 / (1.0 + 7.0 * reach * reach);
    falloff *= 1.0 - smoothstep(0.65, 1.0, reach);

    float facing = max(dot(normal, toLamp), 0.0);
    facing = mix(facing, sqrt(facing), 0.5);

    float seconds = GameTime * 1200.0;
    float flicker = 0.94 + 0.035 * sin(seconds * 7.1) + 0.025 * sin(seconds * 12.7 + 1.3);

    float light = falloff * facing * flicker;

    if (ahead > 0.0) {
        float eye = length(fromEye);
        vec3 view = fromEye / max(eye, 1.0e-4);
        float cone = smoothstep(0.82, 0.97, dot(view, look));
        float depthFade = 1.0 - smoothstep(ahead * 0.35, ahead, eye);
        light += cone * depthFade * max(dot(normal, -view), 0.0) * 0.35;
    }

    fragColor = vec4(ColorModulator.rgb * light * gain, 1.0);
}
