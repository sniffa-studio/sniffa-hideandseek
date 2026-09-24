#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;

out vec2 plate;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    plate = UV0;
}
