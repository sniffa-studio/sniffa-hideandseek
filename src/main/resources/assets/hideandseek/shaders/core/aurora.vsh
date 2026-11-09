#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 bandCoord;
out float bandSeed;
out float along;
out float edgeFade;
out float distanceToEye;

const float SEED_SPACING = 10000.0;
const float SECONDS = 1200.0;

void main() {
    float scale = TextureMat[1][3] > 0.0 ? TextureMat[1][3] : 1.0;
    float base = TextureMat[2][3];
    float time = GameTime * SECONDS;

    float seed = floor(UV0.x / SEED_SPACING);
    float u = UV0.x - seed * SEED_SPACING;
    float v = UV0.y;

    vec2 normal = Color.rg * 2.0 - 1.0;
    float wave = sin(u * 0.035 + time * 0.35 + seed * 1.7) * 5.0
               + sin(u * 0.012 - time * 0.2 + seed * 2.9) * 11.0;
    wave *= 0.6 + 0.4 * v;

    vec2 ground = Position.xz * scale + normal * wave * scale;
    float y = Position.y;
    if (base > 0.0 && y > base) {
        y = base + (y - base) * scale;
    }

    vec3 pos = vec3(ground.x, y, ground.y) + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    bandCoord = vec2(u, v);
    bandSeed = seed;
    along = Color.a;
    edgeFade = Color.b;
    distanceToEye = length(pos);
}
