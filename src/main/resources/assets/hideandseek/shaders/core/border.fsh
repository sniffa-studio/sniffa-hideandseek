#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;

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
const float SHRINK_SWEEP = -1.0;

const float BREATH_CYCLES = 600.0;
const float BREATH_STRENGTH = 0.25;

const float NEAR_BLOCKS = 6.0;
const float NEAR_FADE = 56.0;
const float NEAR_BOOST = 0.4;

const float HOT_BLOCKS = 7.0;
const float HOT_FILL = 0.3;
const float HOT_LIGHT = 2.2;

const int RIPPLES = 3;
const float RIPPLE_SECONDS = 1.6;
const float RIPPLE_SPEED = 9.0;
const float RIPPLE_WIDTH = 1.4;
const float RIPPLE_STRENGTH = 2.6;
const float IMPACT_CELLS = 1.6;
const float IMPACT_SECONDS = 0.35;

const float REVEAL_BLOCKS = 60.0;
const float REVEAL_JITTER = 0.25;
const float REVEAL_EDGE = 0.08;
const float REVEAL_TAIL = 0.3;
const float REVEAL_FLASH = 14.0;
const float REVEAL_FLASH_STRENGTH = 1.2;

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

float ripples(vec2 centre, float around) {
    float total = 0.0;

    for (int slot = 0; slot < RIPPLES; slot++) {
        vec3 ripple = TextureMat[slot].xyz;
        if (ripple.z < 0.0 || ripple.z > RIPPLE_SECONDS) {
            continue;
        }

        vec2 delta = centre - ripple.xy;
        delta.x -= around * round(delta.x / around);
        float reach = length(delta);

        float fade = 1.0 - ripple.z / RIPPLE_SECONDS;
        float ring = exp(-pow((reach - ripple.z * RIPPLE_SPEED) / RIPPLE_WIDTH, 2.0)) * fade;
        float impact = exp(-reach / IMPACT_CELLS) * max(0.0, 1.0 - ripple.z / IMPACT_SECONDS);

        total += ring + impact;
    }

    return total * RIPPLE_STRENGTH;
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

    float shrinking = step(TextureMat[0][3], -0.5);
    float pace = mix(1.0, SHRINK_SWEEP, shrinking);

    float sweep = pow(pulse(id.y / SWEEP_CELLS - time * SWEEP_CYCLES * pace + seed * 0.06), SWEEP_SHARPNESS);
    sweep *= SWEEP_STRENGTH * legible;

    float wave = ripples(cell.zw * HEX_STEP, columns * HEX_STEP.x) * legible;
    float hot = TextureMat[3][1] * exp(-distanceToEye / HOT_BLOCKS);

    float cellBlocks = TextureMat[3][3];
    float cellHeight = heightFromViewer - (wallCoord.y - cell.w) * cellBlocks;
    float rise = min(abs(cellHeight) / REVEAL_BLOCKS, 1.0) + seed * REVEAL_JITTER;
    float revealed = TextureMat[3][2] * (1.0 + REVEAL_JITTER + REVEAL_EDGE + REVEAL_TAIL) - rise;
    float shown = clamp(revealed / REVEAL_EDGE, 0.0, 1.0);
    float flash = revealed > 0.0 ? exp(-revealed * REVEAL_FLASH) * REVEAL_FLASH_STRENGTH : 0.0;

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float near = 1.0 + NEAR_BOOST * (1.0 - smoothstep(NEAR_BLOCKS, NEAR_FADE, distanceFromViewer));
    float breath = pulse(time * BREATH_CYCLES) * BREATH_STRENGTH * shrinking;

    float strength = standing * near * shown;

    float lift = sweep + wave + flash + breath;

    float body = (fill + HOT_FILL * hot) * (1.0 + lift) * strength;
    float shine = (bloom + rim) * legible * (1.0 + lift + HOT_LIGHT * hot) * strength;
    float core = line * legible * (1.0 + 0.5 * sweep + wave + flash + HOT_LIGHT * hot) * strength;

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
