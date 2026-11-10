#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 puff;
out float seed;
out float light;
out float fade;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ModelOffset, 1.0);
    puff = UV0;
    seed = Color.r;
    light = Color.g;
    fade = Color.a;
}
