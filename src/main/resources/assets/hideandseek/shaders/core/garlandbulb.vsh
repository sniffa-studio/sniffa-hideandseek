#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 corner;
out vec3 tint;
out float strength;

void main() {
    vec4 centre = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    float distance = length(centre.xyz);
    centre.xyz += normalize(-centre.xyz) * TextureMat[0].y;
    float size = TextureMat[0].x * (1.0 + max(distance - 16.0, 0.0) * 0.02);
    centre.xy += UV0 * size;
    gl_Position = ProjMat * centre;

    float seconds = GameTime * 1200.0;
    float phase = Color.a * 6.2831853;
    float twinkle = 0.72 + 0.28 * sin(seconds * (1.3 + Color.a * 1.7) + phase);
    float flare = pow(max(sin(seconds * 0.37 + phase * 3.0), 0.0), 24.0) * 0.6;
    float fade = 1.0 - smoothstep(TextureMat[0].z, TextureMat[0].w, distance);

    corner = UV0;
    tint = Color.rgb;
    strength = (twinkle + flare) * fade * ColorModulator.a;
}
