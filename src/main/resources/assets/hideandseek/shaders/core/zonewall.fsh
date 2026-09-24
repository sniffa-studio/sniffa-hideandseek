#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in float heightFromViewer;

out vec4 fragColor;

const float BODY = 0.34;

const float EYE_STRENGTH = 0.26;
const float EYE_BLOCKS = 7.0;

const float FADE_FROM = 26.0;
const float FADE_TO = 95.0;

void main() {
    float standing = 1.0 - smoothstep(FADE_FROM, FADE_TO, abs(heightFromViewer));
    float eyeline = exp(-abs(heightFromViewer) / EYE_BLOCKS) * EYE_STRENGTH;

    float alpha = clamp((BODY + eyeline) * standing, 0.0, 1.0) * ColorModulator.a;

    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(ColorModulator.rgb, alpha);
}
