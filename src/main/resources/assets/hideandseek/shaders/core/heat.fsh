#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 texCoord;
flat in mat4 InverseProjection;

out vec4 fragColor;

const float BODY_RADIUS = 0.5;
const float BODY_HALF_LENGTH = 0.48;

const float HAZE_REACH = 0.45;
const float HAZE_LIGHT = 0.16;

const float SWEEP_SECONDS = 1.8;
const float SWEEP_SHARPNESS = 5.0;
const float SWEEP_LIGHT = 0.3;

const float EDGE_BLOTCH = 0.14;
const float EDGE_SCALE = 3.4;

const float INNER_BLOTCH = 0.22;
const float INNER_SCALE = 3.0;

const float DRIFT = 0.35;

const float PULSE_DEPTH = 0.07;
const float PULSE_SPEED = 2.1;

const float INTENSITY = 0.95;

const float CONTOUR_LIGHT = 0.55;

const float SCANLINE_PIXELS = 3.0;
const float SCANLINE_DEPTH = 0.12;

float wobble(vec3 p) {
    float coarse = sin(p.x) * sin(p.y * 1.3 + 1.7) * sin(p.z * 0.7 + 0.4);
    float fine = sin(p.x * 2.1 + p.z * 1.1) * sin(p.y * 1.9 - 0.6);
    return (coarse + 0.5 * fine) / 1.5;
}

vec3 heatColour(float t) {
    vec3 cold = vec3(0.08, 0.20, 0.55);
    vec3 cool = vec3(0.10, 0.70, 0.35);
    vec3 warm = vec3(0.90, 0.85, 0.20);
    vec3 hotter = vec3(1.00, 0.45, 0.08);
    vec3 hottest = vec3(1.00, 0.16, 0.10);
    vec3 glare = vec3(1.00, 0.92, 0.85);

    vec3 shade = mix(cold, cool, smoothstep(0.00, 0.22, t));
    shade = mix(shade, warm, smoothstep(0.22, 0.50, t));
    shade = mix(shade, hotter, smoothstep(0.50, 0.74, t));
    shade = mix(shade, hottest, smoothstep(0.74, 0.92, t));
    shade = mix(shade, glare, smoothstep(0.92, 1.00, t));
    return shade;
}

void main() {

    vec4 clip = vec4(texCoord * 2.0 - 1.0, 0.0, 1.0);
    vec4 onNearPlane = InverseProjection * clip;
    vec3 towards = normalize(onNearPlane.xyz / onNearPlane.w);
    float pixelAngle = max(length(dFdx(towards)), length(dFdy(towards)));

    vec3 up = normalize(mat3(ModelViewMat) * vec3(0.0, 1.0, 0.0));

    float seconds = TextureMat[0].y;
    vec3 drift = vec3(seconds * DRIFT);

    float reading = 0.0;
    float rimPixels = 0.0;
    float haze = 0.0;
    float sweep = 0.0;

    for (int slot = 1; slot <= 3; slot++) {
        vec4 body = TextureMat[slot];
        if (body.w < 0.5) {
            continue;
        }

        vec3 axis = up * BODY_HALF_LENGTH;
        vec3 start = body.xyz - axis;
        vec3 segment = 2.0 * axis;

        float b = dot(towards, segment);
        float d = dot(towards, -start);
        float e = dot(segment, -start);
        float c = dot(segment, segment);
        float denom = c - b * b;
        float t = denom > 1.0e-5 ? clamp((e - b * d) / denom, 0.0, 1.0) : 0.5;
        float along = b * t - d;
        if (along <= 0.0) {
            continue;
        }

        vec3 onAxis = start + segment * t;
        vec3 nearest = towards * along;
        vec3 offset = nearest - onAxis;
        float miss = length(offset);

        vec3 facing = miss > 1.0e-4 ? offset / miss : up;
        float edge = BODY_RADIUS * (1.0 + EDGE_BLOTCH * wobble(facing * EDGE_SCALE + drift));

        haze = max(haze, pow(1.0 - clamp((miss - edge) / HAZE_REACH, 0.0, 1.0), 2.0) * step(edge, miss));

        if (miss >= edge) {
            continue;
        }

        float through = sqrt(edge * edge - miss * miss) / edge;
        through *= 1.0 + INNER_BLOTCH * wobble((nearest - body.xyz) * INNER_SCALE + drift);
        through = clamp(through, 0.0, 1.0);

        if (through > reading) {
            reading = through;
            rimPixels = (edge - miss) / max(along * pixelAngle, 1.0e-6);
            float height = dot(nearest - body.xyz, up) / (BODY_HALF_LENGTH + BODY_RADIUS);
            float sweepAt = sin(seconds * 6.2831853 / SWEEP_SECONDS);
            sweep = exp(-pow((height - sweepAt) * SWEEP_SHARPNESS, 2.0));
        }
    }

    if (reading <= 0.0 && haze <= 0.0) {
        discard;
    }

    reading *= 1.0 + PULSE_DEPTH * sin(seconds * PULSE_SPEED);
    reading = clamp(reading, 0.0, 1.0);

    float inside = clamp(rimPixels, 0.0, 1.0) * step(0.0, reading - 1.0e-4);
    float contour = smoothstep(0.0, 1.0, rimPixels) * (1.0 - smoothstep(1.5, 3.5, rimPixels)) * inside;
    float scanline = 1.0 - SCANLINE_DEPTH * step(mod(floor(gl_FragCoord.y), SCANLINE_PIXELS), 0.5);

    float body = reading * inside;
    float glint = sweep * SWEEP_LIGHT * inside;
    float mist = haze * HAZE_LIGHT * (1.0 - inside);
    float light = body + contour * CONTOUR_LIGHT + glint + mist;

    float alpha = clamp(light, 0.0, 1.0) * INTENSITY * scanline * ColorModulator.a;

    if (alpha < 0.004) {
        discard;
    }

    vec3 tint = (heatColour(reading) * body + vec3(1.0) * (contour * CONTOUR_LIGHT + glint) + heatColour(0.12) * mist)
              / max(light, 1.0e-5);
    fragColor = vec4(tint * alpha, alpha);
}
