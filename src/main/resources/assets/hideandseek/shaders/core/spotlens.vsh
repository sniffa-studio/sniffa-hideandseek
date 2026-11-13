#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;

out vec2 corner;

void main() {
    vec4 centre = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    centre.xyz += normalize(-centre.xyz) * TextureMat[0].y;
    centre.xy += UV0 * TextureMat[0].x;
    gl_Position = ProjMat * centre;
    corner = UV0;
}
