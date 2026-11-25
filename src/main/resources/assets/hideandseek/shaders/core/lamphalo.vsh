#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <hideandseek:flicker.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 corner;
out vec3 tint;
out float strength;

void main() {
    vec4 centre = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    float distance = length(centre.xyz);
    float size = abs(UV0.x) * (1.0 + max(distance - 16.0, 0.0) * 0.015);
    centre.xyz += normalize(-centre.xyz) * min(0.3, size * 0.3);
    centre.xy += sign(UV0) * size;
    gl_Position = ProjMat * centre;

    float code = abs(UV0.y);
    float flame = floor(code) - 1.0;
    float phase = fract(code);
    float fade = (1.0 - smoothstep(TextureMat[0].x, TextureMat[0].y, distance)) * smoothstep(0.5, 1.6, distance);

    corner = sign(UV0);
    tint = Color.rgb;
    strength = sniffaFlicker(flame, phase, GameTime * 1200.0) * fade;
}
