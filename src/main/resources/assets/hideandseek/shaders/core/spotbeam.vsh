#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec3 fromEye;
out vec3 shellNormal;
out float along;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    fromEye = pos;
    shellNormal = Color.rgb * 2.0 - 1.0;
    along = UV0.x;
}
