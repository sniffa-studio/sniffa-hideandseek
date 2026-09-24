#version 330

#moj_import <minecraft:projection.glsl>

in vec3 Position;

out vec2 texCoord;

flat out mat4 InverseProjection;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    texCoord = Position.xy * 0.5 + 0.5;

    InverseProjection = inverse(ProjMat);
}
