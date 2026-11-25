#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D DepthSampler;

flat in vec3 lightAt;
flat in float reach;
flat in vec3 tint;
flat in mat4 unproject;

out vec4 fragColor;

void main() {
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec2 ndc = gl_FragCoord.xy / ScreenSize * 2.0 - 1.0;
    vec4 view = unproject * vec4(ndc, depth, 1.0);
    vec3 p = view.xyz / view.w;
    vec3 n = cross(dFdx(p), dFdy(p));

    if (depth <= 0.0) discard;
    vec3 toLight = lightAt - p;
    float dist = length(toLight);
    if (dist >= reach) discard;

    float x = dist / reach;
    float falloff = (1.0 - x * x);
    falloff = falloff * falloff / (1.0 + 5.0 * x * x);

    float facing = 1.0;
    float size = length(n);
    if (size > 1.0e-7) {
        n /= size;
        if (dot(n, p) > 0.0) n = -n;
        facing = clamp((dot(n, toLight / max(dist, 1.0e-4)) + 0.3) / 1.3, 0.0, 1.0);
    }
    fragColor = vec4(tint * falloff * facing, 1.0);
}
