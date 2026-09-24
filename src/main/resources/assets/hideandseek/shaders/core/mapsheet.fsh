#version 330

in vec2 shapeCoord;
in vec4 tint;

out vec4 fragColor;

const vec3 PAPER_LIT = vec3(0.898, 0.827, 0.671);
const vec3 PAPER_SHADE = vec3(0.706, 0.604, 0.427);
const vec3 PAPER_BURN = vec3(0.482, 0.376, 0.243);
const vec3 INK = vec3(0.196, 0.137, 0.090);

const float GRAIN_SCALE = 260.0;
const float GRAIN_STRENGTH = 0.13;

const float BLOTCH_SCALE = 7.0;
const float BLOTCH_STRENGTH = 0.22;

const float FIBRE_SCALE = 900.0;
const float FIBRE_STRENGTH = 0.05;

const float BURN_PIXELS = 64.0;
const float BURN_STRENGTH = 0.32;

const float RULE_OUTER_PIXELS = 9.0;
const float RULE_INNER_PIXELS = 14.0;
const float RULE_WEIGHT = 1.3;

const float CORNER_PIXELS = 26.0;

float hash(vec2 at) {
    return fract(sin(dot(at, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 at) {
    vec2 cell = floor(at);
    vec2 into = fract(at);
    vec2 eased = into * into * (3.0 - 2.0 * into);

    return mix(
        mix(hash(cell), hash(cell + vec2(1.0, 0.0)), eased.x),
        mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), eased.x),
        eased.y
    );
}

float fbm(vec2 at) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 5; octave++) {
        sum += amplitude * noise(at);
        at *= 2.0;
        amplitude *= 0.5;
    }
    return sum;
}

void main() {
    float perX = length(vec2(dFdx(shapeCoord.x), dFdy(shapeCoord.x)));
    float perY = length(vec2(dFdx(shapeCoord.y), dFdy(shapeCoord.y)));
    if (perX <= 0.0 || perY <= 0.0) {
        discard;
    }

    vec2 pixel = vec2(shapeCoord.x / perX, shapeCoord.y / perY);
    vec2 toEdge = vec2(
        min(shapeCoord.x, 1.0 - shapeCoord.x) / perX,
        min(shapeCoord.y, 1.0 - shapeCoord.y) / perY
    );
    float edge = min(toEdge.x, toEdge.y);

    float blotch = fbm(shapeCoord * BLOTCH_SCALE);
    float grain = noise(pixel / GRAIN_SCALE * 40.0);
    float fibre = noise(vec2(pixel.x * 0.9, pixel.y * FIBRE_SCALE * 0.02));

    vec3 paper = mix(PAPER_SHADE, PAPER_LIT, smoothstep(0.25, 0.80, blotch));
    paper += (grain - 0.5) * GRAIN_STRENGTH;
    paper += (fibre - 0.5) * FIBRE_STRENGTH;

    float burn = (1.0 - smoothstep(0.0, BURN_PIXELS, edge)) * BURN_STRENGTH;
    burn *= 0.65 + 0.35 * fbm(shapeCoord * 11.0 + 4.0);
    paper = mix(paper, PAPER_BURN, clamp(burn, 0.0, 1.0));

    float wobble = (fbm(shapeCoord * 30.0) - 0.5) * 1.6;
    float outerRule = 1.0 - smoothstep(0.0, RULE_WEIGHT, abs(edge - (RULE_OUTER_PIXELS + wobble)));
    float innerRule = 1.0 - smoothstep(0.0, RULE_WEIGHT * 0.8,
                                       abs(edge - (RULE_INNER_PIXELS + wobble)));
    float rule = max(outerRule, innerRule * 0.55);

    float corner = step(toEdge.x, CORNER_PIXELS) * step(toEdge.y, CORNER_PIXELS);
    float diagonal = 1.0 - smoothstep(0.0, RULE_WEIGHT,
                                      abs(toEdge.x - toEdge.y) - RULE_OUTER_PIXELS * 0.25);
    rule = max(rule, corner * diagonal * step(edge, RULE_INNER_PIXELS + 3.0) * 0.8);

    vec3 colour = mix(paper, INK, clamp(rule, 0.0, 1.0) * 0.85);

    float alpha = tint.a;
    fragColor = vec4(colour * alpha, alpha);
}
