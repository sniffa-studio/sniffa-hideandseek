#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out float across;
out float coverage;

void main() {
    vec4 point = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    vec3 slope = mat3(ModelViewMat) * (Color.rgb * 2.0 - 1.0);
    vec3 side = cross(slope, -point.xyz);
    float size = length(side);
    side = size > 1.0e-5 ? side / size : vec3(1.0, 0.0, 0.0);
    float width = max(TextureMat[0].x, length(point.xyz) * 0.0008);
    point.xyz += side * UV0.x * width;
    gl_Position = ProjMat * point;
    across = UV0.x;
    coverage = clamp(TextureMat[0].x / width * 1.5, 0.5, 1.0);
}
