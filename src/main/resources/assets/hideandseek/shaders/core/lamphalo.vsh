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

void main() {
    vec4 centre = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    float distance = length(centre.xyz);
    centre.xyz += normalize(-centre.xyz) * 0.3;
    centre.xy += UV0;
    gl_Position = ProjMat * centre;

    float seconds = GameTime * 1200.0;
    float phase = Color.a * 6.2831853;
    float flicker = 0.9 + 0.06 * sin(seconds * 7.3 + phase) + 0.04 * sin(seconds * 13.1 + phase * 2.0);
    float fade = 1.0 - smoothstep(TextureMat[0].x, TextureMat[0].y, distance);

    corner = sign(UV0);
    tint = Color.rgb;
    strength = flicker * fade;
}
