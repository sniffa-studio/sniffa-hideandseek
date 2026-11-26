#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 corner;
out vec3 tint;
out float phase;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    corner = UV0;
    tint = Color.rgb;
    phase = Color.a;
}
