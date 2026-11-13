#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;

out vec3 fromLight;
out vec3 surfaceNormal;
out float distanceToEye;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    fromLight = Position;
    surfaceNormal = Color.rgb * 2.0 - 1.0;
    distanceToEye = length(pos);
}
