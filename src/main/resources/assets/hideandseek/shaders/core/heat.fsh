#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 texCoord;
flat in mat4 InverseProjection;

out vec4 fragColor;

const float BODY_RADIUS = 2.4;

const float EDGE_BLOTCH = 0.30;
const float EDGE_SCALE = 2.6;

const float INNER_BLOTCH = 0.34;
const float INNER_SCALE = 1.5;

const float DRIFT = 0.35;

const float PULSE_DEPTH = 0.07;
const float PULSE_SPEED = 2.1;

const float INTENSITY = 0.95;

const float CONTOUR_FROM = 0.08;
const float CONTOUR_TO = 0.22;
const float CONTOUR_LIGHT = 0.45;

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

    float seconds = TextureMat[0].y;
    vec3 drift = vec3(seconds * DRIFT);

    float reading = 0.0;

    for (int slot = 1; slot <= 3; slot++) {
        vec4 body = TextureMat[slot];
        if (body.w < 0.5) {
            continue;
        }

        float along = dot(towards, body.xyz);
        if (along <= 0.0) {
            continue;
        }

        vec3 nearest = towards * along;
        vec3 offset = nearest - body.xyz;
        float miss = length(offset);

        vec3 facing = miss > 1.0e-4 ? offset / miss : vec3(0.0, 1.0, 0.0);
        float edge = BODY_RADIUS * (1.0 + EDGE_BLOTCH * wobble(facing * EDGE_SCALE + drift));

        if (miss >= edge) {
            continue;
        }

        float through = sqrt(edge * edge - miss * miss) / edge;

        through *= 1.0 + INNER_BLOTCH * wobble(nearest * INNER_SCALE + drift);

        reading = max(reading, clamp(through, 0.0, 1.0));
    }

    if (reading <= 0.0) {
        discard;
    }

    reading *= 1.0 + PULSE_DEPTH * sin(seconds * PULSE_SPEED);
    reading = clamp(reading, 0.0, 1.0);

    float contour = smoothstep(0.0, CONTOUR_FROM, reading) * (1.0 - smoothstep(CONTOUR_FROM, CONTOUR_TO, reading));
    float scanline = 1.0 - SCANLINE_DEPTH * step(0.5, fract(gl_FragCoord.y / SCANLINE_PIXELS));

    float alpha = max(reading, contour * CONTOUR_LIGHT) * INTENSITY * scanline * ColorModulator.a;

    if (alpha < 0.004) {
        discard;
    }

    vec3 tint = mix(heatColour(reading), vec3(1.0), contour * CONTOUR_LIGHT);
    fragColor = vec4(tint * alpha, alpha);
}
