#version 330

in vec2 shapeCoord;
in vec4 tint;

out vec4 fragColor;

const float RIM_PIXELS = 1.4;
const float RIM_STRENGTH = 0.95;

const float GLOW_PIXELS = 5.0;
const float GLOW_STRENGTH = 0.30;

const float FILL_STRENGTH = 0.30;

const float FILL_FALLOFF = 2.2;

const float TICK_INNER = 1.20;
const float TICK_OUTER = 1.55;
const float TICK_PIXELS = 1.3;
const float TICK_STRENGTH = 0.85;

const float CENTRE_PIXELS = 1.3;
const float CENTRE_STRENGTH = 1.0;

void main() {
    float d = length(shapeCoord);

    float perPixel = length(vec2(dFdx(d), dFdy(d)));
    if (perPixel <= 0.0) {
        discard;
    }
    float radiusPixels = 1.0 / perPixel;

    float fromRim = abs(d - 1.0) * radiusPixels;
    float outside = max(0.0, d - 1.0) * radiusPixels;

    float rim = (1.0 - smoothstep(0.0, RIM_PIXELS, fromRim)) * RIM_STRENGTH;
    float glow = exp(-outside / GLOW_PIXELS) * GLOW_STRENGTH;

    float inside = 1.0 - smoothstep(1.0 - perPixel, 1.0 + perPixel, d);
    float fill = pow(clamp(d, 0.0, 1.0), FILL_FALLOFF) * FILL_STRENGTH * inside;

    float offAxis = min(abs(shapeCoord.x), abs(shapeCoord.y)) * radiusPixels;
    float onAxis = 1.0 - smoothstep(0.0, TICK_PIXELS, offAxis);
    float band = smoothstep(TICK_INNER - 0.06, TICK_INNER, d) * (1.0 - smoothstep(TICK_OUTER, TICK_OUTER + 0.06, d));
    float ticks = onAxis * band * TICK_STRENGTH;

    float centre = (1.0 - smoothstep(0.0, CENTRE_PIXELS, d * radiusPixels)) * CENTRE_STRENGTH;

    float light = max(max(rim, ticks), max(centre, fill + glow));
    float alpha = clamp(light, 0.0, 1.0) * tint.a;

    if (alpha < 0.004) {
        discard;
    }

    vec3 colour = mix(tint.rgb, vec3(1.0), smoothstep(0.5, 1.0, max(rim, centre)) * 0.75);

    fragColor = vec4(colour * alpha, alpha);
}
