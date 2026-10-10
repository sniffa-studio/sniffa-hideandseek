#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;

out vec4 fragColor;

const float TAU = 6.2831853;

const float LINE_PIXELS = 2.2;
const float LINE_STRENGTH = 0.95;
const float LINE_CELLS = 0.06;
const float LINE_WHITE = 0.72;

const float GLOW_PIXELS = 14.0;
const float GLOW_STRENGTH = 0.34;
const float GLOW_CELLS = 0.2;

const float RIM_CELLS = 0.07;
const float RIM_STRENGTH = 0.3;

const float FILL = 0.15;
const float FILL_FAR = 0.07;
const float FILL_SPREAD = 0.14;
const float FILL_SHADE = 0.85;

const float SPARK_SHARE = 0.07;
const float SPARK_STRENGTH = 0.24;

const float TWINKLE_CYCLES = 150.0;

const float SWEEP_CYCLES = 200.0;
const float SWEEP_CELLS = 18.0;
const float SWEEP_SHARPNESS = 10.0;
const float SWEEP_STRENGTH = 0.9;

const float NEAR_BLOCKS = 6.0;
const float NEAR_FADE = 56.0;
const float NEAR_BOOST = 0.4;

const float FADE_FROM = 40.0;
const float FADE_TO = 150.0;

const vec2 HEX_STEP = vec2(1.7320508, 1.0);
const vec2 HEX_HALF = vec2(0.8660254, 0.5);

float seamless(float delta) {
    return delta - HEX_STEP.x * round(delta / HEX_STEP.x);
}

vec4 hexCell(vec2 p) {
    vec2 centreA = round(p / HEX_STEP);
    vec2 centreB = round((p - HEX_HALF) / HEX_STEP) + 0.5;

    vec2 offsetA = p - centreA * HEX_STEP;
    vec2 offsetB = p - centreB * HEX_STEP;

    return dot(offsetA, offsetA) < dot(offsetB, offsetB)
        ? vec4(offsetA, centreA)
        : vec4(offsetB, centreB);
}

vec3 hexEdge(vec2 offset) {
    vec2 folded = abs(offset);

    float towardsFlat = folded.y;
    float towardsCorner = dot(folded, HEX_HALF);

    vec2 normal = towardsCorner > towardsFlat ? HEX_HALF : vec2(0.0, 1.0);
    return vec3(0.5 - max(towardsFlat, towardsCorner), normal * sign(offset));
}

float hash(vec2 id) {
    vec3 p = fract(vec3(id.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

void main() {
    vec4 cell = hexCell(wallCoord);
    vec2 offset = cell.xy;

    float columns = max(TextureMat[3][0], 1.0);
    vec2 id = vec2(mod(cell.z, columns), cell.w);

    vec3 edge = hexEdge(offset);
    float toEdge = edge.x;
    vec2 normal = edge.yz;

    vec2 alongX = vec2(seamless(dFdx(wallCoord.x)), dFdx(wallCoord.y));
    vec2 alongY = vec2(seamless(dFdy(wallCoord.x)), dFdy(wallCoord.y));

    float perPixel = length(vec2(dot(alongX, normal), dot(alongY, normal)));
    float pixelsToEdge = toEdge / max(perPixel, 1.0e-6);

    float cellsPerPixel = max(length(alongX), length(alongY));
    float legible = 1.0 - smoothstep(0.1, 0.6, cellsPerPixel);

    float seed = hash(id);
    float spark = hash(id + 71.0);

    float time = GameTime;

    float lineWidth = min(LINE_PIXELS, LINE_CELLS / max(cellsPerPixel, 1.0e-6));
    float line = (1.0 - smoothstep(0.0, lineWidth, pixelsToEdge)) * LINE_STRENGTH;
    float bloom = (1.0 - smoothstep(0.0, GLOW_PIXELS, pixelsToEdge)) * (1.0 - smoothstep(0.0, GLOW_CELLS, toEdge));
    bloom *= GLOW_STRENGTH;
    float rim = exp(-toEdge / RIM_CELLS) * RIM_STRENGTH;

    float twinkle = pulse(time * TWINKLE_CYCLES + seed);
    float lit = step(1.0 - SPARK_SHARE, spark) * pulse(time * TWINKLE_CYCLES * 2.0 + spark * 7.0);
    float fill = mix(FILL_FAR, FILL + FILL_SPREAD * seed * twinkle + SPARK_STRENGTH * lit, legible);

    float sweep = pow(pulse(id.y / SWEEP_CELLS - time * SWEEP_CYCLES + seed * 0.06), SWEEP_SHARPNESS);
    sweep *= SWEEP_STRENGTH * legible;

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float near = 1.0 + NEAR_BOOST * (1.0 - smoothstep(NEAR_BLOCKS, NEAR_FADE, distanceFromViewer));
    float strength = standing * near;

    float body = fill * (1.0 + sweep) * strength;
    float shine = (bloom + rim) * legible * (1.0 + sweep) * strength;
    float core = line * legible * (1.0 + 0.5 * sweep) * strength;

    vec3 tint = ColorModulator.rgb;
    vec3 lineColour = mix(tint, vec3(1.0), LINE_WHITE);

    float total = body + shine + core;
    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 colour = (tint * FILL_SHADE * body + tint * shine + lineColour * core) / total;
    float alpha = clamp(total, 0.0, 1.0) * ColorModulator.a;

    fragColor = vec4(colour, alpha);
}
