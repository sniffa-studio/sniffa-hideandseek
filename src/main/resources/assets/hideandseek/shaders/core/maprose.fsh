#version 330

in vec2 shapeCoord;
in vec4 tint;

out vec4 fragColor;

const float POINT_SHARPNESS = 0.44;

const float MAIN_POINTS = 0.78;
const float CROSS_POINTS = 0.44;

const float RING_AT = 1.0;
const float RING_WEIGHT = 1.2;

const float INNER_RING_AT = 0.30;
const float INNER_RING_WEIGHT = 1.0;

const float HUB = 0.07;

const float EDGE_SOFTNESS = 1.2;

float star(vec2 at, float size) {
    vec2 folded = pow(abs(at / size), vec2(POINT_SHARPNESS));
    return pow(folded.x + folded.y, 1.0 / POINT_SHARPNESS);
}

void main() {
    float perPixel = length(vec2(dFdx(shapeCoord.x), dFdy(shapeCoord.x)));
    if (perPixel <= 0.0) {
        discard;
    }
    float radiusPixels = 1.0 / perPixel;

    vec2 turned = vec2(
        shapeCoord.x * 0.7071068 - shapeCoord.y * 0.7071068,
        shapeCoord.x * 0.7071068 + shapeCoord.y * 0.7071068
    );

    float mainStar = star(shapeCoord, MAIN_POINTS);
    float crossStar = star(turned, CROSS_POINTS);

    float body = min(mainStar, crossStar);
    float filled = 1.0 - smoothstep(1.0 - EDGE_SOFTNESS * perPixel / MAIN_POINTS,
                                    1.0 + EDGE_SOFTNESS * perPixel / MAIN_POINTS, body);

    float d = length(shapeCoord);

    float ring = 1.0 - smoothstep(0.0, RING_WEIGHT, abs(d - RING_AT) * radiusPixels);
    float innerRing = 1.0 - smoothstep(0.0, INNER_RING_WEIGHT,
                                       abs(d - INNER_RING_AT) * radiusPixels);
    float hub = 1.0 - smoothstep(0.0, EDGE_SOFTNESS, (d - HUB) * radiusPixels);

    float north = step(0.0, -shapeCoord.y) * step(abs(shapeCoord.x), -shapeCoord.y);
    float weight = max(0.35, north);

    float light = max(max(filled * weight, hub), max(ring, innerRing * 0.7));
    float alpha = clamp(light, 0.0, 1.0) * tint.a;

    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(tint.rgb * alpha, alpha);
}
