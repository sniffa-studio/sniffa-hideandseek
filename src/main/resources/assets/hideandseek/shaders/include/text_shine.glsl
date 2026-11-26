#version 330

const int SHINE_GOLD = 1;
const int SHINE_TEXT = 2;
const int SHINE_GREEN = 3;
const int TITLE_BLUE = 4;
const int TITLE_RED = 5;
const int TITLE_XMAS = 6;
const int TITLE_WINNER = 7;
const int SHADOW_OFFSET = 100;

int shine_kind(vec3 colour) {
    ivec3 c = ivec3(round(colour * 255.0));
    if (c == ivec3(242, 197, 91)) return SHINE_GOLD;
    if (c == ivec3(207, 215, 227)) return SHINE_TEXT;
    if (c == ivec3(79, 208, 140)) return SHINE_GREEN;
    if (c == ivec3(95, 162, 249)) return TITLE_BLUE;
    if (c == ivec3(253, 60, 79)) return TITLE_RED;
    if (c == ivec3(195, 20, 47)) return TITLE_XMAS;
    if (c == ivec3(203, 26, 53)) return TITLE_WINNER;
    if (c == ivec3(23, 40, 62)) return SHADOW_OFFSET + TITLE_BLUE;
    if (c == ivec3(63, 15, 19)) return SHADOW_OFFSET + TITLE_RED;
    if (c == ivec3(48, 5, 11)) return SHADOW_OFFSET + TITLE_XMAS;
    if (c == ivec3(50, 6, 13)) return SHADOW_OFFSET + TITLE_WINNER;
    return 0;
}

float shine_hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float shine_band(vec2 frag, vec2 screen, float seconds, float period, float width) {
    float across = (frag.x + frag.y * 0.5) / screen.y;
    float sweep = fract(seconds / period) / 0.85;
    if (sweep > 1.0) return 0.0;
    float span = screen.x / screen.y + 0.5;
    float centre = mix(-0.3, span + 0.3, sweep);
    float d = (across - centre) / width;
    return exp(-d * d);
}

float shine_twinkle(vec2 texel, float seconds, float chance) {
    float slot = floor(seconds * 2.5);
    float h = shine_hash(texel + vec2(slot * 17.13, slot * 3.71));
    if (h < 1.0 - chance) return 0.0;
    return sin(fract(seconds * 2.5) * 3.14159265);
}

vec4 shine_glow(int kind, float alpha) {
    vec3 glow = vec3(0.0);
    if (kind == TITLE_BLUE) glow = vec3(0.227, 0.482, 1.0);
    if (kind == TITLE_RED) glow = vec3(1.0, 0.157, 0.251);
    if (kind == TITLE_XMAS) glow = vec3(1.0, 0.227, 0.29);
    if (kind == TITLE_WINNER) glow = vec3(0.949, 0.722, 0.29);
    return vec4(glow, alpha * 0.3);
}

vec3 shine_colour(int kind, vec3 base, vec2 frag, vec2 screen, vec2 texel, float seconds) {
    vec3 colour = base;
    float x = frag.x / screen.y;
    float strength = 0.0;
    float width = 0.05;
    float sparkle = 0.0;
    if (kind == SHINE_GOLD) {
        strength = 0.75;
        sparkle = 0.012;
    } else if (kind == SHINE_TEXT || kind == SHINE_GREEN) {
        strength = 0.65;
    } else if (kind == TITLE_BLUE) {
        colour = mix(vec3(0.435, 0.706, 1.0), vec3(0.902, 0.949, 1.0), 0.5 + 0.5 * sin(x * 9.0 - seconds * 1.3));
        strength = 0.6;
        sparkle = 0.01;
    } else if (kind == TITLE_RED) {
        colour = mix(vec3(1.0, 0.231, 0.306), vec3(1.0, 0.776, 0.627), 0.5 + 0.5 * sin(x * 9.0 - seconds * 1.3));
        strength = 0.6;
        sparkle = 0.014;
    } else if (kind == TITLE_XMAS) {
        colour = vec3(0.886, 0.094, 0.18);
        strength = 0.75;
        sparkle = 0.012;
    } else if (kind == TITLE_WINNER) {
        colour = vec3(0.886, 0.094, 0.18);
        strength = 0.8;
        sparkle = 0.03;
    }
    vec3 gleam = vec3(1.0);
    if (kind == TITLE_XMAS) gleam = vec3(1.0, 0.96, 0.88);
    if (kind == TITLE_WINNER) gleam = vec3(1.0, 0.84, 0.42);
    colour = mix(colour, gleam, shine_band(frag, screen, seconds, 5.5, width) * strength);
    if (sparkle > 0.0) {
        colour = mix(colour, vec3(1.0), shine_twinkle(texel, seconds, sparkle) * 0.85);
    }
    return colour;
}
