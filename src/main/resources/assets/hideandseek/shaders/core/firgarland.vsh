#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec3 normal;
out vec2 surface;
out float light;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ModelOffset, 1.0);
    normal = Color.rgb * 2.0 - 1.0;
    surface = UV0;
    light = Color.a;
}
