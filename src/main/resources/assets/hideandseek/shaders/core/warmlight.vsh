#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <hideandseek:flicker.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

flat out vec3 lightAt;
flat out float reach;
flat out vec3 tint;
flat out mat4 unproject;

vec2 coverCorner(vec3 centre, float r, float d, vec2 corner, out bool whole) {
    whole = d < r + 0.35;
    if (whole) return corner;
    vec3 n = centre / d;
    vec3 helper = abs(n.y) > 0.9 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
    vec3 u = normalize(cross(n, helper));
    vec3 v = cross(u, n);
    float s = r * d / sqrt(d * d - r * r);
    vec2 low = vec2(1.0e9);
    vec2 high = vec2(-1.0e9);
    for (int i = 0; i < 4; i++) {
        vec2 c = vec2(i == 1 || i == 2 ? 1.0 : -1.0, i >= 2 ? 1.0 : -1.0);
        vec4 clip = ProjMat * vec4(centre + (u * c.x + v * c.y) * s, 1.0);
        if (clip.w <= 1.0e-4) {
            whole = true;
            return corner;
        }
        vec2 ndc = clip.xy / clip.w;
        low = min(low, ndc);
        high = max(high, ndc);
    }
    low = clamp(low, -1.0, 1.0);
    high = clamp(high, -1.0, 1.0);
    return vec2(corner.x < 0.0 ? low.x : high.x, corner.y < 0.0 ? low.y : high.y);
}

void main() {
    vec3 centre = (ModelViewMat * vec4(Position + ModelOffset, 1.0)).xyz;
    float r = abs(UV0.x);
    vec2 corner = sign(UV0);
    float d = length(centre);
    float code = abs(UV0.y);
    float fade = 1.0 - smoothstep(TextureMat[0].x, TextureMat[0].y, d - r * 0.5);
    float strength = sniffaFlicker(floor(code) - 1.0, fract(code), GameTime * 1200.0) * fade * ColorModulator.a;

    lightAt = centre;
    reach = r;
    tint = Color.rgb * 2.0 * strength * TextureMat[0].z;
    unproject = inverse(ProjMat);

    float farZ = centre.z - r;
    if (strength <= 0.002 || farZ >= -0.05) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }
    vec4 farClip = ProjMat * vec4(0.0, 0.0, farZ, 1.0);
    bool whole;
    vec2 xy = coverCorner(centre, r, d, corner, whole);
    gl_Position = vec4(xy, clamp(farClip.z / farClip.w, 0.0, 1.0), 1.0);
}
