#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D DepthSampler;

in vec2 texCoord;

flat in mat4 InverseProjection;

out vec4 fragColor;

const int WAVES = 3;

const float ECHO_FALLOFF = 0.38;
const float ECHO_SPREAD = 0.35;
const float ECHO_WIDENING = 0.85;

const float FRONT_EDGE = 0.30;
const float CORE_WIDTH = 0.75;
const float GLOW_WIDTH = 5.4;

const float TAIL_LENGTH = 16.0;

const float GLOW_STRENGTH = 0.36;
const float TAIL_STRENGTH = 0.16;

const float EDGE_BOOST = 2.6;

const float FACING_FLOOR = 0.35;

const float AIR_STRENGTH = 0.34;
const float AIR_MAX_BOOST = 6.0;
const float AIR_SHARPNESS = 2.0;

const float NEAR_CLIP = 0.4;

const float RING_SPACING = 10.0;

const float RING_WIDTH = 0.09;

const float RING_STRENGTH = 0.20;

const float MAX_RING_SOFTEN = 0.5;

const float RANGE_RING_STRENGTH = 0.50;

const float LOCK_FLASH = 2.2;

const float PHOSPHOR = 0.055;

const float SWEPT_SOFTNESS = 1.4;

const float NOISE_DEPTH = 0.12;
const float NOISE_BANDS = 3.0;
const float NOISE_HZ = 9.0;

const float TAU = 6.2831853;

const float FRINGE_AHEAD = 0.18;
const float FRINGE_WIDTH = 0.12;
const float FRINGE_LIGHT = 0.9;

const float TRAIN_GAP = 1.6;
const float TRAIN_SHARPNESS = 10.0;
const float TRAIN_FADE = 5.0;
const float TRAIN_LIGHT = 0.32;

float speckle(float bin) {
    return fract(sin(bin * 12.9898) * 43758.5453);
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;

    vec4 clip = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InverseProjection * clip;
    view /= view.w;

    vec3 centre = ModelOffset;
    float surfaceDistance = distance(view.xyz, centre);

    vec3 acrossX = dFdx(view.xyz);
    vec3 acrossY = dFdy(view.xyz);

    float relief = length(vec2(length(acrossX), length(acrossY))) / max(length(view.xyz), 1.0);
    float edge = smoothstep(0.010, 0.075, relief);

    float perPixel = length(vec2(dFdx(surfaceDistance), dFdy(surfaceDistance)));

    vec3 normal = normalize(cross(acrossX, acrossY));
    vec3 fromPing = normalize(view.xyz - centre);
    float facing = FACING_FLOOR + (1.0 - FACING_FLOOR) * abs(dot(normal, fromPing));

    bool hasSurface = depth < 1.0;

    vec3 towards = normalize(view.xyz);
    float toSurface = hasSurface ? length(view.xyz) : 1.0e9;

    float leading = TextureMat[0].x;
    float spacing = TextureMat[0].y;
    float thickness = TextureMat[0].z;
    float range = TextureMat[0].w;

    float along = dot(towards, centre);
    float fromEye = dot(centre, centre);

    float light = 0.0;
    float crest = 0.0;

    for (int wave = 0; wave < WAVES; wave++) {
        float echo = float(wave);

        float radius = leading - spacing * echo * (1.0 + ECHO_SPREAD * echo);
        float width = thickness * (1.0 + ECHO_WIDENING * echo);
        float loudness = pow(ECHO_FALLOFF, echo);

        if (radius <= 0.0 || radius > range + 4.0 * width) {
            continue;
        }

        float spent = 1.0 - 0.6 * clamp(radius / max(range, 1.0), 0.0, 1.0);
        float strength = loudness * spent * (1.0 - smoothstep(range, range + 4.0 * width, radius));

        if (hasSurface && surfaceDistance <= range) {

            float behind = radius - surfaceDistance;

            float ahead = max(0.0, -behind) / (FRONT_EDGE * width);
            float trail = max(0.0, behind);

            float arrived = exp(-ahead * ahead);

            float core = exp(-(trail * trail) / (CORE_WIDTH * CORE_WIDTH * width * width));
            float halo = exp(-(trail * trail) / (GLOW_WIDTH * GLOW_WIDTH * width * width)) * GLOW_STRENGTH;
            float wake = exp(-trail / (TAIL_LENGTH * width)) * TAIL_STRENGTH;

            float period = TRAIN_GAP * width;
            float train = pow(0.5 + 0.5 * cos(TAU * trail / period), TRAIN_SHARPNESS)
                * exp(-trail / (TRAIN_FADE * width)) * step(0.5 * period, trail)
                * (1.0 - smoothstep(0.25 * period, 0.5 * period, perPixel)) * TRAIN_LIGHT;

            float fringeAt = (behind + FRINGE_AHEAD * width) / (FRINGE_WIDTH * width);
            float fringe = exp(-fringeAt * fringeAt) * (1.0 - smoothstep(0.5 * FRINGE_WIDTH * width, FRINGE_WIDTH * width, perPixel));

            float lit = arrived * ((core + halo) * (1.0 + EDGE_BOOST * edge) + wake + train) + fringe * FRINGE_LIGHT;

            lit *= facing * strength;

            light += lit;
            crest = max(crest, max(arrived * core, fringe) * strength);
        }

        float discriminant = along * along - (fromEye - radius * radius);
        if (discriminant <= 0.0) {
            continue;
        }

        float spread = sqrt(discriminant);
        float closest = sqrt(max(fromEye - along * along, 0.0));
        float silhouette = smoothstep(0.0, 0.5 * width, radius - closest);

        float grazing = min(radius / max(spread, 1.0e-3), AIR_MAX_BOOST) / AIR_MAX_BOOST;
        float glow = AIR_STRENGTH * strength * pow(grazing, AIR_SHARPNESS) * AIR_MAX_BOOST * silhouette;

        float entering = along - spread;
        float leaving = along + spread;

        if (entering > NEAR_CLIP && entering < toSurface) {
            light += glow;
        }
        if (leaving > NEAR_CLIP && leaving < toSurface) {
            light += glow;
        }
    }

    if (hasSurface) {
        float bin = floor(surfaceDistance * NOISE_BANDS) + floor(TextureMat[1].x * NOISE_HZ) * 17.0;
        float grain = NOISE_DEPTH * (1.0 - smoothstep(0.08, 0.2, perPixel));
        light *= mix(1.0 - grain, 1.0 + grain, speckle(bin));
    }

    if (hasSurface && surfaceDistance <= range) {

        float reached = min(leading, range);
        float swept = smoothstep(0.0, SWEPT_SOFTNESS, reached - surfaceDistance);

        light += PHOSPHOR * swept * facing;

        float pitch = mod(surfaceDistance, RING_SPACING);
        float offRing = min(pitch, RING_SPACING - pitch);
        float ringWidth = max(RING_WIDTH, perPixel);
        light += RING_STRENGTH * swept * (RING_WIDTH / ringWidth) * (1.0 - smoothstep(MAX_RING_SOFTEN, 3.0, perPixel))
            * (1.0 - smoothstep(0.0, ringWidth, offRing));

        float lock = smoothstep(range - 2.0, range, leading) * (1.0 - smoothstep(range, range + 8.0, leading));
        float rangeWidth = max(RING_WIDTH * 2.0, perPixel);
        light += RANGE_RING_STRENGTH * swept * (1.0 + LOCK_FLASH * lock) * min(1.0, RING_WIDTH * 4.0 / rangeWidth)
            * (1.0 - smoothstep(0.0, rangeWidth, abs(surfaceDistance - range)));
    }

    float alpha = clamp(light, 0.0, 1.0) * ColorModulator.a;

    if (alpha < 0.004) {
        discard;
    }

    vec3 deep = ColorModulator.rgb * 0.4;
    vec3 hot = mix(ColorModulator.rgb, vec3(1.0), 0.85);

    vec3 tint = mix(deep, ColorModulator.rgb, smoothstep(0.0, 0.30, light));
    tint = mix(tint, hot, smoothstep(0.55, 1.10, crest));

    fragColor = vec4(tint * alpha, alpha);
}
