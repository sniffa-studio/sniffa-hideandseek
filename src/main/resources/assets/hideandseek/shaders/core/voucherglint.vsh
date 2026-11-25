#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 face;
out float sweep;
out float strength;

void main() {
    vec4 at = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    gl_Position = ProjMat * at;

    float seconds = GameTime * 1200.0;
    float cycle = fract(seconds / 4.0 + Color.a);
    face = UV0;
    sweep = cycle / 0.16;
    strength = Color.r * (1.0 - smoothstep(TextureMat[0].x, TextureMat[0].y, length(at.xyz)));
}
