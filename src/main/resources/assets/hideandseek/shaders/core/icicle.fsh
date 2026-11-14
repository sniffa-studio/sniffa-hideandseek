#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 worldNormal;
in vec3 viewNormal;
in vec3 viewPosition;
in float along;
in float seed;

out vec4 fragColor;

void main() {
    vec3 n = normalize(worldNormal);
    vec3 v = normalize(-viewPosition);
    vec3 nv = normalize(viewNormal);
    float light = ColorModulator.a;
    float shade = 0.55 + 0.45 * max(dot(n, normalize(vec3(0.35, 0.75, 0.45))), 0.0);
    float rim = pow(1.0 - abs(dot(nv, v)), 2.2);
    float level = 0.22 + 0.78 * light * light;
    vec3 ice = mix(vec3(0.72, 0.86, 1.0), vec3(0.9, 0.97, 1.0), along);
    float seconds = GameTime * 1200.0;
    float glint = pow(max(sin(seconds * (1.5 + seed * 2.5) + seed * 40.0 + along * 6.0), 0.0), 40.0);
    vec3 colour = ice * shade * level + vec3(0.75, 0.9, 1.0) * rim * (0.18 + 0.3 * light) + vec3(glint) * (0.35 + 0.45 * light);
    float alpha = mix(0.82, 0.5, along) + rim * 0.15;
    fragColor = vec4(colour, clamp(alpha, 0.0, 1.0));
}
