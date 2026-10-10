#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 wallCoord;
in float heightFromViewer;
in float distanceFromViewer;
in float distanceToEye;

out vec4 fragColor;

const float TAU = 6.2831853;
const float PERIOD_CELLS = 1.7320508;

const float BODY = 0.1;
const float EYE_STRENGTH = 0.16;
const float EYE_BLOCKS = 7.0;

const float STRIPE_CELLS = 3.0;
const float STRIPE_SHARE = 0.5;
const float STRIPE_STRENGTH = 0.14;
const float STRIPE_CYCLES = 120.0;

const float EDGE_STRENGTH = 0.55;
const float EDGE_PIXELS = 1.4;
const float EDGE_GLOW_PIXELS = 8.0;
const float EDGE_GLOW_STRENGTH = 0.22;

const float SCAN_CELLS = 0.5;
const float SCAN_STRENGTH = 0.03;
const float SCAN_CYCLES = 900.0;

const float CALM_CYCLES = 600.0;
const float URGENT_CYCLES = 2400.0;
const float URGENT_SECONDS = 10.0;
const float PULSE_STRENGTH = 0.25;

const float WHITE = 0.35;

const float FADE_FROM = 26.0;
const float FADE_TO = 95.0;

const float VISIBLE_FROM = 48.0;
const float VISIBLE_TO = 96.0;

float pulse(float phase) {
    return 0.5 + 0.5 * sin(TAU * phase);
}

float band(float phase, float share, float soft) {
    float into = fract(phase);
    return smoothstep(0.0, soft, into) * (1.0 - smoothstep(share - soft, share, into));
}

void main() {
    float around = max(TextureMat[3][0], 1.0) * PERIOD_CELLS;
    float stripes = max(1.0, floor(around / STRIPE_CELLS + 0.5));
    float stripeCells = around / stripes;

    float phase = (wallCoord.x + wallCoord.y) / stripeCells - GameTime * STRIPE_CYCLES;
    float soft = clamp(fwidth(phase) * 1.5, 0.002, 0.25);
    float stripe = band(phase, STRIPE_SHARE, soft);

    float into = fract(phase);
    float toEdge = min(min(into, 1.0 - into), abs(into - STRIPE_SHARE));
    float edgeDistance = toEdge / max(fwidth(phase), 1.0e-5);
    float edge = (1.0 - smoothstep(0.0, EDGE_PIXELS, edgeDistance)) * EDGE_STRENGTH;
    float edgeGlow = exp(-edgeDistance / EDGE_GLOW_PIXELS) * EDGE_GLOW_STRENGTH;
    float legible = 1.0 - smoothstep(0.05, 0.3, fwidth(phase));

    float scan = band(wallCoord.y / SCAN_CELLS - GameTime * SCAN_CYCLES, 0.15, 0.05) * SCAN_STRENGTH;

    float secondsLeft = TextureMat[0][0];
    float urgent = 1.0 - smoothstep(URGENT_SECONDS * 0.5, URGENT_SECONDS, secondsLeft);
    float beat = pulse(GameTime * mix(CALM_CYCLES, URGENT_CYCLES, urgent));
    float throb = 1.0 + PULSE_STRENGTH * (0.4 + 0.6 * urgent) * beat;

    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float visible = 1.0 - smoothstep(VISIBLE_FROM, VISIBLE_TO, distanceFromViewer);
    float eyeline = exp(-abs(heightFromViewer) / EYE_BLOCKS) * EYE_STRENGTH;

    float body = (BODY + eyeline + STRIPE_STRENGTH * stripe * legible + scan) * throb;
    float shine = (edge + edgeGlow) * legible * throb;

    float strength = standing * visible;
    float total = (body + shine) * strength;

    if (total * ColorModulator.a < 0.004) {
        discard;
    }

    vec3 tint = ColorModulator.rgb;
    vec3 colour = (tint * body + mix(tint, vec3(1.0), WHITE) * shine) / (body + shine);

    fragColor = vec4(colour, clamp(total, 0.0, 1.0) * ColorModulator.a);
}
