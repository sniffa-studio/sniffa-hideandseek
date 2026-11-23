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
    float scale = TextureMat[1][3] > 0.0 ? TextureMat[1][3] : 1.0;
    vec3 pos = vec3(Position.x * scale, Position.y, Position.z * scale) + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    wallCoord = UV0;
    heightFromViewer = pos.y;
    distanceFromViewer = length(pos.xz);
    distanceToEye = length(pos);
}
