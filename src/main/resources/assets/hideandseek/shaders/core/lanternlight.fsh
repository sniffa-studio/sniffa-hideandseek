#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 local;
in vec3 surfaceNormal;
in vec3 fromEye;
in float visible;

out vec4 fragColor;

void main() {
    vec3 lamp = TextureMat[0].xyz;
    float range = TextureMat[0].w;
    vec3 look = TextureMat[1].xyz;
    float gain = TextureMat[1].w;
    float ahead = TextureMat[2].x;
    float lift = TextureMat[2].y;

    vec3 normal = normalize(surfaceNormal);
    float distanceToLamp = length(lamp - local);
    float reach = clamp(distanceToLamp / range, 0.0, 1.0);
    float edge = 1.0 - reach * reach;
    float falloff = edge * edge / (1.0 + 1.8 * reach * reach);

    vec3 toSource = normalize(lamp + vec3(0.0, lift, 0.0) - local);
    float facing = 0.3 + 0.7 * max(dot(normal, toSource), 0.0);

    float seconds = GameTime * 1200.0;
    float flicker = 0.97 + 0.02 * sin(seconds * 1.7) + 0.012 * sin(seconds * 2.9 + 1.3);

    float light = falloff * facing * flicker * smoothstep(0.0, 1.0, visible);
    light = light / (0.6 + 0.4 * light);

    if (ahead > 0.0) {
        float eye = length(fromEye);
        vec3 view = fromEye / max(eye, 1.0e-4);
        float cone = smoothstep(0.82, 0.97, dot(view, look));
        float depthFade = 1.0 - smoothstep(ahead * 0.35, ahead, eye);
        light += cone * depthFade * max(dot(normal, -view), 0.0) * 0.3 * visible;
    }

    fragColor = vec4(ColorModulator.rgb * light * gain, 1.0);
}
