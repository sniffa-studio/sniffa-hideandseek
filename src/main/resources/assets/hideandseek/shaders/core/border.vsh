#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;

out vec2 wallCoord;

out float heightFromViewer;
out float distanceFromViewer;
out float distanceToEye;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    wallCoord = UV0;
    heightFromViewer = pos.y;
    distanceFromViewer = length(pos.xz);
    distanceToEye = length(pos);
}
