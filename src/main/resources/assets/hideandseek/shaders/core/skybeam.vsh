#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out float across;
out float rise;
out float along;
out float eye;
out vec3 tint;

void main() {
    vec3 pos = Position + ModelOffset;
    float distance = length(pos);
    float farthest = TextureMat[0].x;
    if (distance > farthest) pos *= farthest / distance;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    across = UV0.x;
    rise = UV0.y;
    along = Color.a;
    eye = distance;
    tint = Color.rgb;
}
