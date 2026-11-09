#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;
in vec3 viewPos;
in vec3 sphereNormal;

out vec4 fragColor;

const float PERIOD_CELLS = 1.7320508;
const float SECONDS_PER_DAY = 1200.0;

const vec3 ICE = vec3(0.94, 0.97, 1.0);
const vec3 STORM = vec3(0.26, 0.36, 0.52);

const float HAZE = 0.18;
const float HAZE_LOW = 0.12;
const float HAZE_LOW_BLOCKS = 10.0;

const float GRAZE_FLOOR = 0.22;
const float GRAZE_STRENGTH = 0.55;

const float CLOUD_BLOCKS = 7.0;
const float CLOUD_WIND = 3.5;
const float CLOUD_STRENGTH = 0.5;

const float SHADOW_STRENGTH = 0.22;

const float STREAK_NEAR_CELL = 1.4;
const float STREAK_NEAR_WIND = 13.0;
const float STREAK_FAR_CELL = 3.2;
const float STREAK_FAR_WIND = 8.0;
const float STREAK_SHARE = 0.45;
const float STREAK_SLANT = 0.3;
const float STREAK_PIXELS = 1.1;
const float STREAK_STRENGTH = 0.9;

const float FLAKE_CELL = 0.55;
const float FLAKE_WIND = 6.0;
const float FLAKE_FALL = 1.6;
const float FLAKE_SHARE = 0.5;
const float FLAKE_PIXELS = 1.4;
const float FLAKE_STRENGTH = 1.0;

const float NEAR_FROM = 8.0;
const float NEAR_TO = 40.0;
const float NEAR_FLOOR = 0.25;

const float URGENT_SECONDS = 10.0;
const float URGENT_SPEED = 0.8;
const float URGENT_DENSITY = 0.7;
const float WHITEOUT_SECONDS = 5.0;
const float WHITEOUT_STRENGTH = 0.35;

const float FADE_FROM = 12.0;
const float FADE_TO = 45.0;
const float VISIBLE_FROM = 64.0;
const float VISIBLE_TO = 128.0;

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(vec2(mod(i.x, period), i.y));
    float b = hash(vec2(mod(i.x + 1.0, period), i.y));
    float c = hash(vec2(mod(i.x, period), i.y + 1.0));
    float d = hash(vec2(mod(i.x + 1.0, period), i.y + 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float clouds(vec2 coord, float around, float seconds, float speed) {
    float columns = max(1.0, floor(around / CLOUD_BLOCKS + 0.5));
    vec2 p = vec2(coord.x / around * columns - seconds * CLOUD_WIND * speed / CLOUD_BLOCKS,
                  coord.y / CLOUD_BLOCKS + seconds * 0.05);
    float value = 0.0;
    float weight = 0.5;
    float period = columns;
    for (int octave = 0; octave < 4; octave++) {
        value += weight * noise(p, period);
        p = p * 2.0 + vec2(0.0, 3.7);
        period *= 2.0;
        weight *= 0.5;
    }
    return value;
}

float streaks(vec2 coord, float around, float seconds, float cell, float wind, float share, float perPixel) {
    float columns = max(1.0, floor(around / cell + 0.5));
    float rows = cell * 0.45;
    vec2 slanted = vec2(coord.x + coord.y * STREAK_SLANT, coord.y);
    vec2 grid = vec2(slanted.x / around * columns - seconds * wind / cell, slanted.y / rows);
    vec2 id = vec2(mod(floor(grid.x), columns), floor(grid.y));
    vec2 inside = fract(grid);

    float span = 0.35 + 0.4 * hash(id + 5.0);
    float start = hash(id + 3.0) * (1.0 - span);
    float along = smoothstep(start, start + 0.08, inside.x) * (1.0 - smoothstep(start + span - 0.2, start + span, inside.x));
    float fade = mix(0.35, 1.0, (inside.x - start) / max(span, 1.0e-3));

    float height = 0.2 + 0.6 * hash(id + 9.0);
    float across = abs(inside.y - height) * rows / max(perPixel, 1.0e-6);
    float line = 1.0 - smoothstep(0.0, STREAK_PIXELS, across);

    return step(hash(id + 21.0), share) * along * fade * line;
}

float flakes(vec2 coord, float around, float seconds, float speed, float share, float perPixel) {
    float columns = max(1.0, floor(around / FLAKE_CELL + 0.5));
    vec2 grid = vec2(coord.x / around * columns - seconds * FLAKE_WIND * speed / FLAKE_CELL,
                     coord.y / FLAKE_CELL + seconds * FLAKE_FALL * speed / FLAKE_CELL);
    vec2 id = vec2(mod(floor(grid.x), columns), floor(grid.y));
    vec2 at = vec2(hash(id + 3.0), hash(id + 9.0));
    vec2 from = (fract(grid) - at) * FLAKE_CELL;
    float pixels = length(from) / max(perPixel, 1.0e-6);
    return step(hash(id + 21.0), share) * (1.0 - smoothstep(0.0, FLAKE_PIXELS, pixels));
}

void main() {
    float columns = max(TextureMat[3][0], 1.0);
    float around = columns * PERIOD_CELLS;
    float seconds = GameTime * SECONDS_PER_DAY;

    float dx = dFdx(wallCoord.x);
    float dy = dFdy(wallCoord.x);
    dx -= around * round(dx / around);
    dy -= around * round(dy / around);
    float perPixel = max(length(vec2(dx, dFdx(wallCoord.y))), length(vec2(dy, dFdy(wallCoord.y))));
    float legible = 1.0 - smoothstep(0.08, 0.5, perPixel);

    float secondsLeft = TextureMat[0][0];
    float urgent = 1.0 - smoothstep(URGENT_SECONDS * 0.5, URGENT_SECONDS, secondsLeft);
    float whiteout = (1.0 - smoothstep(0.0, WHITEOUT_SECONDS, secondsLeft)) * WHITEOUT_STRENGTH;
    float speed = 1.0 + URGENT_SPEED * urgent;
    float density = 1.0 + URGENT_DENSITY * urgent;

    vec3 toEye = normalize(-viewPos);
    vec3 normal = normalize(vec3(sphereNormal.x, 0.0, sphereNormal.z));
    float facing = abs(dot(toEye, normal));
    float graze = GRAZE_STRENGTH * (1.0 - smoothstep(GRAZE_FLOOR, 0.9, facing));

    float near = mix(NEAR_FLOOR, 1.0, 1.0 - smoothstep(NEAR_FROM, NEAR_TO, distanceToEye));
    float low = exp(-max(heightFromViewer + 2.0, 0.0) / HAZE_LOW_BLOCKS) * HAZE_LOW;

    float cloud = clouds(wallCoord, around, seconds, speed);
    float puffs = smoothstep(0.42, 0.78, cloud) * CLOUD_STRENGTH * density;
    float shade = smoothstep(0.55, 0.25, cloud) * SHADOW_STRENGTH;

    float driven = streaks(wallCoord, around, seconds * speed, STREAK_NEAR_CELL, STREAK_NEAR_WIND, STREAK_SHARE * density, perPixel) * near
                 + streaks(wallCoord + vec2(17.0, 3.0), around, seconds * speed, STREAK_FAR_CELL, STREAK_FAR_WIND, STREAK_SHARE, perPixel) * 0.6;
    driven *= STREAK_STRENGTH * legible;

    float snow = flakes(wallCoord, around, seconds, speed, FLAKE_SHARE * density, perPixel) * FLAKE_STRENGTH * legible * near;

    float body = (HAZE + low + graze) * density + shade;
    float white = puffs + driven + snow + whiteout;

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float visible = 1.0 - smoothstep(VISIBLE_FROM, VISIBLE_TO, distanceFromViewer);
    float strength = standing * visible;
    float total = (body + white) * strength;

    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 storm = mix(STORM, ColorModulator.rgb, 0.35);
    vec3 colour = (storm * body + ICE * white) / max(body + white, 1.0e-5);
    colour = mix(colour, ICE, whiteout);

    fragColor = vec4(colour, clamp(total, 0.0, 0.92) * ColorModulator.a);
}
