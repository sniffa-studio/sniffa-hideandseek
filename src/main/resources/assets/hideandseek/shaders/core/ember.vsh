#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 corner;
out vec3 tint;

void main() {
    vec4 centre = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    float distance = length(centre.xyz);
    centre.xy += UV0 * TextureMat[0].x * (1.0 + distance * 0.03);
    gl_Position = ProjMat * centre;
    corner = UV0;
    tint = Color.rgb;
}
