#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;

out float elevation;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    elevation = UV0.x;
}
