#version 330

in vec2 corner;
in float light;

out vec4 fragColor;

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

void main() {
    vec2 p = vec2(abs(corner.x), corner.y);
    vec2 loop = (p - vec2(0.5, 0.28)) / vec2(0.48, 0.34);
    float loopEdge = length(loop);
    float hole = length((p - vec2(0.55, 0.3)) / vec2(0.2, 0.12));
    float knot = length((corner - vec2(0.0, 0.22)) / vec2(0.17, 0.2));
    float tail = segment(p, vec2(0.06, 0.12), vec2(0.42, -0.88)) / 0.12;
    float tailCut = p.y < -0.88 + (p.x - 0.3) * 0.6 ? 1.0 : 0.0;
    bool inLoop = loopEdge < 1.0 && hole > 1.0;
    bool inKnot = knot < 1.0;
    bool inTail = tail < 1.0 && tailCut < 0.5;
    if (!inLoop && !inKnot && !inTail) discard;
    vec3 red = vec3(0.78, 0.05, 0.08);
    float shade = 1.0;
    if (inKnot) {
        shade = 1.05 - knot * 0.35;
    } else if (inLoop) {
        shade = 0.65 + 0.45 * smoothstep(1.0, 0.4, loopEdge) - 0.25 * smoothstep(1.6, 1.0, hole);
    } else {
        shade = 0.75 + 0.25 * (1.0 - tail);
    }
    vec3 colour = red * shade + vec3(0.25, 0.08, 0.08) * pow(max(1.0 - loopEdge, 0.0), 3.0) * 0.6;
    fragColor = vec4(colour * max(light, 0.6), 1.0);
}
