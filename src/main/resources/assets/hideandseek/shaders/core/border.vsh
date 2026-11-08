#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;

out vec2 wallCoord;

out float heightFromViewer;
out float distanceFromViewer;
out float distanceToEye;
out float domeShare;
out vec3 viewPos;
out vec3 sphereNormal;

void main() {
    float scale = TextureMat[1][3] > 0.0 ? TextureMat[1][3] : 1.0;
    float base = TextureMat[2][3];
    float y = Position.y;
    if (base > 0.0 && y > base) {
        y = base + (y - base) * scale;
    }
    domeShare = base > 0.0 ? clamp((Position.y - base) / 8.0, 0.0, 1.0) : 0.0;
    vec3 pos = vec3(Position.x * scale, y, Position.z * scale) + ModelOffset;
    vec3 centre = vec3(ModelOffset.x, (base > 0.0 ? base : y) + ModelOffset.y, ModelOffset.z);
    vec3 outward = pos - centre;
    if (base <= 0.0 || y <= base) {
        outward.y = 0.0;
    }
    sphereNormal = normalize(outward + vec3(0.0, 1.0e-4, 0.0));
    viewPos = pos;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    wallCoord = UV0;
    heightFromViewer = pos.y;
    distanceFromViewer = length(pos.xz);
    distanceToEye = length(pos);
}
