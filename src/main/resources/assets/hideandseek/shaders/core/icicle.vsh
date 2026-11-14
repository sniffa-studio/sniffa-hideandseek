#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec3 worldNormal;
out vec3 viewNormal;
out vec3 viewPosition;
out float along;
out float seed;

void main() {
    vec4 point = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    gl_Position = ProjMat * point;
    worldNormal = Color.rgb * 2.0 - 1.0;
    viewNormal = mat3(ModelViewMat) * worldNormal;
    viewPosition = point.xyz;
    along = UV0.x;
    seed = Color.a;
}
