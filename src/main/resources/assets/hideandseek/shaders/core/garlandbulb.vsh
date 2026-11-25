#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 corner;
out vec3 tint;
out float strength;
out float far;
out float night;

void main() {
    vec4 centre = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    float distance = length(centre.xyz);
    centre.xyz += normalize(-centre.xyz) * TextureMat[0].y;
    float size = TextureMat[0].x * (1.0 + max(distance - 16.0, 0.0) * 0.015);
    centre.xy += UV0 * size;
    gl_Position = ProjMat * centre;

    float seconds = GameTime * 1200.0;
    float a = Color.a;
    float b = fract(a * 7.31 + 0.17);
    float c = fract(a * 13.73 + 0.53);
    float shimmer = 0.9 + 0.06 * sin(seconds * (2.1 + b * 2.3) + a * 6.2831853) + 0.04 * sin(seconds * (5.3 + c * 3.1) + b * 6.2831853);
    float slow = 0.5 + 0.5 * sin(seconds * (0.21 + c * 0.27) + b * 6.2831853);
    float dip = 1.0 - 0.45 * pow(slow, 10.0);
    float flare = pow(max(sin(seconds * (0.29 + b * 0.31) + a * 18.849556), 0.0), 40.0) * 0.45;
    float fade = (1.0 - smoothstep(TextureMat[0].z, TextureMat[0].w, distance)) * smoothstep(0.4, 1.4, distance);

    corner = UV0;
    tint = Color.rgb;
    far = smoothstep(12.0, 64.0, distance);
    night = ColorModulator.r;
    strength = (shimmer * dip + flare) * fade * ColorModulator.a;
}
